// FilterPanel.jsx — Sidebar with category, price, availability filters.
// On mobile: collapsible with a toggle button.
// On desktop (lg+): always visible as a sticky sidebar.
//
// HOW STAGING WORKS:
//   The component maintains its own internal "draft" state (stagedFilters).
//   Changes to dropdowns/inputs/checkboxes update ONLY the draft — the parent
//   (CataloguePage) is NOT notified and no API call is made.
//   When the user clicks "Apply Filters", the draft is pushed to the parent
//   via onChange(), which triggers the actual book search.
//   This lets users configure multiple filters before triggering a fetch.
//
// Props:
//   categories  (array)    — list of { id, name } category objects
//   filters     (object)   — APPLIED filters (from parent / URL state)
//   onChange    (function) — called with new filters when user clicks Apply
//   onReset     (function) — called when user clears all filters

import { useState, useEffect } from 'react';

export default function FilterPanel({ categories, filters, onChange, onReset }) {
  const [open, setOpen] = useState(false); // mobile collapsed state

  // ── Staged (draft) filters — only committed on "Apply Filters" click ────────
  // Initialised from the applied filters so the UI reflects the current state.
  const [staged, setStaged] = useState({ ...filters });

  // Keep staged in sync if the parent resets filters (e.g. "Clear filters" link)
  useEffect(() => {
    setStaged({ ...filters });
  }, [filters]);

  // Update a single staged field — does NOT call onChange
  const handleStage = (field, value) => setStaged((prev) => ({ ...prev, [field]: value }));

  // Push staged filters to parent → triggers book search
  const handleApply = () => {
    onChange({ ...staged });
    setOpen(false); // close mobile drawer after applying
  };

  // Reset both staged and applied filters to empty
  const handleReset = () => {
    onReset();
    setOpen(false);
  };

  // Count active APPLIED filters for the badge on the mobile toggle button
  const appliedCount = [filters.categoryId, filters.minPrice, filters.maxPrice, filters.available]
    .filter(Boolean).length;

  // Count staged (pending) changes vs applied — to show "pending" state on Apply button
  const hasPendingChanges =
    staged.categoryId !== filters.categoryId ||
    staged.minPrice   !== filters.minPrice   ||
    staged.maxPrice   !== filters.maxPrice   ||
    staged.available  !== filters.available;

  // ── Filter form content (shared between mobile drawer and desktop sidebar) ──
  const content = (
    <div className="space-y-5">
      {/* Category */}
      <div>
        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
          Category
        </label>
        <select
          value={staged.categoryId || ''}
          onChange={(e) => handleStage('categoryId', e.target.value || null)}
          className="w-full border border-gray-300 rounded-xl px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none bg-white"
        >
          <option value="">All Categories</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>{cat.name}</option>
          ))}
        </select>
      </div>

      {/* Price range */}
      <div>
        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
          Price Range (₹)
        </label>
        <div className="flex gap-2">
          {/* min="0" prevents the spinner from going negative.
              The onChange guard also rejects any manually typed negative value. */}
          <input
            type="number" min="0" placeholder="Min"
            value={staged.minPrice || ''}
            onChange={(e) => {
              const v = e.target.value;
              if (v !== '' && Number(v) < 0) return; // reject negatives
              handleStage('minPrice', v || null);
            }}
            className="w-1/2 border border-gray-300 rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
          />
          <input
            type="number" min="0" placeholder="Max"
            value={staged.maxPrice || ''}
            onChange={(e) => {
              const v = e.target.value;
              if (v !== '' && Number(v) < 0) return; // reject negatives
              handleStage('maxPrice', v || null);
            }}
            className="w-1/2 border border-gray-300 rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
          />
        </div>
      </div>

      {/* Availability */}
      <label className="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox"
          checked={!!staged.available}
          onChange={(e) => handleStage('available', e.target.checked ? true : null)}
          className="w-4 h-4 rounded accent-blue-600"
        />
        <span className="text-sm text-gray-700 font-medium">In Stock only</span>
      </label>

      {/* ── Apply Filters button ─────────────────────────────────────────────
          Always visible. Highlighted when there are pending (unapplied) changes.
          This is the ONLY way filters are sent to the parent / trigger a search. */}
      <button
        onClick={handleApply}
        className={`w-full font-semibold rounded-xl py-2.5 text-sm transition-colors
          ${hasPendingChanges
            ? 'bg-blue-600 text-white hover:bg-blue-700'          // pending changes → prominent
            : 'bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200'  // up-to-date → subtle
          }`}
      >
        {hasPendingChanges ? '🔍 Apply Filters' : '✓ Filters Applied'}
      </button>

      {/* Clear all — only shown when at least one filter is applied */}
      {appliedCount > 0 && (
        <button
          onClick={handleReset}
          className="w-full text-sm text-red-600 hover:text-red-800 font-semibold border border-red-200 rounded-xl py-2 hover:bg-red-50 transition-colors"
        >
          Clear all filters ({appliedCount})
        </button>
      )}
    </div>
  );

  return (
    <aside className="w-full lg:w-64 flex-shrink-0">
      {/* ── Mobile: collapsible toggle button ─────────────────────────────── */}
      <div className="lg:hidden mb-4">
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          className="flex items-center justify-between w-full bg-white border border-gray-200 rounded-2xl px-4 py-3 shadow-sm"
        >
          <div className="flex items-center gap-2">
            <span className="text-lg">🔍</span>
            <span className="font-semibold text-gray-800 text-sm">Filters</span>
            {appliedCount > 0 && (
              <span className="bg-blue-600 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                {appliedCount}
              </span>
            )}
            {hasPendingChanges && (
              <span className="bg-yellow-400 text-yellow-900 text-xs font-bold rounded-full px-2 py-0.5">
                pending
              </span>
            )}
          </div>
          <span className="text-gray-400 text-lg">{open ? '▲' : '▼'}</span>
        </button>

        {/* Mobile filter drawer */}
        {open && (
          <div className="bg-white border border-gray-200 rounded-2xl p-4 mt-2 shadow-sm">
            {content}
          </div>
        )}
      </div>

      {/* ── Desktop: always visible sticky sidebar ─────────────────────────── */}
      <div className="hidden lg:block">
        <div className="bg-white border border-gray-200 rounded-2xl p-5 sticky top-20">
          <div className="flex items-center justify-between mb-5">
            <h2 className="font-bold text-gray-800">Filters</h2>
            {appliedCount > 0 && (
              <button
                onClick={handleReset}
                className="text-xs text-red-600 hover:underline font-semibold"
              >
                Clear all
              </button>
            )}
          </div>
          {content}
        </div>
      </div>
    </aside>
  );
}
