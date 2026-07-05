-- ============================================================================
-- Planet Bowl — INSTALLATION TOUT-EN-UN (idempotente)
-- Copiez TOUT ce fichier dans le SQL Editor de Supabase et cliquez Run
-- (sans texte sélectionné). Peut être relancé sans risque : il ne crée que
-- ce qui manque et ne touche pas aux tables d'anciens essais.
--
-- Ce fichier recompose exactement 0001_init.sql + 0002_secure_order_pricing.sql
-- + 0003_menu_and_builder.sql + 0004_print_status.sql + 0005_english_identifiers.sql
-- + seed.sql. Si l'un de ces fichiers change, reportez le changement ici
-- aussi (ou relancez uniquement le fichier modifié dans un projet existant).
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
  category     text not null default 'signature',
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
  print_status   text not null default 'pending' check (print_status in ('pending','printed')),
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

create table if not exists public.option_items (
  category   text not null check (category in ('base','garnish','protein','extra_protein','topping','sauce')),
  name       text not null,
  price      numeric(8,2) not null default 0 check (price >= 0),
  sort_order integer not null default 0,
  primary key (category, name)
);

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

-- Création de commande côté serveur : seule autorité sur les prix (voir
-- 0002_secure_order_pricing.sql et 0003_menu_and_builder.sql pour le détail
-- du problème corrigé et des catégories d'options prises en charge).
create or replace function public.place_order(
  p_restaurant_id uuid,
  p_pickup_slot   text,
  p_items         jsonb
)
returns public.orders
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user_id    uuid := auth.uid();
  v_order      public.orders;
  v_item       jsonb;
  v_cust       jsonb;
  v_product    public.products%rowtype;
  v_name       text;
  v_quantity   integer;
  v_unit_price numeric(8,2);
  v_extra      numeric(8,2);
  v_subtotal   numeric(8,2) := 0;
begin
  if v_user_id is null then
    raise exception 'Non authentifié';
  end if;

  if not exists (
    select 1 from public.restaurants where id = p_restaurant_id and is_active
  ) then
    raise exception 'Restaurant invalide ou fermé';
  end if;

  if p_pickup_slot is null or length(trim(p_pickup_slot)) = 0 then
    raise exception 'Créneau de retrait requis';
  end if;

  if p_items is null or jsonb_array_length(p_items) = 0 then
    raise exception 'Le panier est vide';
  end if;

  insert into public.orders (user_id, restaurant_id, pickup_slot, subtotal, total)
  values (v_user_id, p_restaurant_id, p_pickup_slot, 0, 0)
  returning * into v_order;

  for v_item in select * from jsonb_array_elements(p_items)
  loop
    v_quantity := (v_item ->> 'quantity')::integer;
    if v_quantity is null or v_quantity < 1 then
      raise exception 'Quantité invalide';
    end if;

    select * into v_product from public.products
      where id = (v_item ->> 'product_id')::uuid and is_available;
    if not found then
      raise exception 'Produit indisponible';
    end if;

    v_cust := v_item -> 'customizations';

    -- Le Bowl Builder ("build_your_own") exige une base et une protéine :
    -- sans ce garde-fou, un produit à prix 0.00 pourrait être commandé
    -- gratuitement si le client omet ces champs.
    if v_product.category = 'build_your_own' then
      if coalesce(v_cust ->> 'base', '') = '' or coalesce(v_cust ->> 'protein', '') = '' then
        raise exception 'Base et protéine requises pour un bowl composé';
      end if;
    end if;

    -- Prix de départ : toujours celui en base (promo si active, 0.00 pour
    -- le bowl builder), jamais celui envoyé par le client.
    v_unit_price := coalesce(v_product.promo_price, v_product.price);
    v_extra := 0;

    if coalesce(v_cust ->> 'base', '') <> '' then
      v_extra := v_extra + coalesce(
        (select price from public.option_items where category = 'base' and name = v_cust ->> 'base'), 0
      );
    end if;

    if coalesce(v_cust ->> 'protein', '') <> '' then
      v_extra := v_extra + coalesce(
        (select price from public.option_items where category = 'protein' and name = v_cust ->> 'protein'), 0
      );
    end if;

    if jsonb_typeof(v_cust -> 'garnish') = 'array' then
      for v_name in select jsonb_array_elements_text(v_cust -> 'garnish') loop
        v_extra := v_extra + coalesce(
          (select price from public.option_items where category = 'garnish' and name = v_name), 0
        );
      end loop;
    end if;

    if jsonb_typeof(v_cust -> 'extra_protein') = 'array' then
      for v_name in select jsonb_array_elements_text(v_cust -> 'extra_protein') loop
        v_extra := v_extra + coalesce(
          (select price from public.option_items where category = 'extra_protein' and name = v_name), 0
        );
      end loop;
    end if;

    if jsonb_typeof(v_cust -> 'topping') = 'array' then
      for v_name in select jsonb_array_elements_text(v_cust -> 'topping') loop
        v_extra := v_extra + coalesce(
          (select price from public.option_items where category = 'topping' and name = v_name), 0
        );
      end loop;
    end if;

    if jsonb_typeof(v_cust -> 'sauce') = 'array' then
      for v_name in select jsonb_array_elements_text(v_cust -> 'sauce') loop
        v_extra := v_extra + coalesce(
          (select price from public.option_items where category = 'sauce' and name = v_name), 0
        );
      end loop;
    end if;

    v_unit_price := v_unit_price + v_extra;

    insert into public.order_items (order_id, product_id, name, unit_price, quantity, customizations)
    values (v_order.id, v_product.id, v_product.name, v_unit_price, v_quantity, v_cust);

    v_subtotal := v_subtotal + v_unit_price * v_quantity;
  end loop;

  update public.orders set subtotal = v_subtotal, total = v_subtotal
    where id = v_order.id
    returning * into v_order;

  return v_order;
end;
$$;

revoke execute on function public.place_order(uuid, text, jsonb) from anon;

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
-- Plus de policy d'insertion directe ici : seule place_order() (security
-- definer) peut créer une commande, afin que les prix soient toujours
-- recalculés côté serveur. On s'assure aussi de retirer l'ancienne policy si
-- ce script est relancé sur un projet créé avant ce correctif.
drop policy if exists "commandes créées par le client" on public.orders;
drop policy if exists "commandes mises à jour par admin" on public.orders;
create policy "commandes mises à jour par admin" on public.orders
  for update using (public.is_admin()) with check (public.is_admin());

drop policy if exists "articles lisibles avec la commande" on public.order_items;
create policy "articles lisibles avec la commande" on public.order_items
  for select using (
    exists (select 1 from public.orders o where o.id = order_id and (o.user_id = auth.uid() or public.is_admin()))
  );
-- Idem : plus d'insertion directe, place_order() s'en charge.
drop policy if exists "articles créés avec la commande" on public.order_items;

alter table public.option_items enable row level security;
drop policy if exists "options publiques en lecture" on public.option_items;
create policy "options publiques en lecture" on public.option_items
  for select using (true);
drop policy if exists "options gérées par admin" on public.option_items;
create policy "options gérées par admin" on public.option_items
  for all using (public.is_admin()) with check (public.is_admin());

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
  ('compose',    'Compose ton bowl', 'Composez votre bowl : base, garnitures, protéine, extras, toppings et sauces à volonté.', 0.00,  'build_your_own', 'Votre choix', 'compose-ton-bowl.jpg', 0),
  ('nordique',   'Le nordique',      'Dés de saumon cru, riz jasmin, avocat, concombre, algues wakame, radis, aneth, oignon frit, sésame noir.',              22.00, 'signature', 'Best-seller', 'le-nordique.jpg',      1),
  ('hawaien',    'L’hawaïen',        'Dés de thon cru, quinoa, edamame, fenouil, oignon rouge, grenade, coriandre, avocat, gingembre, sésame blanc.',          22.00, 'signature', null,          'l-hawaien.jpg',        2),
  ('atlantique', 'L’atlantique',     'Crevettes, riz venere, avocat, edamame, tomate, fenouil, mangue, oignon frit, algues wakame.',                           21.00, 'signature', null,          'l-atlantique.jpg',     3),
  ('indien',     'L’indien',         'Poulet grillé, riz jasmin, concombre, carotte râpée, chou rouge, radis, avocat, cacahuète.',                             20.00, 'signature', null,          'l-indien.jpg',         4),
  ('libanais',   'Le libanais',      'Falafels maison, houmous, tomate, concombre, feta, oignon rouge, grenade, menthe.',                                     20.00, 'signature', null,          'le-libanais.jpg',      5),
  ('bouda',      'Le bouda',         'Tofu grillé, quinoa, chou rouge, mangue, avocat, courgette, ciboulette, algues wakame.',                                 20.00, 'signature', 'Veggie',      'le-bouda.jpg',         6),
  ('cookie-choco-noisette', 'Cookies double chocolat & noisettes',      'Fait maison, fondant.',            4.50, 'desserts', null, 'cookie-double-chocolat-noisettes.png', 1),
  ('cookie-gruyere-pecan',  'Cookies crème de Gruyère & noix de Pécan', 'Recette signature.',                4.50, 'desserts', null, 'cookie-creme-gruyere-noix-pecan.png',  2),
  ('muffin-chocolat',       'Muffin au chocolat',                       'Moelleux, cœur chocolat.',          4.50, 'desserts', null, 'muffin-au-chocolat.png',               3),
  ('muffin-fruits-rouges',  'Muffin aux fruits rouges',                 'Fruits rouges de saison.',          4.50, 'desserts', null, 'muffin-aux-fruits-rouges.png',         4),
  ('salade-de-fruits',      'Salade de fruits',                         'Fruits frais de saison.',           4.50, 'desserts', null, 'salade-de-fruits.png',                 5),
  ('charitea-black',    'Charitea Black',             'Thé noir glacé bio & équitable.', 5.00, 'drinks', null, 'charitea-black.jpg',    1),
  ('charitea-green',    'Charitea Green',             'Thé vert glacé bio & équitable.', 5.00, 'drinks', null, 'charitea-green.jpg',    2),
  ('charitea-mate',     'Charitea Mate',              'Maté glacé bio & équitable.',     5.00, 'drinks', null, 'charitea-mate.jpg',     3),
  ('charitea-red',      'Charitea Red',               'Hibiscus glacé bio & équitable.', 5.00, 'drinks', null, 'charitea-red.jpg',      4),
  ('coca-cola',         'Coca-Cola (33cl)',           null,                               4.00, 'drinks', null, 'coca-cola.jpg',         5),
  ('coca-cola-zero',    'Coca-Cola Zéro (33cl)',      null,                               4.00, 'drinks', null, 'coca-cola-zero.jpg',    6),
  ('henniez-gazeuse',   'Eau gazeuse Henniez (50cl)', null,                               4.00, 'drinks', null, 'henniez-gazeuse.jpg',   7),
  ('henniez-plate',     'Eau plate Henniez (50cl)',   null,                               4.00, 'drinks', null, 'henniez-plate.jpg',     8),
  ('go-ginger-curcuma', 'Go Ginger & Curcuma',        'Boisson tonique maison.',          5.00, 'drinks', null, 'go-ginger-curcuma.jpg', 9),
  ('limonade-maison',   'Limonade maison citron',     'Pressée maison.',                  4.00, 'drinks', null, 'limonade-maison.jpg',   10),
  ('the-froid-maison',  'Thé froid maison',           'Infusion maison du jour.',         4.00, 'drinks', null, 'the-froid-maison.jpg',  11)
on conflict (slug) do nothing;

insert into public.option_items (category, name, price, sort_order) values
  ('base', 'Salade',      5.00, 1),
  ('base', 'Riz venere',  5.00, 2),
  ('base', 'Riz',         5.00, 3),
  ('base', 'Quinoa',      5.00, 4),
  ('garnish', 'Houmous',        2.00, 1),
  ('garnish', 'Grenade',        2.00, 2),
  ('garnish', 'Mangue',         2.00, 3),
  ('garnish', 'Concombre',      2.00, 4),
  ('garnish', 'Carotte',        2.00, 5),
  ('garnish', 'Choux rouge',    2.00, 6),
  ('garnish', 'Courgette',      2.00, 7),
  ('garnish', 'Edamame',        2.00, 8),
  ('garnish', 'Fenouil',        2.00, 9),
  ('garnish', 'Tomate',         2.00, 10),
  ('garnish', 'Oignon rouge',   2.00, 11),
  ('garnish', 'Radis',          2.00, 12),
  ('garnish', 'Algues wakame',  2.00, 13),
  ('garnish', 'Avocat',         2.00, 14),
  ('protein', 'Feta',           6.00, 1),
  ('protein', 'Falafel',        6.00, 2),
  ('protein', 'Crevettes',      7.00, 3),
  ('protein', 'Tofu',           7.00, 4),
  ('protein', 'Poulet grillé',  7.00, 5),
  ('protein', 'Saumon',         7.00, 6),
  ('protein', 'Thon',           7.00, 7),
  ('extra_protein', 'Feta',           3.00, 1),
  ('extra_protein', 'Falafel',        3.00, 2),
  ('extra_protein', 'Crevettes',      3.00, 3),
  ('extra_protein', 'Tofu',           3.00, 4),
  ('extra_protein', 'Poulet grillé',  3.00, 5),
  ('extra_protein', 'Saumon',         3.00, 6),
  ('extra_protein', 'Thon',           3.00, 7),
  ('topping', 'Sésame blanc et noir', 1.00, 1),
  ('topping', 'Cacahuètes',           1.00, 2),
  ('topping', 'Ciboulette',           1.00, 3),
  ('topping', 'Aneth',                1.00, 4),
  ('topping', 'Coriandre',            1.00, 5),
  ('topping', 'Menthe',               1.00, 6),
  ('topping', 'Gingembre gari',       1.00, 7),
  ('topping', 'Graines courge',       1.00, 8),
  ('topping', 'Oignon frit',          1.00, 9),
  ('sauce', 'Sauce teriyaki sucrée', 1.00, 1),
  ('sauce', 'Classique',             1.00, 2),
  ('sauce', 'Mayonnaise spicy',      1.00, 3),
  ('sauce', 'Teriyaki classique',    1.00, 4),
  ('sauce', 'Teriyaki relevé',       1.00, 5),
  ('sauce', 'Crème de sésame',       1.00, 6)
on conflict (category, name) do nothing;

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
-- Remplacez l'adresse ci-dessous par la vôtre (inscrivez-vous d'abord dans
-- l'app, puis relancez cette ligne) avant d'exécuter ce script.

update public.profiles set role = 'admin' where email = 'votre-email@exemple.ch';

-- 7. Contrôle final : doit afficher 9 restaurants, 23 produits, 47 options --

select
  (select count(*) from public.restaurants)   as restaurants,
  (select count(*) from public.products)      as produits,
  (select count(*) from public.option_items)  as options,
  (select count(*) from public.profiles)      as profils,
  (select count(*) from public.profiles where role = 'admin') as admins;
