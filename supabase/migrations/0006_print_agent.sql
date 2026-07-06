-- Planet Bowl — agent d'impression des tickets de caisse (tablette Android / ESC-POS)
-- À exécuter après 0005_english_identifiers.sql.
--
-- Remplace l'ancienne colonne orders.print_status (0004), trop simple pour
-- résister à une tablette qui redémarre, une imprimante hors-ligne ou deux
-- tablettes qui réclament le même ticket : print_jobs porte son propre cycle
-- de vie (claim + expiration + compteur de tentatives) indépendant de
-- l'avancement métier de la commande. print_status est laissée en place
-- (colonne inutilisée côté frontend) pour ne pas casser un éventuel usage
-- existant ; c'est print_jobs qui fait foi pour l'impression.

-- ---------------------------------------------------------------------------
-- Table des jobs d'impression
-- ---------------------------------------------------------------------------
create table public.print_jobs (
  id                uuid primary key default gen_random_uuid(),
  -- unique : une commande ne peut avoir qu'un seul ticket à imprimer, même si
  -- son statut oscille plusieurs fois entre 'received' et 'preparing'.
  order_id          uuid not null unique references public.orders(id) on delete cascade,
  restaurant_id     uuid not null references public.restaurants(id),
  status            text not null default 'PENDING'
                       check (status in ('PENDING', 'CLAIMED', 'CONFIRMED', 'MANUAL_REVIEW')),
  -- claim_token : jeton d'exclusivité posé au moment où une tablette réclame
  -- le job, pour qu'elle seule puisse ensuite le confirmer ou le libérer.
  claim_token       uuid,
  -- claim_expires_at : si la tablette plante entre le claim et l'impression,
  -- le job redevient réclamable après ce délai plutôt que de rester bloqué.
  claim_expires_at  timestamptz,
  attempt_count     integer not null default 0,
  max_attempts      integer not null default 5,
  last_error_code   text,
  last_error_message text,
  created_at        timestamptz not null default now(),
  printed_at        timestamptz,
  confirmed_at      timestamptz
);

-- Sert la file d'attente de la tablette : jobs d'un restaurant, par statut, du plus ancien au plus récent.
create index print_jobs_queue_idx on public.print_jobs(restaurant_id, status, created_at);

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------
alter table public.print_jobs enable row level security;

-- Lecture réservée à l'admin (futur dashboard de supervision des impressions).
create policy "print_jobs lisibles par admin" on public.print_jobs
  for select using (public.is_admin());

-- Volontairement aucune policy insert/update, y compris pour l'admin : la
-- tablette et l'Edge Function print-agent s'authentifient avec la clé
-- service_role, qui contourne RLS. Créer une policy d'écriture ouvrirait la
-- porte à un client authentifié qui manipulerait directement la table
-- (fausserait le compteur de tentatives, voire imprimerait deux fois).

-- ---------------------------------------------------------------------------
-- File d'impression alimentée automatiquement par le passage en préparation
-- ---------------------------------------------------------------------------
-- Le moment où la cuisine "accepte" une commande (received -> preparing) est
-- l'équivalent exact de l'ancien déclencheur Windows qui sortait le ticket.
create or replace function public.enqueue_print_job()
returns trigger
language plpgsql security definer
set search_path = public
as $$
begin
  insert into public.print_jobs (order_id, restaurant_id)
  values (new.id, new.restaurant_id)
  on conflict (order_id) do nothing;
  return new;
end;
$$;

create trigger orders_enqueue_print_job
  after update on public.orders
  for each row
  when (old.status = 'received' and new.status = 'preparing')
  execute function public.enqueue_print_job();
