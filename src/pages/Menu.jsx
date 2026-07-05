import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, MapPin, Plus, ShoppingBag } from '@phosphor-icons/react';
import { Header, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';
import { CATEGORY_LABELS } from '../data/catalog.js';

// Ordre d'affichage imposé des sections du menu.
const CATEGORY_ORDER = ['signature', 'build_your_own', 'desserts', 'drinks'];
const isCustomizable = category => category === 'signature' || category === 'build_your_own';

export default function MenuPage() {
  const navigate = useNavigate();
  const { restaurant, products, optionItems, cartCount, addToCart } = useShop();

  // Catégories réellement présentes, dans l'ordre imposé.
  const categories = CATEGORY_ORDER.filter(c => products.some(p => p.category === c && p.is_available !== false));
  const [activeCat, setActiveCat] = useState(categories[0]);
  const sectionRefs = useRef({});

  // Prix plancher du Bowl Builder = base la moins chère + protéine la moins
  // chère (les deux seuls choix obligatoires), calculé depuis les prix réels.
  const builderFrom = () => {
    const bases = optionItems.filter(o => o.category === 'base').map(o => o.price);
    const proteins = optionItems.filter(o => o.category === 'protein').map(o => o.price);
    if (!bases.length || !proteins.length) return null;
    return Math.min(...bases) + Math.min(...proteins);
  };

  const quickAdd = p => addToCart({
    productId: p.id, slug: p.slug, name: p.name, image: p.image,
    unitPrice: Number(p.promo_price ?? p.price), quantity: 1, customizations: null,
  });

  // Clic sur un onglet : on défile en douceur vers la section (plus de filtrage).
  const goToCategory = c => {
    setActiveCat(c);
    document.getElementById(`menu-${c}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  // « Scroll spy » : surligne l'onglet de la section actuellement visible.
  useEffect(() => {
    const observer = new IntersectionObserver(
      entries => entries.forEach(e => { if (e.isIntersecting) setActiveCat(e.target.dataset.cat); }),
      { rootMargin: '-45% 0px -50% 0px' },
    );
    Object.values(sectionRefs.current).forEach(el => el && observer.observe(el));
    return () => observer.disconnect();
  }, [categories.join(',')]);

  const renderCard = (p, i) => {
    const customizable = isCustomizable(p.category);
    const onOpen = () => customizable ? navigate(`/personnaliser/${p.slug}`) : quickAdd(p);
    const label = customizable ? `Personnaliser ${p.name}` : `Ajouter ${p.name}`;

    // Carte « Compose ton bowl » : traitement à part, plus généreux, avec un
    // appel à l'action orange explicite au lieu du simple « + ».
    if (p.category === 'build_your_own') {
      return (
        <article className="product-card compose-card" key={p.id}>
          <button className="product-image" onClick={onOpen} aria-label={label}>
            <img src={`/images/${p.image}`} alt={p.name} loading="lazy" />
          </button>
          {p.tag && <span className="product-tag">{p.tag}</span>}
          <div className="compose-body">
            <h2>{p.name}</h2>
            <p>{p.description}</p>
            <strong>{builderFrom() != null ? `Dès ${chf(builderFrom())}` : 'Sur mesure'}</strong>
            <button className="compose-cta" onClick={onOpen}>
              Je souhaite composer mon bowl<ArrowRight weight="bold" />
            </button>
          </div>
        </article>
      );
    }

    return (
      <article className={`product-card product-${i}`} key={p.id}>
        <button className="product-image" onClick={onOpen} aria-label={label}>
          <img src={`/images/${p.image}`} alt={p.name} loading="lazy" />
        </button>
        {p.tag && <span className="product-tag">{p.tag}</span>}
        {p.promo_price != null && <span className="product-tag promo">Promo</span>}
        <h2>{p.name}</h2>
        <p>{p.description}</p>
        <div>
          <strong>
            {p.promo_price != null
              ? <><s>{chf(p.price)}</s> {chf(p.promo_price)}</>
              : chf(p.price)}
          </strong>
          <button className="add-button" onClick={onOpen} aria-label={label}><Plus /></button>
        </div>
      </article>
    );
  };

  return (
    <div className="screen menu-screen">
      <Header />
      <main className="page-content menu-content">
        <div className="menu-intro">
          <button className="store-chip" onClick={() => navigate('/restaurants')}>
            <MapPin />{restaurant ? restaurant.name.replace('Lausanne ', '') : 'Choisir un restaurant'} · <span>Ouvert</span>
          </button>
          <h1>Qu’est-ce qui vous fait envie ?</h1>
        </div>

        <nav className="category-nav" aria-label="Catégories du menu">
          <div className="category-row menu-categories" role="tablist">
            {categories.map(c => (
              <button key={c} role="tab" aria-selected={activeCat === c} className={activeCat === c ? 'active' : ''} onClick={() => goToCategory(c)}>
                {CATEGORY_LABELS[c] ?? c}
              </button>
            ))}
            <button role="tab" aria-selected={activeCat === 'duo'} className={`category-duo ${activeCat === 'duo' ? 'active' : ''}`} onClick={() => goToCategory('duo')}>
              Offres du moment
            </button>
          </div>
        </nav>

        {categories.map(c => {
          const list = products.filter(p => p.category === c && p.is_available !== false);
          return (
            <section
              key={c}
              id={`menu-${c}`}
              className="menu-section"
              data-cat={c}
              ref={el => { sectionRefs.current[c] = el; }}
            >
              <h2 className="menu-section-title">{CATEGORY_LABELS[c] ?? c}</h2>
              <div className={`product-grid ${c === 'build_your_own' ? 'compose-grid' : c === 'signature' ? 'signature-grid' : ''}`}>
                {list.map(renderCard)}
              </div>
            </section>
          );
        })}

        <section
          id="menu-duo"
          className="menu-section duo-promo-section"
          data-cat="duo"
          ref={el => { sectionRefs.current.duo = el; }}
        >
          <h2 className="menu-section-title">Offres du moment</h2>
          <div className="duo-promo">
            <div className="duo-promo-img">
              <img src="/images/bowl-salmon.png" alt="Offres du moment — 2 bowls et 2 boissons" loading="lazy" />
              <b>Offre</b>
            </div>
            <div className="duo-promo-body">
              <h3>2 bowls + 2 boissons</h3>
              <p>Choisissez 2 bowls signature et 2 boissons — à partager ou à savourer, au meilleur prix.</p>
              <strong>CHF 39.00</strong>
              <button className="duo-promo-cta" onClick={() => navigate('/offre/le-duo')}>
                Découvrir l’offre<ArrowRight weight="bold" />
              </button>
            </div>
          </div>
        </section>
      </main>

      <div className="sticky-footer menu-footer">
        <span>{cartCount ? `${cartCount} article${cartCount > 1 ? 's' : ''}` : 'Choisissez votre bowl'}</span>
        <button onClick={() => navigate('/panier')} aria-label={`Voir le panier, ${cartCount} article${cartCount > 1 ? 's' : ''}`}>
          <ShoppingBag /><span>Panier</span><b>{cartCount}</b>
        </button>
      </div>
    </div>
  );
}
