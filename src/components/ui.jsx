import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ArrowLeft, ArrowRight, ShoppingBag, UserCircle } from '@phosphor-icons/react';
import { useShop } from '../context/ShopContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';

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
          <button className="icon-button cart-button" onClick={() => navigate('/panier')} aria-label={`Panier, ${cartCount} article${cartCount > 1 ? 's' : ''}`}>
            <ShoppingBag />{cartCount > 0 && <span>{cartCount}</span>}
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

export const statusLabels = {
  received: 'Reçue',
  preparing: 'En préparation',
  ready: 'Prête à retirer',
  picked_up: 'Retirée',
  cancelled: 'Annulée',
};
