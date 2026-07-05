import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock, MapPin, Minus, Plus, ShoppingBag, Trash } from '@phosphor-icons/react';
import { Header, PrimaryButton, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';

export default function Cart() {
  const navigate = useNavigate();
  const {
    cart, restaurant, updateQuantity, removeItem,
    pickupSlot, setPickupSlot, pickupSlots, cartTotal, cartCount,
  } = useShop();

  const [promoCode, setPromoCode] = useState('');
  const [promoMsg, setPromoMsg] = useState(null);
  const applyPromo = () => setPromoMsg(
    promoCode.trim()
      ? { tone: 'error', text: `Le code « ${promoCode.trim()} » est invalide ou expiré.` }
      : { tone: 'info', text: 'Aucune promotion n’est disponible actuellement.' },
  );

  const describe = item => {
    const c = item.customizations;
    if (!c) return item.description;
    if (c.bundle) return c.bundle.join(' · ');
    return [
      c.base, c.protein,
      ...(c.garnish ?? []),
      ...(c.extra_protein ?? []),
      ...(c.topping ?? []),
      ...(c.sauce ?? []),
    ].filter(Boolean).join(' · ');
  };

  return (
    <div className="screen">
      <Header back="/menu" title="Votre commande" progress="Étape 3 sur 4" showCart={false} />
      <main className="page-content cart-page">
        <section className="pickup-summary">
          <MapPin />
          <div>
            <h2>Retrait · {restaurant ? restaurant.name : 'À choisir'}</h2>
            <p>{restaurant?.address ?? 'Sélectionnez votre restaurant de retrait'}</p>
            <span>● Ouvert</span>
          </div>
          <button onClick={() => navigate('/restaurants')}>Modifier</button>
        </section>
        {cart.length === 0 ? (
          <section className="empty-cart">
            <ShoppingBag />
            <h2>Votre panier est vide</h2>
            <button onClick={() => navigate('/menu')}>Voir le menu</button>
          </section>
        ) : cart.map((item, i) => (
          <article className="cart-item" key={`${item.slug}-${i}`}>
            <img src={`/images/${item.image}`} alt="" />
            <div>
              <h2>{item.name}</h2>
              <p>{describe(item)}</p>
              <div className="quantity small">
                <button onClick={() => updateQuantity(i, -1)} aria-label="Réduire la quantité"><Minus /></button>
                <b>{item.quantity}</b>
                <button onClick={() => updateQuantity(i, 1)} aria-label="Augmenter la quantité"><Plus /></button>
              </div>
            </div>
            <aside>
              <strong>{chf(item.unitPrice * item.quantity)}</strong>
              <button onClick={() => removeItem(i)} aria-label={`Retirer ${item.name}`}><Trash />Retirer</button>
            </aside>
          </article>
        ))}
        {cart.length > 0 && (
          <>
            <label className="promo-input">
              <span>Code promo</span>
              <input
                placeholder="Votre code"
                value={promoCode}
                onChange={e => { setPromoCode(e.target.value); setPromoMsg(null); }}
                onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); applyPromo(); } }}
              />
              <button type="button" onClick={applyPromo}>Appliquer</button>
              {promoMsg && <em className={`promo-message promo-${promoMsg.tone}`} role="status">{promoMsg.text}</em>}
            </label>
            <section className="pickup-times">
              <h2>Quand venez-vous ?</h2>
              <div>
                {pickupSlots.map((t, i) => (
                  <button key={t} className={pickupSlot === t ? 'active' : ''} onClick={() => setPickupSlot(t)} aria-pressed={pickupSlot === t}>
                    {i === 0 && <small>Dès que possible</small>}{t}
                  </button>
                ))}
              </div>
            </section>
            <section className="totals">
              <p><span>Sous-total ({cartCount} article{cartCount > 1 ? 's' : ''})</span><b>{chf(cartTotal)}</b></p>
              <p><span>Frais</span><b>CHF 0.–</b></p>
              <p className="total"><span>Total</span><b>{chf(cartTotal)}</b></p>
            </section>
          </>
        )}
      </main>
      {cart.length > 0 && (
        <div className="sticky-footer pay-footer">
          <PrimaryButton disabled={!restaurant || !pickupSlot} onClick={() => navigate('/paiement')}>
            {!restaurant ? 'Choisissez un restaurant' : !pickupSlot ? 'Choisissez un créneau' : `Continuer · ${chf(cartTotal)}`}
          </PrimaryButton>
          <small><Lock />Paiement sécurisé</small>
        </div>
      )}
    </div>
  );
}
