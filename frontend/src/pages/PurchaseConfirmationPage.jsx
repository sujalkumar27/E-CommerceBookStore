// PurchaseConfirmationPage.jsx — Order success summary after payment.
//
// URL: /checkout/confirmation (auth required)
//
// DATA SOURCE:
//   The PaymentResponse object is passed via React Router `state` from CheckoutPage.
//   No extra API call is needed — everything we need is in location.state.order.
//
// If the user navigates here directly (no state), redirect to /orders.

import { useLocation, useNavigate, Link } from 'react-router-dom';
import OrderItemRow from '../components/order/OrderItemRow.jsx';

export default function PurchaseConfirmationPage() {
  const location = useLocation();
  const navigate  = useNavigate();
  const order = location.state?.order;

  // Guard: no order data means the user refreshed or navigated directly
  if (!order) {
    navigate('/orders', { replace: true });
    return null;
  }

  const formatDate = (iso) => iso
    ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
    : '—';

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-12">
      {/* Success header */}
      <div className="text-center mb-10">
        <div className="text-6xl mb-4">🎉</div>
        <h1 className="text-2xl font-bold text-gray-900">Order Confirmed!</h1>
        <p className="text-gray-500 mt-1 text-sm">
          Order ID: <span className="font-mono font-semibold text-gray-700">{order.orderId?.slice(0, 8).toUpperCase()}</span>
        </p>
        <p className="text-xs text-gray-400 mt-1">Confirmed at {formatDate(order.paymentConfirmedAt)}</p>
      </div>

      {/* Items */}
      <div className="bg-white border border-gray-200 rounded-2xl p-6 mb-6">
        <h2 className="font-bold text-gray-800 mb-4">Items Ordered</h2>
        {order.items?.map((item, i) => (
          <OrderItemRow key={i} item={item} />
        ))}
      </div>

      {/* Price breakdown */}
      <div className="bg-white border border-gray-200 rounded-2xl p-6 mb-6 space-y-2 text-sm">
        <h2 className="font-bold text-gray-800 mb-3">Payment Summary</h2>
        <div className="flex justify-between text-gray-600">
          <span>Order Total</span>
          <span>₹{order.orderTotal?.toFixed(2)}</span>
        </div>
        {order.giftPointsRedeemed > 0 && (
          <div className="flex justify-between text-green-600">
            <span>Gift Points Redeemed ({order.giftPointsRedeemed} pts)</span>
            <span>− ₹{order.giftPointsRedeemed?.toFixed(2)}</span>
          </div>
        )}
        <div className="flex justify-between font-bold text-gray-900 border-t pt-2 mt-2">
          <span>Amount Charged</span>
          <span>₹{order.amountCharged?.toFixed(2)}</span>
        </div>
        {order.remainingGiftPointBalance !== undefined && (
          <div className="flex justify-between text-yellow-600 text-xs pt-1">
            <span>Remaining Gift Points</span>
            <span>{order.remainingGiftPointBalance} pts</span>
          </div>
        )}
      </div>

      {/* Delivery address */}
      {order.deliveryAddress && (
        <div className="bg-white border border-gray-200 rounded-2xl p-6 mb-6 text-sm">
          <h2 className="font-bold text-gray-800 mb-3">🚚 Delivery Address</h2>
          <p className="font-semibold text-gray-900">{order.deliveryAddress.fullName}</p>
          <p className="text-gray-600">{order.deliveryAddress.line1}</p>
          <p className="text-gray-600">{order.deliveryAddress.city}, {order.deliveryAddress.state} – {order.deliveryAddress.pincode}</p>
        </div>
      )}

      {/* Cancellation deadline */}
      {order.cancellationDeadline && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-yellow-800 mb-8">
          ⚠️ You can cancel this order until{' '}
          <strong>{formatDate(order.cancellationDeadline)}</strong>
        </div>
      )}

      {/* Actions */}
      <div className="flex flex-col sm:flex-row gap-3">
        <Link
          to="/"
          className="flex-1 text-center bg-blue-600 text-white font-semibold py-3 rounded-xl hover:bg-blue-700 transition-colors"
        >
          Continue Shopping
        </Link>
        <Link
          to="/orders"
          className="flex-1 text-center border border-gray-300 text-gray-700 font-semibold py-3 rounded-xl hover:bg-gray-50 transition-colors"
        >
          View Order History
        </Link>
      </div>
    </div>
  );
}
