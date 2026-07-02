# Planet Bowl — prototype navigable

Prototype mobile complet du parcours de commande :

- hero promotionnel ;
- choix du restaurant ;
- menu et catégories ;
- personnalisation du bowl ;
- panier et créneau de retrait ;
- confirmation et suivi de commande.

## Lancer le projet

```bash
npm install
npm run dev -- --port 5173
```

Puis ouvrir : http://127.0.0.1:5173/

## Build de production

```bash
npm run build
npm run preview -- --port 5173
```

Le dossier `dist/` inclus dans l'archive contient déjà un build de production.

## Données à confirmer

Le site public Planet Bowl liste actuellement huit adresses. L'interface est prévue pour neuf restaurants ; la neuvième adresse est volontairement indiquée comme donnée à compléter dans `src/App.jsx`.
