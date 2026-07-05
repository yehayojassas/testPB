-- Planet Bowl — sécurisation du calcul des prix de commande
-- À exécuter après 0001_init.sql.
--
-- Problème corrigé : le prix unitaire et le total étaient calculés côté
-- navigateur puis insérés tels quels dans orders/order_items. N'importe qui
-- pouvait falsifier une requête (devtools, appel direct au client Supabase)
-- pour commander à un prix arbitraire. Désormais, seule la fonction
-- place_order() peut créer une commande : elle recalcule chaque prix depuis
-- la table products (source de vérité serveur) et les insertions directes
-- des clients sur orders/order_items sont bloquées par RLS.

-- ---------------------------------------------------------------------------
-- Prix des garnitures payantes (ex. Wakame) : jusqu'ici hardcodés uniquement
-- côté frontend (src/data/catalog.js) et donc non vérifiables côté serveur.
-- ---------------------------------------------------------------------------
create table public.extra_options (
  name  text primary key,
  price numeric(8,2) not null default 0 check (price >= 0)
);

alter table public.extra_options enable row level security;

create policy "extras publics en lecture" on public.extra_options
  for select using (true);
create policy "extras gérés par admin" on public.extra_options
  for all using (public.is_admin()) with check (public.is_admin());

insert into public.extra_options (name, price) values ('Wakame', 1.5);

-- ---------------------------------------------------------------------------
-- Création de commande côté serveur : seule autorité sur les prix.
-- p_items : jsonb du type [{ "product_id": uuid, "quantity": int, "customizations": jsonb|null }]
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
  v_product    public.products%rowtype;
  v_topping    text;
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

    -- Prix de base : toujours celui en base (promo si active), jamais celui envoyé par le client.
    v_unit_price := coalesce(v_product.promo_price, v_product.price);

    -- Supplément garnitures payantes, également recalculé depuis la base.
    v_extra := 0;
    if jsonb_typeof(v_item -> 'customizations' -> 'toppings') = 'array' then
      for v_topping in select jsonb_array_elements_text(v_item -> 'customizations' -> 'toppings')
      loop
        v_extra := v_extra + coalesce(
          (select price from public.extra_options where name = v_topping), 0
        );
      end loop;
    end if;
    v_unit_price := v_unit_price + v_extra;

    insert into public.order_items (order_id, product_id, name, unit_price, quantity, customizations)
    values (v_order.id, v_product.id, v_product.name, v_unit_price, v_quantity, v_item -> 'customizations');

    v_subtotal := v_subtotal + v_unit_price * v_quantity;
  end loop;

  update public.orders set subtotal = v_subtotal, total = v_subtotal
    where id = v_order.id
    returning * into v_order;

  return v_order;
end;
$$;

revoke execute on function public.place_order(uuid, text, jsonb) from anon;

-- ---------------------------------------------------------------------------
-- On retire le droit d'insertion directe : toute commande doit désormais
-- passer par place_order(), seule fonction habilitée à fixer les prix.
-- ---------------------------------------------------------------------------
drop policy if exists "commandes créées par le client" on public.orders;
drop policy if exists "articles créés avec la commande" on public.order_items;
