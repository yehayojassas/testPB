import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header, PrimaryButton, FormField, Notice } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';

// Page cible du lien « mot de passe oublié » envoyé par e-mail.
export default function ResetPassword() {
  const navigate = useNavigate();
  const { updatePassword } = useAuth();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async e => {
    e.preventDefault();
    if (password !== confirm) {
      setError('Les deux mots de passe ne correspondent pas.');
      return;
    }
    setError(null); setBusy(true);
    try {
      await updatePassword(password);
      navigate('/compte', { replace: true });
    } catch {
      setError('Le lien a peut-être expiré. Redemandez un e-mail de réinitialisation.');
      setBusy(false);
    }
  };

  return (
    <div className="screen">
      <Header back="/compte" title="Nouveau mot de passe" showCart={false} />
      <main className="page-content account-page">
        <h1>Choisissez un nouveau mot de passe</h1>
        <form className="auth-form" onSubmit={submit}>
          <FormField label="Nouveau mot de passe" hint="Au moins 8 caractères.">
            <input required type="password" minLength={8} autoComplete="new-password" value={password} onChange={e => setPassword(e.target.value)} />
          </FormField>
          <FormField label="Confirmez le mot de passe">
            <input required type="password" autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} />
          </FormField>
          {error && <Notice tone="error">{error}</Notice>}
          <PrimaryButton type="submit" disabled={busy}>{busy ? 'Un instant…' : 'Enregistrer'}</PrimaryButton>
        </form>
      </main>
    </div>
  );
}
