import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreditCard, Lock, MapPin, Storefront } from '@phosphor-icons/react';
import { Header, PrimaryButton, Notice, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { placeOrder } from '../lib/api.js';

export default function Checkout() {
  const navigate = useNavigate();
  const { cart, restaurant, pickupSlot, cartTotal, clearCart } = useShop();
  const { user, isSupabaseConfigured } = useAuth();
  const [method, setMethod] = useState('on_site');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const confirm = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const order = await placeOrder({
        userId: user.id,
        restaurantId: restaurant.id,
        pickupSlot,
        items: cart.map(i => ({
          productId: i.productId,
          name: i.name,
          unitPrice: i.unitPrice,
          quantity: i.quantity,
          customizations: i.customizations,
        })),
      });
      clearCart();
      navigate(`/confirmation/${order.id}`, { replace: true });
    } catch (e) {
      setError('La commande n’a pas pu être enregistrée. Merci de réessayer.');
      setSubmitting(false);
    }
  };

  useEffect(() => {
    if (cart.length === 0 && !submitting) navigate('/menu', { replace: true });
  }, [cart.length]);
  if (cart.length === 0) return null;

  return (
    <div className="screen">
      <Header back="/panier" title="Paiement" progress="Étape 4 sur 4" showCart={false} />
      <main className="page-content checkout-page">
        <section className="pickup-summary">
          <MapPin />
          <div>
            <h2>Retrait · {restaurant?.name}</h2>
            <p>{restaurant?.address}</p>
            <span>Prêt vers {pickupSlot}</span>
          </div>
          <button onClick={() => navigate('/panier')}>Modifier</button>
        </section>

        {!isSupabaseConfigured ? (
          <Notice tone="warn">
            La base de données n’est pas encore configurée (voir le README, section Supabase).
            La commande ne peut pas être enregistrée sans elle.
          </Notice>
        ) : !user ? (
          <section className="checkout-auth">
            <h2>Presque terminé !</h2>
            <p>Connectez-vous pour enregistrer votre commande et suivre sa préparation.</p>
            <PrimaryButton onClick={() => navigate('/compte', { state: { from: '/paiement' } })}>
              Se connecter ou créer un compte
            </PrimaryButton>
          </section>
        ) : (
          <>
            <section className="payment-methods">
              <h2>Comment souhaitez-vous payer ?</h2>
              <button className={method === 'on_site' ? 'selected' : ''} onClick={() => setMethod('on_site')} aria-pressed={method === 'on_site'}>
                <Storefront />
                <span><b>Sur place au retrait</b><small>Carte, TWINT ou espèces</small></span>
                <span className="radio">{method === 'on_site' && <i />}</span>
              </button>
              <button className="disabled-method" disabled aria-disabled="true">
                <CreditCard />
                <span><b>Carte en ligne</b><small>Bientôt disponible (Stripe)</small></span>
                <span className="radio" />
              </button>
            </section>
            <section className="totals">
              <p className="total"><span>Total à régler</span><b>{chf(cartTotal)}</b></p>
            </section>
            {error && <Notice tone="error">{error}</Notice>}
          </>
        )}
      </main>
      {user && isSupabaseConfigured && (
        <div className="sticky-footer pay-footer">
          <PrimaryButton disabled={submitting} onClick={confirm}>
            {submitting ? 'Envoi en cours…' : `Confirmer la commande · ${chf(cartTotal)}`}
          </PrimaryButton>
          <small><Lock />Paiement au retrait · aucune carte débitée en ligne</small>
        </div>
      )}
    </div>
  );
}
