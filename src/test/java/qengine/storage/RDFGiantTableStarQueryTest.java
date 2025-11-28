package qengine.storage;

public class RDFGiantTableStarQueryTest extends AbstractStarQueryTest {
    @Override
    protected RDFStorage createStorage() {
        return new RDFGiantTable();
    }
}