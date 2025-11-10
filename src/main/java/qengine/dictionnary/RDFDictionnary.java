package qengine.dictionnary;

import java.util.HashMap;
import java.util.Map;

public class RDFDictionnary {
    //Singleton pour garantir la cohérence entre plusieurs HexaStore
    private static RDFDictionnary instance = null;

    private Map<String, Integer> resourceToId = new HashMap<>();
    private Map<Integer, String> idToResource = new HashMap<>();
    private int nextId = 0;

    private RDFDictionnary() {}

    public static RDFDictionnary getInstance() {
        return instance == null ? new RDFDictionnary() : instance;
    }

    public int encode(String resource){
        //TO TEST
        return resourceToId.computeIfAbsent(resource, r -> {
            int id = nextId++;
            idToResource.put(id, r);
            return id;
        });
    }

    public String decode(int id){
        //TO TEST
        return idToResource.get(id);
    }
}
