// CartPage.jsx — Shows the user's cart with quantity controls and checkout button.
//
// URL: /cart (auth required)
//
// WHAT THIS PAGE DOES:
//   1. GET /api/cart on mount — loads items + total
//   2. Inline quantity changes → PUT /api/cart/items/:id
//   3. Remove item → DELETE /api/cart/items/:id
//   4. GET /api/recommendations (shown in a panel below the cart)
//   5. "Proceed to Checkout" → navigate to /checkout

import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, updateItem, removeItem } from '../api/cartApi.js';
import { getRecommendations } from '../api/recommendationApi.js';
import { useCart } from '../context/CartContext.jsx';
import CartItemRow         from '../components/cart/CartItemRow.jsx';
import CartSummary         from '../components/cart/CartSummary.jsx';
import RecommendationPanel from '../components/recommendations/RecommendationPanel.jsx';
import LoadingSpinner      from '../components/common/LoadingSpinner.jsx';
import ErrorMessage        from '../components/common/ErrorMessage.jsx';
import EmptyState          from '../components/common/EmptyState.jsx';

export default function CartPage() {
  const navigate = useNavigate();
  const { refreshCartCount } = useCart();

  const [cart,           setCart]           = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const [loading,        setLoading]        = useState(true);
  const [error,          setError]          = useState(null);
  const [updating,       setUpdating]       = useState(false); // locks controls during API call

  const loadCart = useCallback(() => {
    setLoading(true);
    setError(null);
    getCart()
      .then((res) => setCart(res.data))
      .catch(() => setError('Could not load your cart. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadCart(); }, [loadCart]);

  // Load recommendations (non-critical — failure is silently ignored)
  useEffect(() => {
    getRecommendations()
      .then((res) => setRecommendations(res.data || []))
      .catch(() => {});
  }, []);

  const handleUpdateQuantity = async (itemId, newQty) => {
    setUpdating(true);
    try {
      const res = await updateItem(itemId, newQty);
      setCart(res.data);          // backend returns updated cart
      await refreshCartCount();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not update item.');
    } finally {
      setUpdating(false);
    }
  };

  const handleRemove = async (itemId) => {
    setUpdating(true);
    try {
      const res = await removeItem(itemId);
      setCart(res.data);
      await refreshCartCount();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not remove item.');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <div className="max-w-4xl mx-auto px-4 py-12"><LoadingSpinner message="Loading cart…" /></div>;

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">Your Cart</h1>

      {error && <ErrorMessage message={error} onRetry={loadCart} />}

      {!error && cart?.items?.length === 0 && (
        <EmptyState
          icon="🛒"
          title="Your cart is empty"
          subtitle="Find something great to read!"
          action={
            <button
              onClick={() => navigate('/')}
              className="bg-blue-600 text-white px-6 py-2 rounded-xl text-sm font-semibold hover:bg-blue-700"
            >
              Browse Books
            </button>
          }
        />
      )}

      {cart?.items?.length > 0 && (
        <div className="flex flex-col lg:flex-row gap-10">
          {/* Cart items list */}
          <div className="flex-1">
            {cart.items.map((item) => (
              <CartItemRow
                key={item.id}
                item={item}
                onUpdate={handleUpdateQuantity}
                onRemove={handleRemove}
                disabled={updating}
              />
            ))}
          </div>

          {/* Order summary sidebar */}
          <div className="w-full lg:w-80">
            <CartSummary
              cartTotal={cart.cartTotal}
              onCheckout={() => navigate('/checkout')}
              disabled={updating}
            />
          </div>
        </div>
      )}

      {/* Recommendations panel */}
      <RecommendationPanel books={recommendations} />
    </div>
  );
}
