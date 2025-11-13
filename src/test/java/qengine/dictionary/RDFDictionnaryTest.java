package qengine.dictionary;

import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import qengine.dictionnary.RDFDictionnary;

import static org.junit.jupiter.api.Assertions.*;

public class RDFDictionnaryTest {

    private RDFDictionnary dict;
    private TermFactory termFactory;

    @BeforeEach
    public void setUp() {
        // Comme getInstance() ne garantit pas vraiment un vrai singleton (erreur dans la classe),
        // on crée manuellement une nouvelle instance pour isoler les tests.
        dict = RDFDictionnary.getInstance();
        termFactory = SameObjectTermFactory.instance();
    }

    @Test
    public void testEncodeReturnsUniqueIds() {
        Term term1 = termFactory.createOrGetLiteral("<http://example.org/Alice>");
        Term term2 = termFactory.createOrGetLiteral("<http://example.org/Bob>");
        
        int id1 = dict.encode(term1);
        int id2 = dict.encode(term2);

        assertNotEquals(id1, id2, "Chaque ressource doit avoir un ID unique");
    }

    @Test
    public void testEncodeSameResourceReturnsSameId() {
        Term term = termFactory.createOrGetLiteral("<http://example.org/Alice>");
        
        int id1 = dict.encode(term);
        int id2 = dict.encode(term);

        assertEquals(id1, id2, "La même ressource doit toujours avoir le même ID");
    }

    @Test
    public void testDecodeReturnsOriginalResource() {
        Term resource = termFactory.createOrGetLiteral("<http://example.org/Charlie>");
        int id = dict.encode(resource);

        Term decoded = dict.decode(id);
        assertEquals(resource, decoded, "Le décodage doit renvoyer la ressource originale");
    }

    @Test
    public void testDecodeUnknownIdReturnsNull() {
        assertNull(dict.decode(9999), "Décoder un ID inconnu doit renvoyer null");
    }

    @Test
    public void testEncodeVariable() {
        Term variable = termFactory.createOrGetVariable("?x");
        int id = dict.encode(variable);
        
        Term decoded = dict.decode(id);
        assertEquals(variable, decoded, "Le décodage d'une variable doit renvoyer la variable originale");
    }

    @Test
    public void testMultipleEncodingsPreserveEquality() {
        Term term1 = termFactory.createOrGetLiteral("<http://example.org/Alice>");
        Term term2 = termFactory.createOrGetLiteral("<http://example.org/Alice>");
        
        // Avec SameObjectTermFactory, les deux références devraient être identiques
        int id1 = dict.encode(term1);
        int id2 = dict.encode(term2);
        
        assertEquals(id1, id2, "Des termes identiques doivent avoir le même ID");
    }

}
