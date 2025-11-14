package qengine.storage;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
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

    /*public Substitution matchTriple(RDFTriple pattern, RDFTriple data){
        Substitution substitution = new SubstitutionImpl();

        Term[] patternTerms = pattern.getTerms();
        Term[] dataTerms = data.getTerms();

        for (int i = 0; i < patternTerms.length; i++) {
            Term p = patternTerms[i];
            Term d = dataTerms[i];

            if (p instanceof Variable) {
                substitution.put((Variable) p, d);
            } else if (!p.equals(d)) {
                // constante différente => pas de match
                return null;
            }
        }

        return substitution;
    }*/

    @Override
    public Iterator<Substitution> match(RDFTriple pattern) {
        List<Substitution> results = new ArrayList<>();

        for (RDFTriple triple : triples) {
            Term[] dataTerms = triple.getTerms();
            Term[] patternTerms = pattern.getTerms();
            Substitution sub = new SubstitutionImpl();

            boolean matches = true;
            for (int i = 0; i < 3; i++) {
                Term p = patternTerms[i];
                Term d = dataTerms[i];

                if (p instanceof Variable) {
                    sub.add((Variable) p, d);
                } else if (!p.equals(d)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                results.add(sub);
            }
        }

        return results.iterator();
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        return null;
    }

    @Override
    public long howMany(RDFTriple a) {
        Iterator<Substitution> it = match(a);
        int res = 0;
        while (it.hasNext()) {
            it.next();
        }
        return res;
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
