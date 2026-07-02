import { Header } from '../components/ui.jsx';

export default function Privacy() {
  return (
    <div className="screen">
      <Header back="/" title="Confidentialité" showCart={false} />
      <main className="page-content privacy-page">
        <h1>Politique de confidentialité</h1>
        <p className="lede">Conforme à la LPD suisse et au RGPD européen.</p>

        <h2>Quelles données collectons-nous ?</h2>
        <p>Uniquement le nécessaire au traitement de vos commandes : nom, prénom, adresse e-mail, numéro de téléphone et historique de commandes. Votre mot de passe est haché de manière irréversible — personne, pas même notre équipe, ne peut le lire.</p>

        <h2>Pourquoi ?</h2>
        <p>Pour préparer vos commandes, vous prévenir quand elles sont prêtes et, <b>seulement si vous l’avez accepté</b>, vous envoyer nos offres. Le consentement marketing est facultatif, séparé de la création du compte, et révocable à tout moment depuis votre profil.</p>

        <h2>Qui y a accès ?</h2>
        <p>Vos données sont hébergées de manière sécurisée (Supabase / PostgreSQL, chiffrement en transit et au repos) et protégées par des règles d’accès strictes : vous seul·e accédez à votre profil, et seul le personnel autorisé de Planet Bowl accède aux commandes. Nous ne vendons ni ne partageons vos données.</p>

        <h2>Vos droits</h2>
        <p>Vous pouvez consulter et corriger vos données dans votre profil, retirer votre consentement marketing d’un simple clic et <b>supprimer définitivement votre compte</b> (profil, préférences et historique) depuis la page « Mon compte ». Pour toute question : privacy@planetbowl.ch.</p>

        <h2>Durée de conservation</h2>
        <p>Vos données sont conservées tant que votre compte est actif. La suppression du compte efface immédiatement l’ensemble de vos données personnelles.</p>
      </main>
    </div>
  );
}
