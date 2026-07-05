import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, CheckCircle, ShoppingBag, UserCircle } from '@phosphor-icons/react';
import { useShop } from '../context/ShopContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { optionImage } from '../data/optionImages.js';

export function Logo() {
  const navigate = useNavigate();
  return (
    <button className="logo-button" onClick={() => navigate('/')} aria-label="Retour à l'accueil">
      <img src="/images/logo.png" alt="Planet Bowl" />
    </button>
  );
}

export function Header({ back, title, progress, showCart = true }) {
  const navigate = useNavigate();
  const { cartCount } = useShop();
  const { user } = useAuth();
  return (
    <header className="app-header">
      <div className="header-side">
        {back
          ? <button className="icon-button" onClick={() => navigate(back)} aria-label="Retour"><ArrowLeft /></button>
          : <Logo />}
      </div>
      {title ? (
        <div className="header-title"><strong>{title}</strong>{progress && <small>{progress}</small>}</div>
      ) : <div />}
      <div className="header-side header-right">
        {showCart && (
          <button className="cart-pill" onClick={() => navigate('/panier')} aria-label={`Panier, ${cartCount} article${cartCount > 1 ? 's' : ''}`}>
            <ShoppingBag weight="bold" /><span>Panier</span>{cartCount > 0 && <b>{cartCount}</b>}
          </button>
        )}
        <button className={`icon-button ${user ? 'logged-in' : ''}`} onClick={() => navigate('/compte')} aria-label={user ? 'Mon compte' : 'Se connecter'}>
          <UserCircle weight={user ? 'fill' : 'regular'} />
        </button>
      </div>
    </header>
  );
}

export function PrimaryButton({ children, onClick, disabled, type = 'button', className = '' }) {
  return (
    <button type={type} className={`primary-button ${className}`} disabled={disabled} onClick={onClick}>
      {children}<ArrowRight weight="bold" aria-hidden />
    </button>
  );
}

export function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo({ top: 0 }); }, [pathname]);
  return null;
}

export function FormField({ label, children, hint }) {
  return (
    <label className="form-field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function Notice({ tone = 'info', children }) {
  return <p className={`notice notice-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</p>;
}

export const chf = value => `CHF ${Number(value).toFixed(2)}`;

// Étape à choix unique (ex. base, protéine du Bowl Builder) : chaque option
// a son propre prix, affiché à côté du nom.
export function SingleChoiceSection({ title, options, value, onSelect }) {
  return (
    <section className="choice-section simple-options">
      <h2>{title}</h2>
      {options.map(option => (
        <button
          key={option.name}
          type="button"
          className={value === option.name ? 'selected' : ''}
          onClick={() => onSelect(option.name)}
          aria-pressed={value === option.name}
        >
          <span className="radio">{value === option.name && <i />}</span>
          <b>{option.name}</b>
          <small>{chf(option.price)}</small>
          {value === option.name && <Check />}
        </button>
      ))}
    </section>
  );
}

// Étape à choix multiple, sans limite (ex. garnitures, toppings, sauces) :
// chaque option ajoutée a un prix affiché sous forme de supplément "+ CHF".
export function MultiChoiceSection({ title, hint, options, selected, onToggle }) {
  return (
    <section className="choice-section">
      <div className="section-heading">
        <div><h2>{title}</h2>{hint && <p>{hint}</p>}</div>
      </div>
      <div className="topping-grid">
        {options.map(option => {
          const image = optionImage(option.name);
          return (
            <button
              key={option.name}
              type="button"
              className={selected.includes(option.name) ? 'selected' : ''}
              onClick={() => onToggle(option.name)}
              aria-pressed={selected.includes(option.name)}
            >
              {image
                ? <img src={image} alt={option.name} />
                : <span className="thumb-placeholder" aria-hidden />}
              <span>{option.name}</span>
              {selected.includes(option.name) ? <CheckCircle weight="fill" /> : <i />}
              <small>+ {chf(option.price)}</small>
            </button>
          );
        })}
      </div>
    </section>
  );
}

export const statusLabels = {
  received: 'Reçue',
  preparing: 'En préparation',
  ready: 'Prête à retirer',
  picked_up: 'Retirée',
  cancelled: 'Annulée',
};
