// BookCard.jsx — Displays one book in the catalogue grid.
//
// Props:
//   book      (object)   — BookSummary from the API
//   onAddToCart (function) — called with bookId when "Add to Cart" is clicked
//   isLoggedIn  (boolean) — if false, "Add to Cart" redirects to /login

import { Link } from 'react-router-dom';

export default function BookCard({ book, onAddToCart, isLoggedIn }) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-shadow flex flex-col">
      {/* Book cover image — fallback to a placeholder if no URL */}
      <Link to={`/books/${book.id}`} className="block">
        <img
          src={book.coverImageUrl || `https://via.placeholder.com/200x280?text=${encodeURIComponent(book.title)}`}
          alt={`Cover of ${book.title}`}
          className="w-full h-48 object-cover"
          loading="lazy"
          onError={(e) => { e.target.src = `https://via.placeholder.com/200x280?text=No+Cover`; }}
        />
      </Link>

      <div className="p-4 flex flex-col flex-1">
        {/* Category badge */}
        {book.category && (
          <span className="text-xs text-blue-600 font-medium mb-1">
            {book.category.name}
          </span>
        )}

        {/* Title — links to product detail page */}
        <Link to={`/books/${book.id}`} className="hover:text-blue-700">
          <h3 className="font-semibold text-gray-900 text-sm line-clamp-2 mb-1">
            {book.title}
          </h3>
        </Link>

        <p className="text-xs text-gray-500 mb-2">{book.author}</p>

        {/* Price and availability */}
        <div className="mt-auto flex items-center justify-between">
          <span className="font-bold text-gray-900">₹{book.price?.toFixed(2)}</span>
          {book.available ? (
            <span className="text-xs text-green-600 font-medium">In Stock</span>
          ) : (
            <span className="text-xs text-red-500 font-medium">Out of Stock</span>
          )}
        </div>

        {/* Add to Cart button */}
        <button
          onClick={() => onAddToCart(book.id)}
          disabled={!book.available}
          className={`mt-3 w-full py-2 text-sm font-semibold rounded-lg transition-colors
            ${book.available
              ? 'bg-blue-600 text-white hover:bg-blue-700'
              : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
        >
          {book.available
            ? isLoggedIn ? 'Add to Cart' : 'Login to Buy'
            : 'Unavailable'}
        </button>
      </div>
    </div>
  );
}
