# print-agent

Edge Function consommée par la tablette Android connectée à l'imprimante de
caisse (TCP/ESC-POS, port 9100). Elle expose une file d'attente de tickets à
imprimer (`print_jobs`, voir `supabase/migrations/0006_print_agent.sql`) avec
un mécanisme de claim + idempotence pour éviter les doubles impressions.

Un ticket est mis en file automatiquement quand une commande passe de
`received` à `preparing` (déclencheur SQL, aucun appel API nécessaire pour
ça).

## Déploiement

```bash
supabase functions deploy print-agent
```

## Secret d'authentification

La tablette s'authentifie avec un token statique partagé, jamais commité.
À définir une fois par projet Supabase :

```bash
supabase secrets set PRINT_AGENT_TOKEN=<valeur-longue-et-aleatoire>
```

`SUPABASE_URL` et `SUPABASE_SERVICE_ROLE_KEY` sont injectées automatiquement
par le runtime des Edge Functions, rien à configurer pour celles-ci.

## Test en local

```bash
supabase start
supabase functions serve print-agent --env-file supabase/functions/print-agent/.env.local
```

(`.env.local`, non commité, doit contenir au minimum `PRINT_AGENT_TOKEN=...`.)

Toutes les routes ci-dessous sont préfixées par
`http://127.0.0.1:54321/functions/v1/print-agent` en local, ou
`https://<projet>.supabase.co/functions/v1/print-agent` en production.

## Exemples curl

Variables utilisées dans les exemples :

```bash
export PRINT_AGENT_TOKEN="votre-token"
export BASE_URL="http://127.0.0.1:54321/functions/v1/print-agent"
export RESTAURANT_ID="00000000-0000-0000-0000-000000000000"
export ORDER_ID="11111111-1111-1111-1111-111111111111"
```

### 1. Lister les tickets à imprimer

```bash
curl -s "$BASE_URL/jobs?restaurantId=$RESTAURANT_ID&limit=20" \
  -H "Authorization: Bearer $PRINT_AGENT_TOKEN"
```

Réponse `200` : un tableau JSON brut (pas d'enveloppe), un objet par commande.

### 2. Réclamer un ticket (claim)

Pas de `restaurantId` ici : la tablette ne le connaît déjà que via `/jobs`
(filtré côté serveur) ; `orderId` seul identifie le job à réclamer.

```bash
curl -s -X POST "$BASE_URL/jobs/$ORDER_ID/claim" \
  -H "Authorization: Bearer $PRINT_AGENT_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)"
```

Réponse `200` : `{"claimToken": "...", "expiresAt": "..."}`.
Réponse `409` possible : `ALREADY_CLAIMED`, `ALREADY_PRINTED` ou
`MAX_ATTEMPTS_REACHED`.

### 3. Confirmer l'impression

```bash
curl -s -X POST "$BASE_URL/jobs/$ORDER_ID/printed" \
  -H "Authorization: Bearer $PRINT_AGENT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"claimToken":"<claimToken reçu au claim>","printedAt":"2026-07-06T10:15:00Z"}'
```

Réponse `200` : `{"status":"CONFIRMED"}` (ou `"ALREADY_CONFIRMED"` si rejoué).

### 4. Signaler un échec d'impression

```bash
curl -s -X POST "$BASE_URL/jobs/$ORDER_ID/failed" \
  -H "Authorization: Bearer $PRINT_AGENT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"claimToken":"<claimToken reçu au claim>","errorCode":"PRINTER_OFFLINE","errorMessage":"Connexion TCP refusée port 9100"}'
```

Réponse `200` : `{"status":"RELEASED"}` (le ticket redevient réclamable) ou
`{"status":"MANUAL_REVIEW"}` si le nombre maximal de tentatives est atteint
(intervention humaine nécessaire).
