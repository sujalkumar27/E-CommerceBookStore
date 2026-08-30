// Pagination.jsx — Page navigation controls.
//
// Props:
//   currentPage  (number)   — 0-based page index (Spring Boot convention)
//   totalPages   (number)   — total number of pages from the API
//   onPageChange (function) — called with new 0-based page number

export default function Pagination({ currentPage, totalPages, onPageChange }) {
  if (totalPages <= 1) return null; // no controls needed for a single page

  return (
    <div className="flex items-center justify-center gap-2 mt-8">
      {/* Previous button */}
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className="px-4 py-2 text-sm font-medium border border-gray-300 rounded-lg
                   disabled:opacity-40 disabled:cursor-not-allowed
                   hover:bg-gray-100 transition-colors"
      >
        ← Prev
      </button>

      {/* Page number pills */}
      {Array.from({ length: totalPages }, (_, i) => (
        <button
          key={i}
          onClick={() => onPageChange(i)}
          className={`w-9 h-9 text-sm font-medium rounded-lg transition-colors
            ${i === currentPage
              ? 'bg-blue-600 text-white'
              : 'border border-gray-300 hover:bg-gray-100 text-gray-700'
            }`}
        >
          {i + 1}
        </button>
      ))}

      {/* Next button */}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        className="px-4 py-2 text-sm font-medium border border-gray-300 rounded-lg
                   disabled:opacity-40 disabled:cursor-not-allowed
                   hover:bg-gray-100 transition-colors"
      >
        Next →
      </button>
    </div>
  );
}
