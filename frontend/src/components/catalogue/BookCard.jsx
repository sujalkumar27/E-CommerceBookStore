// BookCard.jsx — Displays one book in the catalogue grid.
// Redesigned: richer card with hover effect, category badge, better typography.

import { Link } from 'react-router-dom';

export default function BookCard({ book, onAddToCart, isLoggedIn }) {
  return (
    <div className="bg-white border border-gray-100 rounded-2xl overflow-hidden shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-200 flex flex-col group">
      {/* Book cover image */}
      <Link to={`/books/${book.id}`} className="block relative overflow-hidden bg-gradient-to-br from-slate-100 to-slate-200">
        <img
          src={book.coverImageUrl || `https://placehold.co/200x280/1e3a5f/ffffff?text=${encodeURIComponent(book.title.slice(0,12))}`}
          alt={`Cover of ${book.title}`}
          className="w-full h-52 object-cover group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
          onError={(e) => { e.target.src = `https://placehold.co/200x280/1e3a5f/ffffff?text=📚`; }}
        />
        {/* Availability ribbon */}
        {!book.available && (
          <div className="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
            Out of Stock
          </div>
        )}
      </Link>

      <div className="p-4 flex flex-col flex-1">
        {/* Category + year */}
        <div className="flex items-center justify-between mb-2">
          {book.category && (
            <span className="text-xs font-bold text-blue-600 uppercase tracking-wide bg-blue-50 px-2 py-0.5 rounded-full">
              {book.category.name}
            </span>
          )}
          {book.publishedYear && (
            <span className="text-xs text-gray-400">{book.publishedYear}</span>
          )}
        </div>

        {/* Title */}
        <Link to={`/books/${book.id}`} className="hover:text-blue-700 flex-1">
          <h3 className="font-bold text-gray-900 text-sm leading-snug line-clamp-2 mb-1">
            {book.title}
          </h3>
        </Link>

        <p className="text-xs text-gray-500 mb-3">{book.author}</p>

        {/* Price + stock */}
        <div className="flex items-center justify-between mb-3">
          <span className="text-lg font-extrabold text-gray-900">₹{book.price?.toFixed(2)}</span>
          {book.available && (
            <span className="text-xs text-emerald-600 font-semibold flex items-center gap-1">
              ✓ In Stock
            </span>
          )}
        </div>

        {/* CTA button */}
        <button
          onClick={() => onAddToCart(book.id)}
          disabled={!book.available}
          className={`w-full py-2.5 text-sm font-bold rounded-xl transition-all
            ${book.available
              ? isLoggedIn
                ? 'bg-slate-900 hover:bg-blue-600 text-white'
                : 'bg-blue-50 hover:bg-blue-600 text-blue-700 hover:text-white border border-blue-200'
              : 'bg-gray-100 text-gray-400 cursor-not-allowed'
            }`}
        >
          {book.available
            ? isLoggedIn ? '🛒 Add to Cart' : '🔑 Login to Buy'
            : 'Unavailable'}
        </button>
      </div>
    </div>
  );
}
