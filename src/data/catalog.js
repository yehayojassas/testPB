// Catalogue de secours : mêmes données que supabase/seed.sql.
// Utilisé uniquement pour la navigation lorsque Supabase n'est pas configuré
// ou injoignable — les comptes et commandes exigent Supabase.

// product.category est un code technique en anglais (voir migration 0005) ;
// ce mapping fournit le libellé affiché au client.
export const CATEGORY_LABELS = {
  signature: 'Signature',
  build_your_own: 'À composer',
  drinks: 'Boissons',
  desserts: 'Desserts',
};

export const fallbackRestaurants = [
  { id: 'grancy', slug: 'grancy', name: 'Lausanne Grancy', address: 'Boulevard de Grancy 58, 1006 Lausanne', city: 'Lausanne', image: 'restaurant-grancy.jpg', prep_minutes: 12, is_active: true, sort_order: 1 },
  { id: 'saint-laurent', slug: 'saint-laurent', name: 'Lausanne Saint-Laurent', address: 'Rue Saint-Laurent 38, 1003 Lausanne', city: 'Lausanne', image: 'restaurant-saint-laurent.jpg', prep_minutes: 12, is_active: true, sort_order: 2 },
  { id: 'renens', slug: 'renens', name: 'Renens', address: 'Rue de la Gare de Triage 6, 1020 Renens', city: 'Renens', image: 'restaurant-renens.jpg', prep_minutes: 15, is_active: true, sort_order: 3 },
  { id: 'morges', slug: 'morges', name: 'Morges', address: 'Grand-Rue 6, 1110 Morges', city: 'Morges', image: 'restaurant-morges.jpg', prep_minutes: 15, is_active: true, sort_order: 4 },
  { id: 'vevey', slug: 'vevey', name: 'Vevey', address: 'Rue du Lac 8, 1800 Vevey', city: 'Vevey', image: 'restaurant-vevey.jpg', prep_minutes: 15, is_active: true, sort_order: 5 },
  { id: 'geneve', slug: 'geneve', name: 'Genève', address: 'Rue du Môle 31, 1201 Genève', city: 'Genève', image: 'restaurant-geneve.jpg', prep_minutes: 15, is_active: true, sort_order: 6 },
  { id: 'yverdon', slug: 'yverdon', name: 'Yverdon-les-Bains', address: 'Rue du Milieu 6, 1400 Yverdon-les-Bains', city: 'Yverdon-les-Bains', image: 'restaurant-yverdon.jpg', prep_minutes: 15, is_active: true, sort_order: 7 },
  { id: 'romont', slug: 'romont', name: 'Romont', address: 'Place de la Gare 1, 1680 Romont', city: 'Romont', image: 'restaurant-romont.jpg', prep_minutes: 15, is_active: true, sort_order: 8 },
];

export const fallbackProducts = [
  { id: 'compose', slug: 'compose', name: 'Compose ton bowl', description: 'Composez votre bowl : base, garnitures, protéine, extras, toppings et sauces à volonté.', price: 0, promo_price: null, category: 'build_your_own', tag: 'Votre choix', image: 'compose-ton-bowl.jpg', is_available: true, sort_order: 0 },
  { id: 'nordique', slug: 'nordique', name: 'Le nordique', description: 'Dés de saumon cru, riz jasmin, avocat, concombre, algues wakame, radis, aneth, oignon frit, sésame noir.', price: 22, promo_price: null, category: 'signature', tag: 'Best-seller', image: 'le-nordique.jpg', is_available: true, sort_order: 1 },
  { id: 'hawaien', slug: 'hawaien', name: 'L’hawaïen', description: 'Dés de thon cru, quinoa, edamame, fenouil, oignon rouge, grenade, coriandre, avocat, gingembre, sésame blanc.', price: 22, promo_price: null, category: 'signature', tag: null, image: 'l-hawaien.jpg', is_available: true, sort_order: 2 },
  { id: 'atlantique', slug: 'atlantique', name: 'L’atlantique', description: 'Crevettes, riz venere, avocat, edamame, tomate, fenouil, mangue, oignon frit, algues wakame.', price: 21, promo_price: null, category: 'signature', tag: null, image: 'l-atlantique.jpg', is_available: true, sort_order: 3 },
  { id: 'indien', slug: 'indien', name: 'L’indien', description: 'Poulet grillé, riz jasmin, concombre, carotte râpée, chou rouge, radis, avocat, cacahuète.', price: 20, promo_price: null, category: 'signature', tag: null, image: 'l-indien.jpg', is_available: true, sort_order: 4 },
  { id: 'libanais', slug: 'libanais', name: 'Le libanais', description: 'Falafels maison, houmous, tomate, concombre, feta, oignon rouge, grenade, menthe.', price: 20, promo_price: null, category: 'signature', tag: null, image: 'le-libanais.jpg', is_available: true, sort_order: 5 },
  { id: 'bouda', slug: 'bouda', name: 'Le bouda', description: 'Tofu grillé, quinoa, chou rouge, mangue, avocat, courgette, ciboulette, algues wakame.', price: 20, promo_price: null, category: 'signature', tag: 'Veggie', image: 'le-bouda.jpg', is_available: true, sort_order: 6 },
  { id: 'cookie-choco-noisette', slug: 'cookie-choco-noisette', name: 'Cookies double chocolat & noisettes', description: 'Fait maison, fondant.', price: 4.5, promo_price: null, category: 'desserts', tag: null, image: 'cookie-double-chocolat-noisettes.png', is_available: true, sort_order: 1 },
  { id: 'cookie-gruyere-pecan', slug: 'cookie-gruyere-pecan', name: 'Cookies crème de Gruyère & noix de Pécan', description: 'Recette signature.', price: 4.5, promo_price: null, category: 'desserts', tag: null, image: 'cookie-creme-gruyere-noix-pecan.png', is_available: true, sort_order: 2 },
  { id: 'muffin-chocolat', slug: 'muffin-chocolat', name: 'Muffin au chocolat', description: 'Moelleux, cœur chocolat.', price: 4.5, promo_price: null, category: 'desserts', tag: null, image: 'muffin-au-chocolat.png', is_available: true, sort_order: 3 },
  { id: 'muffin-fruits-rouges', slug: 'muffin-fruits-rouges', name: 'Muffin aux fruits rouges', description: 'Fruits rouges de saison.', price: 4.5, promo_price: null, category: 'desserts', tag: null, image: 'muffin-aux-fruits-rouges.png', is_available: true, sort_order: 4 },
  { id: 'salade-de-fruits', slug: 'salade-de-fruits', name: 'Salade de fruits', description: 'Fruits frais de saison.', price: 4.5, promo_price: null, category: 'desserts', tag: null, image: 'salade-de-fruits.png', is_available: true, sort_order: 5 },
  { id: 'charitea-black', slug: 'charitea-black', name: 'Charitea Black', description: 'Thé noir glacé bio & équitable.', price: 5, promo_price: null, category: 'drinks', tag: null, image: 'charitea-black.jpg', is_available: true, sort_order: 1 },
  { id: 'charitea-green', slug: 'charitea-green', name: 'Charitea Green', description: 'Thé vert glacé bio & équitable.', price: 5, promo_price: null, category: 'drinks', tag: null, image: 'charitea-green.jpg', is_available: true, sort_order: 2 },
  { id: 'charitea-mate', slug: 'charitea-mate', name: 'Charitea Mate', description: 'Maté glacé bio & équitable.', price: 5, promo_price: null, category: 'drinks', tag: null, image: 'charitea-mate.jpg', is_available: true, sort_order: 3 },
  { id: 'charitea-red', slug: 'charitea-red', name: 'Charitea Red', description: 'Hibiscus glacé bio & équitable.', price: 5, promo_price: null, category: 'drinks', tag: null, image: 'charitea-red.jpg', is_available: true, sort_order: 4 },
  { id: 'coca-cola', slug: 'coca-cola', name: 'Coca-Cola (33cl)', description: null, price: 4, promo_price: null, category: 'drinks', tag: null, image: 'coca-cola.jpg', is_available: true, sort_order: 5 },
  { id: 'coca-cola-zero', slug: 'coca-cola-zero', name: 'Coca-Cola Zéro (33cl)', description: null, price: 4, promo_price: null, category: 'drinks', tag: null, image: 'coca-cola-zero.jpg', is_available: true, sort_order: 6 },
  { id: 'henniez-gazeuse', slug: 'henniez-gazeuse', name: 'Eau gazeuse Henniez (50cl)', description: null, price: 4, promo_price: null, category: 'drinks', tag: null, image: 'henniez-gazeuse.jpg', is_available: true, sort_order: 7 },
  { id: 'henniez-plate', slug: 'henniez-plate', name: 'Eau plate Henniez (50cl)', description: null, price: 4, promo_price: null, category: 'drinks', tag: null, image: 'henniez-plate.jpg', is_available: true, sort_order: 8 },
  { id: 'go-ginger-curcuma', slug: 'go-ginger-curcuma', name: 'Go Ginger & Curcuma', description: 'Boisson tonique maison.', price: 5, promo_price: null, category: 'drinks', tag: null, image: 'go-ginger-curcuma.jpg', is_available: true, sort_order: 9 },
  { id: 'limonade-maison', slug: 'limonade-maison', name: 'Limonade maison citron', description: 'Pressée maison.', price: 4, promo_price: null, category: 'drinks', tag: null, image: 'limonade-maison.jpg', is_available: true, sort_order: 10 },
  { id: 'the-froid-maison', slug: 'the-froid-maison', name: 'Thé froid maison', description: 'Infusion maison du jour.', price: 4, promo_price: null, category: 'drinks', tag: null, image: 'the-froid-maison.jpg', is_available: true, sort_order: 11 },
];

// Miroir de la table option_items (Bowl Builder + suppléments sur les bowls
// signature). Utilisé uniquement en mode catalogue local.
export const fallbackOptionItems = [
  { category: 'base', name: 'Salade', price: 5, sort_order: 1 },
  { category: 'base', name: 'Riz venere', price: 5, sort_order: 2 },
  { category: 'base', name: 'Riz', price: 5, sort_order: 3 },
  { category: 'base', name: 'Quinoa', price: 5, sort_order: 4 },

  { category: 'garnish', name: 'Houmous', price: 2, sort_order: 1 },
  { category: 'garnish', name: 'Grenade', price: 2, sort_order: 2 },
  { category: 'garnish', name: 'Mangue', price: 2, sort_order: 3 },
  { category: 'garnish', name: 'Concombre', price: 2, sort_order: 4 },
  { category: 'garnish', name: 'Carotte', price: 2, sort_order: 5 },
  { category: 'garnish', name: 'Choux rouge', price: 2, sort_order: 6 },
  { category: 'garnish', name: 'Courgette', price: 2, sort_order: 7 },
  { category: 'garnish', name: 'Edamame', price: 2, sort_order: 8 },
  { category: 'garnish', name: 'Fenouil', price: 2, sort_order: 9 },
  { category: 'garnish', name: 'Tomate', price: 2, sort_order: 10 },
  { category: 'garnish', name: 'Oignon rouge', price: 2, sort_order: 11 },
  { category: 'garnish', name: 'Radis', price: 2, sort_order: 12 },
  { category: 'garnish', name: 'Algues wakame', price: 2, sort_order: 13 },
  { category: 'garnish', name: 'Avocat', price: 2, sort_order: 14 },

  { category: 'protein', name: 'Feta', price: 6, sort_order: 1 },
  { category: 'protein', name: 'Falafel', price: 6, sort_order: 2 },
  { category: 'protein', name: 'Crevettes', price: 7, sort_order: 3 },
  { category: 'protein', name: 'Tofu', price: 7, sort_order: 4 },
  { category: 'protein', name: 'Poulet grillé', price: 7, sort_order: 5 },
  { category: 'protein', name: 'Saumon', price: 7, sort_order: 6 },
  { category: 'protein', name: 'Thon', price: 7, sort_order: 7 },

  { category: 'extra_protein', name: 'Feta', price: 3, sort_order: 1 },
  { category: 'extra_protein', name: 'Falafel', price: 3, sort_order: 2 },
  { category: 'extra_protein', name: 'Crevettes', price: 3, sort_order: 3 },
  { category: 'extra_protein', name: 'Tofu', price: 3, sort_order: 4 },
  { category: 'extra_protein', name: 'Poulet grillé', price: 3, sort_order: 5 },
  { category: 'extra_protein', name: 'Saumon', price: 3, sort_order: 6 },
  { category: 'extra_protein', name: 'Thon', price: 3, sort_order: 7 },

  { category: 'topping', name: 'Sésame blanc et noir', price: 1, sort_order: 1 },
  { category: 'topping', name: 'Cacahuètes', price: 1, sort_order: 2 },
  { category: 'topping', name: 'Ciboulette', price: 1, sort_order: 3 },
  { category: 'topping', name: 'Aneth', price: 1, sort_order: 4 },
  { category: 'topping', name: 'Coriandre', price: 1, sort_order: 5 },
  { category: 'topping', name: 'Menthe', price: 1, sort_order: 6 },
  { category: 'topping', name: 'Gingembre gari', price: 1, sort_order: 7 },
  { category: 'topping', name: 'Graines courge', price: 1, sort_order: 8 },
  { category: 'topping', name: 'Oignon frit', price: 1, sort_order: 9 },

  { category: 'sauce', name: 'Sauce teriyaki sucrée', price: 1, sort_order: 1 },
  { category: 'sauce', name: 'Classique', price: 1, sort_order: 2 },
  { category: 'sauce', name: 'Mayonnaise spicy', price: 1, sort_order: 3 },
  { category: 'sauce', name: 'Teriyaki classique', price: 1, sort_order: 4 },
  { category: 'sauce', name: 'Teriyaki relevé', price: 1, sort_order: 5 },
  { category: 'sauce', name: 'Crème de sésame', price: 1, sort_order: 6 },
];

export const promos = [
  { kicker: 'PROMO', title: 'Offres du moment', text: '2 bowls + 2 boissons', price: 'CHF 39.–', color: 'coral', image: 'bowl-salmon.png', link: '/offre/le-duo' },
  { kicker: 'SIGNATURE', title: 'Le nordique', text: 'Frais, généreux, prêt minute', price: 'CHF 22.–', color: 'mango', image: 'le-nordique.jpg', link: '/personnaliser/nordique' },
  { kicker: 'PAUSE MIDI', title: 'Prêt en 15 min', text: 'Commandez, passez, savourez', price: 'Dès CHF 20.–', color: 'green', image: 'l-indien.jpg', link: '/menu' },
];
