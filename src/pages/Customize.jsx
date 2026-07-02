import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Check, CheckCircle, Minus, Plus } from '@phosphor-icons/react';
import { Header, PrimaryButton, chf } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';
import { bowlOptions } from '../data/catalog.js';

const steps = ['Base', 'Protéine', 'Garnitures', 'Sauce'];

export default function Customize() {
  const navigate = useNavigate();
  const { slug } = useParams();
  const { products, addToCart } = useShop();
  const product = products.find(p => p.slug === slug);

  // Le parcours commence toujours à l'étape 1
  const [step, setStep] = useState(1);
  const [base, setBase] = useState(null);
  const [protein, setProtein] = useState(null);
  const [toppings, setToppings] = useState([]);
  const [sauce, setSauce] = useState(null);
  const [quantity, setQuantity] = useState(1);

  const basePrice = product ? Number(product.promo_price ?? product.price) : 0;
  const extrasPrice = useMemo(
    () => toppings.reduce((n, t) => n + (bowlOptions.paidToppings[t] ?? 0), 0),
    [toppings]
  );
  const unitPrice = basePrice + extrasPrice;

  if (!product) {
    return (
      <div className="screen">
        <Header back="/menu" title="Personnaliser" />
        <main className="page-content"><p>Produit introuvable.</p></main>
      </div>
    );
  }

  const toggleTopping = name => setToppings(s =>
    s.includes(name) ? s.filter(x => x !== name)
      : s.length < bowlOptions.includedToppings ? [...s, name] : s);

  const stepDone = i =>
    (i === 1 && base) || (i === 2 && protein) || (i === 3 && toppings.length > 0) || (i === 4 && sauce);
  const canAdd = base && protein && sauce;

  const add = () => {
    addToCart({
      productId: typeof product.id === 'string' && product.id.length > 20 ? product.id : null,
      slug: product.slug,
      name: product.name,
      image: product.image,
      unitPrice,
      quantity,
      customizations: { base, protein, toppings, sauce },
    });
    navigate('/panier');
  };

  const simpleChoices = step === 1
    ? { title: 'Choisissez votre base', options: bowlOptions.bases, value: base, set: setBase }
    : step === 2
      ? { title: 'Choisissez votre protéine', options: bowlOptions.proteins, value: protein, set: setProtein }
      : { title: 'Choisissez votre sauce', options: bowlOptions.sauces, value: sauce, set: setSauce };

  return (
    <div className="screen">
      <Header back="/menu" title="Personnaliser" progress="Étape 2 sur 4" />
      <main className="customize-page">
        <section className="product-hero">
          <div>
            <h1>{product.name}</h1>
            <strong>{chf(unitPrice)}</strong>
            <button>Infos & allergènes</button>
          </div>
          <img src={`/images/${product.image}`} alt={product.name} />
        </section>
        <nav className="stepper" aria-label="Étapes de personnalisation">
          {steps.map((s, i) => (
            <button
              key={s}
              onClick={() => setStep(i + 1)}
              className={step === i + 1 ? 'active' : stepDone(i + 1) ? 'done' : ''}
              aria-current={step === i + 1 ? 'step' : undefined}
            >
              <b>{stepDone(i + 1) && step !== i + 1 ? <Check /> : i + 1}</b><span>{s}</span>
            </button>
          ))}
        </nav>
        {step === 3 ? (
          <section className="choice-section">
            <div className="section-heading">
              <div><h2>Choisissez vos garnitures</h2><p>{bowlOptions.includedToppings} incluses</p></div>
              <span aria-live="polite">{toppings.length}/{bowlOptions.includedToppings} choisies</span>
            </div>
            <div className="topping-grid">
              {bowlOptions.toppings.map((t, i) => (
                <button key={t} className={toppings.includes(t) ? 'selected' : ''} onClick={() => toggleTopping(t)} aria-pressed={toppings.includes(t)}>
                  <img src="/images/bowl-salmon.png" style={{ objectPosition: `${(i % 3) * 38 + 12}% ${Math.floor(i / 3) * 45 + 28}%` }} alt="" />
                  <span>{t}</span>
                  {toppings.includes(t) ? <CheckCircle weight="fill" /> : <i />}
                  {bowlOptions.paidToppings[t] && <small>+ {chf(bowlOptions.paidToppings[t])}</small>}
                </button>
              ))}
            </div>
            <div className="step-nav">
              <button className="outline-button" onClick={() => setStep(4)}>Continuer vers la sauce</button>
            </div>
          </section>
        ) : (
          <section className="choice-section simple-options">
            <h2>{simpleChoices.title}</h2>
            {simpleChoices.options.map(option => (
              <button
                key={option}
                className={simpleChoices.value === option ? 'selected' : ''}
                onClick={() => { simpleChoices.set(option); setStep(Math.min(step + 1, 4)); }}
                aria-pressed={simpleChoices.value === option}
              >
                <span className="radio">{simpleChoices.value === option && <i />}</span>
                <b>{option}</b>
                {simpleChoices.value === option && <Check />}
              </button>
            ))}
          </section>
        )}
      </main>
      <div className="sticky-footer customize-footer">
        <div className="quantity">
          <button onClick={() => setQuantity(Math.max(1, quantity - 1))} aria-label="Réduire la quantité"><Minus /></button>
          <b aria-live="polite">{quantity}</b>
          <button onClick={() => setQuantity(quantity + 1)} aria-label="Augmenter la quantité"><Plus /></button>
        </div>
        <PrimaryButton disabled={!canAdd} onClick={add}>
          {canAdd ? `Ajouter · ${chf(unitPrice * quantity)}` : 'Complétez votre bowl'}
        </PrimaryButton>
      </div>
    </div>
  );
}
