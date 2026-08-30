// OrderItemRow.jsx — One book row inside an order detail view.
//
// Props:
//   item (object) — { bookId, title, author, quantity, lineTotal, tentativeDeliveryDate }

export default function OrderItemRow({ item }) {
  return (
    <div className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0 text-sm">
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-gray-900 line-clamp-1">{item.title}</p>
        {item.author && <p className="text-xs text-gray-500">{item.author}</p>}
        {item.tentativeDeliveryDate && (
          <p className="text-xs text-blue-600 mt-0.5">
            Est. delivery: {new Date(item.tentativeDeliveryDate).toLocaleDateString('en-IN', {
              year: 'numeric', month: 'short', day: 'numeric',
            })}
          </p>
        )}
      </div>
      <div className="text-right flex-shrink-0 ml-4">
        <p className="text-gray-600">× {item.quantity}</p>
        <p className="font-bold text-gray-900">₹{item.lineTotal?.toFixed(2)}</p>
      </div>
    </div>
  );
}
