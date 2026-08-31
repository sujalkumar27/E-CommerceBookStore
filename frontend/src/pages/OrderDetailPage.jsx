// OrderDetailPage.jsx — Full detail for one order.
//
// URL: /orders/:id (auth required)
//
// ACTIONS:
//   - Cancel order (if cancellable) → POST /api/orders/:id/cancel
//   - Buy Again → POST /api/orders/:id/buy-again
//
// FIX: Replaced window.confirm() with inline modal confirmation dialog.

import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getOrderById, cancelOrder, buyAgain } from '../api/orderApi.js';
import { useCart } from '../context/CartContext.jsx';
import OrderItemRow   from '../components/order/OrderItemRow.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorMessage   from '../components/common/ErrorMessage.jsx';

export default function OrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { refreshCartCount } = useCart();

  const [order,           setOrder]           = useState(null);
  const [loading,         setLoading]         = useState(true);
  const [error,           setError]           = useState(null);
  const [toast,           setToast]           = useState(null);
  // showCancelConfirm: whether the inline cancel confirmation modal is visible
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);

  const showToast = (msg, isError = false) => {
    setToast({ msg, isError });
    setTimeout(() => setToast(null), 3500);
  };

  const loadOrder = () => {
    setLoading(true);
    setError(null);
    getOrderById(id)
      .then((res) => {
        setOrder(res.data);
        document.title = `Order #${res.data.id?.slice(0, 8).toUpperCase()} | BookStore`;
      })
      .catch(() => setError('Could not load this order.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadOrder(); }, [id]);

  // Step 1: user clicks "Cancel Order" → show inline modal
  const handleCancel = () => setShowCancelConfirm(true);

  // Step 2a: user confirms inside the modal
  const handleConfirmCancel = async () => {
    setShowCancelConfirm(false);
    try {
      await cancelOrder(id);
      showToast('✅ Order cancelled.');
      loadOrder();
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not cancel order.'), true);
    }
  };

  const handleBuyAgain = async () => {
    try {
      await buyAgain(id);
      await refreshCartCount();
      showToast('✅ Items added to cart!');
      navigate('/cart');
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not add items.'), true);
    }
  };

  const formatDate = (iso) => iso
    ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
    : '—';

  const statusColour = {
    CONFIRMED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-600',
    DELIVERED: 'bg-blue-100 text-blue-700',
  };

  if (loading) return <div className="max-w-2xl mx-auto px-4 py-12"><LoadingSpinner message="Loading order…" /></div>;
  if (error)   return <div className="max-w-2xl mx-auto px-4 py-12"><ErrorMessage message={error} /></div>;
  if (!order)  return null;

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-10">
      {/* Inline cancel confirmation modal */}
      {showCancelConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-xl p-6 mx-4 max-w-sm w-full">
            <p className="font-semibold text-gray-900 mb-1">Cancel this order?</p>
            <p className="text-sm text-gray-500 mb-5">This action cannot be undone.</p>
            <div className="flex gap-3">
              <button
                onClick={handleConfirmCancel}
                className="flex-1 bg-red-600 text-white font-semibold py-2 rounded-xl hover:bg-red-700 text-sm"
              >
                Yes, cancel it
              </button>
              <button
                onClick={() => setShowCancelConfirm(false)}
                className="flex-1 border border-gray-300 text-gray-700 font-medium py-2 rounded-xl hover:bg-gray-50 text-sm"
              >
                Keep order
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className={`fixed top-20 right-4 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium
          ${toast.isError ? 'bg-red-600 text-white' : 'bg-green-600 text-white'}`}>
          {toast.msg}
        </div>
      )}

      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-6">
        <Link to="/orders" className="hover:text-blue-700">Orders</Link>
        <span className="mx-2">›</span>
        <span className="font-mono">{order.id?.slice(0, 8).toUpperCase()}</span>
      </nav>

      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">
            Order #{order.id?.slice(0, 8).toUpperCase()}
          </h1>
          <p className="text-xs text-gray-400 mt-0.5">Placed {formatDate(order.createdAt)}</p>
        </div>
        <span className={`text-xs font-semibold px-3 py-1.5 rounded-full ${statusColour[order.status] || 'bg-gray-100 text-gray-600'}`}>
          {order.status}
        </span>
      </div>

      {/* Items */}
      <div className="bg-white border border-gray-200 rounded-2xl p-5 mb-5">
        <h2 className="font-bold text-gray-800 mb-3">Items</h2>
        {order.items?.map((item, i) => <OrderItemRow key={i} item={item} />)}
      </div>

      {/* Price summary */}
      <div className="bg-white border border-gray-200 rounded-2xl p-5 mb-5 text-sm space-y-2">
        <div className="flex justify-between text-gray-600">
          <span>Order Total</span>
          <span>₹{order.orderTotal?.toFixed(2)}</span>
        </div>
        {order.giftPointsRedeemed > 0 && (
          <div className="flex justify-between text-green-600">
            <span>Gift Points Redeemed</span>
            <span>− ₹{order.giftPointsRedeemed}</span>
          </div>
        )}
        <div className="flex justify-between font-bold text-gray-900 border-t pt-2">
          <span>Amount Charged</span>
          <span>₹{order.amountCharged?.toFixed(2)}</span>
        </div>
      </div>

      {/* Delivery address */}
      {order.deliveryAddress && (
        <div className="bg-white border border-gray-200 rounded-2xl p-5 mb-5 text-sm">
          <h2 className="font-bold text-gray-800 mb-2">🚚 Delivery Address</h2>
          <p className="font-semibold">{order.deliveryAddress.fullName}</p>
          <p className="text-gray-600">{order.deliveryAddress.line1}</p>
          <p className="text-gray-600">
            {order.deliveryAddress.city}, {order.deliveryAddress.state} – {order.deliveryAddress.pincode}
          </p>
        </div>
      )}

      {/* Cancellation deadline */}
      {order.cancellable && order.cancellationDeadline && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-yellow-800 mb-6">
          ⚠️ Cancellable until <strong>{formatDate(order.cancellationDeadline)}</strong>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3">
        <button
          onClick={handleBuyAgain}
          className="flex-1 border border-blue-300 text-blue-700 font-semibold py-2.5 rounded-xl hover:bg-blue-50 text-sm"
        >
          Buy Again
        </button>
        {order.cancellable && (
          <button
            onClick={handleCancel}
            className="flex-1 border border-red-300 text-red-600 font-semibold py-2.5 rounded-xl hover:bg-red-50 text-sm"
          >
            Cancel Order
          </button>
        )}
      </div>
    </div>
  );
}
