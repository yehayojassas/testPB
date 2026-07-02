# Planet Bowl — Commande en ligne

Application complète de commande « click & collect » pour Planet Bowl :
parcours de commande mobile-first, comptes clients, stockage sécurisé des
données (Supabase / PostgreSQL avec Row Level Security) et tableau de bord
administrateur.

## Fonctionnalités

**Côté client**
- Hero promotionnel en carrousel automatique (pause au survol/focus).
- Choix du restaurant de retrait (9 adresses, recherche, restaurant favori).
- Menu officiel avec catégories, promotions et disponibilités en temps réel.
- Personnalisation du bowl en 4 étapes (base, protéine, garnitures, sauce).
- Panier persistant, choix du créneau de retrait calculé sur l'heure réelle.
- Paiement au retrait (structure prête pour une intégration Stripe).
- Confirmation avec code de retrait et **suivi de statut en temps réel**.
- Compte client : inscription, connexion, mot de passe oublié, profil,
  historique des commandes, recommande en un clic, restaurant favori,
  consentement marketing séparé et révocable, **suppression du compte**.
- Politique de confidentialité accessible (LPD / RGPD).

**Côté administrateur (`/admin`, réservé au rôle `admin`)**
- Recherche des clients, coordonnées et consentement marketing.
- Export CSV limité aux clients ayant consenti au marketing.
- Commandes filtrables par restaurant, date et statut.
- Mise à jour du statut : reçue → en préparation → prête → retirée / annulée.
- Gestion des produits : prix, promotions, badges, disponibilité, création.
- Gestion des restaurants : adresse, horaires, temps de préparation, ouverture.

## Stack

- **Frontend** : React 19 + Vite, React Router, Phosphor Icons.
- **Backend** : [Supabase](https://supabase.com) — Auth (mots de passe hachés,
  jamais stockés en clair), PostgreSQL, Row Level Security, Realtime.
- Sans configuration Supabase, l'app démarre en mode « catalogue local » :
  navigation et panier fonctionnent, mais comptes et commandes sont désactivés.

## Installation

```bash
npm install
cp .env.example .env   # puis remplir les valeurs (voir ci-dessous)
npm run dev -- --port 5173
```

Ouvrir <http://127.0.0.1:5173/>.

Build de production :

```bash
npm run build
npm run preview -- --port 5173
```

## Configuration Supabase

1. Créez un projet sur [supabase.com](https://supabase.com) (région `eu-central`
   recommandée pour l'hébergement des données en Europe).
2. Dans le **SQL Editor**, exécutez dans l'ordre :
   1. `supabase/migrations/0001_init.sql` — tables, triggers, fonctions, RLS ;
   2. `supabase/seed.sql` — restaurants et produits officiels.
3. Dans **Authentication → URL Configuration**, définissez la *Site URL*
   (ex. `http://127.0.0.1:5173` en local) pour que les liens « mot de passe
   oublié » redirigent vers `/compte/nouveau-mot-de-passe`.
4. (Optionnel) Dans **Database → Replication**, activez la publication
   Realtime pour la table `orders` afin d'obtenir le suivi de commande en
   temps réel (sinon l'app se rabat sur un rafraîchissement périodique).
5. Récupérez l'URL du projet et la clé `anon` dans **Settings → API** et
   remplissez `.env`.

### Variables d'environnement

| Variable                 | Description                                     |
| ------------------------ | ----------------------------------------------- |
| `VITE_SUPABASE_URL`      | URL du projet Supabase                          |
| `VITE_SUPABASE_ANON_KEY` | Clé publique `anon` (protégée par les RLS)      |

⚠️ `.env` est ignoré par git. Ne mettez **jamais** la clé `service_role`
dans le frontend ni dans le dépôt.

### Créer un compte administrateur

1. Inscrivez-vous normalement dans l'application (`/compte`).
2. Dans le SQL Editor de Supabase :

```sql
update public.profiles set role = 'admin' where email = 'votre-email@exemple.ch';
```

3. Reconnectez-vous : l'entrée « Tableau de bord admin » apparaît dans le
   profil et `/admin` devient accessible. Les clients ordinaires qui ouvrent
   `/admin` voient « Accès réservé » et les politiques RLS bloquent de toute
   façon leurs requêtes côté base.

## Modèle de données

| Table                  | Contenu                                              |
| ---------------------- | ---------------------------------------------------- |
| `profiles`             | Nom, prénom, e-mail, téléphone, consentement, rôle   |
| `customer_preferences` | Restaurant favori, bowl favori                       |
| `restaurants`          | Adresses, horaires, temps de préparation, activation |
| `products`             | Menu, prix, promotions, disponibilité                |
| `orders`               | Commandes, statut, créneau, totaux, code de retrait  |
| `order_items`          | Articles avec personnalisations (JSON)               |

Sécurité : RLS activée partout. Un client ne lit/modifie que ses propres
données ; le catalogue est en lecture publique ; seuls les admins
(fonction `is_admin()`, `security definer`) gèrent commandes, produits,
restaurants et consultent les clients. La suppression de compte
(`delete_own_account()`) efface en cascade profil, préférences et commandes.

## Protection des données (LPD / RGPD)

- Consentement marketing **facultatif, séparé** de l'inscription, horodaté
  et révocable depuis le profil.
- Collecte minimale : uniquement les données nécessaires à la commande.
- Droit à l'effacement : suppression du compte en self-service.
- Export CSV marketing limité aux clients consentants.
- Mots de passe hachés par Supabase Auth (bcrypt) — jamais en clair.
- Politique de confidentialité accessible sur `/confidentialite`.

## Limites avant mise en production

- **Paiement en ligne** : l'écran de paiement est prêt mais Stripe n'est pas
  branché (nécessite un endpoint serveur — Supabase Edge Function — pour
  créer les PaymentIntents). Actuellement : paiement au retrait.
- **E-mails transactionnels** : confirmation de commande / « commande prête »
  à brancher (Resend ou autre) ; seuls les e-mails Supabase Auth partent.
- **Notifications push** en option pour le suivi de commande.
- Le code promo est décoratif (table `promotions` à ajouter si besoin).
- Horaires d'ouverture stockés mais pas encore vérifiés à la commande.
- Ajouter un vrai capacity planning des créneaux si le volume augmente.
- Prévoir un domaine + HTTPS + configuration `Site URL` de production.
