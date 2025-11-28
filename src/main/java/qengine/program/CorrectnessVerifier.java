package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFStorage;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CorrectnessVerifier {

    private static final String DATA_FILE = "data/sample_data.nt";
    private static final String QUERY_FILE = "data/sample_query.queryset";

    public static void main(String[] args) throws IOException {
        System.out.println("=== Démarrage de la vérification (Mode Iterator) ===");

        FactBase oracleStore = new SimpleInMemoryGraphStore();
        RDFStorage myStore = new RDFHexaStore();

        System.out.println("Chargement des données...");
        loadData(DATA_FILE, oracleStore, myStore);

        System.out.println("Lecture des requêtes...");
        List<StarQuery> queries = parseQueries(QUERY_FILE);

        int totalQueries = queries.size();
        int correctQueries = 0;

        for (StarQuery query : queries) {
            boolean isCorrect = compareExecutions(query, oracleStore, myStore);
            if (isCorrect) {
                correctQueries++;
            } else {
                System.err.println("ERREUR de divergence sur la requête : " + query.getLabel());
            }
        }

        System.out.println("\n=== Résultat de la vérification ===");
        System.out.println("Requêtes correctes : " + correctQueries + " / " + totalQueries);
        if (correctQueries == totalQueries) {
            System.out.println("SUCCÈS : Votre moteur est correct et complet !");
        } else {
            System.out.println("ÉCHEC : Il y a des divergences.");
        }
    }

    private static void loadData(String filePath, FactBase oracle, RDFStorage myStore) throws IOException {
        try (RDFTriplesParser parser = new RDFTriplesParser(new FileReader(filePath), RDFFormat.NTRIPLES)) {
            while (parser.hasNext()) {
                RDFTriple triple = parser.next();
                oracle.add(triple);
                myStore.add(triple);
            }
        }
        System.out.println("Données chargées.");
    }

    private static List<StarQuery> parseQueries(String filePath) throws IOException {
        List<StarQuery> queries = new ArrayList<>();
        try (StarQuerySparQLParser parser = new StarQuerySparQLParser(filePath)) {
            while (parser.hasNext()) {
                fr.boreal.model.query.api.Query q = parser.next();
                if (q instanceof StarQuery) {
                    queries.add((StarQuery) q);
                }
            }
        }
        return queries;
    }

    /**
     * Exécute la requête sur les deux systèmes et compare les résultats.
     */
    private static boolean compareExecutions(StarQuery query, FactBase oracle, RDFStorage myStore) {
        // --- Exécution Oracle ---
        Set<String> oracleResults = executeWithOracle(query, oracle);

        // --- Exécution dans l'Hexastore  ---
        Iterator<Substitution> myResultsIterator = myStore. match(query);
        Set<String> myResults = extractResultLabels(myResultsIterator, query);

        // --- Comparaison ---
        boolean areEqual = oracleResults.equals(myResults);

        if (!areEqual) {
            System.out.println("\n--- Détail de l'erreur pour " + query.getLabel() + " ---");
            System.out.println("Attendu (Oracle) : " + oracleResults);
            System.out.println("Obtenu (Moi)     : " + myResults);

            Set<String> missing = new HashSet<>(oracleResults);
            missing.removeAll(myResults);
            if (!missing.isEmpty()) System.out.println("Manquant (Complétude KO) : " + missing);

            Set<String> unexpected = new HashSet<>(myResults);
            unexpected.removeAll(oracleResults);
            if (!unexpected.isEmpty()) System.out.println("Inattendu (Correction KO) : " + unexpected);
        }

        return areEqual;
    }

    private static Set<String> executeWithOracle(StarQuery query, FactBase oracle) {
        FOQuery<FOFormulaConjunction> foQuery = query.asFOQuery();
        FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance();
        Iterator<Substitution> iterator = evaluator.evaluate(foQuery, oracle);

        Set<String> results = new HashSet<>();
        while (iterator.hasNext()) {
            Substitution sub = iterator.next();
            Term val = sub.toMap().get(query.getCentralVariable());
            if (val != null) {
                results.add(val.label());
            }
        }
        return results;
    }

    /**
     * CHANGEMENT : Accepte maintenant un Iterator<Substitution>
     * Consomme l'itérateur jusqu'à la fin pour remplir le Set.
     */
    private static Set<String> extractResultLabels(Iterator<Substitution> myResultsIterator, StarQuery query) {
        Set<String> results = new HashSet<>();
        Variable centralVar = query.getCentralVariable();

        // On parcourt l'itérateur avec une boucle while
        while (myResultsIterator.hasNext()) {
            Substitution sub = myResultsIterator.next();
            Term term = sub.toMap().get(centralVar);

            if (term != null) {
                results.add(term.label());
            }
        }
        return results;
    }
}