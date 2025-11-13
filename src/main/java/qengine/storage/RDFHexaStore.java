package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
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
        throw new NotImplementedException();
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
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
