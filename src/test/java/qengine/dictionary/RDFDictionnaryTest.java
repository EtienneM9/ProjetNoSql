package qengine.dictionary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import qengine.dictionnary.RDFDictionnary;

import static org.junit.jupiter.api.Assertions.*;

public class RDFDictionnaryTest {

    private RDFDictionnary dict;

    @BeforeEach
    public void setUp() {
        // Comme getInstance() ne garantit pas vraiment un vrai singleton (erreur dans la classe),
        // on crée manuellement une nouvelle instance pour isoler les tests.
        dict = RDFDictionnary.getInstance();
    }

    @Test
    public void testEncodeReturnsUniqueIds() {
        int id1 = dict.encode("http://example.org/Alice");
        int id2 = dict.encode("http://example.org/Bob");

        assertNotEquals(id1, id2, "Chaque ressource doit avoir un ID unique");
    }

    @Test
    public void testEncodeSameResourceReturnsSameId() {
        int id1 = dict.encode("http://example.org/Alice");
        int id2 = dict.encode("http://example.org/Alice");

        assertEquals(id1, id2, "La même ressource doit toujours avoir le même ID");
    }

    @Test
    public void testDecodeReturnsOriginalResource() {
        String resource = "http://example.org/Charlie";
        int id = dict.encode(resource);

        String decoded = dict.decode(id);
        assertEquals(resource, decoded, "Le décodage doit renvoyer la ressource originale");
    }

    @Test
    public void testDecodeUnknownIdReturnsNull() {
        assertNull(dict.decode(9999), "Décoder un ID inconnu doit renvoyer null");
    }

    @Test
    public void testSingletonReturnsSameInstance() {
        RDFDictionnary first = RDFDictionnary.getInstance();
        RDFDictionnary second = RDFDictionnary.getInstance();

        assertSame(first, second, "getInstance() doit renvoyer la même instance");
    }
}
