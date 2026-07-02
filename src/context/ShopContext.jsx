import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { fetchProducts, fetchRestaurants } from '../lib/api.js';

const ShopContext = createContext(null);
const STORAGE_KEY = 'planet-bowl-shop';

function readStored() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? {};
  } catch {
    return {};
  }
}

export function ShopProvider({ children }) {
  const stored = useMemo(readStored, []);
  const [restaurants, setRestaurants] = useState([]);
  const [products, setProducts] = useState([]);
  const [restaurantId, setRestaurantId] = useState(stored.restaurantId ?? null);
  const [cart, setCart] = useState(stored.cart ?? []);
  const [pickupSlot, setPickupSlot] = useState(stored.pickupSlot ?? null);
  const [favoriteRestaurantId, setFavoriteRestaurantId] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    Promise.all([fetchRestaurants(), fetchProducts()]).then(([r, p]) => {
      setRestaurants(r);
      setProducts(p);
      setReady(true);
    });
  }, []);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ restaurantId, cart, pickupSlot }));
  }, [restaurantId, cart, pickupSlot]);

  const restaurant = restaurants.find(r => r.id === restaurantId) ?? null;

  const addToCart = item => setCart(c => [...c, item]);
  const updateQuantity = (index, delta) =>
    setCart(c => c.map((item, i) => i === index
      ? { ...item, quantity: Math.max(1, item.quantity + delta) }
      : item));
  const removeItem = index => setCart(c => c.filter((_, i) => i !== index));
  const clearCart = () => { setCart([]); setPickupSlot(null); };

  const cartCount = cart.reduce((n, i) => n + i.quantity, 0);
  const cartTotal = cart.reduce((n, i) => n + i.unitPrice * i.quantity, 0);

  // Créneaux de retrait basés sur l'heure réelle et le temps de préparation
  const pickupSlots = useMemo(() => {
    const prep = restaurant?.prep_minutes ?? 15;
    const now = new Date();
    return [0, 10, 25].map(extra => {
      const t = new Date(now.getTime() + (prep + extra) * 60000);
      const minutes = Math.ceil(t.getMinutes() / 5) * 5;
      t.setMinutes(minutes, 0, 0);
      return t.toTimeString().slice(0, 5);
    });
  }, [restaurant?.prep_minutes, cart.length]);

  const value = {
    ready, restaurants, products,
    restaurant, restaurantId, setRestaurantId,
    cart, addToCart, updateQuantity, removeItem, clearCart,
    cartCount, cartTotal,
    pickupSlot, setPickupSlot, pickupSlots,
    favoriteRestaurantId, setFavoriteRestaurantId,
  };
  return <ShopContext.Provider value={value}>{children}</ShopContext.Provider>;
}

export const useShop = () => useContext(ShopContext);
