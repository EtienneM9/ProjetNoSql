# Project Type Hierarchy - RDF Query Engine

## Main Package Structure

```
qengine/
├── dictionnary/
├── model/
├── storage/
├── parser/
└── program/
```

## Type Tree Structure

### 1. Dictionary Package (`qengine.dictionnary`)

```
RDFDictionnary (Singleton)
├── Fields:
│   ├── Map<Term, Integer> resourceToId
│   ├── Map<Integer, Term> idToResource
│   └── int nextId
├── Methods:
│   ├── getInstance() : RDFDictionnary
│   ├── encode(Term resource) : int
│   └── decode(int id) : Term
```

**Purpose**: Maps RDF Terms to integer IDs for efficient storage

---

### 2. Model Package (`qengine.model`)

```
RDFTriple extends AtomImpl
├── Static Field:
│   └── TRIPLE_PREDICATE : Predicate (arity=3)
├── Constructors:
│   ├── RDFTriple(Atom a)
│   ├── RDFTriple(List<Term> terms)
│   └── RDFTriple(Term... terms)
├── Methods:
│   ├── getTripleSubject() : Term
│   ├── getTriplePredicate() : Term
│   ├── getTripleObject() : Term
│   └── toString() : String
└── Validates: exactly 3 terms (subject, predicate, object)

StarQuery (extends FOQuery)
├── Fields:
│   ├── String sourceString
│   ├── List<RDFTriple> rdfAtoms
│   └── List<Variable> answerVariables
└── Represents: Star-shaped SPARQL queries with a central variable
```

---

### 3. Storage Package (`qengine.storage`)

```
RDFStorage (Interface)
├── Methods:
│   ├── add(RDFTriple t) : boolean
│   ├── match(RDFTriple a) : Iterator<Substitution>
│   ├── match(StarQuery q) : Iterator<Substitution>
│   ├── howMany(RDFTriple a) : long
│   ├── size() : long
│   ├── getAtoms() : Collection<RDFTriple>
│   ├── addAll(Stream<RDFTriple> atoms) : boolean
│   └── addAll(Collection<RDFTriple> atoms) : boolean

RDFHexaStore implements RDFStorage
├── Implements: Six indexed permutations of triples (SPO, SOP, PSO, POS, OSP, OPS)
└── Purpose: Efficient query matching using multiple indexes

RDFGiantTable implements RDFStorage
├── Implements: Single-table storage approach
└── Purpose: Simple storage for small datasets
```

---

### 4. Parser Package (`qengine.parser`)

```
RDFTriplesParser implements Parser<Atom>
├── Purpose: Parse RDF triples from .nt files
└── Returns: Iterator<Atom> of RDFTriple objects

StarQuerySparQLParser implements Parser<Query>
├── Purpose: Parse SPARQL queries into StarQuery objects
├── Uses: RDF4J SPARQLParser
└── Returns: Iterator<Query> of StarQuery objects
```

---

## External Boreal Library Types

### Core Logical Elements (`fr.boreal.model.logicalElements.api`)

```
Term (interface) - Base type for all RDF terms
├── Literal (interface extends Term)
│   └── Represents: Concrete values (URIs, strings, numbers)
├── Variable (interface extends Term)
│   └── Represents: Query variables (e.g., ?x, ?subject)
└── Constant (interface extends Term)

Atom (interface)
├── Represents: Atomic formulas (predicates with terms)
├── Methods:
│   ├── getPredicate() : Predicate
│   ├── getTerms() : List<Term>
│   └── getTerm(int index) : Term

Predicate (interface)
├── Represents: Relation names with arity
└── Methods:
    ├── getLabel() : String
    └── getArity() : int

Substitution (interface)
├── Represents: Variable bindings/assignments
└── Purpose: Maps variables to terms in query results
```

### Factories (`fr.boreal.model.logicalElements.factory`)

```
SameObjectTermFactory implements TermFactory
├── Pattern: Flyweight (reuses identical terms)
├── Methods:
│   ├── instance() : SameObjectTermFactory
│   ├── createOrGetLiteral(String value) : Literal
│   ├── createOrGetVariable(String name) : Variable
│   └── createOrGetConstant(String value) : Constant

SameObjectPredicateFactory implements PredicateFactory
├── Pattern: Flyweight (reuses identical predicates)
└── Methods:
    ├── instance() : SameObjectPredicateFactory
    └── createOrGetPredicate(String label, int arity) : Predicate
```

---

## Type Relationships Diagram

```
┌─────────────────────────────────────────────────────┐
│           fr.boreal Library Types                    │
├─────────────────────────────────────────────────────┤
│  Term (interface)                                    │
│  ├── Literal                                         │
│  ├── Variable                                        │
│  └── Constant                                        │
│                                                       │
│  Atom (interface)                                    │
│  Predicate (interface)                               │
│  Substitution (interface)                            │
└──────────────┬──────────────────────────────────────┘
               │ extends/uses
               ▼
┌─────────────────────────────────────────────────────┐
│              qengine Project Types                   │
├─────────────────────────────────────────────────────┤
│                                                       │
│  RDFTriple extends AtomImpl                          │
│  ─────────────────────────────                       │
│  - Contains 3 Terms (S, P, O)                        │
│  - Uses TRIPLE_PREDICATE                             │
│                                                       │
│  RDFDictionnary (Singleton)                          │
│  ──────────────────────────                          │
│  - encode: Term → int                                │
│  - decode: int → Term                                │
│                                                       │
│  RDFStorage (interface)                              │
│  ───────────────────────                             │
│  - add, match, size operations                       │
│                                                       │
│  RDFHexaStore implements RDFStorage                  │
│  ───────────────────────────────────                 │
│  - 6 indexes for efficient queries                   │
│                                                       │
│  StarQuery extends FOQuery                           │
│  ──────────────────────────                          │
│  - List<RDFTriple> + List<Variable>                 │
│                                                       │
└─────────────────────────────────────────────────────┘
```

---

## Test Class Modifications Summary

### Changes Made to `RDFDictionnaryTest.java`:

1. **Added Import**: `SameObjectTermFactory` for creating Term objects
2. **Fixed Type Issues**: Changed from `String` to `Term` types
3. **Fixed Syntax Error**: Removed incomplete line `Term id1 =`
4. **Updated All Tests**: Now use proper `termFactory.createOrGetLiteral()` and `termFactory.createOrGetVariable()`
5. **Added New Tests**:
   - `testEncodeVariable()`: Tests encoding/decoding of Variable terms
   - `testMultipleEncodingsPreserveEquality()`: Tests that identical terms get same ID

### Test Method Summary:

| Test Method | Purpose |
|------------|---------|
| `testEncodeReturnsUniqueIds()` | Different terms get different IDs |
| `testEncodeSameResourceReturnsSameId()` | Same term always gets same ID |
| `testDecodeReturnsOriginalResource()` | Decode returns original term |
| `testDecodeUnknownIdReturnsNull()` | Unknown ID returns null |
| `testEncodeVariable()` | Variables can be encoded/decoded |
| `testMultipleEncodingsPreserveEquality()` | Factory pattern preserves identity |

---

## Key Concepts

### 1. Term vs String
- **Before**: Tests used `String` directly
- **After**: Tests use `Term` objects created via `SameObjectTermFactory`
- **Why**: `RDFDictionnary.encode()` expects Term, not String

### 2. Factory Pattern
- `SameObjectTermFactory.instance()` returns singleton
- `createOrGetLiteral()` — reuses existing literals
- `createOrGetVariable()` — reuses existing variables

### 3. RDF Triple Structure
```
<subject> <predicate> <object>
   ↓          ↓          ↓
  Term      Term       Term
```

### 4. Dictionary Purpose
Maps Terms to integers for:
- Memory efficiency
- Faster comparisons
- Index optimization in RDFHexaStore
