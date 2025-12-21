package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.query.api.Query;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import fr.boreal.views.builder.ViewBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFGiantTable;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFStorage;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import qengine.storage.RDFStorage;


public final class Example {

	private static final String WORKING_DIR = "data/";
	private static final String SAMPLE_DATA_FILE = WORKING_DIR + "sample_data.nt";
	private static final String SAMPLE_QUERY_FILE = WORKING_DIR + "sample_query.queryset";
	private static final String FIRST_DATA_FILE = WORKING_DIR + "500K.nt";
	private static final String SECOND_DATA_FILE = WORKING_DIR + "2M.nt";
	private static final RDFStorage hexaStore = new RDFHexaStore();
	private static final RDFStorage giantTable = new RDFGiantTable();

	public static void main(String[] args) throws IOException {
		/*
		 * Exemple d'utilisation des deux parsers
		 */
		System.out.println("=== Parsing RDF Data ===");
		//List<RDFTriple> rdfAtoms = parseRDFData(SAMPLE_DATA_FILE);
		List<RDFTriple> rdfAtoms = parseRDFData(FIRST_DATA_FILE);
		List<RDFTriple> rdfAtoms2 = parseRDFData(SECOND_DATA_FILE);

		System.out.println("\n=== Parsing Sample Queries ===");
		List<StarQuery> starQueries = parseSparQLQueries(SAMPLE_QUERY_FILE);

		SyswarmUp(hexaStore, starQueries);
	    SyswarmUp(giantTable, starQueries);

		List<Long> firstWriteDurations = write(rdfAtoms);
		List<Long> secondWriteDurations = write(rdfAtoms2);

		benchmarkRead(giantTable, hexaStore, starQueries);
		/*
		 * Exemple d'utilisation de l'évaluation de requetes par Integraal avec les objets parsés
		 */
//		System.out.println("\n=== Executing the queries with Integraal ===");
//		FactBase factBase = new SimpleInMemoryGraphStore();
//		for (RDFTriple triple : rdfAtoms) {
//			factBase.add(triple);  // Stocker chaque RDFAtom dans le store
//		}
//
//		// Exécuter les requêtes sur le store
//		for (StarQuery starQuery : starQueries) {
//			executeStarQuery(starQuery, factBase);
//		}
	}

	/**
	 * Warm up le systeme avant de lancer des tests .
	 */
	/**
	 * Exécute un sous-ensemble de requêtes pour "chauffer" la JVM et le cache système.
	 * Cette phase ne doit pas être chronométrée.
	 *
	 * @param store   Le système de stockage (déjà chargé).
	 * @param queries La liste complète des requêtes parsées.
	 */
	public static void SyswarmUp(RDFStorage store, List<StarQuery> queries) {
		System.out.println("--- Démarrage du Warm-up (Chauffage JIT + Cache) ---");

		int limit = Math.min(queries.size(), 200);
		int count = 0;

		for (StarQuery q : queries) {
			if (count >= limit) break;

			try {
				// 1. Appel de la méthode de résolution
				Iterator<Substitution> it = store.match(q);

				// 2. CONSOMMATION OBLIGATOIRE :
				// Il faut parcourir l'itérateur pour forcer le moteur à calculer tous les résultats.
				// Si on ne fait que l'appel .match(), certains moteurs "lazy" ne feront rien du tout.
				while (it.hasNext()) {
					it.next();
				}
			} catch (Exception e) {
				// On ignore les erreurs silencieusement pendant le warm-up
			}
			count++;
		}

		// 3. Nettoyage final :
		System.gc();

		System.out.println("--- Warm-up terminé (" + count + " requêtes jouées) ---");
		System.out.println("Lancement de la mesure dans 1 seconde...");

		// Petite pause pour laisser le système se stabiliser
		try { Thread.sleep(1000); } catch (InterruptedException e) {}
	}
	/**
	 * Parse et affiche le contenu d'un fichier RDF.
	 *
	 * @param rdfFilePath Chemin vers le fichier RDF à parser
	 * @return Liste des RDFAtoms parsés
	 */
	private static List<RDFTriple> parseRDFData(String rdfFilePath) throws IOException {
		FileReader rdfFile = new FileReader(rdfFilePath);
		List<RDFTriple> rdfAtoms = new ArrayList<>();

		try (RDFTriplesParser rdfAtomParser = new RDFTriplesParser(rdfFile, RDFFormat.NTRIPLES)) {
			int count = 0;
			while (rdfAtomParser.hasNext()) {
				RDFTriple triple = rdfAtomParser.next();
				rdfAtoms.add(triple);  // Stocker le triplet dans la collection
				System.out.println("RDF Triple #" + (++count) + ": " + triple);
			}
			System.out.println("Total RDF Triples parsed: " + count);
		}
		return rdfAtoms;
	}

	/**
	 * Parse et affiche le contenu d'un fichier de requêtes SparQL.
	 *
	 * @param queryFilePath Chemin vers le fichier de requêtes SparQL
	 * @return Liste des StarQueries parsées
	 */
	private static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
		List<StarQuery> starQueries = new ArrayList<>();

		try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(queryFilePath)) {
			int queryCount = 0;

			while (queryParser.hasNext()) {
				Query query = queryParser.next();
				if (query instanceof StarQuery starQuery) {
					starQueries.add(starQuery);  // Stocker la requête dans la collection
					System.out.println("Star Query #" + (++queryCount) + ":");
					System.out.println("  Central Variable: " + starQuery.getCentralVariable().label());
					System.out.println("  RDF Atoms:");
					starQuery.getRdfAtoms().forEach(triple -> System.out.println("    " + triple));
				} else {
					System.err.println("Requête inconnue ignorée.");
				}
			}
			System.out.println("Total Queries parsed: " + starQueries.size());
		}
		return starQueries;
	}

	/**
	 * Exécute une requête en étoile sur le store et affiche les résultats.
	 *
	 * @param starQuery La requête à exécuter
	 * @param factBase  Le store contenant les triplets
	 */
	private static void executeStarQuery(StarQuery starQuery, FactBase factBase) {
		FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery(); // Conversion en FOQuery
		FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance(); // Créer un évaluateur
		Iterator<Substitution> queryResults = evaluator.evaluate(foQuery, factBase); // Évaluer la requête

		System.out.printf("Execution of  %s:%n", starQuery);
		System.out.println("Answers:");
		if (!queryResults.hasNext()) {
			System.out.println("No answer.");
		}
		while (queryResults.hasNext()) {
			Substitution result = queryResults.next();
			System.out.println(result); // Afficher chaque réponse
		}
		System.out.println();
	}

	private static List<Long> write(Collection<RDFTriple> triples){

		long giantStart = System.nanoTime();
		giantTable.addAll(triples);
		long giantEnd = System.nanoTime();
		long giantDuration = giantEnd - giantStart;

		long hexaStart = System.nanoTime();
		hexaStore.addAll(triples);
		long hexaEnd = System.nanoTime();
		long hexaDuration = hexaEnd - hexaStart;

		System.out.println("GiantTable write time (ns): " + giantDuration);
		System.out.println("HexaStore write time (ns): " + hexaDuration);

		return List.of(giantDuration, hexaDuration);
	}

	/**
	 * Compare les performances de lecture (Querying) entre GiantTable et HexaStore.
	 * Cette méthode exécute toutes les requêtes fournies sur les deux systèmes.
	 *
	 * @param giantTable L'instance de la GiantTable (doit contenir les données).
	 * @param hexaStore  L'instance du HexaStore (doit contenir les données).
	 * @param queries    La liste des requêtes StarQuery à exécuter.
	 */
	public static void benchmarkRead(RDFStorage giantTable, RDFStorage hexaStore, List<StarQuery> queries) {
		System.out.println("--- Démarrage du Benchmark de Lecture (Querying) ---");
		System.out.println("Nombre de requêtes à exécuter : " + queries.size());

		// --- MESURE POUR GIANT TABLE ---
		long startGT = System.nanoTime();
		int countGT = 0;
		for (StarQuery q : queries) {
			try {
				// On récupère l'itérateur de résultats
				Iterator<Substitution> it = giantTable.match(q);
				// IMPORTANT : Il faut consommer l'itérateur pour que le moteur cherche vraiment les résultats
				while (it.hasNext()) {
					it.next();
					countGT++;
				}
			} catch (Exception e) {
				// On peut logger l'erreur ou l'ignorer pour ne pas stopper le benchmark
				// System.err.println("Erreur requête GT : " + e.getMessage());
			}
		}
		long timeGT = (System.nanoTime() - startGT) / 1_000_000; // Conversion en ms

		// --- MESURE POUR HEXASTORE ---
		long startHS = System.nanoTime();
		int countHS = 0;
		for (StarQuery q : queries) {
			try {
				Iterator<Substitution> it = hexaStore.match(q);
				while (it.hasNext()) {
					it.next();
					countHS++;
				}
			} catch (Exception e) {
				// System.err.println("Erreur requête HS : " + e.getMessage());
			}
		}
		long timeHS = (System.nanoTime() - startHS) / 1_000_000; // Conversion en ms

		// --- AFFICHAGE DES RÉSULTATS ---
		System.out.println("\n=== RÉSULTATS DE LECTURE ===");
		System.out.println("GiantTable : " + timeGT + " ms (Résultats trouvés : " + countGT + ")");
		System.out.println("HexaStore  : " + timeHS + " ms (Résultats trouvés : " + countHS + ")");

		// Petit check de cohérence (optionnel)
		if (countGT != countHS) {
			System.err.println("/!\\ ATTENTION : Les deux systèmes n'ont pas retourné le même nombre de résultats !");
		} else {
			System.out.println("Validité : OK (Même nombre de résultats)");
		}
		System.out.println("============================");
	}

}
