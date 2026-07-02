import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import '@fontsource/dm-sans/400.css';
import '@fontsource/dm-sans/600.css';
import '@fontsource/cormorant-garamond/600.css';

import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import { ShopProvider, useShop } from './context/ShopContext.jsx';
import { ScrollToTop } from './components/ui.jsx';
import { fetchPreferences } from './lib/api.js';

import Home from './pages/Home.jsx';
import Locations from './pages/Locations.jsx';
import Menu from './pages/Menu.jsx';
import Customize from './pages/Customize.jsx';
import Cart from './pages/Cart.jsx';
import Checkout from './pages/Checkout.jsx';
import Confirmation from './pages/Confirmation.jsx';
import Account from './pages/Account.jsx';
import OrderHistory from './pages/OrderHistory.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import Privacy from './pages/Privacy.jsx';

// L'espace admin n'est chargé que lorsqu'on y accède
const Admin = lazy(() => import('./admin/Admin.jsx'));

// Au chargement d'une session, applique le restaurant favori du client
function PreferencesBootstrap() {
  const { user } = useAuth();
  const { restaurantId, setRestaurantId, setFavoriteRestaurantId, ready } = useShop();
  useEffect(() => {
    if (!user || !ready) return;
    fetchPreferences(user.id).then(prefs => {
      if (prefs?.favorite_restaurant_id) {
        setFavoriteRestaurantId(prefs.favorite_restaurant_id);
        if (!restaurantId) setRestaurantId(prefs.favorite_restaurant_id);
      }
    }).catch(() => {});
  }, [user?.id, ready]);
  return null;
}

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ShopProvider>
          <PreferencesBootstrap />
          <ScrollToTop />
          <div className="app-shell">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/restaurants" element={<Locations />} />
              <Route path="/menu" element={<Menu />} />
              <Route path="/personnaliser/:slug" element={<Customize />} />
              <Route path="/panier" element={<Cart />} />
              <Route path="/paiement" element={<Checkout />} />
              <Route path="/confirmation/:orderId" element={<Confirmation />} />
              <Route path="/compte" element={<Account />} />
              <Route path="/compte/commandes" element={<OrderHistory />} />
              <Route path="/compte/nouveau-mot-de-passe" element={<ResetPassword />} />
              <Route path="/confidentialite" element={<Privacy />} />
              <Route path="/admin" element={
                <Suspense fallback={<div className="screen"><main className="page-content loading-state"><p>Chargement…</p></main></div>}>
                  <Admin />
                </Suspense>
              } />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </ShopProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
