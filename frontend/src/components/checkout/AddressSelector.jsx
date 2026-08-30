// AddressSelector.jsx — Step 1 of checkout: pick a saved address or add a new one.
//
// Props:
//   addresses          (array)    — user's saved addresses from GET /api/addresses
//   selectedAddressId  (string)   — UUID of currently selected address
//   onSelect           (function) — called with addressId when user picks one
//   onAddNew           (function) — called with addressData when user submits the new address form

import { useState } from 'react';

// Empty form state for a new address
const emptyForm = { fullName: '', line1: '', line2: '', city: '', state: '', pincode: '' };

export default function AddressSelector({ addresses, selectedAddressId, onSelect, onAddNew }) {
  const [showForm, setShowForm]   = useState(false);
  const [form, setForm]           = useState(emptyForm);
  const [errors, setErrors]       = useState({});

  const handleField = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  // Basic client-side validation before submitting
  const validate = () => {
    const e = {};
    if (!form.fullName.trim()) e.fullName = 'Full name is required.';
    if (!form.line1.trim())    e.line1    = 'Address line 1 is required.';
    if (!form.city.trim())     e.city     = 'City is required.';
    if (!form.state.trim())    e.state    = 'State is required.';
    if (!/^\d{6}$/.test(form.pincode)) e.pincode = 'Enter a valid 6-digit pincode.';
    return e;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    onAddNew(form);
    setForm(emptyForm);
    setErrors({});
    setShowForm(false);
  };

  return (
    <div>
      <h3 className="text-base font-semibold text-gray-800 mb-3">Select Delivery Address</h3>

      {/* List of existing addresses */}
      <div className="space-y-3 mb-4">
        {addresses.map((addr) => (
          <label
            key={addr.id}
            className={`flex items-start gap-3 p-4 border rounded-xl cursor-pointer transition-colors
              ${selectedAddressId === addr.id
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-blue-300'
              }`}
          >
            <input
              type="radio"
              name="address"
              value={addr.id}
              checked={selectedAddressId === addr.id}
              onChange={() => onSelect(addr.id)}
              className="mt-1 accent-blue-600"
            />
            <div className="text-sm">
              <p className="font-semibold text-gray-900">{addr.fullName}</p>
              <p className="text-gray-600">{addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}</p>
              <p className="text-gray-600">{addr.city}, {addr.state} – {addr.pincode}</p>
            </div>
          </label>
        ))}
      </div>

      {/* Toggle new address form */}
      <button
        type="button"
        onClick={() => setShowForm((v) => !v)}
        className="text-sm text-blue-600 font-medium hover:underline"
      >
        {showForm ? '− Cancel' : '+ Add New Address'}
      </button>

      {/* New address form */}
      {showForm && (
        <form onSubmit={handleSubmit} className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
          {[
            { name: 'fullName',  label: 'Full Name',         colSpan: 'sm:col-span-2' },
            { name: 'line1',     label: 'Address Line 1',    colSpan: 'sm:col-span-2' },
            { name: 'line2',     label: 'Address Line 2 (optional)', colSpan: 'sm:col-span-2' },
            { name: 'city',      label: 'City' },
            { name: 'state',     label: 'State' },
            { name: 'pincode',   label: 'Pincode' },
          ].map(({ name, label, colSpan }) => (
            <div key={name} className={colSpan || ''}>
              <label className="block text-xs font-medium text-gray-700 mb-1">{label}</label>
              <input
                type="text"
                name={name}
                value={form[name]}
                onChange={handleField}
                className={`w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none
                  ${errors[name] ? 'border-red-400' : 'border-gray-300'}`}
              />
              {errors[name] && <p className="text-xs text-red-500 mt-1">{errors[name]}</p>}
            </div>
          ))}
          <div className="sm:col-span-2">
            <button
              type="submit"
              className="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-semibold hover:bg-blue-700"
            >
              Save Address
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
