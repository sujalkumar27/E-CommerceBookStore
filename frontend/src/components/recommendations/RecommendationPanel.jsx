// RecommendationPanel.jsx — Horizontal scrollable strip of recommended books.
// Shown in CartPage and on ProductDetailPage.
//
// Props:
//   books (array) — array of BookSummary objects (from /api/recommendations)

import { Link } from 'react-router-dom';

export default function RecommendationPanel({ books }) {
  if (!books || books.length === 0) return null;

  return (
    <section className="mt-10">
      <h2 className="text-lg font-bold text-gray-800 mb-4">You May Also Like</h2>
      <div className="flex gap-4 overflow-x-auto pb-3 scrollbar-thin">
        {books.map((book) => (
          <Link
            key={book.id}
            to={`/books/${book.id}`}
            className="flex-shrink-0 w-36 bg-white border border-gray-200 rounded-xl overflow-hidden
                       shadow-sm hover:shadow-md transition-shadow"
          >
            <img
              src={book.coverImageUrl || 'https://via.placeholder.com/144x200?text=Book'}
              alt={book.title}
              className="w-full h-48 object-cover"
              loading="lazy"
            />
            <div className="p-2">
              <p className="text-xs font-semibold text-gray-800 line-clamp-2">{book.title}</p>
              <p className="text-xs text-gray-500 mt-0.5">{book.author}</p>
              <p className="text-sm font-bold text-gray-900 mt-1">₹{book.price?.toFixed(2)}</p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
