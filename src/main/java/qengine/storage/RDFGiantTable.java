package qengine.storage;

import fr.boreal.model.logicalElements.api.Substitution;
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
        return null;
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        return null;
    }

    @Override
    public long howMany(RDFTriple a) {
        return 0;
    }

    @Override
    public long size() {
        return triples.size();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return List.of();
    }
}
