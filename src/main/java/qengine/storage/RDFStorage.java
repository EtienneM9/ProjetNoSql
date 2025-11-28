package qengine.storage;

import java.util.*;
import java.util.stream.Stream;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

/**
 * Contrat pour un système de stockage de données RDF
 */
public interface RDFStorage {

    /**
     * Ajoute un RDFAtom dans le store.
     *
     * @param t le triplet à ajouter
     * @return true si le RDFAtom a été ajouté avec succès, false s'il est déjà présent
     */
    boolean add(RDFTriple t);

    /**
     * @param a atom
     * @return un itérateur de substitutions correspondant aux match des atomes
     *          (i.e., sur quels termes s'envoient les variables)
     */
    Iterator<Substitution> match(RDFTriple a);


    /**
     * @param q star query
     * @return an itérateur de subsitutions décrivrant les réponses à la requete
     */
    default Iterator<Substitution> match(StarQuery q) {
        List<RDFTriple> patterns = new ArrayList<>(q.getRdfAtoms());

        //On trie les motifs par sélectivité
        patterns.sort((t1, t2) -> Long.compare(this.howMany(t1), this.howMany(t2)));

        if (patterns.isEmpty()) {
            return Collections.emptyIterator();
        }

        //Motif le plus sélectif
        RDFTriple firstPattern = patterns.get(0);
        Iterator<Substitution> candidates = this.match(firstPattern);

        // Si un seul motif, on a fini
        if (patterns.size() == 1) {
            return candidates;
        }

        // Les autres motifs à vérifier
        List<RDFTriple> remainingPatterns = patterns.subList(1, patterns.size());
        Variable centralVar = q.getCentralVariable();

        // Substitutions candidates
        List<Substitution> results = new ArrayList<>();

        while (candidates.hasNext()) {
            Substitution sub = candidates.next();
            Term centralValue = sub.toMap().get(centralVar);

            // Vérifier si cette valeur permet de satisfaire TOUS les autres motifs
            boolean matchesAll = true;
            for (RDFTriple pattern : remainingPatterns) {

                RDFTriple instantiatedTriple = substitute(pattern, centralVar, centralValue);

                // Si le store ne contient pas ce triplet précis, ce candidat est invalide
                if (this.howMany(instantiatedTriple) == 0) {
                    matchesAll = false;
                    break;
                }
            }

            if (matchesAll) {
                results.add(sub);
            }
        }

        return results.iterator();
    }

    private RDFTriple substitute(RDFTriple t, Variable var, Term value) {
        Term s = t.getTripleSubject().equals(var) ? value : t.getTripleSubject();
        Term p = t.getTriplePredicate().equals(var) ? value : t.getTriplePredicate();
        Term o = t.getTripleObject().equals(var) ? value : t.getTripleObject();
        return new RDFTriple(s, p, o);
    }

    /**
     * @param a atom
     * @return
     */
    default long howMany(RDFTriple a){
        Iterator<Substitution> matchedAtoms = this.match(a);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        return matchedList.size();
    };


    /**
     * Retourne le nombre d'atomes dans le Store.
     *
     * @return le nombre d'atomes
     */
    long size();

    /**
     * Retourne une collections contenant tous les atomes du store.
     * Utile pour les tests unitaires.
     *
     * @return une collection d'atomes
     */
    Collection<RDFTriple> getAtoms();

    /**
     * Ajoute des RDFAtom dans le store.
     *
     * @param atoms les RDFAtom à ajouter
     * @return true si au moins un RDFAtom a été ajouté, false s'ils sont tous déjà présents
     */
    default boolean addAll(Stream<RDFTriple> atoms) {
        return atoms.map(this::add).reduce(Boolean::logicalOr).orElse(false);
    }

    /**
     * Ajoute des RDFAtom dans le store.
     *
     * @param atoms les RDFAtom à ajouter
     * @return true si au moins un RDFAtom a été ajouté, false s'ils sont tous déjà présents
     */
    default boolean addAll(Collection<RDFTriple> atoms) {
        return this.addAll(atoms.stream());
    }
}
