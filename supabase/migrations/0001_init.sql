-- Planet Bowl — schéma initial
-- À exécuter dans le SQL Editor de Supabase (ou via `supabase db push`).

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Restaurants
-- ---------------------------------------------------------------------------
create table public.restaurants (
  id            uuid primary key default gen_random_uuid(),
  slug          text unique not null,
  name          text not null,
  address       text not null,
  city          text,
  phone         text,
  image         text,
  opening_hours jsonb not null default '{"mon-fri":"10:30-21:30","sat":"11:00-21:30","sun":"11:00-21:00"}',
  prep_minutes  integer not null default 15,
  is_active     boolean not null default true,
  sort_order    integer not null default 0,
  created_at    timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Produits
-- ---------------------------------------------------------------------------
create table public.products (
  id           uuid primary key default gen_random_uuid(),
  slug         text unique not null,
  name         text not null,
  description  text,
  price        numeric(8,2) not null check (price >= 0),
  promo_price  numeric(8,2) check (promo_price is null or promo_price >= 0),
  category     text not null default 'Signature',
  tag          text,
  image        text,
  is_available boolean not null default true,
  sort_order   integer not null default 0,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Profils clients (1-1 avec auth.users, jamais de mot de passe ici :
-- les mots de passe sont hachés et gérés exclusivement par Supabase Auth)
-- ---------------------------------------------------------------------------
create table public.profiles (
  id                   uuid primary key references auth.users(id) on delete cascade,
  first_name           text,
  last_name            text,
  email                text,
  phone                text,
  marketing_consent    boolean not null default false,
  marketing_consent_at timestamptz,
  role                 text not null default 'customer' check (role in ('customer','admin')),
  created_at           timestamptz not null default now(),
  updated_at           timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Préférences client (restaurant favori, bowl favori pour recommander vite)
-- ---------------------------------------------------------------------------
create table public.customer_preferences (
  user_id                uuid primary key references public.profiles(id) on delete cascade,
  favorite_restaurant_id uuid references public.restaurants(id) on delete set null,
  favorite_bowl          jsonb,
  updated_at             timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Commandes
-- ---------------------------------------------------------------------------
create type public.order_status as enum ('received','preparing','ready','picked_up','cancelled');

create sequence public.order_code_seq start 101;

create table public.orders (
  id             uuid primary key default gen_random_uuid(),
  code           text unique not null default 'PB-' || nextval('public.order_code_seq'),
  user_id        uuid not null references public.profiles(id) on delete cascade,
  restaurant_id  uuid not null references public.restaurants(id),
  status         public.order_status not null default 'received',
  pickup_slot    text not null,
  subtotal       numeric(8,2) not null default 0,
  total          numeric(8,2) not null default 0,
  payment_status text not null default 'pending' check (payment_status in ('pending','paid','refunded')),
  payment_method text not null default 'on_site',
  note           text,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now()
);

create index orders_user_idx on public.orders(user_id, created_at desc);
create index orders_restaurant_idx on public.orders(restaurant_id, created_at desc);
create index orders_status_idx on public.orders(status);

create table public.order_items (
  id             uuid primary key default gen_random_uuid(),
  order_id       uuid not null references public.orders(id) on delete cascade,
  product_id     uuid references public.products(id) on delete set null,
  name           text not null,
  unit_price     numeric(8,2) not null,
  quantity       integer not null check (quantity > 0),
  customizations jsonb
);

create index order_items_order_idx on public.order_items(order_id);

-- ---------------------------------------------------------------------------
-- Fonctions utilitaires
-- ---------------------------------------------------------------------------

-- Vérifie le rôle admin sans récursion RLS (security definer)
create or replace function public.is_admin()
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and role = 'admin'
  );
$$;

-- Création automatique du profil à l'inscription
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, first_name, last_name, email, phone, marketing_consent, marketing_consent_at)
  values (
    new.id,
    new.raw_user_meta_data ->> 'first_name',
    new.raw_user_meta_data ->> 'last_name',
    new.email,
    new.raw_user_meta_data ->> 'phone',
    coalesce((new.raw_user_meta_data ->> 'marketing_consent')::boolean, false),
    case when coalesce((new.raw_user_meta_data ->> 'marketing_consent')::boolean, false) then now() end
  );
  insert into public.customer_preferences (user_id) values (new.id);
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- updated_at automatique
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_touch before update on public.profiles
  for each row execute function public.touch_updated_at();
create trigger products_touch before update on public.products
  for each row execute function public.touch_updated_at();
create trigger orders_touch before update on public.orders
  for each row execute function public.touch_updated_at();

-- Droit à l'effacement (LPD / RGPD) : un client supprime son propre compte.
-- La suppression cascade sur profiles, preferences, orders, order_items.
create or replace function public.delete_own_account()
returns void
language plpgsql security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Non authentifié';
  end if;
  delete from auth.users where id = auth.uid();
end;
$$;

revoke execute on function public.delete_own_account() from anon;

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------
alter table public.restaurants enable row level security;
alter table public.products enable row level security;
alter table public.profiles enable row level security;
alter table public.customer_preferences enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- Catalogue : lecture publique (actifs uniquement pour les non-admins)
create policy "restaurants publics en lecture" on public.restaurants
  for select using (is_active or public.is_admin());
create policy "restaurants gérés par admin" on public.restaurants
  for all using (public.is_admin()) with check (public.is_admin());

create policy "produits publics en lecture" on public.products
  for select using (is_available or public.is_admin());
create policy "produits gérés par admin" on public.products
  for all using (public.is_admin()) with check (public.is_admin());

-- Profils : chacun le sien, l'admin voit tout (mais ne modifie pas les rôles ici)
create policy "profil lisible par son propriétaire" on public.profiles
  for select using (auth.uid() = id or public.is_admin());
-- Le with check empêche un client de s'auto-promouvoir admin
create policy "profil modifiable par son propriétaire" on public.profiles
  for update using (auth.uid() = id)
  with check (auth.uid() = id and (role = 'customer' or public.is_admin()));

-- Préférences : privées
create policy "préférences du propriétaire" on public.customer_preferences
  for all using (auth.uid() = user_id or public.is_admin())
  with check (auth.uid() = user_id);

-- Commandes : le client crée et lit les siennes ; l'admin lit et met à jour tout
create policy "commandes lisibles par le client" on public.orders
  for select using (auth.uid() = user_id or public.is_admin());
create policy "commandes créées par le client" on public.orders
  for insert with check (auth.uid() = user_id);
create policy "commandes mises à jour par admin" on public.orders
  for update using (public.is_admin()) with check (public.is_admin());

create policy "articles lisibles avec la commande" on public.order_items
  for select using (
    exists (select 1 from public.orders o where o.id = order_id and (o.user_id = auth.uid() or public.is_admin()))
  );
create policy "articles créés avec la commande" on public.order_items
  for insert with check (
    exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid())
  );
