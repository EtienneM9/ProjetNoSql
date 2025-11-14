package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.dictionnary.RDFDictionnary;

import java.util.*;

/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */
public class RDFHexaStore implements RDFStorage {

    Map<Integer, Map<Integer, List<Integer>>> SPO;
    Map<Integer, Map<Integer, List<Integer>>> SOP;
    Map<Integer, Map<Integer, List<Integer>>> POS;
    Map<Integer, Map<Integer, List<Integer>>> PSO;
    Map<Integer, Map<Integer, List<Integer>>> OPS;
    Map<Integer, Map<Integer, List<Integer>>> OSP;

    private final RDFDictionnary dict = RDFDictionnary.getInstance();
    private TermFactory termFactory;


    @Override
    public boolean add(RDFTriple triple) {
        termFactory = SameObjectTermFactory.instance();

        int s = dict.encode(termFactory.createOrGetLiteral(triple.getTripleSubject().toString()));
        int o = dict.encode(termFactory.createOrGetLiteral(triple.getTripleObject().toString()));
        int p = dict.encode(termFactory.createOrGetLiteral(triple.getTriplePredicate().toString()));

        // Ajout dans les 6 index
        addToIndex(SPO, s, p, o);
        addToIndex(SOP, s, o, p);
        addToIndex(PSO, p, s, o);
        addToIndex(POS, p, o, s);
        addToIndex(OSP, o, s, p);
        addToIndex(OPS, o, p, s);

        return true;

    }

    // Fonction d'ajout annexe
    private void addToIndex(Map<Integer, Map<Integer, List<Integer>>> index, int first, int second, int third) {
        index.computeIfAbsent(first, k -> new HashMap<>())
                .computeIfAbsent(second, k -> new ArrayList<>())
                .add(third);
    }

    @Override
    public long size() {
        return SPO.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToLong(List::size)
                .sum();
    }

    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
        TermFactory termFactory = SameObjectTermFactory.instance();

        List<Substitution> substitutions = new ArrayList<>();

        Term s = triple.getTripleSubject();
        Term o = triple.getTripleObject();
        Term p = triple.getTriplePredicate();

        int sId = s.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTripleSubject().toString()));
        int oId = s.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTripleObject().toString()));
        int pId = s.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTriplePredicate().toString()));

        boolean sIsBound = sId == -1;
        boolean oIsBound = oId == -1;
        boolean pIsBound = pId == -1;

        //séection de l'index selon les SOP connus
        //----------- CAS 1 avec trois liés -----------------
        if(sIsBound && oIsBound && pIsBound){
            if(SPO.containsKey(sId) && SPO.get(sId).containsKey(pId) && SPO.get(sId).get(pId).contains(sId)){
                //Ici le triplet exacteexiste mais pas de substitution à créer car pasde variable.
                //On renvoie alors une substituion vide
                substitutions.add(new SubstitutionImpl());
            }
        }

        //------ CAS 2 avec deux liés -------------------
        else if(sIsBound && pIsBound){
            if(SPO.containsKey(sId) && SPO.get(sId).containsKey(pId)){
                //Ici, on connait S et P, mais il faut trouver les différentes substituions pour o
                findMatchesAndSubstitute(substitutions, s, p, o, SPO.get(sId).get(pId), null, null, true);
            }
        }
        else if(sIsBound && oIsBound){
            if(SOP.containsKey(sId) && SOP.get(sId).containsKey(oId)){
                //Ici, on connait S et O, mais il faut trouver les différentes substituions pour o
                findMatchesAndSubstitute(substitutions, s, p, o, SPO.get(sId).get(oId), null, true, null);
            }
        }
        else if(oIsBound && pIsBound){
            if(OPS.containsKey(sId) && OPS.get(sId).containsKey(oId)){
                //Ici, on connait S et P, mais il faut trouver les différentes substituions pour o
                findMatchesAndSubstitute(substitutions, s, p, o, SPO.get(sId).get(oId), true, null, null);
            }
        }

        //------ CAS 3 avec un lié -------------------
        else if(sIsBound){
            if (SPO.containsKey(sId)) {
                for (var pEntry : SPO.get(sId).entrySet()) {
                    Term predMatch = dict.decode(pEntry.getKey());
                    for (int objId : pEntry.getValue()) {
                        Term objMatch = dict.decode(objId);
                        createAndAddSubstitution(substitutions, s, p, o, s, predMatch, objMatch);
                    }
                }
            }
        }
        else if(pIsBound) {
            if (PSO.containsKey(pId)) {
                for (var sEntry : PSO.get(pId).entrySet()) {
                    Term predMatch = dict.decode(sEntry.getKey());
                    for (int objId : sEntry.getValue()) {
                        Term objMatch = dict.decode(objId);
                        createAndAddSubstitution(substitutions, s, p, o, predMatch, p, objMatch);
                    }
                }
            }
        }
        else if(oIsBound){
            if (OPS.containsKey(pId)) {
                for (var oEntry : OPS.get(oId).entrySet()) {
                    Term predMatch = dict.decode(oEntry.getKey());
                    for (int objId : oEntry.getValue()) {
                        Term objMatch = dict.decode(objId);
                        createAndAddSubstitution(substitutions, s, p, o, predMatch, objMatch, o);
                    }
                }
            }
        }

        //------------- CAS 4 avec aucun lié -------------------

        else{
            //On sait un scan complet pour trouver les substitutions adapté
            for (var sEntry : SPO.entrySet()) {
                Term subjMatch = dict.decode(sEntry.getKey());
                for (var pEntry : sEntry.getValue().entrySet()) {
                    Term predMatch = dict.decode(pEntry.getKey());
                    for (int objId : pEntry.getValue()) {
                        Term objMatch = dict.decode(objId);
                        createAndAddSubstitution(substitutions, s, p, o, subjMatch, predMatch, objMatch);
                    }
                }
            }
        }

        throw new NotImplementedException();
    }

    /**
     * Fonction d'aide pour le Cas 2 : Trouve les correspondances dans un index de troisième niveau (liste)
     * et crée les substitutions.
     * @param substitutions La liste à remplir.
     * @param qs, qp, qo Les termes de la requête (y compris les variables).
     * @param idsToDecode La liste des IDs à décoder (correspondant au terme non lié).
     * @param isSubject La position non liée est-elle le Sujet ? (boolean peut être null)
     * @param isPredicate La position non liée est-elle le Prédicat ?
     * @param isObject La position non liée est-elle l'Objet ?
     */
    private void findMatchesAndSubstitute(List<Substitution> substitutions, Term qs, Term qp, Term qo, List<Integer> idsToDecode, Boolean isSubject, Boolean isPredicate, Boolean isObject) {}

    /**
     * Fonction d'aide pour créer et ajouter une Substitution à la liste.
     * @param substitutions La liste de substitutions à remplir.
     * @param qs, qp, qo Les termes de la requête (potentiellement des variables).
     * @param sMatch, pMatch, oMatch Les termes du triplet trouvé (Literal/Constant).
     */
    private void createAndAddSubstitution(List<Substitution> substitutions, Term qs, Term qp, Term qo, Term sMatch, Term pMatch, Term oMatch) {}

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        //TODO
        throw new NotImplementedException();
    }

    @Override
    public long howMany(RDFTriple triple) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        List<RDFTriple> triples = new ArrayList<>();

        for (var sEntry : SPO.entrySet()) {
            int s = sEntry.getKey();
            for (var pEntry : sEntry.getValue().entrySet()) {
                int p = pEntry.getKey();
                for (int o : pEntry.getValue()) {
                    Term subj = dict.decode(s);
                    Term pred = dict.decode(p);
                    Term obj = dict.decode(o);

                    triples.add(new RDFTriple(subj, pred, obj));
                }
            }
        }
        return triples;
    }
}
