// Catalogue de secours : mêmes données que supabase/seed.sql.
// Utilisé uniquement pour la navigation lorsque Supabase n'est pas configuré
// ou injoignable — les comptes et commandes exigent Supabase.

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
  { id: 'compose', slug: 'compose', name: 'Compose ton bowl', description: 'Créez votre bowl, étape par étape', price: 18, promo_price: null, category: 'À composer', tag: 'Votre choix', image: 'compose-ton-bowl.jpg', is_available: true, sort_order: 0 },
  { id: 'nordique', slug: 'nordique', name: 'Le nordique', description: 'Saumon · edamame · wakame', price: 22, promo_price: null, category: 'Signature', tag: 'Best-seller', image: 'le-nordique.jpg', is_available: true, sort_order: 1 },
  { id: 'hawaien', slug: 'hawaien', name: 'L’hawaïen', description: 'Thon · avocat · notes exotiques', price: 22, promo_price: null, category: 'Signature', tag: null, image: 'l-hawaien.jpg', is_available: true, sort_order: 2 },
  { id: 'atlantique', slug: 'atlantique', name: 'L’atlantique', description: 'Saumon fumé · fraîcheur marine', price: 21, promo_price: null, category: 'Signature', tag: null, image: 'l-atlantique.jpg', is_available: true, sort_order: 3 },
  { id: 'indien', slug: 'indien', name: 'L’indien', description: 'Poulet · légumes · saveurs épicées', price: 20, promo_price: null, category: 'Signature', tag: null, image: 'l-indien.jpg', is_available: true, sort_order: 4 },
  { id: 'libanais', slug: 'libanais', name: 'Le libanais', description: 'Falafel · houmous · grenade', price: 20, promo_price: null, category: 'Signature', tag: null, image: 'le-libanais.jpg', is_available: true, sort_order: 5 },
  { id: 'bouda', slug: 'bouda', name: 'Le bouda', description: 'Tofu · légumes · option végétale', price: 20, promo_price: null, category: 'Signature', tag: 'Veggie', image: 'le-bouda.jpg', is_available: true, sort_order: 6 },
];

export const bowlOptions = {
  bases: ['Riz sushi', 'Quinoa', 'Salade'],
  proteins: ['Saumon', 'Poulet teriyaki', 'Tofu'],
  toppings: ['Avocat', 'Mangue', 'Edamame', 'Concombre', 'Chou rouge', 'Wakame'],
  paidToppings: { Wakame: 1.5 },
  sauces: ['Sésame soja', 'Mango spicy', 'Sans sauce'],
  includedToppings: 4,
};

export const promos = [
  { kicker: 'OFFRE DU MOMENT', title: 'Le Duo', text: '2 bowls + 2 boissons', price: 'CHF 39.–', color: 'coral', image: 'bowl-salmon.png' },
  { kicker: 'SIGNATURE', title: 'Le nordique', text: 'Frais, généreux, prêt minute', price: 'CHF 22.–', color: 'mango', image: 'le-nordique.jpg' },
  { kicker: 'PAUSE MIDI', title: 'Prêt en 15 min', text: 'Commandez, passez, savourez', price: 'Dès CHF 20.–', color: 'green', image: 'l-indien.jpg' },
];
