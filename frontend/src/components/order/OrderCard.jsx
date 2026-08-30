// OrderCard.jsx — A summary row in the order history list (OrderHistoryPage).
//
// Props:
//   order      (object)   — OrderDto from the API
//   onCancel   (function) — called with orderId
//   onBuyAgain (function) — called with orderId

import { Link } from 'react-router-dom';

export default function OrderCard({ order, onCancel, onBuyAgain }) {
  // Format ISO date string to a readable date
  const formatDate = (iso) => iso ? new Date(iso).toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  }) : '—';

  // Status badge colour mapping
  const statusColour = {
    CONFIRMED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-600',
    DELIVERED: 'bg-blue-100 text-blue-700',
  };

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex flex-wrap items-start justify-between gap-2 mb-3">
        {/* Order ID + date */}
        <div>
          <Link to={`/orders/${order.id}`} className="text-sm font-semibold text-blue-700 hover:underline">
            Order #{order.id.slice(0, 8).toUpperCase()}
          </Link>
          <p className="text-xs text-gray-400 mt-0.5">Placed {formatDate(order.createdAt)}</p>
        </div>

        {/* Status badge */}
        <span className={`text-xs font-semibold px-3 py-1 rounded-full ${statusColour[order.status] || 'bg-gray-100 text-gray-600'}`}>
          {order.status}
        </span>
      </div>

      {/* First 2 item titles (preview) */}
      <div className="text-sm text-gray-600 mb-3">
        {order.items?.slice(0, 2).map((item) => (
          <p key={item.bookId} className="line-clamp-1">
            📖 {item.title} × {item.quantity}
          </p>
        ))}
        {order.items?.length > 2 && (
          <p className="text-xs text-gray-400">+{order.items.length - 2} more item(s)</p>
        )}
      </div>

      {/* Total + actions */}
      <div className="flex items-center justify-between">
        <span className="font-bold text-gray-900">₹{order.orderTotal?.toFixed(2)}</span>
        <div className="flex gap-2">
          <button
            onClick={() => onBuyAgain(order.id)}
            className="text-xs font-medium text-blue-600 border border-blue-300 px-3 py-1.5 rounded-lg hover:bg-blue-50"
          >
            Buy Again
          </button>
          {order.cancellable && (
            <button
              onClick={() => onCancel(order.id)}
              className="text-xs font-medium text-red-600 border border-red-300 px-3 py-1.5 rounded-lg hover:bg-red-50"
            >
              Cancel
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
