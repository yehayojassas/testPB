import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft, CheckCircle, DownloadSimple, MagnifyingGlass,
  PencilSimple, Storefront, XCircle,
} from '@phosphor-icons/react';
import { Notice, chf, statusLabels } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useShop } from '../context/ShopContext.jsx';
import {
  adminFetchCustomers, adminFetchOrders, adminUpdateOrderStatus,
  adminSaveProduct, adminSaveRestaurant, customersToCsv,
} from '../lib/api.js';

const statusFlow = ['received', 'preparing', 'ready', 'picked_up', 'cancelled'];

function Customers() {
  const [search, setSearch] = useState('');
  const [customers, setCustomers] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      adminFetchCustomers({ search }).then(setCustomers).catch(() => setError('Chargement impossible.'));
    }, 250);
    return () => clearTimeout(timer);
  }, [search]);

  const exportCsv = () => {
    const csv = customersToCsv(customers);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `clients-marketing-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const consenting = customers.filter(c => c.marketing_consent).length;

  return (
    <section>
      <div className="admin-toolbar">
        <label className="search-field">
          <MagnifyingGlass aria-hidden />
          <input placeholder="Rechercher nom, e-mail, téléphone…" value={search} onChange={e => setSearch(e.target.value)} />
        </label>
        <button className="admin-action" onClick={exportCsv} disabled={consenting === 0}>
          <DownloadSimple />Exporter CSV ({consenting} consentant{consenting > 1 ? 's' : ''})
        </button>
      </div>
      {error && <Notice tone="error">{error}</Notice>}
      <p className="admin-hint">Seuls les clients ayant donné leur consentement marketing sont inclus dans l’export.</p>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>Client</th><th>E-mail</th><th>Téléphone</th><th>Marketing</th><th>Inscrit le</th></tr></thead>
          <tbody>
            {customers.map(c => (
              <tr key={c.id}>
                <td><b>{c.first_name} {c.last_name}</b></td>
                <td>{c.email}</td>
                <td>{c.phone}</td>
                <td>{c.marketing_consent
                  ? <span className="consent yes"><CheckCircle weight="fill" />Oui</span>
                  : <span className="consent no"><XCircle />Non</span>}</td>
                <td>{c.created_at?.slice(0, 10)}</td>
              </tr>
            ))}
            {customers.length === 0 && <tr><td colSpan={5} className="empty-row">Aucun client trouvé.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function Orders() {
  const { restaurants } = useShop();
  const [filters, setFilters] = useState({ restaurantId: '', status: '', date: '' });
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState(null);

  const load = () => {
    adminFetchOrders({
      restaurantId: filters.restaurantId || undefined,
      status: filters.status || undefined,
      date: filters.date || undefined,
    }).then(setOrders).catch(() => setError('Chargement impossible.'));
  };
  useEffect(load, [filters]);

  const setStatus = async (order, status) => {
    try {
      await adminUpdateOrderStatus(order.id, status);
      setOrders(list => list.map(o => o.id === order.id ? { ...o, status } : o));
    } catch {
      setError('Mise à jour du statut impossible.');
    }
  };

  return (
    <section>
      <div className="admin-toolbar filters">
        <select value={filters.restaurantId} onChange={e => setFilters(f => ({ ...f, restaurantId: e.target.value }))} aria-label="Filtrer par restaurant">
          <option value="">Tous les restaurants</option>
          {restaurants.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
        </select>
        <select value={filters.status} onChange={e => setFilters(f => ({ ...f, status: e.target.value }))} aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          {statusFlow.map(s => <option key={s} value={s}>{statusLabels[s]}</option>)}
        </select>
        <input type="date" value={filters.date} onChange={e => setFilters(f => ({ ...f, date: e.target.value }))} aria-label="Filtrer par date" />
      </div>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="admin-orders">
        {orders.map(o => (
          <article className="admin-order-card" key={o.id}>
            <header>
              <div>
                <b>{o.code}</b>
                <small>{new Date(o.created_at).toLocaleString('fr-CH')} · {o.restaurants?.name} · retrait {o.pickup_slot}</small>
                <small>{o.profiles?.first_name} {o.profiles?.last_name} · {o.profiles?.phone}</small>
              </div>
              <strong>{chf(o.total)}</strong>
            </header>
            <ul>{o.order_items?.map(i => <li key={i.id}>{i.quantity} × {i.name}</li>)}</ul>
            <div className="status-buttons" role="group" aria-label={`Statut de la commande ${o.code}`}>
              {statusFlow.map(s => (
                <button key={s} className={`status-pill status-${s} ${o.status === s ? 'current' : ''}`} onClick={() => setStatus(o, s)} aria-pressed={o.status === s}>
                  {statusLabels[s]}
                </button>
              ))}
            </div>
          </article>
        ))}
        {orders.length === 0 && <p className="empty-row">Aucune commande pour ces filtres.</p>}
      </div>
    </section>
  );
}

function ProductForm({ product, onSaved, onCancel }) {
  const [form, setForm] = useState({
    name: product?.name ?? '', description: product?.description ?? '',
    price: product?.price ?? '', promo_price: product?.promo_price ?? '',
    category: product?.category ?? 'Signature', tag: product?.tag ?? '',
    is_available: product?.is_available ?? true,
  });
  const [error, setError] = useState(null);
  const set = key => e => setForm(f => ({ ...f, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const save = async e => {
    e.preventDefault();
    try {
      await adminSaveProduct({
        id: product?.id,
        ...form,
        price: Number(form.price),
        promo_price: form.promo_price === '' ? null : Number(form.promo_price),
        tag: form.tag || null,
        ...(product ? {} : { slug: form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-'), image: 'bowl-salmon.png' }),
      });
      onSaved();
    } catch {
      setError('Enregistrement impossible.');
    }
  };

  return (
    <form className="admin-form" onSubmit={save}>
      <label><span>Nom</span><input required value={form.name} onChange={set('name')} /></label>
      <label><span>Description</span><input value={form.description} onChange={set('description')} /></label>
      <div className="form-grid">
        <label><span>Prix (CHF)</span><input required type="number" step="0.5" min="0" value={form.price} onChange={set('price')} /></label>
        <label><span>Prix promo (CHF)</span><input type="number" step="0.5" min="0" value={form.promo_price ?? ''} onChange={set('promo_price')} placeholder="—" /></label>
      </div>
      <div className="form-grid">
        <label><span>Catégorie</span>
          <select value={form.category} onChange={set('category')}>
            <option>Signature</option><option>À composer</option><option>Boissons</option><option>Desserts</option>
          </select>
        </label>
        <label><span>Badge</span><input value={form.tag} onChange={set('tag')} placeholder="Best-seller, Veggie…" /></label>
      </div>
      <label className="consent-check">
        <input type="checkbox" checked={form.is_available} onChange={set('is_available')} />
        <span>Disponible à la vente</span>
      </label>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="admin-form-actions">
        <button type="submit" className="admin-action">Enregistrer</button>
        <button type="button" className="text-button" onClick={onCancel}>Annuler</button>
      </div>
    </form>
  );
}

function Products() {
  const [products, setProducts] = useState([]);
  const [editing, setEditing] = useState(null);
  const [creating, setCreating] = useState(false);
  const { products: shopProducts } = useShop();

  const load = async () => {
    const { supabase } = await import('../lib/supabase.js');
    const { data } = await supabase.from('products').select('*').order('sort_order');
    setProducts(data ?? shopProducts);
  };
  useEffect(() => { load(); }, []);

  const done = () => { setEditing(null); setCreating(false); load(); };

  return (
    <section>
      <div className="admin-toolbar">
        <p className="admin-hint">Prix, promotions et disponibilité sont répercutés immédiatement sur le menu client.</p>
        <button className="admin-action" onClick={() => { setCreating(true); setEditing(null); }}>Nouveau produit</button>
      </div>
      {creating && <ProductForm onSaved={done} onCancel={() => setCreating(false)} />}
      <div className="admin-products">
        {products.map(p => editing === p.id
          ? <ProductForm key={p.id} product={p} onSaved={done} onCancel={() => setEditing(null)} />
          : (
            <article className={`admin-product-row ${p.is_available === false ? 'unavailable' : ''}`} key={p.id}>
              <img src={`/images/${p.image}`} alt="" />
              <div>
                <b>{p.name}</b>
                <small>{p.category}{p.tag ? ` · ${p.tag}` : ''}{p.is_available === false ? ' · Indisponible' : ''}</small>
              </div>
              <strong>{p.promo_price != null ? <><s>{chf(p.price)}</s> {chf(p.promo_price)}</> : chf(p.price)}</strong>
              <button className="icon-button" onClick={() => { setEditing(p.id); setCreating(false); }} aria-label={`Modifier ${p.name}`}><PencilSimple /></button>
            </article>
          ))}
      </div>
    </section>
  );
}

function Restaurants() {
  const [rows, setRows] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({});
  const [error, setError] = useState(null);

  const load = async () => {
    const { supabase } = await import('../lib/supabase.js');
    const { data } = await supabase.from('restaurants').select('*').order('sort_order');
    setRows(data ?? []);
  };
  useEffect(() => { load(); }, []);

  const startEdit = r => {
    setEditing(r.id);
    setForm({
      name: r.name, address: r.address, phone: r.phone ?? '',
      prep_minutes: r.prep_minutes, is_active: r.is_active,
      hours: typeof r.opening_hours === 'object' ? JSON.stringify(r.opening_hours) : r.opening_hours,
    });
  };

  const save = async e => {
    e.preventDefault();
    try {
      let opening_hours;
      try { opening_hours = JSON.parse(form.hours); } catch { opening_hours = { info: form.hours }; }
      await adminSaveRestaurant({
        id: editing,
        name: form.name, address: form.address, phone: form.phone || null,
        prep_minutes: Number(form.prep_minutes), is_active: form.is_active, opening_hours,
      });
      setEditing(null);
      load();
    } catch {
      setError('Enregistrement impossible.');
    }
  };

  return (
    <section>
      <p className="admin-hint">Horaires au format JSON, par exemple {'{"mon-fri":"10:30-21:30","sat":"11:00-21:30"}'}.</p>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="admin-products">
        {rows.map(r => editing === r.id ? (
          <form className="admin-form" key={r.id} onSubmit={save}>
            <label><span>Nom</span><input required value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} /></label>
            <label><span>Adresse</span><input required value={form.address} onChange={e => setForm(f => ({ ...f, address: e.target.value }))} /></label>
            <div className="form-grid">
              <label><span>Téléphone</span><input value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} /></label>
              <label><span>Préparation (min)</span><input type="number" min="5" value={form.prep_minutes} onChange={e => setForm(f => ({ ...f, prep_minutes: e.target.value }))} /></label>
            </div>
            <label><span>Horaires (JSON)</span><input value={form.hours} onChange={e => setForm(f => ({ ...f, hours: e.target.value }))} /></label>
            <label className="consent-check">
              <input type="checkbox" checked={form.is_active} onChange={e => setForm(f => ({ ...f, is_active: e.target.checked }))} />
              <span>Restaurant ouvert à la commande</span>
            </label>
            <div className="admin-form-actions">
              <button type="submit" className="admin-action">Enregistrer</button>
              <button type="button" className="text-button" onClick={() => setEditing(null)}>Annuler</button>
            </div>
          </form>
        ) : (
          <article className={`admin-product-row ${r.is_active ? '' : 'unavailable'}`} key={r.id}>
            <img src={`/images/${r.image}`} alt="" />
            <div>
              <b>{r.name}</b>
              <small>{r.address}{r.is_active ? '' : ' · Fermé'}</small>
            </div>
            <strong>~{r.prep_minutes} min</strong>
            <button className="icon-button" onClick={() => startEdit(r)} aria-label={`Modifier ${r.name}`}><PencilSimple /></button>
          </article>
        ))}
      </div>
    </section>
  );
}

const tabs = [
  { id: 'orders', label: 'Commandes', component: Orders },
  { id: 'customers', label: 'Clients', component: Customers },
  { id: 'products', label: 'Produits', component: Products },
  { id: 'restaurants', label: 'Restaurants', component: Restaurants },
];

export default function Admin() {
  const navigate = useNavigate();
  const { user, profile, isAdmin, loading, isSupabaseConfigured } = useAuth();
  const [tab, setTab] = useState('orders');

  const Current = useMemo(() => tabs.find(t => t.id === tab)?.component ?? Orders, [tab]);

  // Garde d'accès : la page est réservée aux administrateurs. Les données
  // restent de toute façon protégées côté base par les politiques RLS.
  if (!isSupabaseConfigured) {
    return (
      <div className="screen admin-screen">
        <main className="page-content"><Notice tone="warn">L’administration nécessite la configuration de Supabase (voir README).</Notice></main>
      </div>
    );
  }
  if (loading || (user && !profile)) {
    return <div className="screen admin-screen"><main className="page-content loading-state"><p>Vérification des accès…</p></main></div>;
  }
  if (!user || !isAdmin) {
    return (
      <div className="screen admin-screen">
        <main className="page-content admin-denied">
          <h1>Accès réservé</h1>
          <p>Cette zone est réservée à l’équipe Planet Bowl.</p>
          <button className="outline-button" onClick={() => navigate(user ? '/' : '/compte')}>
            {user ? 'Retour à l’accueil' : 'Se connecter'}
          </button>
        </main>
      </div>
    );
  }

  return (
    <div className="screen admin-screen">
      <header className="admin-header">
        <button className="icon-button" onClick={() => navigate('/')} aria-label="Retour au site"><ArrowLeft /></button>
        <div><Storefront /><b>Planet Bowl · Administration</b></div>
        <small>{profile.first_name}</small>
      </header>
      <nav className="admin-tabs" role="tablist">
        {tabs.map(t => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} className={tab === t.id ? 'active' : ''} onClick={() => setTab(t.id)}>{t.label}</button>
        ))}
      </nav>
      <main className="admin-content">
        <Current />
      </main>
    </div>
  );
}
