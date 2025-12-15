# StarQuery Cleaner Audit Script

This utility script (`clean_queries_audit.py`) automates the cleaning of `.queryset` files used in the NoSQL Project. It processes SPARQL queries to separate valid unique queries from duplicates and empty entries, without overwriting your original data.

## 🚀 Features

* **Metadata Stripping**: Removes `` tags.
* **Smart De-duplication**: Identifies and removes duplicate queries by normalizing whitespace and case (e.g., `SELECT` vs `select` are treated as the same).
* **Validation**: Filters out empty or malformed query blocks.
* **Non-destructive Audit**: 
    * Creates a `_clean.queryset` file with valid queries.
    * Creates a `_removed.queryset` file with rejected queries and the reason why.
* **Safety**: Automatically ignores its own output files to prevent infinite processing loops.

## 📋 Prerequisites

* **Python 3.x** installed on your system.
* No external libraries are required (uses standard `os`, `re`, `sys`).

## 🛠️ Usage

### 1. Installation
Save the python script as `clean_queries_audit.py` at the root of your project (or anywhere you prefer).

### 2. Basic Usage
By default, the script looks for a folder named `data` in the same directory as the script:

```bash
python clean_queries_audit.py
```
### 3. Custom Directory
If your .queryset files are in a different folder, pass the path as an argument:

```bash
python clean_queries_audit.py ./path/to/my/datasets
```

### 📂 Output Files
For every input file (e.g., workload.queryset), the script generates two new files in the same directory:

1. workload_clean.queryset
Contains only the valid, unique, and cleaned SPARQL queries.

Use this file for your project benchmarks and tests.

2. workload_removed.queryset
Contains every query that was deleted.

Includes comments explaining the reason for deletion:

[RAISON: DOUBLON]: The query was a duplicate of a previous one in the file.

[RAISON: VIDE]: The query block was empty or contained only whitespace.

## 📝 Example
Input File (data.queryset):

SELECT ?v0 WHERE { ?v0 <http://likes> <http://User0> }

SELECT ?v0 WHERE { ?v0 <http://likes> <http://User0> }
Resulting data_clean.queryset:

SELECT ?v0 WHERE { ?v0 <http://likes> <http://User0> }
Resulting data_removed.queryset:


Audit de suppression pour data.queryset\
Requêtes conservées : 1\
Requêtes supprimées : 1


[RAISON: DOUBLON]
SELECT ?v0 WHERE { ?v0 <http://likes> <http://User0> }