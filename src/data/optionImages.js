// Correspondance centralisée entre le libellé d'un supplément
// (option_items.name, tel qu'affiché au client) et le fichier image réel
// présent dans public/images/. Centraliser ici évite toute logique
// conditionnelle dispersée dans les composants.
//
// La CLÉ suit exactement le libellé affiché dans l'interface ; la VALEUR est
// le nom de fichier réel (accents, espaces et majuscules compris). Certains
// libellés diffèrent volontairement du nom de fichier — ex. « Choux rouge »
// (interface) → « Chou rouge.png » (fichier).
const OPTION_IMAGE_FILES = {
  // Garnitures
  'Houmous': 'Houmous.png',
  'Grenade': 'Grenade.png',
  'Mangue': 'Mangue.png',
  'Concombre': 'Concombre.png',
  'Carotte': 'Carotte.png',
  'Choux rouge': 'Chou rouge.png', // libellé ≠ nom de fichier
  'Courgette': 'Courgette.png',
  'Edamame': 'Edamame.png',
  'Fenouil': 'Fenouil.png',
  'Tomate': 'Tomate.png',
  'Oignon rouge': 'Oignon rouge.png',
  'Radis': 'Radis.png',
  'Algues wakame': 'Algues wakame.png',
  'Avocat': 'Avocat.png',

  // Protéines (base « protein » et supplément « extra_protein »
  // partagent le même visuel)
  'Feta': 'Feta.png',
  'Falafel': 'Falafel.png',
  'Crevettes': 'Crevettes.png',
  'Tofu': 'Tofu.png',
  'Poulet grillé': 'Poulet grillé.png',
  'Saumon': 'Saumon.png',
  'Thon': 'Thon.png',

  // Toppings
  'Sésame blanc et noir': 'Sésame blanc et noir.png',
  'Cacahuètes': 'Cacahuètes.png',
  'Ciboulette': 'Ciboulette.png',
};

// Chemin public de l'image d'un supplément, ou null si aucune image dédiée
// n'existe encore (le composant affiche alors un placeholder neutre plutôt
// que de réutiliser à tort l'image d'un autre produit).
// encodeURI gère les espaces et les accents des noms de fichiers.
export function optionImage(name) {
  const file = OPTION_IMAGE_FILES[name];
  return file ? `/images/${encodeURI(file)}` : null;
}
