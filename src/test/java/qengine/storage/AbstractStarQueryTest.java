package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.VariableImpl;
import org.junit.jupiter.api.Test;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classe de test générique pour vérifier l'évaluation des requêtes en étoile.
 * Elle permet de tester la méthode default match(StarQuery) sur n'importe quel RDFStorage.
 */
public abstract class AbstractStarQueryTest {

    protected RDFStorage storage;
    protected final TermFactory factory = SameObjectTermFactory.instance();

    // Variables et Termes pour les tests
    Variable x = new VariableImpl("x");
    Literal alice = factory.createOrGetLiteral("Alice");
    Literal bob = factory.createOrGetLiteral("Bob");
    Literal charlie = factory.createOrGetLiteral("Charlie");
    Literal type = factory.createOrGetLiteral("type");
    Literal livesIn = factory.createOrGetLiteral("livesIn");
    Literal loves = factory.createOrGetLiteral("loves");
    Literal person = factory.createOrGetLiteral("Person");
    Literal paris = factory.createOrGetLiteral("Paris");
    Literal london = factory.createOrGetLiteral("London");
    Literal artist = factory.createOrGetLiteral("Artist");
    Literal sushi = factory.createOrGetLiteral("Sushi");

    /**
     * Cette méthode doit être implémentée par les classes filles pour fournir
     * soit un RDFHexaStore, soit une RDFGiantTable.
     */
    protected abstract RDFStorage createStorage();

    public void setUp() {
        this.storage = createStorage();

        // --- JEU DE DONNÉES DE TEST (Micro-Data) ---
        // Alice : Personne, Vit à Paris, Aime les Sushi
        storage.add(new RDFTriple(alice, type, person));
        storage.add(new RDFTriple(alice, livesIn, paris));
        storage.add(new RDFTriple(alice, loves, sushi));

        // Bob : Personne, Vit à Londres, Aime les Sushi
        storage.add(new RDFTriple(bob, type, person));
        storage.add(new RDFTriple(bob, livesIn, london));
        storage.add(new RDFTriple(bob, loves, sushi));

        // Charlie : Artiste, Vit à Paris
        storage.add(new RDFTriple(charlie, type, artist));
        storage.add(new RDFTriple(charlie, livesIn, paris));
    }

    @Test
    public void testMatchSinglePattern() {
        setUp();
        // Requete : SELECT ?x WHERE { ?x type Person }
        // Résultat attendu : Alice, Bob
        List<RDFTriple> patterns = List.of(new RDFTriple(x, type, person));
        StarQuery q = new StarQuery("Q1", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(2, results.size());
        assertTrue(results.contains(alice));
        assertTrue(results.contains(bob));
    }

    @Test
    public void testMatchTwoPatterns_JoinSuccess() {
        setUp();
        // Requete : SELECT ?x WHERE { ?x type Person . ?x livesIn Paris }
        // Résultat attendu : Alice (Bob est éliminé car il vit à Londres)
        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, type, person),
                new RDFTriple(x, livesIn, paris)
        );
        StarQuery q = new StarQuery("Q2", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(1, results.size());
        assertTrue(results.contains(alice));
    }

    @Test
    public void testMatchOptimization_OrderMatters() {
        setUp();
        // Cas conçu pour vérifier l'optimisation par selectivité
        // Pattern 1 : ?x loves Sushi (3 résultats : Alice, Bob) -> Note: j'ai oublié d'ajouter Charlie loves sushi, disons 2 résultats.
        // Pattern 2 : ?x type Artist (1 résultat : Charlie)

        // Si on cherche : { ?x loves Sushi . ?x type Artist }
        // Charlie n'aime pas les sushis dans ma base -> 0 résultat.

        // Ajoutons un cas plus subtil :
        // Requete : Qui vit à Paris (Alice, Charlie) ET est une Personne (Alice, Bob) ?
        // Réponse : Alice.

        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, livesIn, paris), // 2 candidats (Alice, Charlie)
                new RDFTriple(x, type, person)    // 2 candidats (Alice, Bob)
        );
        StarQuery q = new StarQuery("Q3", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(1, results.size());
        assertTrue(results.contains(alice));
    }

    @Test
    public void testMatchNoResult() {
        setUp();
        // Requete : SELECT ?x WHERE { ?x type Artist . ?x livesIn London }
        // Charlie est artiste mais à Paris. Bob est à Londres mais pas artiste.
        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, type, artist),
                new RDFTriple(x, livesIn, london)
        );
        StarQuery q = new StarQuery("Q4", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);

        assertEquals(0, getResultsCount(it));
    }

    @Test
    public void testMatchWithManyPatterns() {
        setUp();
        // Requete : { ?x type Person . ?x livesIn Paris . ?x loves Sushi }
        // Alice matche tout.
        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, type, person),
                new RDFTriple(x, livesIn, paris),
                new RDFTriple(x, loves, sushi)
        );
        StarQuery q = new StarQuery("Q5", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(1, results.size());
        assertTrue(results.contains(alice));
    }

    // --- Méthodes utilitaires pour les tests ---

    private List<Term> getResultsForVar(Iterator<Substitution> it, Variable v) {
        List<Term> terms = new ArrayList<>();
        it.forEachRemaining(s -> terms.add(s.toMap().get(v)));
        return terms;
    }

    private int getResultsCount(Iterator<Substitution> it) {
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    @Test
    public void testMatchCentralVariableInObjectPosition() {
        setUp();
        // Requete : Qui est aimé par Alice ? (SELECT ?x WHERE { Alice loves ?x })
        // Pattern : <Alice, loves, ?x>
        // Note : Ici ?x est en position OBJET
        List<RDFTriple> patterns = List.of(
                new RDFTriple(alice, loves, x)
        );
        StarQuery q = new StarQuery("Q_Obj", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(1, results.size());
        assertTrue(results.contains(sushi)); // Alice loves Sushi
    }

    @Test
    public void testMatch_FilterRejectsSomeCandidates() {
        setUp();
        // But : Vérifier que le moteur filtre bien les candidats qui ne valident pas tous les patterns.
        // Data : Alice (Paris), Bob (London), Charlie (Paris)
        // Requête : Qui est une Personne ET vit à Paris ?

        // Candidats initiaux pour "type Person" : Alice, Bob.
        // Filtre "livesIn Paris" : Alice passe, Bob est rejeté (car Bob livesIn London).

        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, type, person),
                new RDFTriple(x, livesIn, paris)
        );
        StarQuery q = new StarQuery("Q_Filter_Partial", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(1, results.size());
        assertTrue(results.contains(alice));
        // Ce test confirme que le if (howMany(...) == 0) a bien fonctionné pour Bob
    }

    @Test
    public void testMatch_FilterRejectsAllCandidates() {
        setUp();
        // But : Vérifier que si aucun candidat ne passe le filtre, on renvoie vide.
        // Requête : Qui est une Personne ET vit sur Mars ?

        // Candidats initiaux "type Person" : Alice, Bob.
        // Filtre "livesIn Mars" : Alice échoue, Bob échoue.

        Literal mars = factory.createOrGetLiteral("Mars");

        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, type, person),
                new RDFTriple(x, livesIn, mars)
        );
        StarQuery q = new StarQuery("Q_Filter_All", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);

        assertEquals(0, getResultsCount(it));
    }

    @Test
    public void testMatchMixPosition() {
        setUp();
        // Cas complexe :
        // 1. ?x est une Personne (Sujet)
        // 2. Alice loves ?x (Objet) -> (Bon, Alice n'aime que les sushis qui ne sont pas une personne, le résultat sera vide, mais le test valide la logique)

        // Changeons pour un cas qui marche :
        // 1. ?x vit à Paris (Sujet) -> Alice, Charlie
        // 2. ?x est de type Personne (Sujet) -> Alice, Bob
        // Résultat : Alice.

        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, livesIn, paris),
                new RDFTriple(x, type, person)
        );
        StarQuery q = new StarQuery("Q_Mix", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        List<Term> results = getResultsForVar(it, x);

        assertEquals(1, results.size());
        assertTrue(results.contains(alice));
    }



    @Test
    public void testMatchImmediateEmptyResult() {
        setUp();
        // Requete avec un pattern qui n'existe pas du tout.
        // <?x loves Bob> -> Personne n'aime Bob dans les données :(
        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, loves, bob),
                new RDFTriple(x, type, person)
        );
        StarQuery q = new StarQuery("Q_EmptyStart", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        assertEquals(0, getResultsCount(it));
    }

    @Test
    public void testMatchDisjointSets() {
        setUp();
        // Intersection vide entre deux ensembles non vides.
        // A: Ceux qui vivent à Londres (Bob)
        // B: Ceux qui sont Artistes (Charlie)
        // A inter B = Vide.
        List<RDFTriple> patterns = List.of(
                new RDFTriple(x, livesIn, london),
                new RDFTriple(x, type, artist)
        );
        StarQuery q = new StarQuery("Q_Disjoint", patterns, List.of(x));

        Iterator<Substitution> it = storage.match(q);
        assertEquals(0, getResultsCount(it));
    }
}