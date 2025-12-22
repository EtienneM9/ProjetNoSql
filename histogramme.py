import matplotlib.pyplot as plt

def generer_histogramme_vertical(categories, valeurs):
    """
    Génère un graphique à barres verticales.
    :param categories: Liste de Strings (Abscisse - Axe X)
    :param valeurs: Liste de Long/Entiers (Ordonnée - Axe Y)
    """

    # Création de la figure
    fig, ax = plt.subplots(figsize=(6, 6))

    # Utilisation de bar() pour un affichage vertical
    # X = catégories, Y = valeurs
    bars = ax.bar(categories, valeurs, color='lightcoral', edgecolor='darkred')

    # Ajout de titres et de labels
    ax.set_xlabel('RDFStorage')
    ax.set_ylabel('Temps (ns)')
    ax.set_title('Comparaison des temps d\'insertion pour Hexastore et GiantTable')

    # Ajout des valeurs au-dessus de chaque barre
    ax.bar_label(bars, padding=3, fmt='%d')

    # Rotation des étiquettes de l'axe X si les noms sont longs
    plt.xticks(rotation=45)

    # Ajustement automatique des marges pour éviter de couper le texte
    plt.tight_layout()

    # Affichage du graphique
    plt.show()

# --- Exemple d'utilisation ---

# Vos données
labels = ["GiantTable", "Hexastore"]
donnees_long = [7702836636, 1064660]

generer_histogramme_vertical(labels, donnees_long)