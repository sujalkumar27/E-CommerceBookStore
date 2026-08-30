// OrderHistoryPage.jsx — Lists all orders for the logged-in user.
//
// URL: /orders (auth required)
//
// ACTIONS:
//   - Click an order → navigate to /orders/:id
//   - Buy Again → POST /api/orders/:id/buy-again (re-adds items to cart)
//   - Cancel    → POST /api/orders/:id/cancel (only shown if order.cancellable is true)

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOrders, cancelOrder, buyAgain } from '../api/orderApi.js';
import { useCart } from '../context/CartContext.jsx';
import OrderCard      from '../components/order/OrderCard.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorMessage   from '../components/common/ErrorMessage.jsx';
import EmptyState     from '../components/common/EmptyState.jsx';

export default function OrderHistoryPage() {
  const navigate = useNavigate();
  const { refreshCartCount } = useCart();

  const [orders,  setOrders]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);
  const [toast,   setToast]   = useState(null);

  const loadOrders = () => {
    setLoading(true);
    setError(null);
    getOrders()
      .then((res) => setOrders(res.data || []))
      .catch(() => setError('Could not load orders. Please try again.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadOrders(); }, []);

  const showToast = (msg, isError = false) => {
    setToast({ msg, isError });
    setTimeout(() => setToast(null), 3500);
  };

  const handleBuyAgain = async (orderId) => {
    try {
      await buyAgain(orderId);
      await refreshCartCount();
      showToast('✅ Items added to cart!');
      navigate('/cart');
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not add items to cart.'), true);
    }
  };

  const handleCancel = async (orderId) => {
    if (!window.confirm('Are you sure you want to cancel this order?')) return;
    try {
      await cancelOrder(orderId);
      showToast('✅ Order cancelled.');
      loadOrders(); // refresh the list
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not cancel order.'), true);
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-10">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-20 right-4 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium
          ${toast.isError ? 'bg-red-600 text-white' : 'bg-green-600 text-white'}`}>
          {toast.msg}
        </div>
      )}

      <h1 className="text-2xl font-bold text-gray-900 mb-8">Order History</h1>

      {loading && <LoadingSpinner message="Loading orders…" />}
      {!loading && error && <ErrorMessage message={error} onRetry={loadOrders} />}
      {!loading && !error && orders.length === 0 && (
        <EmptyState
          icon="📦"
          title="No orders yet"
          subtitle="Your orders will appear here after you make a purchase."
          action={
            <button
              onClick={() => navigate('/')}
              className="bg-blue-600 text-white px-6 py-2 rounded-xl text-sm font-semibold hover:bg-blue-700"
            >
              Start Shopping
            </button>
          }
        />
      )}

      {!loading && !error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              onCancel={handleCancel}
              onBuyAgain={handleBuyAgain}
            />
          ))}
        </div>
      )}
    </div>
  );
}
