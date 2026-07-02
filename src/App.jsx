import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft, ArrowRight, Check, CheckCircle, Clock, CreditCard,
  Leaf, Lock, MapPin, Minus, NavigationArrow, Plus, Receipt,
  ShoppingBag, Storefront, UserCircle, X
} from '@phosphor-icons/react';
import '@fontsource/dm-sans/400.css';
import '@fontsource/dm-sans/600.css';
import '@fontsource/cormorant-garamond/600.css';

const restaurants = [
  { id: 'grancy', name: 'Lausanne Grancy', address: 'Boulevard de Grancy 58, 1006 Lausanne', time: '8 min', ready: '12:35', image: 'restaurant-grancy.jpg' },
  { id: 'saint-laurent', name: 'Lausanne Saint-Laurent', address: 'Rue Saint-Laurent 38, 1003 Lausanne', time: '12 min', ready: '12:40', image: 'restaurant-saint-laurent.jpg' },
  { id: 'renens', name: 'Renens', address: 'Rue de la Gare de Triage 6, 1020 Renens', time: '18 min', ready: '12:45', image: 'restaurant-renens.jpg' },
  { id: 'morges', name: 'Morges', address: 'Grand-Rue 6, 1110 Morges', time: '24 min', ready: '12:50', image: 'restaurant-morges.jpg' },
  { id: 'vevey', name: 'Vevey', address: 'Rue du Lac 8, 1800 Vevey', time: '31 min', ready: '12:55', image: 'restaurant-vevey.jpg' },
  { id: 'geneve', name: 'Genève', address: 'Rue du Môle 31, 1201 Genève', time: '42 min', ready: '13:00', image: 'restaurant-geneve.jpg' },
  { id: 'yverdon', name: 'Yverdon-les-Bains', address: 'Rue du Milieu 6, 1400 Yverdon-les-Bains', time: '45 min', ready: '13:05', image: 'restaurant-yverdon.jpg' },
  { id: 'romont', name: 'Romont', address: 'Place de la Gare 1, 1680 Romont', time: '48 min', ready: '13:10', image: 'restaurant-romont.jpg' },
  { id: 'neuvieme', name: '9e restaurant', address: 'Adresse à compléter', time: '—', ready: 'Bientôt', image: 'restaurant-grancy.jpg', disabled: true }
];

const products = [
  { id: 'compose', name: 'Compose ton bowl', detail: 'Créez votre bowl, étape par étape', price: 18, displayPrice: 'Sur mesure', category: 'À composer', tag: 'Votre choix', image: 'compose-ton-bowl.jpg' },
  { id: 'nordique', name: 'Le nordique', detail: 'Saumon · edamame · wakame', price: 22, category: 'Signature', tag: 'Best-seller', image: 'le-nordique.jpg' },
  { id: 'hawaien', name: 'L’hawaïen', detail: 'Thon · avocat · notes exotiques', price: 22, category: 'Signature', image: 'l-hawaien.jpg' },
  { id: 'atlantique', name: 'L’atlantique', detail: 'Saumon fumé · fraîcheur marine', price: 21, category: 'Signature', image: 'l-atlantique.jpg' },
  { id: 'indien', name: 'L’indien', detail: 'Poulet · légumes · saveurs épicées', price: 20, category: 'Signature', image: 'l-indien.jpg' },
  { id: 'libanais', name: 'Le libanais', detail: 'Falafel · houmous · grenade', price: 20, category: 'Signature', image: 'le-libanais.jpg' },
  { id: 'bouda', name: 'Le bouda', detail: 'Tofu · légumes · option végétale', price: 20, category: 'Signature', tag: 'Veggie', image: 'le-bouda.jpg' }
];

const toppings = ['Avocat', 'Mangue', 'Edamame', 'Concombre', 'Chou rouge', 'Wakame'];
const promos = [
  { kicker: 'OFFRE DU MOMENT', title: 'Le Duo', text: '2 bowls + 2 boissons', price: 'CHF 39.–', color: 'coral', image: 'bowl-salmon.png' },
  { kicker: 'SIGNATURE', title: 'Le nordique', text: 'Frais, généreux, prêt minute', price: 'CHF 22.–', color: 'mango', image: 'le-nordique.jpg' },
  { kicker: 'PAUSE MIDI', title: 'Prêt en 15 min', text: 'Commandez, passez, savourez', price: 'Dès CHF 20.–', color: 'green', image: 'l-indien.jpg' }
];

function Logo({ onClick }) {
  return <button className="logo-button" onClick={onClick} aria-label="Retour à l'accueil"><img src="/images/logo.png" alt="Planet Bowl" /></button>;
}

function Header({ onHome, onBack, title, cartCount = 0, onCart, progress }) {
  return <header className="app-header">
    <div className="header-side">{onBack ? <button className="icon-button" onClick={onBack} aria-label="Retour"><ArrowLeft /></button> : <Logo onClick={onHome} />}</div>
    {title ? <div className="header-title"><strong>{title}</strong>{progress && <small>{progress}</small>}</div> : null}
    <div className="header-side header-right">
      {onCart ? <button className="icon-button cart-button" onClick={onCart} aria-label={`Panier, ${cartCount} article`}><ShoppingBag />{cartCount > 0 && <span>{cartCount}</span>}</button> : <button className="icon-button" aria-label="Compte"><UserCircle /></button>}
    </div>
  </header>;
}

function PrimaryButton({ children, onClick, disabled, className = '' }) {
  return <button className={`primary-button ${className}`} disabled={disabled} onClick={onClick}>{children}<ArrowRight weight="bold" /></button>;
}

function Home({ go }) {
  const [promo, setPromo] = useState(0);
  useEffect(() => {
    const timer = window.setInterval(() => setPromo(current => (current + 1) % promos.length), 4500);
    return () => window.clearInterval(timer);
  }, []);
  const p = promos[promo];
  return <div className="screen home-screen">
    <Header onHome={() => {}} />
    <div className="service-strip"><span><i />Ouvert</span><span><Clock />Prêt en 10–15 min</span><span><MapPin />Lausanne</span></div>
    <section className="hero-card">
      <img key={p.image} className="carousel-image" src={`/images/${p.image}`} alt={p.title} />
      <div className={`promo-card ${p.color}`}>
        <b>{p.kicker}</b><h1>{p.title}</h1><p>{p.text}</p><strong>{p.price}</strong>
      </div>
      <div className="carousel-dots">{promos.map((_, i) => <button key={i} className={i === promo ? 'active' : ''} onClick={() => setPromo(i)} aria-label={`Afficher la promotion ${i + 1}`} />)}</div>
      <button className="next-promo" onClick={() => setPromo((promo + 1) % promos.length)} aria-label="Promotion suivante"><ArrowRight /></button>
    </section>
    <div className="home-action">
      <PrimaryButton onClick={() => go('locations')}>Commander maintenant</PrimaryButton>
      <button className="text-button" onClick={() => go('menu')}>Voir le menu</button>
      <div className="trust-row"><span><Leaf />Fait minute</span><span><ShoppingBag />Retrait rapide</span><span><Lock />Paiement sécurisé</span></div>
    </div>
    <section className="about-section">
      <div className="about-photo"><img src="/images/restaurant-grancy.jpg" alt="Restaurant Planet Bowl Grancy"/><span>Depuis Lausanne<br/><b>avec passion.</b></span></div>
      <div className="about-copy"><small>NOTRE HISTOIRE</small><h2>Du frais, du goût, et rien de compliqué.</h2><p>Planet Bowl est né d’une idée simple : préparer chaque jour des repas généreux, colorés et équilibrés. Nos équipes cuisinent au plus près de vous, avec la qualité des produits, l’ultra-fraîcheur et le respect au cœur de chaque bowl.</p>
        <div className="about-stats"><span><b>9</b> adresses</span><span><b>100%</b> préparé minute</span><span><b>Chaque jour</b> des produits frais</span></div>
        <button className="outline-button" onClick={() => go('locations')}>Découvrir nos restaurants</button>
      </div>
    </section>
  </div>;
}

function Locations({ selected, setSelected, go }) {
  const [query, setQuery] = useState('');
  const filtered = restaurants.filter(r => `${r.name} ${r.address}`.toLowerCase().includes(query.toLowerCase()));
  return <div className="screen">
    <Header onBack={() => go('home')} title="Votre restaurant" progress="1/4" />
    <main className="page-content location-page">
      <h1>Où venez-vous chercher votre bowl ?</h1><p className="lede">9 restaurants en Suisse romande</p>
      <button className="location-detect" onClick={() => setSelected('grancy')}><NavigationArrow />Utiliser ma position</button>
      <label className="search-field"><MapPin /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Ville ou adresse" /></label>
      <section className="location-visual" style={{backgroundImage: `url('/images/${restaurants.find(r => r.id === selected)?.image || 'restaurant-grancy.jpg'}')`}}>
        <div><MapPin weight="fill" /><span>9 adresses<br/><b>près de vous</b></span></div>
      </section>
      <h2>Les plus proches</h2>
      <div className="restaurant-list">{filtered.map(r => <button key={r.id} disabled={r.disabled} className={`restaurant-row ${selected === r.id ? 'selected' : ''}`} onClick={() => setSelected(r.id)}>
        <span className="radio">{selected === r.id && <i />}</span>
        <img src={`/images/${r.image}`} alt="" />
        <span className="restaurant-copy"><b>{r.name}</b><small>{r.address}</small><em>{r.disabled ? 'Bientôt' : '● Ouvert'} · {r.time} · {r.ready}</em></span>
      </button>)}</div>
    </main>
    <div className="sticky-footer"><PrimaryButton disabled={!selected || restaurants.find(r => r.id === selected)?.disabled} onClick={() => go('menu')}>Choisir ce restaurant</PrimaryButton></div>
  </div>;
}

function Menu({ restaurant, cart, go }) {
  const [category, setCategory] = useState('Signature');
  const visibleProducts = products.filter(product => product.category === category);
  return <div className="screen menu-screen">
    <Header onHome={() => go('home')} cartCount={cart.length} onCart={() => go('cart')} />
    <main className="page-content">
      <button className="store-chip" onClick={() => go('locations')}><MapPin />{restaurant.name.replace('Lausanne ', '')} · <span>Ouvert</span></button>
      <h1>Qu’est-ce qui vous fait envie ?</h1>
      <div className="category-row menu-categories">{['Signature','À composer'].map(c => <button key={c} className={category === c ? 'active' : ''} onClick={() => setCategory(c)}>{c}</button>)}</div>
      <button className="promo-strip">Le Duo · 2 bowls + 2 boissons · CHF 39.–<ArrowRight /></button>
      <div className="menu-source-note">Menu officiel · Planet Bowl Saint-Laurent</div>
      <div className={`product-grid ${category === 'À composer' ? 'compose-grid' : ''}`}>{visibleProducts.map((p, i) => <article className={`product-card product-${i}`} key={p.id}>
        <button className="product-image" onClick={() => go('customize', p)}><img src={`/images/${p.image}`} alt={p.name} /></button>
        {p.tag && <span className="product-tag">{p.tag}</span>}
        <h2>{p.name}</h2><p>{p.detail}</p><div><strong>{p.displayPrice || `CHF ${p.price.toFixed(2)}`}</strong><button className="add-button" onClick={() => go('customize', p)} aria-label={`Personnaliser ${p.name}`}><Plus /></button></div>
      </article>)}</div>
    </main>
    <div className="sticky-footer menu-footer"><span>{cart.length ? `${cart.length} article${cart.length > 1 ? 's' : ''}` : 'Choisissez votre bowl'}</span><button onClick={() => go('cart')}><ShoppingBag /><b>{cart.length}</b></button></div>
  </div>;
}

function Customize({ product, go, addCustomized }) {
  const [step, setStep] = useState(1);
  const [selected, setSelected] = useState(['Avocat','Mangue','Edamame','Concombre']);
  const [quantity, setQuantity] = useState(1);
  const toggle = name => setSelected(s => s.includes(name) ? s.filter(x => x !== name) : s.length < 4 ? [...s, name] : s);
  return <div className="screen">
    <Header onBack={() => go('menu')} title="Personnaliser" cartCount={0} onCart={() => go('cart')} />
    <main className="customize-page">
      <section className="product-hero"><div><h1>{product.name}</h1><strong>CHF {product.price.toFixed(2)}</strong><button>Infos & allergènes</button></div><img src={`/images/${product.image}`} alt={product.name} /></section>
      <nav className="stepper">{['Base','Protéine','Garnitures','Sauce'].map((s,i) => <button onClick={() => setStep(i+1)} className={step === i+1 ? 'active' : ''} key={s}><b>{i+1}</b><span>{s}</span></button>)}</nav>
      {step === 3 ? <section className="choice-section"><div className="section-heading"><div><h2>Choisissez vos garnitures</h2><p>4 incluses</p></div><span>{selected.length}/4 choisies</span></div>
        <div className="topping-grid">{toppings.map((t,i) => <button key={t} className={selected.includes(t) ? 'selected' : ''} onClick={() => toggle(t)}><img src="/images/bowl-salmon.png" style={{objectPosition: `${(i%3)*38+12}% ${Math.floor(i/3)*45+28}%`}} alt="" /><span>{t}</span>{selected.includes(t) ? <CheckCircle weight="fill" /> : <i />}{t === 'Wakame' && <small>+ CHF 1.50</small>}</button>)}</div>
      </section> : <section className="choice-section simple-options"><h2>{step === 1 ? 'Choisissez votre base' : step === 2 ? 'Choisissez votre protéine' : 'Choisissez votre sauce'}</h2>{(step === 1 ? ['Riz sushi','Quinoa','Salade'] : step === 2 ? ['Saumon','Poulet teriyaki','Tofu'] : ['Sésame soja','Mango spicy','Sans sauce']).map((o,i) => <button key={o} onClick={() => setStep(Math.min(step+1,4))}><span className="radio">{i === 0 && <i />}</span><b>{o}</b><Check /></button>)}</section>}
    </main>
    <div className="sticky-footer customize-footer"><div className="quantity"><button onClick={() => setQuantity(Math.max(1,quantity-1))}><Minus /></button><b>{quantity}</b><button onClick={() => setQuantity(quantity+1)}><Plus /></button></div><PrimaryButton onClick={() => addCustomized({...product, quantity, toppings:selected})}>Ajouter · CHF {(product.price*quantity).toFixed(2)}</PrimaryButton></div>
  </div>;
}

function Cart({ cart, restaurant, pickupTime, setPickupTime, go, updateQuantity }) {
  const total = useMemo(() => cart.reduce((n,item) => n + item.price * item.quantity, 0), [cart]);
  return <div className="screen">
    <Header onBack={() => go('menu')} title="Votre commande" progress="3/4" cartCount={cart.reduce((n,i)=>n+i.quantity,0)} />
    <main className="page-content cart-page">
      <section className="pickup-summary"><MapPin /><div><h2>Retrait · {restaurant.name}</h2><p>{restaurant.address}</p><span>● Ouvert</span></div><button onClick={() => go('locations')}>Modifier</button></section>
      {cart.length === 0 ? <section className="empty-cart"><ShoppingBag /><h2>Votre panier est vide</h2><button onClick={() => go('menu')}>Voir le menu</button></section> : cart.map((item,i) => <article className="cart-item" key={`${item.id}-${i}`}><img src={`/images/${item.image}`} alt="" /><div><h2>{item.name}</h2><p>{item.toppings?.join(' · ') || item.detail}</p><div className="quantity small"><button onClick={() => updateQuantity(i,-1)}><Minus /></button><b>{item.quantity}</b><button onClick={() => updateQuantity(i,1)}><Plus /></button></div></div><aside><strong>CHF {(item.price*item.quantity).toFixed(2)}</strong><button onClick={() => go('customize',item)}>Modifier</button></aside></article>)}
      {cart.length > 0 && <><label className="promo-input"><span>Code promo</span><input placeholder="Votre code"/><button>Appliquer</button></label>
        <section className="pickup-times"><h2>Quand venez-vous ?</h2><div>{['12:35','12:45','13:00'].map((t,i) => <button className={pickupTime===t?'active':''} onClick={()=>setPickupTime(t)} key={t}>{i===0 && <small>Dès que possible</small>}{t}</button>)}</div></section>
        <section className="totals"><p><span>Sous-total</span><b>CHF {total.toFixed(2)}</b></p><p><span>Frais</span><b>CHF 0.–</b></p><p className="total"><span>Total</span><b>CHF {total.toFixed(2)}</b></p></section></>}
    </main>
    {cart.length > 0 && <div className="sticky-footer pay-footer"><PrimaryButton onClick={() => go('confirmation')}>Payer CHF {total.toFixed(2)}</PrimaryButton><small><Lock />Paiement sécurisé</small></div>}
  </div>;
}

function Confirmation({ restaurant, pickupTime, cart, go }) {
  const [status, setStatus] = useState(0);
  const total = cart.reduce((n,item)=>n+item.price*item.quantity,0);
  return <div className="screen confirmation-page">
    <Header onHome={() => go('home')} />
    <main className="page-content confirmation-content"><CheckCircle className="success-icon" weight="duotone" /><h1>C’est commandé !</h1><p>Votre bowl sera prêt vers <b>{pickupTime}.</b></p>
      <section className="pickup-code"><span>Code de retrait</span><strong>PB–248</strong></section>
      <div className="status-track">{['Confirmée','En préparation','Prête à retirer'].map((s,i)=><button key={s} className={i<=status?'active':''} onClick={()=>setStatus(i)}><i>{i<status?<Check />:i+1}</i><span>{s}</span></button>)}</div>
      <section className="confirmation-location"><MapPin /><div><h2>Planet Bowl {restaurant.name}</h2><p>{restaurant.address}</p><button><NavigationArrow />Itinéraire</button></div></section>
      <section className="confirmation-order"><img src="/images/bowl-salmon.png" alt=""/><span>{cart.reduce((n,i)=>n+i.quantity,0)} × Bowl</span><b>CHF {total.toFixed(2)}</b></section>
      <button className="receipt-link"><Receipt />Voir le reçu</button>
      <button className="outline-button" onClick={()=>setStatus((status+1)%3)}>Suivre ma commande</button><small>Nous vous prévenons dès qu’elle est prête.</small>
    </main>
  </div>;
}

export function App() {
  const [screen, setScreen] = useState('home');
  const [restaurantId, setRestaurantId] = useState('grancy');
  const [product, setProduct] = useState(products[0]);
  const [cart, setCart] = useState([]);
  const [pickupTime, setPickupTime] = useState('12:35');
  const go = (next, item) => { if (item) setProduct(item); setScreen(next); window.scrollTo({top:0,behavior:'smooth'}); };
  const addQuick = item => setCart(c => [...c, {...item, quantity:1}]);
  const addCustomized = item => { setCart(c => [...c, item]); go('cart'); };
  const updateQuantity = (index, delta) => setCart(c => c.map((item,i)=>i===index?{...item,quantity:Math.max(1,item.quantity+delta)}:item));
  const restaurant = restaurants.find(r => r.id === restaurantId) || restaurants[0];
  return <div className="app-shell">
    {screen === 'home' && <Home go={go} />}
    {screen === 'locations' && <Locations selected={restaurantId} setSelected={setRestaurantId} go={go} />}
    {screen === 'menu' && <Menu restaurant={restaurant} cart={cart} go={go} />}
    {screen === 'customize' && <Customize product={product} go={go} addCustomized={addCustomized} />}
    {screen === 'cart' && <Cart cart={cart} restaurant={restaurant} pickupTime={pickupTime} setPickupTime={setPickupTime} go={go} updateQuantity={updateQuantity} />}
    {screen === 'confirmation' && <Confirmation restaurant={restaurant} pickupTime={pickupTime} cart={cart} go={go} />}
  </div>;
}
