import { useParams } from 'react-router-dom';
import { Header } from '../components/ui.jsx';
import { useShop } from '../context/ShopContext.jsx';
import BowlBuilder from './BowlBuilder.jsx';
import SignatureExtras from './SignatureExtras.jsx';

export default function Customize() {
  const { slug } = useParams();
  const { products } = useShop();
  const product = products.find(p => p.slug === slug);

  if (!product) {
    return (
      <div className="screen">
        <Header back="/menu" title="Personnaliser" />
        <main className="page-content"><p>Produit introuvable.</p></main>
      </div>
    );
  }

  if (product.category === 'build_your_own') return <BowlBuilder product={product} />;
  if (product.category === 'signature') return <SignatureExtras product={product} />;

  // Filet de sécurité : les catégories sans personnalisation (Desserts,
  // Boissons) sont ajoutées directement au panier depuis Menu.jsx et ne
  // devraient jamais atteindre cette route.
  return (
    <div className="screen">
      <Header back="/menu" title="Personnaliser" />
      <main className="page-content"><p>{product.name} ne nécessite pas de personnalisation.</p></main>
    </div>
  );
}
