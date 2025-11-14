package qengine.storage;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.junit.Test;
import qengine.model.RDFTriple;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class RDFGiantTableTest {
    private static final Literal<String> SUBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> PREDICATE_2 = SameObjectTermFactory.instance().createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Variable VAR_X = SameObjectTermFactory.instance().createOrGetVariable("?x");
    private static final Variable VAR_Y = SameObjectTermFactory.instance().createOrGetVariable("?y");


    @Test
    public void addTripleTest(){
        RDFGiantTable giantTable = new RDFGiantTable();
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);

        assertTrue(giantTable.add(rdfAtom1));

        Collection<RDFTriple> atoms = giantTable.getAtoms();
        assertEquals(1, atoms.size());
        assertTrue(atoms.contains(rdfAtom1));
    }

    @Test
    public void addTwiceTest(){
        RDFGiantTable giantTable = new RDFGiantTable();
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);

        assertTrue(giantTable.add(rdfAtom1));
        assertTrue(giantTable.add(rdfAtom1));

        assertEquals(2, giantTable.size());

        Collection<RDFTriple> atoms = giantTable.getAtoms();
        Collection<RDFTriple> atoms2 = giantTable.getAtoms();
        atoms2.add(rdfAtom1);
        atoms2.add(rdfAtom1);
        assertEquals(atoms, atoms2);
    }

    @Test
    public void sizeTest(){
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple rdfAtom2 = new RDFTriple(SUBJECT_2, PREDICATE_2, OBJECT_2);
        RDFGiantTable giantTable = new RDFGiantTable();
        giantTable.add(rdfAtom1);
        giantTable.add(rdfAtom2);
        assertEquals(2, giantTable.size());
    }

    @Test
    public void MatchTripleAndHowManyTest(){
        RDFGiantTable giantTable = new RDFGiantTable();
        RDFTriple rdfAtom1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFTriple rdfAtom2 = new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_1);

        RDFTriple pattern1 = new RDFTriple(VAR_X, PREDICATE_1, OBJECT_1);

        giantTable.add(rdfAtom1);
        giantTable.add(rdfAtom2);

        Iterator<Substitution> substitutionIterator = giantTable.match(pattern1);
        assertEquals(2,giantTable.howMany(pattern1));

        Map<Variable,Term> map1 = Map.of(VAR_X, (Term) SUBJECT_1);
        Map<Variable,Term> map2 = Map.of(VAR_X, (Term) SUBJECT_2);
        Substitution substitution = new SubstitutionImpl(map1);
        Substitution substitution2 = new SubstitutionImpl(map2);

        assertEquals(substitutionIterator.next(),substitution);
        assertEquals(substitutionIterator.next(),substitution2);
    }
}
