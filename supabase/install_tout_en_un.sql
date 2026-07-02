-- ============================================================================
-- Planet Bowl — INSTALLATION TOUT-EN-UN (idempotente)
-- Copiez TOUT ce fichier dans le SQL Editor de Supabase et cliquez Run
-- (sans texte sélectionné). Peut être relancé sans risque : il ne crée que
-- ce qui manque et ne touche pas aux tables d'anciens essais.
-- ============================================================================

-- 0. Si une table "restaurants" d'un ancien essai existe (reconnaissable :
--    elle n'a pas la colonne prep_minutes), on la renomme sans rien perdre.
do $$
begin
  if exists (select 1 from information_schema.tables
             where table_schema = 'public' and table_name = 'restaurants')
     and not exists (select 1 from information_schema.columns
                     where table_schema = 'public' and table_name = 'restaurants'
                       and column_name = 'prep_minutes')
  then
    alter table public.restaurants rename to restaurants_ancien;
  end if;
end $$;

create extension if not exists "pgcrypto";

-- 1. Tables --------------------------------------------------------------

create table if not exists public.restaurants (
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

create table if not exists public.products (
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

create table if not exists public.profiles (
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

create table if not exists public.customer_preferences (
  user_id                uuid primary key references public.profiles(id) on delete cascade,
  favorite_restaurant_id uuid references public.restaurants(id) on delete set null,
  favorite_bowl          jsonb,
  updated_at             timestamptz not null default now()
);

do $$
begin
  create type public.order_status as enum ('received','preparing','ready','picked_up','cancelled');
exception when duplicate_object then null;
end $$;

create sequence if not exists public.order_code_seq start 101;

create table if not exists public.orders (
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

create index if not exists orders_user_idx on public.orders(user_id, created_at desc);
create index if not exists orders_restaurant_idx on public.orders(restaurant_id, created_at desc);
create index if not exists orders_status_idx on public.orders(status);

create table if not exists public.order_items (
  id             uuid primary key default gen_random_uuid(),
  order_id       uuid not null references public.orders(id) on delete cascade,
  product_id     uuid references public.products(id) on delete set null,
  name           text not null,
  unit_price     numeric(8,2) not null,
  quantity       integer not null check (quantity > 0),
  customizations jsonb
);

create index if not exists order_items_order_idx on public.order_items(order_id);

-- 2. Fonctions et triggers -------------------------------------------------

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

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists profiles_touch on public.profiles;
create trigger profiles_touch before update on public.profiles
  for each row execute function public.touch_updated_at();
drop trigger if exists products_touch on public.products;
create trigger products_touch before update on public.products
  for each row execute function public.touch_updated_at();
drop trigger if exists orders_touch on public.orders;
create trigger orders_touch before update on public.orders
  for each row execute function public.touch_updated_at();

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

-- 3. Row Level Security ------------------------------------------------------

alter table public.restaurants enable row level security;
alter table public.products enable row level security;
alter table public.profiles enable row level security;
alter table public.customer_preferences enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

drop policy if exists "restaurants publics en lecture" on public.restaurants;
create policy "restaurants publics en lecture" on public.restaurants
  for select using (is_active or public.is_admin());
drop policy if exists "restaurants gérés par admin" on public.restaurants;
create policy "restaurants gérés par admin" on public.restaurants
  for all using (public.is_admin()) with check (public.is_admin());

drop policy if exists "produits publics en lecture" on public.products;
create policy "produits publics en lecture" on public.products
  for select using (is_available or public.is_admin());
drop policy if exists "produits gérés par admin" on public.products;
create policy "produits gérés par admin" on public.products
  for all using (public.is_admin()) with check (public.is_admin());

drop policy if exists "profil lisible par son propriétaire" on public.profiles;
create policy "profil lisible par son propriétaire" on public.profiles
  for select using (auth.uid() = id or public.is_admin());
drop policy if exists "profil modifiable par son propriétaire" on public.profiles;
create policy "profil modifiable par son propriétaire" on public.profiles
  for update using (auth.uid() = id)
  with check (auth.uid() = id and (role = 'customer' or public.is_admin()));

drop policy if exists "préférences du propriétaire" on public.customer_preferences;
create policy "préférences du propriétaire" on public.customer_preferences
  for all using (auth.uid() = user_id or public.is_admin())
  with check (auth.uid() = user_id);

drop policy if exists "commandes lisibles par le client" on public.orders;
create policy "commandes lisibles par le client" on public.orders
  for select using (auth.uid() = user_id or public.is_admin());
drop policy if exists "commandes créées par le client" on public.orders;
create policy "commandes créées par le client" on public.orders
  for insert with check (auth.uid() = user_id);
drop policy if exists "commandes mises à jour par admin" on public.orders;
create policy "commandes mises à jour par admin" on public.orders
  for update using (public.is_admin()) with check (public.is_admin());

drop policy if exists "articles lisibles avec la commande" on public.order_items;
create policy "articles lisibles avec la commande" on public.order_items
  for select using (
    exists (select 1 from public.orders o where o.id = order_id and (o.user_id = auth.uid() or public.is_admin()))
  );
drop policy if exists "articles créés avec la commande" on public.order_items;
create policy "articles créés avec la commande" on public.order_items
  for insert with check (
    exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid())
  );

-- 4. Catalogue officiel (ignoré s'il existe déjà) -----------------------------

insert into public.restaurants (slug, name, address, city, image, prep_minutes, is_active, sort_order) values
  ('grancy',        'Lausanne Grancy',        'Boulevard de Grancy 58, 1006 Lausanne',        'Lausanne',           'restaurant-grancy.jpg',        12, true, 1),
  ('saint-laurent', 'Lausanne Saint-Laurent', 'Rue Saint-Laurent 38, 1003 Lausanne',          'Lausanne',           'restaurant-saint-laurent.jpg', 12, true, 2),
  ('renens',        'Renens',                 'Rue de la Gare de Triage 6, 1020 Renens',      'Renens',             'restaurant-renens.jpg',        15, true, 3),
  ('morges',        'Morges',                 'Grand-Rue 6, 1110 Morges',                     'Morges',             'restaurant-morges.jpg',        15, true, 4),
  ('vevey',         'Vevey',                  'Rue du Lac 8, 1800 Vevey',                     'Vevey',              'restaurant-vevey.jpg',         15, true, 5),
  ('geneve',        'Genève',                 'Rue du Môle 31, 1201 Genève',                  'Genève',             'restaurant-geneve.jpg',        15, true, 6),
  ('yverdon',       'Yverdon-les-Bains',      'Rue du Milieu 6, 1400 Yverdon-les-Bains',      'Yverdon-les-Bains',  'restaurant-yverdon.jpg',       15, true, 7),
  ('romont',        'Romont',                 'Place de la Gare 1, 1680 Romont',              'Romont',             'restaurant-romont.jpg',        15, true, 8),
  ('neuvieme',      '9e restaurant',          'Adresse à compléter',                          null,                 'restaurant-grancy.jpg',        15, false, 9)
on conflict (slug) do nothing;

insert into public.products (slug, name, description, price, category, tag, image, sort_order) values
  ('compose',    'Compose ton bowl', 'Créez votre bowl, étape par étape',   18.00, 'À composer', 'Votre choix',  'compose-ton-bowl.jpg', 0),
  ('nordique',   'Le nordique',      'Saumon · edamame · wakame',           22.00, 'Signature',  'Best-seller',  'le-nordique.jpg',      1),
  ('hawaien',    'L’hawaïen',        'Thon · avocat · notes exotiques',     22.00, 'Signature',  null,           'l-hawaien.jpg',        2),
  ('atlantique', 'L’atlantique',     'Saumon fumé · fraîcheur marine',      21.00, 'Signature',  null,           'l-atlantique.jpg',     3),
  ('indien',     'L’indien',         'Poulet · légumes · saveurs épicées',  20.00, 'Signature',  null,           'l-indien.jpg',         4),
  ('libanais',   'Le libanais',      'Falafel · houmous · grenade',         20.00, 'Signature',  null,           'le-libanais.jpg',      5),
  ('bouda',      'Le bouda',         'Tofu · légumes · option végétale',    20.00, 'Signature',  'Veggie',       'le-bouda.jpg',         6)
on conflict (slug) do nothing;

-- 5. Rattrapage des comptes créés avant l'installation ------------------------

insert into public.profiles (id, first_name, last_name, email, phone, marketing_consent, marketing_consent_at)
select
  id,
  raw_user_meta_data ->> 'first_name',
  raw_user_meta_data ->> 'last_name',
  email,
  raw_user_meta_data ->> 'phone',
  coalesce((raw_user_meta_data ->> 'marketing_consent')::boolean, false),
  case when coalesce((raw_user_meta_data ->> 'marketing_consent')::boolean, false) then now() end
from auth.users
on conflict (id) do nothing;

insert into public.customer_preferences (user_id)
select id from public.profiles
on conflict (user_id) do nothing;

-- 6. Compte administrateur ----------------------------------------------------

update public.profiles set role = 'admin' where email = 'mister.jassim@icloud.com';

-- 7. Contrôle final : doit afficher 9 restaurants, 7 produits, vos profils ----

select
  (select count(*) from public.restaurants) as restaurants,
  (select count(*) from public.products)    as produits,
  (select count(*) from public.profiles)    as profils,
  (select count(*) from public.profiles where role = 'admin') as admins;
