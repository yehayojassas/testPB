import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Minus, Plus } from '@phosphor-icons/react';
import { Header, PrimaryButton, MultiChoiceSection, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';

// Les bowls signature ont une recette fixe (affichée en lecture seule) :
// le client ne peut qu'ajouter des suppléments optionnels, jamais changer
// la base ou la protéine de la recette.
export default function SignatureExtras({ product }) {
  const navigate = useNavigate();
  const { optionItems, addToCart } = useShop();

  const [garnish, setGarnish] = useState([]);
  const [extraProtein, setExtraProtein] = useState([]);
  const [topping, setTopping] = useState([]);
  const [sauce, setSauce] = useState([]);
  const [quantity, setQuantity] = useState(1);

  const byCategory = category => optionItems.filter(o => o.category === category).sort((a, b) => a.sort_order - b.sort_order);
  const priceOf = (category, name) => optionItems.find(o => o.category === category && o.name === name)?.price ?? 0;
  const toggle = setList => name => setList(list => list.includes(name) ? list.filter(x => x !== name) : [...list, name]);

  const basePrice = Number(product.promo_price ?? product.price);
  const extrasPrice = useMemo(() => (
    garnish.reduce((n, g) => n + priceOf('garnish', g), 0) +
    extraProtein.reduce((n, g) => n + priceOf('extra_protein', g), 0) +
    topping.reduce((n, g) => n + priceOf('topping', g), 0) +
    sauce.reduce((n, g) => n + priceOf('sauce', g), 0)
  ), [garnish, extraProtein, topping, sauce, optionItems]);
  const unitPrice = basePrice + extrasPrice;

  const add = () => {
    addToCart({
      productId: typeof product.id === 'string' && product.id.length > 20 ? product.id : null,
      slug: product.slug,
      name: product.name,
      image: product.image,
      unitPrice,
      quantity,
      customizations: { base: null, protein: null, garnish, extra_protein: extraProtein, topping, sauce },
    });
    navigate('/panier');
  };

  return (
    <div className="screen">
      <Header back="/menu" title="Personnaliser" />
      <main className="customize-page">
        <section className="product-hero">
          <div>
            <h1>{product.name}</h1>
            <strong>{chf(unitPrice)}</strong>
          </div>
          <img src={`/images/${product.image}`} alt={product.name} />
        </section>
        <section className="choice-section">
          <div className="section-heading"><div><h2>La recette</h2></div></div>
          <p>{product.description}</p>
        </section>
        <MultiChoiceSection title="Ajouter des garnitures" hint="+2.00 CHF chacune, optionnel" options={byCategory('garnish')} selected={garnish} onToggle={toggle(setGarnish)} />
        <MultiChoiceSection title="Ajouter une protéine" hint="+3.00 CHF chacune, optionnel" options={byCategory('extra_protein')} selected={extraProtein} onToggle={toggle(setExtraProtein)} />
        <MultiChoiceSection title="Ajouter des toppings" hint="+1.00 CHF chacun, optionnel" options={byCategory('topping')} selected={topping} onToggle={toggle(setTopping)} />
        <MultiChoiceSection title="Ajouter une sauce" hint="+1.00 CHF chacune, optionnel" options={byCategory('sauce')} selected={sauce} onToggle={toggle(setSauce)} />
      </main>
      <div className="sticky-footer customize-footer">
        <div className="quantity">
          <button onClick={() => setQuantity(Math.max(1, quantity - 1))} aria-label="Réduire la quantité"><Minus /></button>
          <b aria-live="polite">{quantity}</b>
          <button onClick={() => setQuantity(quantity + 1)} aria-label="Augmenter la quantité"><Plus /></button>
        </div>
        <PrimaryButton onClick={add}>Ajouter · {chf(unitPrice * quantity)}</PrimaryButton>
      </div>
    </div>
  );
}
