package qengine.storage;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.dictionnary.RDFDictionnary;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

public class RDFGiantTable implements RDFStorage {

    private List<List<Integer>> triples = new ArrayList<>();
    private RDFDictionnary dictionnary = RDFDictionnary.getInstance();

    @Override
    public boolean add(RDFTriple t) {
        //TO TEST
        Term[] terms = t.getTerms();
        List<Integer> newTriple = new ArrayList<>();
        for (Term term : terms) {
            newTriple.add(dictionnary.encode(term));
        }
        return triples.add(newTriple);
    }

    @Override
    public Iterator<Substitution> match(RDFTriple pattern) {
        List<Substitution> results = new ArrayList<>();
        Term[] terms = pattern.getTerms();

        List<Integer> encodedPattern = new ArrayList<>();
        for (Term term : pattern.getTerms()) {
            encodedPattern.add(dictionnary.encode(term));
        }

        for (List<Integer> triple : triples) {
            Substitution sub = new SubstitutionImpl();

            boolean matches = true;
            for (int i = 0; i < 3; i++) {
                int p = encodedPattern.get(i);
                int d = triple.get(i);

                if (p == -1) {
                    Variable var = (Variable) terms[i];
                    sub.add(var, dictionnary.decode(d));
                } else if (p != d) {
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
    public long howMany(RDFTriple a) {
        Iterator<Substitution> it = match(a);
        int res = 0;
        while (it.hasNext()) {
            res++;
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
        List<RDFTriple> result = new ArrayList<>();
        for (List<Integer> triple : triples) {
            result.add(new RDFTriple(dictionnary.decode(triple.get(0)),
                    dictionnary.decode(triple.get(1)),
                    dictionnary.decode(triple.get(2))));
        }
        return result;
    }
}
