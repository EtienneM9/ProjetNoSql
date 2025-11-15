package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link RDFHexaStore}.
 */
public class RDFHexaStoreTest {
    private static final Literal<String> SUBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> PREDICATE_2 = SameObjectTermFactory.instance().createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3 = SameObjectTermFactory.instance().createOrGetLiteral("object3");
    private static final Variable VAR_X = SameObjectTermFactory.instance().createOrGetVariable("?x");
    private static final Variable VAR_Y = SameObjectTermFactory.instance().createOrGetVariable("?y");
    private static final Variable VAR_Z = SameObjectTermFactory.instance().createOrGetVariable("?z");



    @Test
    public void testAddAllRDFAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        // Version stream
        // Ajouter plusieurs RDFAtom
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple rdfAtom2 = new RDFTriple(SUBJECT_2, PREDICATE_2, OBJECT_2);

        Set<RDFTriple> rdfAtoms = Set.of(rdfAtom1, rdfAtom2);

        assertTrue(store.addAll(rdfAtoms.stream()), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        Collection<RDFTriple> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");

        // Version collection
        store = new RDFHexaStore();
        assertTrue(store.addAll(rdfAtoms), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");
    }

    @Test
    public void testAddRDFAtom() {

        RDFHexaStore store = new RDFHexaStore();
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);

        assertTrue(store.add(rdfAtom1), "Les RDFAtoms devraient être ajoutés avec succès.");

        Collection<RDFTriple> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");

    }

//    @Test
//    public void testAddDuplicateAtom() {
//        throw new NotImplementedException();
//    }

    @Test
    public void testGetAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        RDFTriple atom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple atom2 = new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2);
        RDFTriple atom3 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3);

        store.add(atom1);
        store.add(atom2);
        store.add(atom3);

        Collection<RDFTriple> la = store.getAtoms();
        assertTrue(la.contains(atom1), "The store should conatins the atom1");
        assertTrue(la.contains(atom2), "The store should conatins the atom2");
        assertTrue(la.contains(atom3), "The store should conatins the atom3");

    }

    @Test
    public void howMany() {
        RDFHexaStore store = new RDFHexaStore();
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1)); // RDFAtom(subject1, triple, object1)
        store.add(new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2)); // RDFAtom(subject2, triple, object2)
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3)); // RDFAtom(subject1, triple, object3)

        // Case 1
        RDFTriple matchingAtom = new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X); // RDFAtom(subject1, predicate1, X)
        long nbr = store.howMany(matchingAtom);

        assertEquals(2, nbr, "There should be two matched RDFAtoms");
    }


    @Test
    public void testMatchAtom() {
            RDFHexaStore store = new RDFHexaStore();

            store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1));
            store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3));
            store.add(new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2));

    /* ---------------------------------------------------------
       CAS 1 : 3 TERMES LIES — exact / non exact
       --------------------------------------------------------- */
            RDFTriple exact = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
            List<Substitution> exactRes = toList(store.match(exact));

            assertEquals(1, exactRes.size(), "Cas 1A (3 liés exact) : devrait retourner 1 résultat.");
            assertTrue(exactRes.get(0).isEmpty(), "Cas 1A (3 liés exact) : substitution devrait être vide.");

            RDFTriple noExact = new RDFTriple(SUBJECT_1, PREDICATE_2, OBJECT_1);
            assertEquals(0, toList(store.match(noExact)).size(),
                    "Cas 1B (3 liés non exact) : ne doit retourner aucun résultat.");


    /* ---------------------------------------------------------
       CAS 2 : 2 TERMES LIES
       --------------------------------------------------------- */

            // S,P liés, O variable
            RDFTriple spVar = new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X);
            List<Substitution> spRes = toList(store.match(spVar));

            assertEquals(2, spRes.size(), "Cas 2A (S,P fixés) : devrait retourner 2 résultats.");
            assertTrue(spRes.contains(sub(VAR_X, OBJECT_1)),
                    "Cas 2A (S,P fixés) : substitution X → object1 manquante.");
            assertTrue(spRes.contains(sub(VAR_X, OBJECT_3)),
                    "Cas 2A (S,P fixés) : substitution X → object3 manquante.");

            // S,O liés, P variable
            RDFTriple soVar = new RDFTriple(SUBJECT_1, VAR_X, OBJECT_1);
            List<Substitution> soRes = toList(store.match(soVar));

            assertEquals(1, soRes.size(), "Cas 2B (S,O fixés) : devrait retourner 1 résultat.");
            assertTrue(soRes.contains(sub(VAR_X, PREDICATE_1)),
                    "Cas 2B (S,O fixés) : substitution P → predicate1 manquante.");

            // P,O liés, S variable
            RDFTriple poVar = new RDFTriple(VAR_X, PREDICATE_1, OBJECT_2);
            List<Substitution> poRes = toList(store.match(poVar));

            assertEquals(1, poRes.size(), "Cas 2C (P,O fixés) : devrait retourner 1 résultat.");
            assertTrue(poRes.contains(sub(VAR_X, SUBJECT_2)),
                    "Cas 2C (P,O fixés) : substitution S → subject2 manquante.");

    /* ---------------------------------------------------------
       CAS 3 : 1 TERME LIÉ
       --------------------------------------------------------- */

            // S lié
            RDFTriple sVar = new RDFTriple(SUBJECT_1, VAR_X, VAR_Y);
            List<Substitution> sRes = toList(store.match(sVar));

            assertEquals(2, sRes.size(), "Cas 3A (S fixé) : 2 résultats attendus.");
            assertTrue(sRes.contains(sub(VAR_X, PREDICATE_1, VAR_Y, OBJECT_1)),
                    "Cas 3A (S fixé) : substitution (P → predicate1, O → object1) manquante.");
            assertTrue(sRes.contains(sub(VAR_X, PREDICATE_1, VAR_Y, OBJECT_3)),
                    "Cas 3A (S fixé) : substitution (P → predicate1, O → object3) manquante.");

            // P lié
            RDFTriple pVar = new RDFTriple(VAR_X, PREDICATE_1, VAR_Y);
            List<Substitution> pRes = toList(store.match(pVar));

            assertEquals(3, pRes.size(), "Cas 3B (P fixé) : 3 résultats attendus.");

            // O lié
            RDFTriple oVar = new RDFTriple(VAR_X, VAR_Y, OBJECT_2);
            List<Substitution> oRes = toList(store.match(oVar));

            assertEquals(1, oRes.size(), "Cas 3C (O fixé) : 1 résultat attendu.");
            assertTrue(oRes.contains(sub(VAR_X, SUBJECT_2, VAR_Y, PREDICATE_1)),
                    "Cas 3C (O fixé) : substitution S → subject2, P → predicate1 manquante.");

       /* ---------------------------------------------------------
       CAS 4 : 0 TERMES LIÉS (full variables)
       --------------------------------------------------------- */
            RDFTriple allVar = new RDFTriple(VAR_X, VAR_Y, VAR_Z); // X réutilisé
            List<Substitution> allRes = toList(store.match(allVar));

            assertEquals(3, allRes.size(),
                    "Cas 4 (tous variables) : aucun triple ne vérifie s==o, donc aucun résultat attendu.");

            //TODO:
            //Faire le cas ou s == o (une var et utilisée deux fois
    }


//    @Test
//    public void testMatchStarQuery() {
//        throw new NotImplementedException();
//    }

    private List<Substitution> toList(Iterator<Substitution> it) {
        List<Substitution> l = new ArrayList<>();
        it.forEachRemaining(l::add);
        return l;
    }

    private Substitution sub(Variable v1, Term t1) {
        Substitution s = new SubstitutionImpl();
        s.add(v1, t1);
        return s;
    }

    private Substitution sub(Variable v1, Term t1, Variable v2, Term t2) {
        Substitution s = new SubstitutionImpl();
        s.add(v1, t1);
        s.add(v2, t2);
        return s;
    }


}
