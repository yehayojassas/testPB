# Planète Bowl — Agent d'impression (tablette de caisse)

Cette application Android tourne en permanence sur la tablette de caisse. Elle va
chercher les nouvelles commandes toutes les 5 à 15 secondes et les imprime automatiquement
sur l'imprimante de ticket, sans intervention humaine — vous n'avez rien à cliquer pour
qu'un ticket sorte.

Ce document est écrit pour le responsable du restaurant, pas pour un technicien. Si un
réglage vous semble flou, demandez à votre prestataire technique de vous accompagner la
première fois.

## 1. Ce dont vous avez besoin avant de commencer

- La tablette de caisse (Android 8.0 ou plus récent).
- L'imprimante de ticket connectée en Wi-Fi (pas en USB, pas en Bluetooth).
- **La tablette et l'imprimante doivent être connectées au même réseau Wi-Fi.** Si
  l'imprimante est sur le Wi-Fi "invités" et la tablette sur le Wi-Fi principal, ça ne
  fonctionnera jamais.
- L'URL de l'API, le token (mot de passe technique) et l'identifiant du restaurant,
  fournis par votre prestataire technique.

## 2. Installer l'application

1. Récupérez le fichier `.apk` fourni par votre prestataire (par email, lien de
   téléchargement, ou clé USB).
2. Sur la tablette, ouvrez le fichier `.apk` téléchargé.
3. Si Android affiche un message du type "Installation bloquée" ou "Source inconnue",
   autorisez l'installation pour cette application uniquement quand c'est proposé — c'est
   normal pour une application qui n'est pas installée depuis le Play Store.
4. Une fois installée, ouvrez l'application "Planète Bowl — Agent d'impression".

## 3. Trouver l'adresse IP de l'imprimante

La plupart des imprimantes de ticket Wi-Fi peuvent imprimer une "page de test réseau" qui
affiche leur adresse IP :

- Sur beaucoup de modèles Epson : maintenez le bouton d'alimentation papier enfoncé
  pendant que vous allumez l'imprimante, ou pendant 3 secondes une fois allumée. Un ticket
  sort avec une ligne "IP Address : 192.168.x.x".
- Si ce n'est pas le cas sur votre modèle, cherchez dans le menu de l'imprimante
  (souvent accessible via un petit écran ou des boutons physiques) une section
  "Réseau" / "Network" / "Wi-Fi Status".
- En dernier recours, votre prestataire technique peut la retrouver depuis l'interface
  de votre routeur/box internet (liste des appareils connectés).

L'adresse ressemble à `192.168.1.50` : quatre nombres séparés par des points.

### Pourquoi réserver cette adresse IP (DHCP statique)

Par défaut, votre routeur peut changer l'adresse IP de l'imprimante de temps en temps (par
exemple après une coupure de courant). Si l'adresse change, l'application ne retrouvera
plus l'imprimante et l'impression s'arrêtera jusqu'à ce que vous corrigiez le réglage.

Pour éviter ça, demandez à votre prestataire (ou à la personne qui gère votre box
internet) de **réserver** cette adresse IP pour l'imprimante dans les réglages du routeur
(cherchez "réservation DHCP" ou "IP statique" dans l'interface du routeur). Une fois fait,
l'imprimante gardera toujours la même adresse, même après un redémarrage du routeur.

## 4. Configurer l'application

Ouvrez l'application, vous arrivez directement sur l'écran de réglages :

1. **URL de l'API** : collez l'adresse fournie par votre prestataire (elle commence par
   `https://`).
2. **Token API** : collez le mot de passe technique fourni. Il est masqué à l'écran par
   sécurité ; utilisez l'icône en forme d'œil pour vérifier ce que vous avez saisi avant
   d'enregistrer.
3. **Identifiant restaurant** : collez l'identifiant fourni (une longue suite de lettres
   et chiffres).
4. **Adresse IP de l'imprimante** : celle trouvée à l'étape 3.
5. **Port TCP** : laissez `9100` sauf indication contraire de votre prestataire — c'est
   le réglage standard pour la quasi-totalité des imprimantes de ticket.
6. Ajustez si besoin le **nom du point de vente** (affiché en haut de chaque ticket), la
   **largeur du ticket** (42 ou 48 caractères, selon la largeur du rouleau de papier), la
   **coupe papier automatique** et l'**ouverture du tiroir-caisse**.
7. Appuyez sur **"Enregistrer les réglages"**.

## 5. Autoriser les notifications

L'application affiche une notification permanente ("Agent actif") pendant qu'elle
surveille les commandes — c'est normal et volontaire, cela permet à Android de ne pas
arrêter l'application en arrière-plan. La première fois, Android vous demandera
l'autorisation d'afficher des notifications : acceptez-la, sinon l'agent risque d'être
arrêté par la tablette au bout de quelques minutes.

## 6. Démarrer l'agent

Une fois les réglages enregistrés, appuyez sur **"Démarrer l'agent"**. Le bandeau en haut
de l'écran passe sur "Agent actif". À partir de ce moment, les commandes qui arrivent
s'impriment automatiquement, sans que vous ayez besoin de garder l'application ouverte à
l'écran (vous pouvez verrouiller la tablette, l'agent continue de tourner).

Pour arrêter l'agent (par exemple en fin de journée), rouvrez l'application et appuyez sur
**"Arrêter l'agent"**.

Si la tablette redémarre (mise à jour, coupure de courant), l'agent redémarre
automatiquement tout seul — vous n'avez rien à refaire.

## 7. Tester avant le service

Avant l'ouverture, testez toujours les deux boutons suivants :

- **"Tester la connexion"** : vérifie que la tablette arrive bien à joindre l'imprimante
  sur le réseau, sans imprimer de ticket.
- **"Tester l'impression"** : imprime un ticket factice complet, pour vérifier que le
  papier sort bien, que la coupe et le tiroir-caisse (si activés) fonctionnent.

## 8. Diagnostiquer un problème

### "Imprimante injoignable (port fermé ou éteinte)" / port 9100 fermé

1. Vérifiez que l'imprimante est bien allumée et connectée au Wi-Fi (voyant réseau allumé
   sur l'imprimante).
2. Vérifiez que l'adresse IP saisie dans les réglages est toujours la bonne (voir étape 3
   — une adresse IP qui a changé est la cause la plus fréquente de ce message).
3. Vérifiez que la tablette est bien connectée au **même** réseau Wi-Fi que l'imprimante
   (pas au réseau "invités", pas aux données mobiles).
4. Éteignez puis rallumez l'imprimante, attendez 30 secondes, puis "Tester la connexion"
   à nouveau.

### Le ticket ne sort pas mais "Tester la connexion" fonctionne

- **Vérifiez le papier** : rouleau vide ou mal engagé.
- **Vérifiez le capot** : un capot d'imprimante mal fermé bloque souvent l'impression.
- **Vérifiez qu'aucun bourrage papier n'est visible.**
- Notez que l'application ne peut pas toujours détecter à distance un manque de papier ou
  un capot ouvert (limite technique du protocole de l'imprimante) : un contrôle visuel
  régulier reste nécessaire, surtout en cas de doute après plusieurs ventes.

### L'agent affiche "à l'arrêt" tout seul / des tickets n'arrivent pas

- Vérifiez la section "Diagnostic" de l'écran réglages : "Dernière synchronisation" doit
  afficher une heure récente. Si elle est ancienne, le Wi-Fi de la tablette ou l'accès à
  internet a probablement un problème.
- Vérifiez "Dernière erreur" pour un message explicite (jamais votre token n'y apparaît,
  cette zone est sans risque à lire ou à partager avec le support technique).

### Désactiver l'optimisation de batterie (important, sinon l'agent peut s'arrêter)

Certaines marques de tablettes/téléphones sont connues pour arrêter agressivement les
applications en arrière-plan, même quand une notification permanente est affichée. Sur les
marques suivantes, désactivez explicitement l'optimisation de batterie pour cette
application (les libellés exacts varient selon la version d'Android) :

- **Xiaomi (MIUI)** : Réglages → Applications → Gérer les applications → Planète Bowl
  Agent d'impression → Économie de batterie → "Aucune restriction". Activez aussi
  "Démarrage automatique" pour cette application.
- **Samsung (One UI)** : Réglages → Maintenance de l'appareil → Batterie → Limites
  d'utilisation en arrière-plan → retirez l'application de la liste "Applications mises en
  veille" ou "Applications non surveillées" → ajoutez-la aux applications "non surveillées".
- **Huawei (EMUI/HarmonyOS)** : Réglages → Batterie → Lancement des applications →
  cherchez l'application → désactivez la gestion automatique et activez manuellement
  "Lancement automatique", "Lancement secondaire" et "Fonctionnement en arrière-plan".

Sur les tablettes Android "standard" (Google, Lenovo, la plupart des tablettes
génériques), ce risque est plus faible mais vérifiez tout de même dans Réglages →
Batterie → Optimisation de batterie.

## 9. Récupérer des logs sans exposer votre token

Si votre prestataire technique vous demande des informations de diagnostic :

- La section "Diagnostic" de l'écran réglages (dernière synchronisation, dernière erreur)
  peut être photographiée ou recopiée sans risque : **le token n'y apparaît jamais**,
  même dans les cas d'erreur.
- Ne partagez jamais une capture d'écran de l'écran de réglages **pendant que le champ
  token est affiché en clair** (icône œil activée) — masquez-le d'abord avec l'icône
  correspondante avant toute capture ou partage d'écran.
- Si un export de fichiers de journalisation détaillés est nécessaire, il doit être
  réalisé uniquement par votre prestataire technique via les outils de développement
  Android (adb), jamais en copiant manuellement un fichier de la tablette.

## 10. Ce que cette application ne fait pas (limites connues)

- Elle ne gère pour l'instant que les commandes "à emporter" (aucun mode livraison ou sur
  place n'est proposé, car le système central ne les envoie pas encore).
- Elle ne peut pas détecter à coup sûr un manque de papier ou un capot ouvert : une
  vérification visuelle reste nécessaire en cas de doute.
- Après un plantage de la tablette pendant l'impression d'un ticket précis, ce ticket est
  automatiquement marqué "à vérifier manuellement" plutôt que réimprimé tout seul — c'est
  volontaire, pour éviter d'imprimer deux fois la même commande sans certitude.
