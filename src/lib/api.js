import { supabase, isSupabaseConfigured } from './supabase.js';
import { fallbackRestaurants, fallbackProducts, fallbackOptionItems } from '../data/catalog.js';

// --- Catalogue -------------------------------------------------------------

export async function fetchRestaurants() {
  if (!isSupabaseConfigured) return fallbackRestaurants;
  const { data, error } = await supabase
    .from('restaurants').select('*').order('sort_order');
  if (error || !data?.length) return fallbackRestaurants;
  return data;
}

export async function fetchProducts() {
  if (!isSupabaseConfigured) return fallbackProducts;
  const { data, error } = await supabase
    .from('products').select('*').order('sort_order');
  if (error || !data?.length) return fallbackProducts;
  return data;
}

// Options tarifées du Bowl Builder et des suppléments sur les bowls
// signature (base/garniture/proteine/proteine_extra/topping/sauce). Les prix
// ne sont affichés ici qu'à titre indicatif : place_order() les recalcule
// depuis cette même table côté serveur.
export async function fetchOptionItems() {
  if (!isSupabaseConfigured) return fallbackOptionItems;
  const { data, error } = await supabase
    .from('option_items').select('*').order('category').order('sort_order');
  if (error || !data?.length) return fallbackOptionItems;
  return data;
}

// --- Commandes client ------------------------------------------------------

// Les prix ne sont jamais envoyés par le client : la fonction serveur
// place_order() recalcule chaque prix depuis la table products et rejette
// toute quantité ou produit invalide. Voir supabase/migrations/0002_secure_order_pricing.sql.
export async function placeOrder({ restaurantId, pickupSlot, items }) {
  const { data: order, error } = await supabase.rpc('place_order', {
    p_restaurant_id: restaurantId,
    p_pickup_slot: pickupSlot,
    p_items: items.map(i => ({
      product_id: i.productId,
      quantity: i.quantity,
      customizations: i.customizations ?? null,
    })),
  });
  if (error) throw error;
  return order;
}

export async function fetchOrder(orderId) {
  const { data, error } = await supabase
    .from('orders')
    .select('*, order_items(*), restaurants(name, address, image)')
    .eq('id', orderId)
    .single();
  if (error) throw error;
  return data;
}

export async function fetchMyOrders(userId) {
  const { data, error } = await supabase
    .from('orders')
    .select('*, order_items(*), restaurants(name, address)')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  if (error) throw error;
  return data;
}

// --- Préférences client ----------------------------------------------------

export async function fetchPreferences(userId) {
  const { data } = await supabase
    .from('customer_preferences').select('*').eq('user_id', userId).maybeSingle();
  return data;
}

export async function savePreferences(userId, patch) {
  const { data, error } = await supabase
    .from('customer_preferences')
    .upsert({ user_id: userId, ...patch, updated_at: new Date().toISOString() })
    .select()
    .single();
  if (error) throw error;
  return data;
}

// --- Administration ----------------------------------------------------------

// PostgREST utilise `,` et `()` comme séparateurs/groupes dans les filtres
// .or() : on les retire du terme de recherche pour ne pas pouvoir injecter
// des conditions de filtre supplémentaires via ce champ libre.
const sanitizeFilterTerm = s => s.replace(/[,()]/g, '');

export async function adminFetchCustomers({ search = '' } = {}) {
  let query = supabase
    .from('profiles')
    .select('*')
    .eq('role', 'customer')
    .order('created_at', { ascending: false });
  const term = sanitizeFilterTerm(search);
  if (term) {
    query = query.or(`first_name.ilike.%${term}%,last_name.ilike.%${term}%,email.ilike.%${term}%,phone.ilike.%${term}%`);
  }
  const { data, error } = await query;
  if (error) throw error;
  return data;
}

export async function adminFetchOrders({ restaurantId, status, date } = {}) {
  let query = supabase
    .from('orders')
    .select('*, order_items(*), restaurants(name), profiles(first_name, last_name, email, phone)')
    .order('created_at', { ascending: false })
    .limit(200);
  if (restaurantId) query = query.eq('restaurant_id', restaurantId);
  if (status) query = query.eq('status', status);
  if (date) {
    query = query.gte('created_at', `${date}T00:00:00`).lte('created_at', `${date}T23:59:59`);
  }
  const { data, error } = await query;
  if (error) throw error;
  return data;
}

export async function adminUpdateOrderStatus(orderId, status) {
  const { error } = await supabase.from('orders').update({ status }).eq('id', orderId);
  if (error) throw error;
}

export async function adminSaveProduct(product) {
  const { id, ...fields } = product;
  const query = id
    ? supabase.from('products').update(fields).eq('id', id)
    : supabase.from('products').insert(fields);
  const { error } = await query;
  if (error) throw error;
}

export async function adminSaveRestaurant(restaurant) {
  const { id, ...fields } = restaurant;
  const query = id
    ? supabase.from('restaurants').update(fields).eq('id', id)
    : supabase.from('restaurants').insert(fields);
  const { error } = await query;
  if (error) throw error;
}

// Export CSV des clients ayant donné leur consentement marketing (LPD/RGPD :
// seuls les clients ayant consenti sont exportables à des fins marketing).
export function customersToCsv(customers) {
  const rows = customers.filter(c => c.marketing_consent);
  const header = ['Prénom', 'Nom', 'E-mail', 'Téléphone', 'Consentement le', 'Inscrit le'];
  const escape = v => `"${String(v ?? '').replace(/"/g, '""')}"`;
  const lines = rows.map(c => [
    c.first_name, c.last_name, c.email, c.phone,
    c.marketing_consent_at?.slice(0, 10), c.created_at?.slice(0, 10),
  ].map(escape).join(';'));
  return '﻿' + [header.map(escape).join(';'), ...lines].join('\n');
}
