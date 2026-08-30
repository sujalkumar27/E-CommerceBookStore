// CartItemRow.jsx — One row in the CartPage showing a single cart item.
//
// Props:
//   item       (object)   — { id, book: { title, author, price, coverImageUrl }, quantity, lineTotal }
//   onUpdate   (function) — called with (itemId, newQuantity)
//   onRemove   (function) — called with (itemId)
//   disabled   (boolean)  — disables controls while an API call is in-progress

import { Link } from 'react-router-dom';

export default function CartItemRow({ item, onUpdate, onRemove, disabled }) {
  const { book } = item;

  return (
    <div className="flex gap-4 py-4 border-b border-gray-100 last:border-0">
      {/* Book cover thumbnail */}
      <Link to={`/books/${book.id}`} className="flex-shrink-0">
        <img
          src={book.coverImageUrl || 'https://via.placeholder.com/80x110?text=Book'}
          alt={book.title}
          className="w-16 h-22 object-cover rounded-lg"
        />
      </Link>

      {/* Book info */}
      <div className="flex-1 min-w-0">
        <Link to={`/books/${book.id}`} className="hover:text-blue-700">
          <h4 className="font-semibold text-gray-900 text-sm line-clamp-2">{book.title}</h4>
        </Link>
        <p className="text-xs text-gray-500 mt-0.5">{book.author}</p>
        <p className="text-sm font-medium text-gray-800 mt-1">₹{book.price?.toFixed(2)}</p>
      </div>

      {/* Quantity controls + remove */}
      <div className="flex flex-col items-end gap-2">
        <div className="flex items-center gap-2">
          <button
            onClick={() => item.quantity > 1 && onUpdate(item.id, item.quantity - 1)}
            disabled={disabled || item.quantity <= 1}
            className="w-8 h-8 rounded-full border border-gray-300 text-gray-600
                       hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed
                       text-lg font-bold leading-none flex items-center justify-center"
          >
            −
          </button>
          <span className="w-8 text-center text-sm font-semibold">{item.quantity}</span>
          <button
            onClick={() => onUpdate(item.id, item.quantity + 1)}
            disabled={disabled}
            className="w-8 h-8 rounded-full border border-gray-300 text-gray-600
                       hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed
                       text-lg font-bold leading-none flex items-center justify-center"
          >
            +
          </button>
        </div>

        {/* Line total */}
        <p className="text-sm font-bold text-gray-900">₹{item.lineTotal?.toFixed(2)}</p>

        {/* Remove */}
        <button
          onClick={() => onRemove(item.id)}
          disabled={disabled}
          className="text-xs text-red-500 hover:text-red-700 disabled:opacity-40"
        >
          Remove
        </button>
      </div>
    </div>
  );
}
