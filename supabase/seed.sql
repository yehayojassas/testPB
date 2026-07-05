-- Planet Bowl — données initiales (catalogue officiel)
-- À exécuter après 0001_init.sql.

insert into public.restaurants (slug, name, address, city, image, prep_minutes, is_active, sort_order) values
  ('grancy',        'Lausanne Grancy',        'Boulevard de Grancy 58, 1006 Lausanne',        'Lausanne',           'restaurant-grancy.jpg',        12, true, 1),
  ('saint-laurent', 'Lausanne Saint-Laurent', 'Rue Saint-Laurent 38, 1003 Lausanne',          'Lausanne',           'restaurant-saint-laurent.jpg', 12, true, 2),
  ('renens',        'Renens',                 'Rue de la Gare de Triage 6, 1020 Renens',      'Renens',             'restaurant-renens.jpg',        15, true, 3),
  ('morges',        'Morges',                 'Grand-Rue 6, 1110 Morges',                     'Morges',             'restaurant-morges.jpg',        15, true, 4),
  ('vevey',         'Vevey',                  'Rue du Lac 8, 1800 Vevey',                     'Vevey',              'restaurant-vevey.jpg',         15, true, 5),
  ('geneve',        'Genève',                 'Rue du Môle 31, 1201 Genève',                  'Genève',             'restaurant-geneve.jpg',        15, true, 6),
  ('yverdon',       'Yverdon-les-Bains',      'Rue du Milieu 6, 1400 Yverdon-les-Bains',      'Yverdon-les-Bains',  'restaurant-yverdon.jpg',       15, true, 7),
  ('romont',        'Romont',                 'Place de la Gare 1, 1680 Romont',              'Romont',             'restaurant-romont.jpg',        15, true, 8),
  -- 9e adresse annoncée : à compléter puis activer (is_active = true)
  ('neuvieme',      '9e restaurant',          'Adresse à compléter',                          null,                 'restaurant-grancy.jpg',        15, false, 9);

-- NOTE : ce fichier reflète l'état d'origine (avant l'extension de la carte
-- en 0003_menu_and_builder.sql, qui ajoute desserts/boissons/Bowl Builder et
-- met à jour ces descriptions). Sur un projet neuf, exécutez 0001, 0002, ce
-- fichier, puis 0003+ dans l'ordre — voir install_tout_en_un.sql qui
-- recompose déjà tout dans le bon état final.
insert into public.products (slug, name, description, price, category, tag, image, sort_order) values
  ('compose',    'Compose ton bowl', 'Créez votre bowl, étape par étape',   18.00, 'build_your_own', 'Votre choix', 'compose-ton-bowl.jpg', 0),
  ('nordique',   'Le nordique',      'Saumon · edamame · wakame',           22.00, 'signature',       'Best-seller', 'le-nordique.jpg',      1),
  ('hawaien',    'L’hawaïen',        'Thon · avocat · notes exotiques',     22.00, 'signature',       null,          'l-hawaien.jpg',        2),
  ('atlantique', 'L’atlantique',     'Saumon fumé · fraîcheur marine',      21.00, 'signature',       null,          'l-atlantique.jpg',     3),
  ('indien',     'L’indien',         'Poulet · légumes · saveurs épicées',  20.00, 'signature',       null,          'l-indien.jpg',         4),
  ('libanais',   'Le libanais',      'Falafel · houmous · grenade',         20.00, 'signature',       null,          'le-libanais.jpg',      5),
  ('bouda',      'Le bouda',         'Tofu · légumes · option végétale',    20.00, 'signature',       'Veggie',      'le-bouda.jpg',         6);

-- Pour promouvoir un compte en administrateur (après son inscription dans l'app) :
-- update public.profiles set role = 'admin' where email = 'votre-email@exemple.ch';
