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

    public Map<Integer, Map<Integer, List<Integer>>> SPO = new HashMap<>();
    public Map<Integer, Map<Integer, List<Integer>>> SOP = new HashMap<>();
    public Map<Integer, Map<Integer, List<Integer>>> POS = new HashMap<>();
    public Map<Integer, Map<Integer, List<Integer>>> PSO = new HashMap<>();
    public Map<Integer, Map<Integer, List<Integer>>> OPS = new HashMap<>();
    public Map<Integer, Map<Integer, List<Integer>>> OSP = new HashMap<>();

    private final RDFDictionnary dict = RDFDictionnary.getInstance();
    private long size = 0;
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

        this.size++;

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
        return this.size;
    }

    @Override
    public Iterator<Substitution> match(RDFTriple triple) {
        TermFactory termFactory = SameObjectTermFactory.instance();

        List<Substitution> substitutions = new ArrayList<>();

        Term s = triple.getTripleSubject();
        Term o = triple.getTripleObject();
        Term p = triple.getTriplePredicate();

        int sId = s.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTripleSubject().toString()));
        int oId = o.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTripleObject().toString()));
        int pId = p.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTriplePredicate().toString()));

        boolean sIsBound = sId != -1;
        boolean oIsBound = oId != -1;
        boolean pIsBound = pId != -1;

        //séection de l'index selon les SOP connus
        //----------- CAS 1 avec trois liés -----------------
        if(sIsBound && oIsBound && pIsBound){
            System.out.println(sId + " " + oId + " " + pId);
            if(SPO.containsKey(sId) && SPO.get(sId).containsKey(pId) && SPO.get(sId).get(pId).contains(oId)){
                //Ici le triplet exact existe mais pas de substitution à créer car pasde variable.
                //On renvoie alors une substituion vide
                substitutions.add(new SubstitutionImpl());
            }
        }

        //------ CAS 2 avec deux liés -------------------
        else if(sIsBound && pIsBound){
            if(SPO.containsKey(sId) && SPO.get(sId).containsKey(pId)){
                System.out.println(sId + " " + pId + " " + sIsBound);
                //Ici, on connait S et P, mais il faut trouver les différentes substituions pour o
                findMatchesAndSubstitute(substitutions, s, p, o, SPO.get(sId).get(pId), null, null, true);
            }
        }
        else if(sIsBound && oIsBound){
            if(SOP.containsKey(sId) && SOP.get(sId).containsKey(oId)){
                //Ici, on connait S et O, mais il faut trouver les différentes substituions pour o
                findMatchesAndSubstitute(substitutions, s, p, o, SOP.get(sId).get(oId), null, true, null);
            }
        }
        else if(oIsBound && pIsBound){
            if(OPS.containsKey(oId) && OPS.get(oId).containsKey(pId)){
                //Ici, on connait S et P, mais il faut trouver les différentes substituions pour o
                findMatchesAndSubstitute(substitutions, s, p, o, OPS.get(oId).get(pId), true, null, null);
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
            if (OPS.containsKey(oId)) {
                for (var oEntry : OPS.get(oId).entrySet()) {
                    Term predMatch = dict.decode(oEntry.getKey());
                    for (int objId : oEntry.getValue()) {
                        Term objMatch = dict.decode(objId);
                        createAndAddSubstitution(substitutions, s, p, o, objMatch, predMatch, o);
                    }
                }
            }
        }

        //------------- CAS 4 avec aucun lié -------------------

        else{
            //On fait un scan complet pour trouver les substitutions adapté
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

        return substitutions.iterator();
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
    private void findMatchesAndSubstitute(List<Substitution> substitutions, Term qs, Term qp, Term qo, List<Integer> idsToDecode, Boolean isSubject, Boolean isPredicate, Boolean isObject) {
        // Pour le Cas 2, seule l'une des trois positions est non-liée.
        Term bound1 = isSubject != null ? qs : (isPredicate != null ? qp : qo);
        Term bound2 = isPredicate != null ? qp : (isObject != null ? qo : qs); // La troisième position restante

        // Déterminer les termes liés pour la fonction d'aide
        Term sMatch = isSubject != null ? null : qs;
        Term pMatch = isPredicate != null ? null : qp;
        Term oMatch = isObject != null ? null : qo;

        for (int id : idsToDecode) {
            Term unboundMatch = dict.decode(id);

            // Déterminer quelle position reçoit le terme décodé
            Term current_s = isSubject != null ? unboundMatch : sMatch;
            Term current_p = isPredicate != null ? unboundMatch : pMatch;
            Term current_o = isObject != null ? unboundMatch : oMatch;

            createAndAddSubstitution(substitutions, qs, qp, qo, current_s, current_p, current_o);
        }
    }

    /**
     * Fonction d'aide pour créer et ajouter une Substitution à la liste.
     * @param substitutions La liste de substitutions à remplir.
     * @param qs, qp, qo Les termes de la requête (potentiellement des variables).
     * @param sMatch, pMatch, oMatch Les termes du triplet trouvé (Literal/Constant).
     */
    private void createAndAddSubstitution(List<Substitution> substitutions, Term qs, Term qp, Term qo, Term sMatch, Term pMatch, Term oMatch) {
        Substitution sub = new SubstitutionImpl();

        // Vérifier et lier S
        if (qs.isVariable()) {
            sub.add((Variable) qs, sMatch);
        }

        // Vérifier et lier P
        if (qp.isVariable()) {
            // Note: Dans le cas général RDF, le prédicat est toujours un URI et non une variable,
            // mais l'interface Term permet isVariable(), donc on supporte la substitution.
            sub.add((Variable) qp, pMatch);
        }

        // Vérifier et lier O
        if (qo.isVariable()) {
            sub.add((Variable) qo, oMatch);
        }

        // La substitution est complète et prête à être ajoutée
        substitutions.add(sub);
    }

    @Override
    public long howMany(RDFTriple triple) {
        TermFactory termFactory = SameObjectTermFactory.instance();

        Term s = triple.getTripleSubject();
        Term o = triple.getTripleObject();
        Term p = triple.getTriplePredicate();

        int sId = s.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTripleSubject().toString()));
        int oId = o.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTripleObject().toString()));
        int pId = p.isVariable() ? -1 : dict.encode(termFactory.createOrGetLiteral(triple.getTriplePredicate().toString()));

        boolean sIsBound = sId != -1;
        boolean oIsBound = oId != -1;
        boolean pIsBound = pId != -1;

        //séection de l'index selon les SOP connus
        //----------- CAS 1 avec trois liés -----------------
        if(sIsBound && oIsBound && pIsBound){
            System.out.println(sId + " " + oId + " " + pId);
            if(SPO.containsKey(sId) && SPO.get(sId).containsKey(pId) && SPO.get(sId).get(pId).contains(oId)){
                return 1;
            }
            return 0;
        }

        //------ CAS 2 avec deux liés -------------------
        else if(sIsBound && pIsBound){
            if(SPO.containsKey(sId) && SPO.get(sId).containsKey(pId)){
                return SPO.get(sId).get(pId).size();
            }
            return 0;
        }
        else if(sIsBound && oIsBound){
            if(SOP.containsKey(sId) && SOP.get(sId).containsKey(oId)){
                return SOP.get(sId).get(oId).size();
            }
            return 0;
        }
        else if(oIsBound && pIsBound){
            if(OPS.containsKey(oId) && OPS.get(oId).containsKey(pId)){
                return OPS.get(oId).get(pId).size();
            }
            return 0;
        }

        //------ CAS 3 avec un lié -------------------
        else if(sIsBound){
            if (SPO.containsKey(sId)) {
                long c = 0;
                for (List<Integer> obj : SPO.get(sId).values()) {
                    c += obj.size();
                }
                return c;
            }
            return 0;
        }
        else if(pIsBound) {
            if (PSO.containsKey(pId)) {
                long c = 0;
                for (List<Integer> obj : PSO.get(pId).values()) {
                    c += obj.size();
                }
                return c;
            }
            return 0;
        }
        else if(oIsBound){
            if (OPS.containsKey(oId)) {
                long c = 0;
                for (List<Integer> obj : OPS.get(oId).values()) {
                    c += obj.size();
                }
                return c;
            }
            return 0;

        }

        //------------- CAS 4 avec aucun lié -------------------

        else{
            return this.size();
        }
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
