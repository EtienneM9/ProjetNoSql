import os
import re
import sys

# --- CONFIGURATION DES REGEX ---
# 1. Suppression des métadonnées 
TAG_PATTERN = re.compile(r'\'', re.IGNORECASE)

# 2. Extraction des requêtes SPARQL (SELECT ... WHERE { ... })
QUERY_PATTERN = re.compile(r'(SELECT\s+.*?\s+WHERE\s*\{.*?\})', re.IGNORECASE | re.DOTALL)

# 3. Normalisation pour comparaison (sauts de lignes/espaces multiples -> espace simple)
SPACE_PATTERN = re.compile(r'\s+')

def process_file(filepath):
    """
    Traite un fichier .queryset :
    - Nettoie les balises.
    - Sépare les requêtes uniques des doublons/vides.
    - Écrit deux fichiers de sortie (_clean et _removed).
    """
    filename = os.path.basename(filepath)
    print(filename)
    
    # Évite de retraiter les fichiers déjà générés par ce script
    if filename.endswith("_clean.queryset") or filename.endswith("_removed.queryset"):
        return

    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # Nettoyage global (suppression des tags [source...])
        content_no_tags = TAG_PATTERN.sub('', content)

        # Extraction de tous les blocs qui ressemblent à une requête
        queries = QUERY_PATTERN.findall(content_no_tags)

        if not queries:
            print(f"[SKIP] Aucune requête détectée dans : {filename}")
            return

        unique_queries = []
        removed_items = [] # Liste de tuples (requête, raison)
        seen_hashes = set()

        for q in queries:
            q_stripped = q.strip()
            
            # 1. Vérification requête vide
            if not q_stripped:
                removed_items.append(("", "VIDE (Contenu vide)"))
                continue

            # 2. Vérification doublon
            # On normalise (minuscule + une seule ligne) pour comparer le sens, pas la forme exacte
            q_norm = SPACE_PATTERN.sub(' ', q_stripped).lower()
            q_hash = hash(q_norm)

            if q_hash in seen_hashes:
                removed_items.append((q_stripped, "DOUBLON"))
            else:
                seen_hashes.add(q_hash)
                unique_queries.append(q_stripped)

        # --- GÉNÉRATION DES FICHIERS ---
        base_path, ext = os.path.splitext(filepath)
        path_clean = f"{base_path}_clean{ext}"
        path_removed = f"{base_path}_removed{ext}"

        # Écriture du fichier CLEAN (Requêtes valides)
        with open(path_clean, 'w', encoding='utf-8') as f_clean:
            for q in unique_queries:
                f_clean.write(q + "\n\n")

        # Écriture du fichier REMOVED (Requêtes supprimées)
        # On le crée même s'il est vide pour montrer qu'il n'y a pas eu de pertes si c'est le cas
        with open(path_removed, 'w', encoding='utf-8') as f_removed:
            f_removed.write(f"# Audit de suppression pour {filename}\n")
            f_removed.write(f"# Requêtes conservées : {len(unique_queries)}\n")
            f_removed.write(f"# Requêtes supprimées : {len(removed_items)}\n")
            f_removed.write("-" * 40 + "\n\n")
            
            if removed_items:
                for q, reason in removed_items:
                    f_removed.write(f"# [RAISON: {reason}]\n")
                    f_removed.write(q + "\n\n")
            else:
                f_removed.write("# Aucune requête n'a été supprimée.\n")

        print(f"[OK] {filename} : {len(unique_queries)} gardées -> '{os.path.basename(path_clean)}' | {len(removed_items)} rejetées -> '{os.path.basename(path_removed)}'")

    except Exception as e:
        print(f"[ERREUR] Échec sur {filename} : {e}")

def main():
    target_dir = "data"
    if len(sys.argv) > 1:
        target_dir = sys.argv[1]

    if not os.path.exists(target_dir):
        print(f"Le dossier '{target_dir}' n'existe pas.")
        return

    print(f"--- Analyse et nettoyage dans : {target_dir} ---")
    
    count_processed = 0
    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if file.endswith(".queryset"):
                process_file(os.path.join(root, file))
                count_processed += 1
    
    print(f"--- Terminé. {count_processed} fichiers analysés. ---")

if __name__ == "__main__":
    main()