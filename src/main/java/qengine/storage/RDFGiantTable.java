package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class RDFGiantTable implements RDFStorage {

    private List<RDFTriple> triples = new ArrayList<>();

    @Override
    public boolean add(RDFTriple t) {
        return triples.add(t);
    }



    @Override
    public Iterator<Substitution> match(RDFTriple a) {
        List<Substitution> results = new ArrayList<>();
        for (RDFTriple triple : triples) {
            //Substitution substitution =
        }
        return null;
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        return null;
    }

    @Override
    public long howMany(RDFTriple a) {
        Term subject = a.getTripleSubject();
        Term predicate = a.getTriplePredicate();
        Term object =  a.getTripleObject();
        triples.forEach(triple -> {
            //TODO
        });
        return 0;
    }

    @Override
    public long size() {
        return triples.size();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return triples;
    }
}
