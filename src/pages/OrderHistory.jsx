import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowClockwise, Receipt } from '@phosphor-icons/react';
import { Header, Notice, chf, statusLabels } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useShop } from '../context/ShopContext.jsx';
import { fetchMyOrders } from '../lib/api.js';

export default function OrderHistory() {
  const navigate = useNavigate();
  const { user, loading } = useAuth();
  const { addToCart, setRestaurantId } = useShop();
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) { navigate('/compte', { replace: true }); return; }
    fetchMyOrders(user.id).then(setOrders).catch(() => setError(true));
  }, [user, loading]);

  // Recommander : recharge les articles d'une commande passée dans le panier
  const reorder = order => {
    order.order_items.forEach(item => {
      addToCart({
        productId: item.product_id,
        slug: item.product_id ?? item.name,
        name: item.name,
        image: 'bowl-salmon.png',
        unitPrice: Number(item.unit_price),
        quantity: item.quantity,
        customizations: item.customizations,
      });
    });
    setRestaurantId(order.restaurant_id);
    navigate('/panier');
  };

  return (
    <div className="screen">
      <Header back="/compte" title="Mes commandes" />
      <main className="page-content orders-page">
        {error && <Notice tone="error">Impossible de charger vos commandes.</Notice>}
        {!orders && !error && <p className="loading-state">Chargement…</p>}
        {orders?.length === 0 && (
          <section className="empty-cart">
            <Receipt />
            <h2>Aucune commande pour l’instant</h2>
            <button onClick={() => navigate('/menu')}>Découvrir le menu</button>
          </section>
        )}
        {orders?.map(order => (
          <article className="order-card" key={order.id}>
            <header>
              <div>
                <b>{order.code}</b>
                <small>{new Date(order.created_at).toLocaleDateString('fr-CH', { day: 'numeric', month: 'long', year: 'numeric' })} · {order.restaurants?.name}</small>
              </div>
              <span className={`status-pill status-${order.status}`}>{statusLabels[order.status]}</span>
            </header>
            <ul>
              {order.order_items.map(item => (
                <li key={item.id}>
                  <span>{item.quantity} × {item.name}</span>
                  <b>{chf(item.unit_price * item.quantity)}</b>
                </li>
              ))}
            </ul>
            <footer>
              <strong>Total {chf(order.total)}</strong>
              <div>
                <button className="outline-button small" onClick={() => navigate(`/confirmation/${order.id}`)}>Détails</button>
                <button className="reorder-button" onClick={() => reorder(order)}><ArrowClockwise />Recommander</button>
              </div>
            </footer>
          </article>
        ))}
      </main>
    </div>
  );
}
