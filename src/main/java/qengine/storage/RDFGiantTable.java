package qengine.storage;

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
    public Iterator<Substitution> match(RDFTriple a) {
        List<Substitution> results = new ArrayList<>();
        for (RDFTriple triple : triples) {
            Term[] targets = triple.getTerms();
            Term[] myTerms = a.getTerms();
            boolean flag = false;
            ArrayList<String> substTargets = new ArrayList<>();
            for (int i = 0; i<3; i++){
                Substitution
                Term thisTerm = myTerms[i];
                switch (thisTerm.getClass().getSimpleName()) {
                    case "Literal":
                        if (!(targets[i] ==thisTerm)){
                            flag = true;
                            break;
                        }
                    case "Variable":

                    default:
                        break;
                }
            }
            if (flag) break;
        }
        return results.iterator();
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
