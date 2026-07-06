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

-- ---------------------------------------------------------------------------
-- Opérations atomiques pour l'Edge Function print-agent
-- ---------------------------------------------------------------------------
-- Le claim doit être une seule requête UPDATE ... RETURNING pour éviter que
-- deux tablettes (ou deux tentatives concurrentes de la même tablette)
-- réclament le même ticket : un aller-retour "SELECT puis UPDATE" depuis le
-- code applicatif laisserait une fenêtre de course. Ces trois fonctions sont
-- donc les seules autorisées à écrire dans print_jobs, appelées uniquement
-- par l'Edge Function via la clé service_role (jamais par anon/authenticated).

create or replace function public.claim_print_job(p_order_id uuid, p_restaurant_id uuid)
returns jsonb
language plpgsql security definer
set search_path = public
as $$
declare
  v_row public.print_jobs%rowtype;
begin
  update public.print_jobs
     set status = 'CLAIMED',
         claim_token = gen_random_uuid(),
         claim_expires_at = now() + interval '90 seconds',
         attempt_count = attempt_count + 1
   where order_id = p_order_id
     and restaurant_id = p_restaurant_id
     and (status = 'PENDING' or (status = 'CLAIMED' and claim_expires_at < now()))
     and attempt_count < max_attempts
   returning * into v_row;

  if found then
    return jsonb_build_object(
      'resultStatus', 'CLAIMED',
      'claimToken', v_row.claim_token,
      'claimExpiresAt', v_row.claim_expires_at
    );
  end if;

  -- Rien mis à jour : on relit la ligne pour renvoyer le bon code d'erreur au client.
  select * into v_row from public.print_jobs
    where order_id = p_order_id and restaurant_id = p_restaurant_id;

  if not found then
    return jsonb_build_object('resultStatus', 'NOT_FOUND');
  end if;

  if v_row.status = 'CONFIRMED' then
    return jsonb_build_object('resultStatus', 'ALREADY_PRINTED');
  end if;

  if v_row.attempt_count >= v_row.max_attempts then
    update public.print_jobs set status = 'MANUAL_REVIEW' where order_id = p_order_id;
    return jsonb_build_object('resultStatus', 'MAX_ATTEMPTS_REACHED');
  end if;

  return jsonb_build_object('resultStatus', 'ALREADY_CLAIMED');
end;
$$;

create or replace function public.confirm_print_job(
  p_order_id uuid,
  p_claim_token uuid,
  p_printed_at timestamptz
)
returns jsonb
language plpgsql security definer
set search_path = public
as $$
declare
  v_row public.print_jobs%rowtype;
begin
  update public.print_jobs
     set status = 'CONFIRMED',
         printed_at = coalesce(p_printed_at, now()),
         confirmed_at = now()
   where order_id = p_order_id and claim_token = p_claim_token and status = 'CLAIMED'
   returning * into v_row;

  if found then
    return jsonb_build_object('resultStatus', 'CONFIRMED');
  end if;

  -- Rejeu possible (la tablette n'a pas reçu la réponse la première fois) :
  -- si c'est bien le même jeton qui a déjà confirmé, on répond sans erreur.
  select * into v_row from public.print_jobs where order_id = p_order_id;
  if found and v_row.status = 'CONFIRMED' and v_row.claim_token = p_claim_token then
    return jsonb_build_object('resultStatus', 'ALREADY_CONFIRMED');
  end if;

  return jsonb_build_object('resultStatus', 'CLAIM_MISMATCH');
end;
$$;

create or replace function public.release_print_job(
  p_order_id uuid,
  p_claim_token uuid,
  p_error_code text,
  p_error_message text
)
returns jsonb
language plpgsql security definer
set search_path = public
as $$
declare
  v_row public.print_jobs%rowtype;
  v_next_status text;
begin
  select * into v_row
    from public.print_jobs
    where order_id = p_order_id and claim_token = p_claim_token and status = 'CLAIMED';

  if not found then
    return jsonb_build_object('resultStatus', 'CLAIM_MISMATCH');
  end if;

  v_next_status := case when v_row.attempt_count >= v_row.max_attempts then 'MANUAL_REVIEW' else 'PENDING' end;

  -- Le job est libéré (repasse PENDING) pour qu'un prochain essai puisse le
  -- réclamer, sauf si le nombre maximal de tentatives est atteint : dans ce
  -- cas terminal (MANUAL_REVIEW), on ne réinitialise pas claim_token/expiry
  -- pour garder une trace de la dernière tentative en échec.
  update public.print_jobs
     set status = v_next_status,
         claim_token = case when v_next_status = 'PENDING' then null else claim_token end,
         claim_expires_at = case when v_next_status = 'PENDING' then null else claim_expires_at end,
         last_error_code = left(p_error_code, 100),
         last_error_message = left(p_error_message, 500)
   where order_id = p_order_id and claim_token = p_claim_token;

  return jsonb_build_object('resultStatus', v_next_status);
end;
$$;

-- Ces fonctions ne doivent jamais être appelables par un client (anon ou
-- authenticated) : elles contournent volontairement les vérifications
-- métier normales (place_order, RLS) et ne doivent être invoquées que par
-- l'Edge Function print-agent, authentifiée avec la clé service_role.
revoke execute on function public.claim_print_job(uuid, uuid) from public;
revoke execute on function public.confirm_print_job(uuid, uuid, timestamptz) from public;
revoke execute on function public.release_print_job(uuid, uuid, text, text) from public;

grant execute on function public.claim_print_job(uuid, uuid) to service_role;
grant execute on function public.confirm_print_job(uuid, uuid, timestamptz) to service_role;
grant execute on function public.release_print_job(uuid, uuid, text, text) to service_role;
