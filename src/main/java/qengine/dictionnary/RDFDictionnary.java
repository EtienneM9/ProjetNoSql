package qengine.dictionnary;

import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;

import java.util.HashMap;
import java.util.Map;

public class RDFDictionnary {
    //Singleton pour garantir la cohérence entre plusieurs HexaStore
    private static RDFDictionnary instance = null;

    private static Map<Term, Integer> resourceToId = new HashMap<>();
    private static Map<Integer, Term> idToResource = new HashMap<>();
    private int nextId = 0;

    private RDFDictionnary() {}

    public static RDFDictionnary getInstance() {
        if (instance == null) {
            instance = new RDFDictionnary();
        }
        return instance;
    }

    public int encode(Term resource){
        //TO TEST
        if (resource instanceof Variable){
            return -1;
        }
        return resourceToId.computeIfAbsent(resource, r -> {
            int id = nextId++;
            idToResource.put(id, r);
            return id;
        });
    }

    public Term decode(int id){
        //TO TEST
        return idToResource.get(id);
    }
}
