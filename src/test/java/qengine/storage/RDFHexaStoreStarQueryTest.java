package qengine.storage;

public class RDFHexaStoreStarQueryTest extends AbstractStarQueryTest {
    @Override
    protected RDFStorage createStorage() {
        return new RDFHexaStore();
    }
}