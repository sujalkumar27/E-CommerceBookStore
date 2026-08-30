// FilterPanel.jsx — Sidebar with category, price, availability filters.
// On mobile: collapsible with a toggle button.
// On desktop (lg+): always visible as a sticky sidebar.

import { useState } from 'react';

export default function FilterPanel({ categories, filters, onChange, onReset }) {
  const [open, setOpen] = useState(false); // mobile collapsed state

  const handle = (field, value) => onChange({ ...filters, [field]: value });

  // Count active filters for the badge
  const activeCount = [filters.categoryId, filters.minPrice, filters.maxPrice, filters.available]
    .filter(Boolean).length;

  const content = (
    <div className="space-y-5">
      {/* Category */}
      <div>
        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
          Category
        </label>
        <select
          value={filters.categoryId || ''}
          onChange={(e) => handle('categoryId', e.target.value || null)}
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
          <input
            type="number" min="0" placeholder="Min"
            value={filters.minPrice || ''}
            onChange={(e) => handle('minPrice', e.target.value || null)}
            className="w-1/2 border border-gray-300 rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
          />
          <input
            type="number" min="0" placeholder="Max"
            value={filters.maxPrice || ''}
            onChange={(e) => handle('maxPrice', e.target.value || null)}
            className="w-1/2 border border-gray-300 rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
          />
        </div>
      </div>

      {/* Availability */}
      <label className="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox" id="available"
          checked={!!filters.available}
          onChange={(e) => handle('available', e.target.checked ? true : null)}
          className="w-4 h-4 rounded accent-blue-600"
        />
        <span className="text-sm text-gray-700 font-medium">In Stock only</span>
      </label>

      {/* Reset */}
      {activeCount > 0 && (
        <button
          onClick={() => { onReset(); setOpen(false); }}
          className="w-full text-sm text-red-600 hover:text-red-800 font-semibold border border-red-200 rounded-xl py-2 hover:bg-red-50 transition-colors"
        >
          Clear all filters ({activeCount})
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
            {activeCount > 0 && (
              <span className="bg-blue-600 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                {activeCount}
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
            {activeCount > 0 && (
              <button
                onClick={onReset}
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
