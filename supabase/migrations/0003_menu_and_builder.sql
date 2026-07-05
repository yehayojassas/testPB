-- Planet Bowl — carte complète + Bowl Builder ("À composer")
-- À exécuter après 0002_secure_order_pricing.sql.
--
-- Remplace extra_options (une seule garniture payante) par option_items,
-- qui couvre les 6 catégories d'options utilisées à la fois par le Bowl
-- Builder et par les suppléments optionnels des bowls Signature. Le prix
-- de chaque option reste une source de vérité exclusivement serveur :
-- place_order() ne fait jamais confiance à un prix envoyé par le client,
-- uniquement à des noms utilisés comme clé de recherche.

-- ---------------------------------------------------------------------------
-- 1. Table des options tarifées (remplace extra_options)
-- ---------------------------------------------------------------------------
create table public.option_items (
  category   text not null check (category in ('base','garniture','proteine','proteine_extra','topping','sauce')),
  name       text not null,
  price      numeric(8,2) not null default 0 check (price >= 0),
  sort_order integer not null default 0,
  primary key (category, name)
);

alter table public.option_items enable row level security;

create policy "options publiques en lecture" on public.option_items
  for select using (true);
create policy "options gérées par admin" on public.option_items
  for all using (public.is_admin()) with check (public.is_admin());

-- Bases (Bowl Builder uniquement) — 5.00 CHF chacune
insert into public.option_items (category, name, price, sort_order) values
  ('base', 'Salade',      5.00, 1),
  ('base', 'Riz venere',  5.00, 2),
  ('base', 'Riz',         5.00, 3),
  ('base', 'Quinoa',      5.00, 4);

-- Garnitures (Bowl Builder + supplément sur Signature) — 2.00 CHF chacune
insert into public.option_items (category, name, price, sort_order) values
  ('garniture', 'Houmous',        2.00, 1),
  ('garniture', 'Grenade',        2.00, 2),
  ('garniture', 'Mangue',         2.00, 3),
  ('garniture', 'Concombre',      2.00, 4),
  ('garniture', 'Carotte',        2.00, 5),
  ('garniture', 'Choux rouge',    2.00, 6),
  ('garniture', 'Courgette',      2.00, 7),
  ('garniture', 'Edamame',        2.00, 8),
  ('garniture', 'Fenouil',        2.00, 9),
  ('garniture', 'Tomate',         2.00, 10),
  ('garniture', 'Oignon rouge',   2.00, 11),
  ('garniture', 'Radis',          2.00, 12),
  ('garniture', 'Algues wakame',  2.00, 13),
  ('garniture', 'Avocat',         2.00, 14);

-- Protéine (Bowl Builder, choix unique obligatoire) — prix variable
insert into public.option_items (category, name, price, sort_order) values
  ('proteine', 'Feta',           6.00, 1),
  ('proteine', 'Falafel',        6.00, 2),
  ('proteine', 'Crevettes',      7.00, 3),
  ('proteine', 'Tofu',           7.00, 4),
  ('proteine', 'Poulet grillé',  7.00, 5),
  ('proteine', 'Saumon',         7.00, 6),
  ('proteine', 'Thon',           7.00, 7);

-- Protéine extra (Bowl Builder + supplément sur Signature) — 3.00 CHF flat, mêmes noms
insert into public.option_items (category, name, price, sort_order) values
  ('proteine_extra', 'Feta',           3.00, 1),
  ('proteine_extra', 'Falafel',        3.00, 2),
  ('proteine_extra', 'Crevettes',      3.00, 3),
  ('proteine_extra', 'Tofu',           3.00, 4),
  ('proteine_extra', 'Poulet grillé',  3.00, 5),
  ('proteine_extra', 'Saumon',         3.00, 6),
  ('proteine_extra', 'Thon',           3.00, 7);

-- Topping (Bowl Builder + supplément sur Signature) — 1.00 CHF chacun
insert into public.option_items (category, name, price, sort_order) values
  ('topping', 'Sésame blanc et noir', 1.00, 1),
  ('topping', 'Cacahuètes',           1.00, 2),
  ('topping', 'Ciboulette',           1.00, 3),
  ('topping', 'Aneth',                1.00, 4),
  ('topping', 'Coriandre',            1.00, 5),
  ('topping', 'Menthe',               1.00, 6),
  ('topping', 'Gingembre gari',       1.00, 7),
  ('topping', 'Graines courge',       1.00, 8),
  ('topping', 'Oignon frit',          1.00, 9);

-- Sauces (Bowl Builder + supplément sur Signature) — 1.00 CHF chacune
insert into public.option_items (category, name, price, sort_order) values
  ('sauce', 'Sauce teriyaki sucrée', 1.00, 1),
  ('sauce', 'Classique',             1.00, 2),
  ('sauce', 'Mayonnaise spicy',      1.00, 3),
  ('sauce', 'Teriyaki classique',    1.00, 4),
  ('sauce', 'Teriyaki relevé',       1.00, 5),
  ('sauce', 'Crème de sésame',       1.00, 6);

-- ---------------------------------------------------------------------------
-- 2. Suppression de l'ancienne table extra_options (remplacée intégralement)
-- ---------------------------------------------------------------------------
drop table if exists public.extra_options;

-- ---------------------------------------------------------------------------
-- 3. Mise à jour des produits existants
-- ---------------------------------------------------------------------------
-- Le bowl builder n'a plus de prix propre : place_order() somme uniquement
-- les option_items choisis (base + protéine obligatoires, le reste optionnel).
update public.products set price = 0.00, description =
  'Composez votre bowl : base, garnitures, protéine, extras, toppings et sauces à volonté.'
  where slug = 'compose';

-- Recettes complètes des bowls signature (fixes, informatives uniquement —
-- non modifiables par le client ; seuls des suppléments peuvent être ajoutés).
update public.products set description =
  'Dés de saumon cru, riz jasmin, avocat, concombre, algues wakame, radis, aneth, oignon frit, sésame noir.'
  where slug = 'nordique';
update public.products set description =
  'Dés de thon cru, quinoa, edamame, fenouil, oignon rouge, grenade, coriandre, avocat, gingembre, sésame blanc.'
  where slug = 'hawaien';
update public.products set description =
  'Crevettes, riz venere, avocat, edamame, tomate, fenouil, mangue, oignon frit, algues wakame.'
  where slug = 'atlantique';
update public.products set description =
  'Poulet grillé, riz jasmin, concombre, carotte râpée, chou rouge, radis, avocat, cacahuète.'
  where slug = 'indien';
update public.products set description =
  'Falafels maison, houmous, tomate, concombre, feta, oignon rouge, grenade, menthe.'
  where slug = 'libanais';
update public.products set description =
  'Tofu grillé, quinoa, chou rouge, mangue, avocat, courgette, ciboulette, algues wakame.'
  where slug = 'bouda';

-- ---------------------------------------------------------------------------
-- 4. Nouveaux produits : Desserts et Boissons (prix fixes, sans personnalisation)
-- ---------------------------------------------------------------------------
insert into public.products (slug, name, description, price, category, image, sort_order) values
  ('cookie-choco-noisette', 'Cookies double chocolat & noisettes',                'Fait maison, fondant.',            4.50, 'Desserts', 'cookie-double-chocolat-noisettes.png', 1),
  ('cookie-gruyere-pecan',  'Cookies crème de Gruyère & noix de Pécan',           'Recette signature.',               4.50, 'Desserts', 'cookie-creme-gruyere-noix-pecan.png',  2),
  ('muffin-chocolat',       'Muffin au chocolat',                                 'Moelleux, cœur chocolat.',         4.50, 'Desserts', 'muffin-au-chocolat.png',               3),
  ('muffin-fruits-rouges',  'Muffin aux fruits rouges',                           'Fruits rouges de saison.',         4.50, 'Desserts', 'muffin-aux-fruits-rouges.png',         4),
  ('salade-de-fruits',      'Salade de fruits',                                   'Fruits frais de saison.',          4.50, 'Desserts', 'salade-de-fruits.png',                 5);

insert into public.products (slug, name, description, price, category, image, sort_order) values
  ('charitea-black',    'Charitea Black',            'Thé noir glacé bio & équitable.',  5.00, 'Boissons', 'charitea-black.jpg',    1),
  ('charitea-green',    'Charitea Green',            'Thé vert glacé bio & équitable.',  5.00, 'Boissons', 'charitea-green.jpg',    2),
  ('charitea-mate',     'Charitea Mate',             'Maté glacé bio & équitable.',      5.00, 'Boissons', 'charitea-mate.jpg',     3),
  ('charitea-red',      'Charitea Red',              'Hibiscus glacé bio & équitable.',  5.00, 'Boissons', 'charitea-red.jpg',      4),
  ('coca-cola',         'Coca-Cola (33cl)',          null,                               4.00, 'Boissons', 'coca-cola.jpg',         5),
  ('coca-cola-zero',    'Coca-Cola Zéro (33cl)',     null,                               4.00, 'Boissons', 'coca-cola-zero.jpg',    6),
  ('henniez-gazeuse',   'Eau gazeuse Henniez (50cl)', null,                              4.00, 'Boissons', 'henniez-gazeuse.jpg',   7),
  ('henniez-plate',     'Eau plate Henniez (50cl)',  null,                               4.00, 'Boissons', 'henniez-plate.jpg',     8),
  ('go-ginger-curcuma', 'Go Ginger & Curcuma',       'Boisson tonique maison.',          5.00, 'Boissons', 'go-ginger-curcuma.jpg', 9),
  ('limonade-maison',   'Limonade maison citron',    'Pressée maison.',                  4.00, 'Boissons', 'limonade-maison.jpg',   10),
  ('the-froid-maison',  'Thé froid maison',          'Infusion maison du jour.',         4.00, 'Boissons', 'the-froid-maison.jpg',  11);

-- ---------------------------------------------------------------------------
-- 5. place_order() : ajout du calcul des 6 catégories d'options
-- ---------------------------------------------------------------------------
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

    -- Le Bowl Builder ("À composer") exige une base et une protéine : sans
    -- ce garde-fou, un produit à prix 0.00 pourrait être commandé
    -- gratuitement si le client omet ces champs.
    if v_product.category = 'À composer' then
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
        (select price from public.option_items where category = 'proteine' and name = v_cust ->> 'protein'), 0
      );
    end if;

    if jsonb_typeof(v_cust -> 'garniture') = 'array' then
      for v_name in select jsonb_array_elements_text(v_cust -> 'garniture') loop
        v_extra := v_extra + coalesce(
          (select price from public.option_items where category = 'garniture' and name = v_name), 0
        );
      end loop;
    end if;

    if jsonb_typeof(v_cust -> 'proteine_extra') = 'array' then
      for v_name in select jsonb_array_elements_text(v_cust -> 'proteine_extra') loop
        v_extra := v_extra + coalesce(
          (select price from public.option_items where category = 'proteine_extra' and name = v_name), 0
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
