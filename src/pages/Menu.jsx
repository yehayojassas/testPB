import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, MapPin, Plus, ShoppingBag } from '@phosphor-icons/react';
import { Header, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';

export default function MenuPage() {
  const navigate = useNavigate();
  const { restaurant, products, cartCount } = useShop();
  const [category, setCategory] = useState('Signature');

  const categories = [...new Set(products.map(p => p.category))].sort((a, b) =>
    a === 'Signature' ? -1 : b === 'Signature' ? 1 : 0);
  const visibleProducts = products.filter(p => p.category === category && p.is_available !== false);

  return (
    <div className="screen menu-screen">
      <Header />
      <main className="page-content">
        <button className="store-chip" onClick={() => navigate('/restaurants')}>
          <MapPin />{restaurant ? restaurant.name.replace('Lausanne ', '') : 'Choisir un restaurant'} · <span>Ouvert</span>
        </button>
        <h1>Qu’est-ce qui vous fait envie ?</h1>
        <div className="category-row menu-categories" role="tablist" aria-label="Catégories">
          {categories.map(c => (
            <button key={c} role="tab" aria-selected={category === c} className={category === c ? 'active' : ''} onClick={() => setCategory(c)}>{c}</button>
          ))}
        </div>
        <button className="promo-strip" onClick={() => navigate('/restaurants')}>
          Le Duo · 2 bowls + 2 boissons · CHF 39.–<ArrowRight />
        </button>
        <div className="menu-source-note">Menu officiel · Planet Bowl</div>
        <div className={`product-grid ${category === 'À composer' ? 'compose-grid' : ''}`}>
          {visibleProducts.map((p, i) => (
            <article className={`product-card product-${i}`} key={p.id}>
              <button className="product-image" onClick={() => navigate(`/personnaliser/${p.slug}`)} aria-label={`Personnaliser ${p.name}`}>
                <img src={`/images/${p.image}`} alt={p.name} loading="lazy" />
              </button>
              {p.tag && <span className="product-tag">{p.tag}</span>}
              {p.promo_price != null && <span className="product-tag promo">Promo</span>}
              <h2>{p.name}</h2>
              <p>{p.description}</p>
              <div>
                <strong>
                  {p.category === 'À composer'
                    ? 'Sur mesure'
                    : p.promo_price != null
                      ? <><s>{chf(p.price)}</s> {chf(p.promo_price)}</>
                      : chf(p.price)}
                </strong>
                <button className="add-button" onClick={() => navigate(`/personnaliser/${p.slug}`)} aria-label={`Personnaliser ${p.name}`}><Plus /></button>
              </div>
            </article>
          ))}
        </div>
      </main>
      <div className="sticky-footer menu-footer">
        <span>{cartCount ? `${cartCount} article${cartCount > 1 ? 's' : ''}` : 'Choisissez votre bowl'}</span>
        <button onClick={() => navigate('/panier')} aria-label={`Voir le panier, ${cartCount} article${cartCount > 1 ? 's' : ''}`}>
          <ShoppingBag /><b>{cartCount}</b>
        </button>
      </div>
    </div>
  );
}
