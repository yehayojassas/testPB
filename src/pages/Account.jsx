import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ClockCounterClockwise, Heart, ShieldCheck, SignOut, Trash } from '@phosphor-icons/react';
import { Header, PrimaryButton, FormField, Notice } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useShop } from '../context/ShopContext.jsx';
import { fetchPreferences, savePreferences } from '../lib/api.js';

function AuthForms() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from ?? '/compte';
  const { signIn, signUp, resetPassword, isSupabaseConfigured } = useAuth();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '', password: '', marketingConsent: false });
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = key => e => setForm(f => ({ ...f, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const submit = async e => {
    e.preventDefault();
    setError(null); setMessage(null); setBusy(true);
    try {
      if (mode === 'login') {
        await signIn({ email: form.email, password: form.password });
        navigate(from, { replace: true });
      } else if (mode === 'register') {
        const { session } = await signUp(form);
        if (session) navigate(from, { replace: true });
        else setMessage('Compte créé ! Vérifiez votre boîte e-mail pour confirmer votre adresse, puis connectez-vous.');
      } else {
        await resetPassword(form.email);
        setMessage('Si un compte existe pour cette adresse, un e-mail de réinitialisation vient d’être envoyé.');
      }
    } catch (err) {
      setError(
        err?.message === 'Invalid login credentials' ? 'E-mail ou mot de passe incorrect.'
          : err?.message?.includes('already registered') ? 'Un compte existe déjà avec cette adresse.'
          : err?.message?.includes('at least 6') ? 'Le mot de passe doit contenir au moins 6 caractères.'
          : 'Une erreur est survenue. Merci de réessayer.'
      );
    } finally {
      setBusy(false);
    }
  };

  if (!isSupabaseConfigured) {
    return (
      <main className="page-content account-page">
        <h1>Votre compte</h1>
        <Notice tone="warn">
          Les comptes clients nécessitent la configuration de Supabase.
          Suivez la section « Configuration Supabase » du README, puis relancez l’application.
        </Notice>
      </main>
    );
  }

  return (
    <main className="page-content account-page">
      <h1>{mode === 'login' ? 'Content de vous revoir' : mode === 'register' ? 'Créer votre compte' : 'Mot de passe oublié'}</h1>
      <p className="lede">
        {mode === 'login' ? 'Connectez-vous pour commander plus vite.'
          : mode === 'register' ? 'Vos bowls préférés, en deux gestes.'
          : 'Nous vous enverrons un lien de réinitialisation.'}
      </p>
      <div className="auth-tabs" role="tablist">
        <button role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Connexion</button>
        <button role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Inscription</button>
      </div>
      <form className="auth-form" onSubmit={submit}>
        {mode === 'register' && (
          <div className="form-grid">
            <FormField label="Prénom"><input required autoComplete="given-name" value={form.firstName} onChange={set('firstName')} /></FormField>
            <FormField label="Nom"><input required autoComplete="family-name" value={form.lastName} onChange={set('lastName')} /></FormField>
          </div>
        )}
        <FormField label="E-mail"><input required type="email" autoComplete="email" value={form.email} onChange={set('email')} /></FormField>
        {mode === 'register' && (
          <FormField label="Téléphone" hint="Pour vous prévenir quand la commande est prête.">
            <input required type="tel" autoComplete="tel" placeholder="+41 79 000 00 00" value={form.phone} onChange={set('phone')} />
          </FormField>
        )}
        {mode !== 'forgot' && (
          <FormField label="Mot de passe" hint={mode === 'register' ? 'Au moins 8 caractères.' : undefined}>
            <input required type="password" minLength={mode === 'register' ? 8 : undefined} autoComplete={mode === 'register' ? 'new-password' : 'current-password'} value={form.password} onChange={set('password')} />
          </FormField>
        )}
        {mode === 'register' && (
          <label className="consent-check">
            <input type="checkbox" checked={form.marketingConsent} onChange={set('marketingConsent')} />
            <span>J’accepte de recevoir les offres et nouveautés Planet Bowl par e-mail. <b>(Facultatif — révocable à tout moment.)</b></span>
          </label>
        )}
        {error && <Notice tone="error">{error}</Notice>}
        {message && <Notice tone="success">{message}</Notice>}
        <PrimaryButton type="submit" disabled={busy}>
          {busy ? 'Un instant…' : mode === 'login' ? 'Se connecter' : mode === 'register' ? 'Créer mon compte' : 'Envoyer le lien'}
        </PrimaryButton>
        {mode === 'login' && <button type="button" className="text-button" onClick={() => setMode('forgot')}>Mot de passe oublié ?</button>}
        {mode === 'forgot' && <button type="button" className="text-button" onClick={() => setMode('login')}>Retour à la connexion</button>}
        {mode === 'register' && (
          <p className="privacy-note">
            En créant un compte, vous acceptez notre <button type="button" className="inline-link" onClick={() => navigate('/confidentialite')}>politique de confidentialité</button>.
            Nous ne collectons que le strict nécessaire au traitement de vos commandes.
          </p>
        )}
      </form>
    </main>
  );
}

function ProfileView() {
  const navigate = useNavigate();
  const { user, profile, isAdmin, signOut, updateProfile, deleteAccount } = useAuth();
  const { restaurants, favoriteRestaurantId, setFavoriteRestaurantId } = useShop();
  const [form, setForm] = useState(null);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    if (profile && !form) {
      setForm({
        first_name: profile.first_name ?? '',
        last_name: profile.last_name ?? '',
        phone: profile.phone ?? '',
        marketing_consent: profile.marketing_consent,
      });
    }
  }, [profile]);

  useEffect(() => {
    fetchPreferences(user.id).then(prefs => {
      if (prefs?.favorite_restaurant_id) setFavoriteRestaurantId(prefs.favorite_restaurant_id);
    });
  }, [user.id]);

  const set = key => e => setForm(f => ({ ...f, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const save = async e => {
    e.preventDefault();
    setMessage(null); setError(null);
    try {
      await updateProfile({
        ...form,
        marketing_consent_at: form.marketing_consent && !profile.marketing_consent ? new Date().toISOString() : profile.marketing_consent_at,
      });
      setMessage('Profil mis à jour.');
    } catch {
      setError('La mise à jour a échoué. Merci de réessayer.');
    }
  };

  const saveFavorite = async id => {
    const next = id === favoriteRestaurantId ? null : id;
    setFavoriteRestaurantId(next);
    try {
      await savePreferences(user.id, { favorite_restaurant_id: next });
    } catch {
      setError('Impossible d’enregistrer le favori.');
    }
  };

  const removeAccount = async () => {
    try {
      await deleteAccount();
      navigate('/');
    } catch {
      setError('La suppression a échoué. Contactez-nous si le problème persiste.');
      setConfirmDelete(false);
    }
  };

  if (!profile || !form) {
    return <main className="page-content loading-state"><p>Chargement de votre profil…</p></main>;
  }

  return (
    <main className="page-content account-page">
      <h1>Bonjour {profile.first_name || ''} !</h1>
      <p className="lede">{profile.email}</p>

      <div className="account-links">
        <button onClick={() => navigate('/compte/commandes')}><ClockCounterClockwise />Mes commandes<span /></button>
        {isAdmin && <button onClick={() => navigate('/admin')}><ShieldCheck />Tableau de bord admin<span /></button>}
      </div>

      <form className="auth-form" onSubmit={save}>
        <h2>Mes informations</h2>
        <div className="form-grid">
          <FormField label="Prénom"><input required value={form.first_name} onChange={set('first_name')} /></FormField>
          <FormField label="Nom"><input required value={form.last_name} onChange={set('last_name')} /></FormField>
        </div>
        <FormField label="Téléphone"><input type="tel" value={form.phone} onChange={set('phone')} /></FormField>
        <label className="consent-check">
          <input type="checkbox" checked={form.marketing_consent} onChange={set('marketing_consent')} />
          <span>Recevoir les offres et nouveautés par e-mail. <b>(Révocable à tout moment.)</b></span>
        </label>
        {message && <Notice tone="success">{message}</Notice>}
        {error && <Notice tone="error">{error}</Notice>}
        <PrimaryButton type="submit">Enregistrer</PrimaryButton>
      </form>

      <section className="favorite-section">
        <h2>Mon restaurant favori</h2>
        <p>Il sera présélectionné à chaque commande.</p>
        <div className="favorite-list">
          {restaurants.filter(r => r.is_active !== false).map(r => (
            <button key={r.id} className={r.id === favoriteRestaurantId ? 'selected' : ''} onClick={() => saveFavorite(r.id)} aria-pressed={r.id === favoriteRestaurantId}>
              <Heart weight={r.id === favoriteRestaurantId ? 'fill' : 'regular'} />{r.name}
            </button>
          ))}
        </div>
      </section>

      <section className="danger-zone">
        <button className="text-button" onClick={signOut}><SignOut />Se déconnecter</button>
        {confirmDelete ? (
          <div className="delete-confirm">
            <p><b>Supprimer définitivement votre compte ?</b> Profil, préférences et historique de commandes seront effacés. Cette action est irréversible.</p>
            <div>
              <button className="danger-button" onClick={removeAccount}>Oui, tout supprimer</button>
              <button className="text-button" onClick={() => setConfirmDelete(false)}>Annuler</button>
            </div>
          </div>
        ) : (
          <button className="text-button danger" onClick={() => setConfirmDelete(true)}><Trash />Supprimer mon compte et mes données</button>
        )}
      </section>
    </main>
  );
}

export default function Account() {
  const { user, loading } = useAuth();
  return (
    <div className="screen">
      <Header back="/" title="Mon compte" showCart={false} />
      {loading
        ? <main className="page-content loading-state"><p>Chargement…</p></main>
        : user ? <ProfileView /> : <AuthForms />}
    </div>
  );
}
