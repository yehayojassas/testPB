-- Planet Bowl — statut d'impression des tickets cuisine
-- À exécuter après 0003_menu_and_builder.sql.
--
-- Séparé du statut de préparation (orders.status) pour pouvoir brancher une
-- vraie imprimante/étiqueteuse plus tard sans nouvelle migration : le
-- système d'impression (géré séparément) marquera 'printed' une fois le
-- ticket sorti, indépendamment de l'avancement reçue/en préparation/prête.
alter table public.orders
  add column if not exists print_status text not null default 'pending'
    check (print_status in ('pending', 'printed'));
