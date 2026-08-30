// ProductDetailPage.jsx — Full detail view for one book.
//
// URL: /books/:id
// Public: yes (guests can view)
//
// WHAT THIS PAGE DOES:
//   1. Reads :id from the URL params
//   2. Calls GET /api/books/:id
//   3. Shows full book info: cover, description, ISBN, publisher, delivery estimate
//   4. "Add to Cart" button — auth required; guests are redirected to /login
//   5. Related books strip at the bottom

import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getBookById } from '../api/bookApi.js';
import { addItem } from '../api/cartApi.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorMessage   from '../components/common/ErrorMessage.jsx';

export default function ProductDetailPage() {
  const { id } = useParams();
  const { isLoggedIn } = useAuth();
  const { refreshCartCount } = useCart();
  const navigate = useNavigate();

  const [book,    setBook]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);
  const [adding,  setAdding]  = useState(false);
  const [toast,   setToast]   = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getBookById(id)
      .then((res) => setBook(res.data))
      .catch(() => setError('Book not found or could not be loaded.'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleAddToCart = async () => {
    if (!isLoggedIn) { navigate('/login'); return; }
    setAdding(true);
    try {
      await addItem(id, 1);
      await refreshCartCount();
      setToast({ msg: '✅ Added to cart!', isError: false });
    } catch (err) {
      setToast({ msg: '❌ ' + (err.response?.data?.message || 'Could not add to cart.'), isError: true });
    } finally {
      setAdding(false);
      setTimeout(() => setToast(null), 3000);
    }
  };

  // Calculate tentative delivery date from today + offset days
  const deliveryDate = book?.tentativeDeliveryDays
    ? new Date(Date.now() + book.tentativeDeliveryDays * 86_400_000)
        .toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
    : null;

  if (loading) return <div className="max-w-4xl mx-auto px-4 py-12"><LoadingSpinner message="Loading book details…" /></div>;
  if (error)   return <div className="max-w-4xl mx-auto px-4 py-12"><ErrorMessage message={error} /></div>;
  if (!book)   return null;

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-20 right-4 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium
          ${toast.isError ? 'bg-red-600 text-white' : 'bg-green-600 text-white'}`}>
          {toast.msg}
        </div>
      )}

      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-6">
        <Link to="/" className="hover:text-blue-700">Home</Link>
        <span className="mx-2">›</span>
        <span className="text-gray-700">{book.title}</span>
      </nav>

      {/* Main layout: cover left, details right */}
      <div className="flex flex-col md:flex-row gap-10">
        {/* Book cover */}
        <div className="flex-shrink-0 w-full md:w-64">
          <img
            src={book.coverImageUrl || `https://via.placeholder.com/256x360?text=${encodeURIComponent(book.title)}`}
            alt={`Cover of ${book.title}`}
            className="w-full rounded-2xl shadow-md object-cover"
          />
        </div>

        {/* Details */}
        <div className="flex-1">
          {book.category && (
            <span className="text-xs text-blue-600 font-semibold uppercase tracking-wider">
              {book.category.name}
            </span>
          )}
          <h1 className="text-3xl font-bold text-gray-900 mt-1 mb-2">{book.title}</h1>
          <p className="text-lg text-gray-600 mb-1">by {book.author}</p>
          {book.publisher && <p className="text-sm text-gray-400 mb-4">{book.publisher} · {book.publishedYear}</p>}

          {/* Price */}
          <p className="text-3xl font-bold text-gray-900 mb-4">₹{book.price?.toFixed(2)}</p>

          {/* Availability */}
          <p className={`text-sm font-semibold mb-2 ${book.available ? 'text-green-600' : 'text-red-500'}`}>
            {book.available ? '✅ In Stock' : '❌ Out of Stock'}
          </p>

          {/* Delivery estimate */}
          {deliveryDate && (
            <p className="text-sm text-gray-600 mb-5">
              🚚 Estimated delivery by <strong>{deliveryDate}</strong>
            </p>
          )}

          {/* Add to Cart */}
          <button
            onClick={handleAddToCart}
            disabled={!book.available || adding}
            className={`px-8 py-3 rounded-xl text-base font-bold transition-colors mb-6
              ${book.available
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-gray-200 text-gray-400 cursor-not-allowed'
              } disabled:opacity-60`}
          >
            {adding ? 'Adding…' : book.available ? 'Add to Cart' : 'Unavailable'}
          </button>

          {/* Metadata table */}
          <table className="text-sm text-gray-600 border-collapse w-full max-w-sm">
            <tbody>
              {book.isbn && <tr><td className="py-1 pr-4 text-gray-400 w-28">ISBN</td><td>{book.isbn}</td></tr>}
              {book.publisher && <tr><td className="py-1 pr-4 text-gray-400">Publisher</td><td>{book.publisher}</td></tr>}
              {book.publishedYear && <tr><td className="py-1 pr-4 text-gray-400">Year</td><td>{book.publishedYear}</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      {/* Description */}
      {book.description && (
        <div className="mt-10">
          <h2 className="text-lg font-bold text-gray-800 mb-2">About This Book</h2>
          <p className="text-gray-600 leading-relaxed whitespace-pre-line">{book.description}</p>
        </div>
      )}

      {/* Related books */}
      {book.relatedBooks?.length > 0 && (
        <div className="mt-12">
          <h2 className="text-lg font-bold text-gray-800 mb-4">Related Books</h2>
          <div className="flex gap-4 overflow-x-auto pb-2">
            {book.relatedBooks.map((rb) => (
              <Link
                key={rb.id}
                to={`/books/${rb.id}`}
                className="flex-shrink-0 w-36 bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm hover:shadow-md"
              >
                <img
                  src={rb.coverImageUrl || 'https://via.placeholder.com/144x200?text=Book'}
                  alt={rb.title}
                  className="w-full h-48 object-cover"
                  loading="lazy"
                />
                <div className="p-2">
                  <p className="text-xs font-semibold text-gray-800 line-clamp-2">{rb.title}</p>
                  <p className="text-xs text-gray-500">{rb.author}</p>
                  <p className="text-sm font-bold mt-1">₹{rb.price?.toFixed(2)}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
