// Pagination.jsx — Page navigation controls.
//
// Props:
//   currentPage  (number)   — 0-based page index (Spring Boot convention)
//   totalPages   (number)   — total number of pages from the API
//   onPageChange (function) — called with new 0-based page number
//
// WINDOWED PAGE NUMBERS:
//   Instead of rendering every page number (which breaks at scale), we show at
//   most 5 page buttons using a sliding window centred on the current page.
//   Ellipsis ("…") is shown when pages are skipped, and the first/last page is
//   always visible so users can jump to the ends.

const WINDOW = 2; // pages to show either side of the current page

/**
 * Build the array of page indices (numbers) and gap markers ("…") to render.
 * Always includes page 0, the last page, and up to WINDOW pages around current.
 */
function buildPageList(currentPage, totalPages) {
  const pages = new Set();
  pages.add(0);
  pages.add(totalPages - 1);
  for (let i = currentPage - WINDOW; i <= currentPage + WINDOW; i++) {
    if (i >= 0 && i < totalPages) pages.add(i);
  }

  // Convert to a sorted array and insert "…" wherever there is a gap > 1
  const sorted = Array.from(pages).sort((a, b) => a - b);
  const result = [];
  for (let j = 0; j < sorted.length; j++) {
    if (j > 0 && sorted[j] - sorted[j - 1] > 1) result.push('…');
    result.push(sorted[j]);
  }
  return result;
}

export default function Pagination({ currentPage, totalPages, onPageChange }) {
  if (totalPages <= 1) return null; // no controls needed for a single page

  const pageList = buildPageList(currentPage, totalPages);

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

      {/* Windowed page number pills */}
      {pageList.map((entry, idx) =>
        entry === '…' ? (
          // Gap marker — not interactive
          <span key={`gap-${idx}`} className="px-1 text-gray-400 text-sm select-none">
            …
          </span>
        ) : (
          <button
            key={entry}
            onClick={() => onPageChange(entry)}
            className={`w-9 h-9 text-sm font-medium rounded-lg transition-colors
              ${entry === currentPage
                ? 'bg-blue-600 text-white'
                : 'border border-gray-300 hover:bg-gray-100 text-gray-700'
              }`}
          >
            {entry + 1}
          </button>
        )
      )}

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
