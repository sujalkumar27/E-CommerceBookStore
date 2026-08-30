// FilterPanel.jsx — Sidebar with category, price, availability filters.
//
// Props:
//   categories   (array)    — [{id, name}, ...]
//   filters      (object)   — current filter state { categoryId, minPrice, maxPrice, available }
//   onChange     (function) — called with updated filters when user changes anything
//   onReset      (function) — resets all filters

export default function FilterPanel({ categories, filters, onChange, onReset }) {
  const handle = (field, value) => onChange({ ...filters, [field]: value });

  return (
    <aside className="w-full lg:w-64 flex-shrink-0">
      <div className="bg-white border border-gray-200 rounded-xl p-4 sticky top-20">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-800">Filters</h2>
          <button
            onClick={onReset}
            className="text-xs text-blue-600 hover:underline"
          >
            Reset all
          </button>
        </div>

        {/* Category */}
        <div className="mb-5">
          <label className="block text-xs font-semibold text-gray-500 uppercase mb-2">
            Category
          </label>
          <select
            value={filters.categoryId || ''}
            onChange={(e) => handle('categoryId', e.target.value || null)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
          >
            <option value="">All Categories</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>{cat.name}</option>
            ))}
          </select>
        </div>

        {/* Price range */}
        <div className="mb-5">
          <label className="block text-xs font-semibold text-gray-500 uppercase mb-2">
            Price Range (₹)
          </label>
          <div className="flex gap-2">
            <input
              type="number"
              min="0"
              placeholder="Min"
              value={filters.minPrice || ''}
              onChange={(e) => handle('minPrice', e.target.value || null)}
              className="w-1/2 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
            <input
              type="number"
              min="0"
              placeholder="Max"
              value={filters.maxPrice || ''}
              onChange={(e) => handle('maxPrice', e.target.value || null)}
              className="w-1/2 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>
        </div>

        {/* Availability */}
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="available"
            checked={!!filters.available}
            onChange={(e) => handle('available', e.target.checked ? true : null)}
            className="rounded text-blue-600 focus:ring-blue-400"
          />
          <label htmlFor="available" className="text-sm text-gray-700 cursor-pointer">
            In Stock only
          </label>
        </div>
      </div>
    </aside>
  );
}
