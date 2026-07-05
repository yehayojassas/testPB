import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle } from '@phosphor-icons/react';
import { Header, PrimaryButton, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';

// Offre « Le Duo » : 2 bowls signature + 2 boissons au choix, prix fixe.
const DUO_PRICE = 39;

export default function DuoOffer() {
  const navigate = useNavigate();
  const { products, addToCart } = useShop();

  const bowls = products.filter(p => p.category === 'signature' && p.is_available !== false);
  const drinks = products.filter(p => p.category === 'drinks' && p.is_available !== false);

  // On garde les slugs sélectionnés (avec répétition possible : on peut
  // prendre deux fois le même bowl). Chaque clic ajoute une unité ; on
  // retire en cliquant sur le badge d'une carte déjà choisie.
  const [selBowls, setSelBowls] = useState([]);
  const [selDrinks, setSelDrinks] = useState([]);

  const pick = (setList, slug, max) =>
    setList(prev => (prev.length >= max ? prev : [...prev, slug]));
  const unpick = (setList, slug) =>
    setList(prev => {
      const i = prev.lastIndexOf(slug);
      return i === -1 ? prev : prev.filter((_, idx) => idx !== i);
    });
  const count = (list, slug) => list.filter(s => s === slug).length;

  const complete = selBowls.length === 2 && selDrinks.length === 2;
  const nameOf = (slug, arr) => arr.find(p => p.slug === slug)?.name ?? slug;

  const add = () => {
    const bundle = [
      ...selBowls.map(s => nameOf(s, bowls)),
      ...selDrinks.map(s => nameOf(s, drinks)),
    ];
    addToCart({
      productId: null,
      slug: 'le-duo',
      name: 'Offres du moment',
      image: 'bowl-salmon.png',
      unitPrice: DUO_PRICE,
      quantity: 1,
      customizations: { bundle },
    });
    navigate('/panier');
  };

  const renderGrid = (items, list, setList, max) => (
    <div className="topping-grid duo-grid">
      {items.map(p => {
        const n = count(list, p.slug);
        const full = list.length >= max && n === 0;
        return (
          <button
            key={p.slug}
            type="button"
            className={n > 0 ? 'selected' : ''}
            disabled={full}
            onClick={() => n > 0 ? unpick(setList, p.slug) : pick(setList, p.slug, max)}
            aria-pressed={n > 0}
            aria-label={`${p.name}${n > 0 ? `, sélectionné ${n} fois` : ''}`}
          >
            <img src={`/images/${p.image}`} alt={p.name} loading="lazy" />
            <span>{p.name}</span>
            {n > 0 ? <CheckCircle weight="fill" /> : <i />}
            {n > 1 && <small className="duo-count">×{n}</small>}
          </button>
        );
      })}
    </div>
  );

  return (
    <div className="screen">
      <Header back="/menu" title="Offres du moment" />
      <main className="page-content duo-page">
        <div className="duo-hero">
          <h1>Offres du moment</h1>
          <p className="lede">2 bowls + 2 boissons pour <strong>{chf(DUO_PRICE)}</strong></p>
        </div>

        <section className="duo-step">
          <div className="section-heading"><div><h2>Vos 2 bowls</h2></div><span>{selBowls.length}/2</span></div>
          {renderGrid(bowls, selBowls, setSelBowls, 2)}
        </section>

        <section className="duo-step">
          <div className="section-heading"><div><h2>Vos 2 boissons</h2></div><span>{selDrinks.length}/2</span></div>
          {renderGrid(drinks, selDrinks, setSelDrinks, 2)}
        </section>
      </main>

      <div className="sticky-footer">
        <PrimaryButton disabled={!complete} onClick={add}>
          {complete ? `Ajouter · ${chf(DUO_PRICE)}` : 'Choisissez 2 bowls et 2 boissons'}
        </PrimaryButton>
      </div>
    </div>
  );
}
