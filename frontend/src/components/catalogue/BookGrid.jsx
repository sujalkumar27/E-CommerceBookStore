// BookGrid.jsx — Renders a responsive grid of BookCard components.
//
// Props:
//   books       (array)    — array of BookSummary objects
//   onAddToCart (function) — forwarded to each BookCard
//   isLoggedIn  (boolean)  — forwarded to each BookCard

import BookCard from './BookCard.jsx';

export default function BookGrid({ books, onAddToCart, isLoggedIn }) {
  return (
    // Responsive grid: 1 col on mobile → 2 on sm → 3 on md → 4 on lg
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
      {books.map((book) => (
        <BookCard
          key={book.id}
          book={book}
          onAddToCart={onAddToCart}
          isLoggedIn={isLoggedIn}
        />
      ))}
    </div>
  );
}
