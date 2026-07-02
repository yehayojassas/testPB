import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, MapPin, NavigationArrow } from '@phosphor-icons/react';
import { Header, PrimaryButton } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';

export default function Locations() {
  const navigate = useNavigate();
  const { restaurants, restaurantId, setRestaurantId, favoriteRestaurantId } = useShop();
  const { user } = useAuth();
  const [query, setQuery] = useState('');

  const filtered = restaurants.filter(r => `${r.name} ${r.address}`.toLowerCase().includes(query.toLowerCase()));
  const selected = restaurants.find(r => r.id === restaurantId);
  const sorted = [...filtered].sort((a, b) => {
    if (a.id === favoriteRestaurantId) return -1;
    if (b.id === favoriteRestaurantId) return 1;
    return (a.sort_order ?? 0) - (b.sort_order ?? 0);
  });

  return (
    <div className="screen">
      <Header back="/" title="Votre restaurant" progress="Étape 1 sur 4" />
      <main className="page-content location-page">
        <h1>Où venez-vous chercher votre bowl ?</h1>
        <p className="lede">9 restaurants en Suisse romande</p>
        <button className="location-detect" onClick={() => setRestaurantId(restaurants[0]?.id)}>
          <NavigationArrow />Utiliser ma position
        </button>
        <label className="search-field">
          <MapPin aria-hidden />
          <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Ville ou adresse" aria-label="Rechercher un restaurant" />
        </label>
        <section className="location-visual" style={{ backgroundImage: `url('/images/${selected?.image || 'restaurant-grancy.jpg'}')` }}>
          <div><MapPin weight="fill" /><span>9 adresses<br /><b>près de vous</b></span></div>
        </section>
        <h2>Les plus proches</h2>
        <div className="restaurant-list">
          {sorted.map(r => (
            <button
              key={r.id}
              disabled={r.is_active === false}
              className={`restaurant-row ${restaurantId === r.id ? 'selected' : ''}`}
              onClick={() => setRestaurantId(r.id)}
            >
              <span className="radio">{restaurantId === r.id && <i />}</span>
              <img src={`/images/${r.image}`} alt="" loading="lazy" />
              <span className="restaurant-copy">
                <b>{r.name}{user && r.id === favoriteRestaurantId && <Heart weight="fill" className="favorite-heart" aria-label="Restaurant favori" />}</b>
                <small>{r.address}</small>
                <em>{r.is_active === false ? 'Bientôt' : '● Ouvert'} · prêt en ~{r.prep_minutes ?? 15} min</em>
              </span>
            </button>
          ))}
        </div>
      </main>
      <div className="sticky-footer">
        <PrimaryButton disabled={!selected || selected.is_active === false} onClick={() => navigate('/menu')}>
          Choisir ce restaurant
        </PrimaryButton>
      </div>
    </div>
  );
}
