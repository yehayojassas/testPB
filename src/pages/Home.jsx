import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Clock, Leaf, Lock, MapPin, ShoppingBag } from '@phosphor-icons/react';
import { Header, PrimaryButton } from '../components/ui.jsx';
import { promos } from '../data/catalog.js';

export default function Home() {
  const navigate = useNavigate();
  const [promo, setPromo] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (paused) return;
    const timer = window.setInterval(() => setPromo(current => (current + 1) % promos.length), 4500);
    return () => window.clearInterval(timer);
  }, [paused]);

  const p = promos[promo];
  return (
    <div className="screen home-screen">
      <Header />
      <div className="service-strip">
        <span><i />Ouvert</span>
        <span><Clock />Prêt en 10–15 min</span>
        <span><MapPin />Suisse romande</span>
      </div>
      <section
        className="hero-card"
        aria-roledescription="carrousel"
        aria-label="Promotions"
        onMouseEnter={() => setPaused(true)}
        onMouseLeave={() => setPaused(false)}
        onFocusCapture={() => setPaused(true)}
        onBlurCapture={() => setPaused(false)}
      >
        <img key={p.image} className="carousel-image" src={`/images/${p.image}`} alt={p.title} />
        <div className={`promo-card ${p.color}`}>
          <b>{p.kicker}</b><h1>{p.title}</h1><p>{p.text}</p><strong>{p.price}</strong>
        </div>
        <div className="carousel-dots">
          {promos.map((_, i) => (
            <button key={i} className={i === promo ? 'active' : ''} onClick={() => setPromo(i)} aria-label={`Afficher la promotion ${i + 1}`} aria-current={i === promo} />
          ))}
        </div>
        <button className="next-promo" onClick={() => setPromo((promo + 1) % promos.length)} aria-label="Promotion suivante"><ArrowRight /></button>
      </section>
      <div className="home-action">
        <PrimaryButton onClick={() => navigate('/restaurants')}>Commander maintenant</PrimaryButton>
        <button className="text-button" onClick={() => navigate('/menu')}>Voir le menu</button>
        <div className="trust-row">
          <span><Leaf />Fait minute</span>
          <span><ShoppingBag />Retrait rapide</span>
          <span><Lock />Paiement sécurisé</span>
        </div>
      </div>
      <section className="about-section">
        <div className="about-photo">
          <img src="/images/restaurant-grancy.jpg" alt="Restaurant Planet Bowl Grancy" loading="lazy" />
          <span>Depuis Lausanne<br /><b>avec passion.</b></span>
        </div>
        <div className="about-copy">
          <small>NOTRE HISTOIRE</small>
          <h2>Du frais, du goût, et rien de compliqué.</h2>
          <p>Planet Bowl est né d’une idée simple : préparer chaque jour des repas généreux, colorés et équilibrés. Nos équipes cuisinent au plus près de vous, avec la qualité des produits, l’ultra-fraîcheur et le respect au cœur de chaque bowl.</p>
          <div className="about-stats">
            <span><b>9</b> adresses</span>
            <span><b>100%</b> préparé minute</span>
            <span><b>Chaque jour</b> des produits frais</span>
          </div>
          <button className="outline-button" onClick={() => navigate('/restaurants')}>Découvrir nos restaurants</button>
        </div>
      </section>
      <footer className="site-footer">
        <button onClick={() => navigate('/confidentialite')}>Politique de confidentialité</button>
        <span>© {new Date().getFullYear()} Planet Bowl</span>
      </footer>
    </div>
  );
}
