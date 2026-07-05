import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, Minus, Plus } from '@phosphor-icons/react';
import { Header, PrimaryButton, SingleChoiceSection, MultiChoiceSection, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';

const STEP_NAMES = ['Base', 'Garniture', 'Protéine', 'Protéine extra', 'Topping', 'Sauces'];

export default function BowlBuilder({ product }) {
  const navigate = useNavigate();
  const { optionItems, addToCart } = useShop();

  const [step, setStep] = useState(1);
  // Étape la plus avancée atteinte : on ne peut pas cliquer plus loin que
  // celle-ci (impossible de sauter les étapes), mais on peut revenir en arrière.
  const [maxStep, setMaxStep] = useState(1);
  const advance = n => { setStep(n); setMaxStep(m => Math.max(m, n)); };
  const stepperRef = useRef(null);
  // Sur petit écran le stepper (6 étapes) défile : on recentre l'étape
  // active à chaque changement pour qu'elle reste toujours visible.
  useEffect(() => {
    stepperRef.current?.querySelector('.active')
      ?.scrollIntoView({ inline: 'center', block: 'nearest', behavior: 'smooth' });
  }, [step]);
  const [base, setBase] = useState(null);
  const [protein, setProtein] = useState(null);
  const [garnish, setGarnish] = useState([]);
  const [extraProtein, setExtraProtein] = useState([]);
  const [topping, setTopping] = useState([]);
  const [sauce, setSauce] = useState([]);
  const [quantity, setQuantity] = useState(1);

  const byCategory = category => optionItems.filter(o => o.category === category).sort((a, b) => a.sort_order - b.sort_order);
  const priceOf = (category, name) => optionItems.find(o => o.category === category && o.name === name)?.price ?? 0;
  const toggle = setList => name => setList(list => list.includes(name) ? list.filter(x => x !== name) : [...list, name]);

  const unitPrice = useMemo(() => {
    let total = 0;
    if (base) total += priceOf('base', base);
    if (protein) total += priceOf('protein', protein);
    total += garnish.reduce((n, g) => n + priceOf('garnish', g), 0);
    total += extraProtein.reduce((n, g) => n + priceOf('extra_protein', g), 0);
    total += topping.reduce((n, g) => n + priceOf('topping', g), 0);
    total += sauce.reduce((n, g) => n + priceOf('sauce', g), 0);
    return total;
  }, [base, protein, garnish, extraProtein, topping, sauce, optionItems]);

  const canAdd = Boolean(base && protein);
  const stepDone = i => step > i;

  const add = () => {
    addToCart({
      productId: typeof product.id === 'string' && product.id.length > 20 ? product.id : null,
      slug: product.slug,
      name: product.name,
      image: product.image,
      unitPrice,
      quantity,
      customizations: { base, protein, garnish, extra_protein: extraProtein, topping, sauce },
    });
    navigate('/panier');
  };

  return (
    <div className="screen">
      <Header back="/menu" title="Personnaliser" progress={`Étape ${step} sur 6`} />
      <main className="customize-page">
        <section className="product-hero">
          <div>
            <h1>{product.name}</h1>
            <strong>{chf(unitPrice)}</strong>
          </div>
          <img src={`/images/${product.image}`} alt={product.name} />
        </section>
        <nav className="stepper" ref={stepperRef} aria-label="Étapes de personnalisation">
          {STEP_NAMES.map((s, i) => (
            <button
              key={s}
              onClick={() => setStep(i + 1)}
              disabled={i + 1 > maxStep}
              className={step === i + 1 ? 'active' : stepDone(i + 1) ? 'done' : ''}
              aria-current={step === i + 1 ? 'step' : undefined}
            >
              <b>{stepDone(i + 1) && step !== i + 1 ? <Check /> : i + 1}</b><span>{s}</span>
            </button>
          ))}
        </nav>

        {step === 1 && (
          <SingleChoiceSection
            title="Choisissez votre base"
            options={byCategory('base')}
            value={base}
            onSelect={name => { setBase(name); advance(2); }}
          />
        )}
        {step === 2 && (
          <>
            <MultiChoiceSection
              title="Choisissez vos garnitures"
              hint="Optionnel, autant que vous voulez"
              options={byCategory('garnish')}
              selected={garnish}
              onToggle={toggle(setGarnish)}
            />
            <div className="step-nav"><button className="outline-button" onClick={() => advance(3)}>Continuer vers la protéine</button></div>
          </>
        )}
        {step === 3 && (
          <SingleChoiceSection
            title="Choisissez votre protéine"
            options={byCategory('protein')}
            value={protein}
            onSelect={name => { setProtein(name); advance(4); }}
          />
        )}
        {step === 4 && (
          <>
            <MultiChoiceSection
              title="Protéine supplémentaire"
              hint="Optionnel"
              options={byCategory('extra_protein')}
              selected={extraProtein}
              onToggle={toggle(setExtraProtein)}
            />
            <div className="step-nav"><button className="outline-button" onClick={() => advance(5)}>Continuer vers le topping</button></div>
          </>
        )}
        {step === 5 && (
          <>
            <MultiChoiceSection
              title="Choisissez vos toppings"
              hint="Optionnel"
              options={byCategory('topping')}
              selected={topping}
              onToggle={toggle(setTopping)}
            />
            <div className="step-nav"><button className="outline-button" onClick={() => advance(6)}>Continuer vers la sauce</button></div>
          </>
        )}
        {step === 6 && (
          <MultiChoiceSection
            title="Choisissez vos sauces"
            hint="Optionnel"
            options={byCategory('sauce')}
            selected={sauce}
            onToggle={toggle(setSauce)}
          />
        )}
      </main>
      <div className="sticky-footer customize-footer">
        <div className="quantity">
          <button onClick={() => setQuantity(Math.max(1, quantity - 1))} aria-label="Réduire la quantité"><Minus /></button>
          <b aria-live="polite">{quantity}</b>
          <button onClick={() => setQuantity(quantity + 1)} aria-label="Augmenter la quantité"><Plus /></button>
        </div>
        <PrimaryButton disabled={!canAdd} onClick={add}>
          {canAdd ? `Ajouter · ${chf(unitPrice * quantity)}` : 'Choisissez base et protéine'}
        </PrimaryButton>
      </div>
    </div>
  );
}
