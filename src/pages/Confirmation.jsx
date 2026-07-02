import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Check, CheckCircle, MapPin, NavigationArrow, Receipt } from '@phosphor-icons/react';
import { Header, chf, statusLabels } from '../components/ui.jsx';
import { fetchOrder } from '../lib/api.js';
import { supabase } from '../lib/supabase.js';

const trackSteps = ['received', 'preparing', 'ready'];

export default function Confirmation() {
  const navigate = useNavigate();
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    const load = () => fetchOrder(orderId)
      .then(data => { if (active) setOrder(data); })
      .catch(() => { if (active) setError(true); });
    load();
    // Suivi en temps réel du statut, avec repli sur un rafraîchissement périodique
    const channel = supabase?.channel(`order-${orderId}`)
      .on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'orders', filter: `id=eq.${orderId}` }, load)
      .subscribe();
    const timer = window.setInterval(load, 30000);
    return () => {
      active = false;
      window.clearInterval(timer);
      channel && supabase.removeChannel(channel);
    };
  }, [orderId]);

  if (error) {
    return (
      <div className="screen">
        <Header />
        <main className="page-content"><p>Commande introuvable.</p></main>
      </div>
    );
  }
  if (!order) {
    return (
      <div className="screen">
        <Header />
        <main className="page-content loading-state"><p>Chargement de votre commande…</p></main>
      </div>
    );
  }

  const statusIndex = trackSteps.indexOf(order.status);
  const itemCount = order.order_items?.reduce((n, i) => n + i.quantity, 0) ?? 0;
  const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(order.restaurants?.address ?? '')}`;

  return (
    <div className="screen confirmation-page">
      <Header showCart={false} />
      <main className="page-content confirmation-content">
        <CheckCircle className="success-icon" weight="duotone" />
        <h1>{order.status === 'cancelled' ? 'Commande annulée' : 'C’est commandé !'}</h1>
        <p>
          {order.status === 'ready' ? <>Votre commande est <b>prête à retirer.</b></>
            : order.status === 'picked_up' ? <>Commande retirée. <b>Bon appétit !</b></>
            : order.status === 'cancelled' ? 'Cette commande a été annulée.'
            : <>Votre bowl sera prêt vers <b>{order.pickup_slot}.</b></>}
        </p>
        <section className="pickup-code"><span>Code de retrait</span><strong>{order.code}</strong></section>
        {order.status !== 'cancelled' && (
          <div className="status-track" aria-label="Suivi de commande">
            {trackSteps.map((s, i) => (
              <div key={s} className={i <= statusIndex || order.status === 'picked_up' ? 'active' : ''}>
                <i>{i < statusIndex || order.status === 'picked_up' ? <Check /> : i + 1}</i>
                <span>{statusLabels[s]}</span>
              </div>
            ))}
          </div>
        )}
        <section className="confirmation-location">
          <MapPin />
          <div>
            <h2>Planet Bowl {order.restaurants?.name}</h2>
            <p>{order.restaurants?.address}</p>
            <a className="direction-link" href={mapsUrl} target="_blank" rel="noreferrer"><NavigationArrow />Itinéraire</a>
          </div>
        </section>
        <section className="confirmation-order">
          <img src="/images/bowl-salmon.png" alt="" />
          <span>{itemCount} × Bowl</span>
          <b>{chf(order.total)}</b>
        </section>
        <button className="receipt-link" onClick={() => navigate('/compte/commandes')}><Receipt />Mes commandes</button>
        <button className="outline-button" onClick={() => navigate('/')}>Retour à l’accueil</button>
        <small>Le statut se met à jour automatiquement — nous vous prévenons dès qu’elle est prête.</small>
      </main>
    </div>
  );
}
