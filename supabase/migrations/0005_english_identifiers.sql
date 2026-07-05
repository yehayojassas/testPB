-- Planet Bowl — harmonisation des identifiants techniques en anglais
-- À exécuter après 0004_print_status.sql.
--
-- Jusqu'ici, product.category et option_items.category mélangeaient
-- français ('Signature', 'À composer', 'garniture', 'proteine_extra') et
-- anglais (base, protein, topping, sauce, status, payment_status, role...).
-- Ce script aligne tous les identifiants techniques sur l'anglais ; le
-- français reste réservé à l'affichage utilisateur (géré côté frontend via
-- une table de correspondance).

-- ---------------------------------------------------------------------------
-- 1. Catégories de produits
-- ---------------------------------------------------------------------------
update public.products set category = 'signature'      where category = 'Signature';
update public.products set category = 'build_your_own' where category = 'À composer';
update public.products set category = 'drinks'         where category = 'Boissons';
update public.products set category = 'desserts'       where category = 'Desserts';

alter table public.products alter column category set default 'signature';

-- ---------------------------------------------------------------------------
-- 2. Catégories d'options du Bowl Builder
-- ---------------------------------------------------------------------------
-- La contrainte doit être retirée AVANT la mise à jour des données, sinon
-- chaque ligne renommée viole encore l'ancienne liste de valeurs autorisées.
alter table public.option_items drop constraint if exists option_items_category_check;

update public.option_items set category = 'garnish'       where category = 'garniture';
update public.option_items set category = 'protein'       where category = 'proteine';
update public.option_items set category = 'extra_protein' where category = 'proteine_extra';

alter table public.option_items add constraint option_items_category_check
  check (category in ('base','garnish','protein','extra_protein','topping','sauce'));

-- ---------------------------------------------------------------------------
-- 3. place_order() : mêmes clés JSON et catégories, mises à jour
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
