// AddressSelector.jsx — Step 1 of checkout: pick a saved address or add a new one.
// Updated: includes phone number field with 10-digit Indian mobile validation.

import { useState } from 'react';

const emptyForm = { fullName: '', phone: '', line1: '', line2: '', city: '', state: '', pincode: '' };

// Validate a single address form field
const validateField = (name, value) => {
  switch (name) {
    case 'fullName': return !value.trim() ? 'Full name is required.' : '';
    case 'phone':    return !value.trim() ? 'Phone number is required.'
                         : !/^[6-9]\d{9}$/.test(value) ? 'Enter a valid 10-digit mobile number.' : '';
    case 'line1':    return !value.trim() ? 'Address line 1 is required.' : '';
    case 'city':     return !value.trim() ? 'City is required.' : '';
    case 'state':    return !value.trim() ? 'State is required.' : '';
    case 'pincode':  return !/^\d{6}$/.test(value) ? 'Enter a valid 6-digit pincode.' : '';
    default:         return '';
  }
};

export default function AddressSelector({ addresses, selectedAddressId, onSelect, onAddNew }) {
  const [showForm, setShowForm] = useState(false);
  const [form,     setForm]     = useState(emptyForm);
  const [errors,   setErrors]   = useState({});
  const [touched,  setTouched]  = useState({});

  const handleField = (e) => {
    const { name, value } = e.target;
    // Enforce max digits for phone (10) and pincode (6)
    if (name === 'phone'   && value.replace(/\D/g, '').length > 10) return;
    if (name === 'pincode' && value.replace(/\D/g, '').length > 6)  return;
    setForm((f) => ({ ...f, [name]: value }));
    setTouched((t) => ({ ...t, [name]: true }));
    setErrors((err) => ({ ...err, [name]: validateField(name, value) }));
  };

  const validateAll = () => {
    const required = ['fullName', 'phone', 'line1', 'city', 'state', 'pincode'];
    const errs = {};
    required.forEach((k) => { errs[k] = validateField(k, form[k]); });
    return errs;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errs = validateAll();
    const hasErrors = Object.values(errs).some(Boolean);
    setErrors(errs);
    setTouched({ fullName: true, phone: true, line1: true, city: true, state: true, pincode: true });
    if (hasErrors) return;
    onAddNew(form);
    setForm(emptyForm);
    setErrors({});
    setTouched({});
    setShowForm(false);
  };

  const inp = (name) =>
    `w-full border-2 rounded-xl px-3 py-2 text-sm focus:outline-none transition-colors
     ${touched[name] && errors[name] ? 'border-red-400 focus:border-red-500' : 'border-gray-200 focus:border-blue-500'}`;

  return (
    <div>
      <h3 className="text-base font-semibold text-gray-800 mb-3">Select Delivery Address</h3>

      {/* Saved addresses */}
      <div className="space-y-3 mb-4">
        {addresses.map((addr) => (
          <label key={addr.id}
            className={`flex items-start gap-3 p-4 border-2 rounded-xl cursor-pointer transition-colors
              ${selectedAddressId === addr.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'}`}>
            <input type="radio" name="address" value={addr.id}
              checked={selectedAddressId === addr.id}
              onChange={() => onSelect(addr.id)}
              className="mt-1 accent-blue-600" />
            <div className="text-sm">
              <p className="font-semibold text-gray-900">{addr.fullName}</p>
              {addr.phone && <p className="text-gray-500 text-xs">📱 {addr.phone}</p>}
              <p className="text-gray-600">{addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}</p>
              <p className="text-gray-600">{addr.city}, {addr.state} – {addr.pincode}</p>
            </div>
          </label>
        ))}
      </div>

      {/* Toggle add-new form */}
      <button type="button" onClick={() => setShowForm((v) => !v)}
        className="text-sm text-blue-600 font-semibold hover:underline">
        {showForm ? '− Cancel' : '+ Add New Address'}
      </button>

      {/* Add new address form */}
      {showForm && (
        <form onSubmit={handleSubmit} className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3 bg-gray-50 rounded-2xl p-4 border border-gray-200">
          <h4 className="sm:col-span-2 font-semibold text-gray-700 text-sm">New Address</h4>

          {/* Full Name */}
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-gray-600 mb-1">Full Name *</label>
            <input type="text" name="fullName" value={form.fullName} onChange={handleField}
              placeholder="Recipient name" className={inp('fullName')} />
            {touched.fullName && errors.fullName && <p className="text-xs text-red-500 mt-1">⚠ {errors.fullName}</p>}
          </div>

          {/* Phone */}
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-gray-600 mb-1">Mobile Number * <span className="text-gray-400 font-normal">(10 digits, starts with 6-9)</span></label>
            <input type="tel" name="phone" value={form.phone} onChange={handleField}
              placeholder="9876543210" maxLength={10} className={inp('phone')} />
            {touched.phone && errors.phone && <p className="text-xs text-red-500 mt-1">⚠ {errors.phone}</p>}
          </div>

          {/* Line 1 */}
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-gray-600 mb-1">Address Line 1 *</label>
            <input type="text" name="line1" value={form.line1} onChange={handleField}
              placeholder="House/flat no, street" className={inp('line1')} />
            {touched.line1 && errors.line1 && <p className="text-xs text-red-500 mt-1">⚠ {errors.line1}</p>}
          </div>

          {/* Line 2 */}
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-gray-600 mb-1">Address Line 2 <span className="text-gray-400 font-normal">(optional)</span></label>
            <input type="text" name="line2" value={form.line2} onChange={handleField}
              placeholder="Landmark, apartment" className={inp('line2')} />
          </div>

          {/* City */}
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1">City *</label>
            <input type="text" name="city" value={form.city} onChange={handleField}
              placeholder="Mumbai" className={inp('city')} />
            {touched.city && errors.city && <p className="text-xs text-red-500 mt-1">⚠ {errors.city}</p>}
          </div>

          {/* State */}
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1">State *</label>
            <input type="text" name="state" value={form.state} onChange={handleField}
              placeholder="Maharashtra" className={inp('state')} />
            {touched.state && errors.state && <p className="text-xs text-red-500 mt-1">⚠ {errors.state}</p>}
          </div>

          {/* Pincode */}
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1">Pincode * <span className="text-gray-400 font-normal">(6 digits)</span></label>
            <input type="text" name="pincode" value={form.pincode} onChange={handleField}
              placeholder="400001" maxLength={6} className={inp('pincode')} />
            {touched.pincode && errors.pincode && <p className="text-xs text-red-500 mt-1">⚠ {errors.pincode}</p>}
          </div>

          <div className="sm:col-span-2">
            <button type="submit"
              className="bg-blue-600 text-white px-6 py-2.5 rounded-xl text-sm font-bold hover:bg-blue-700 transition-colors">
              Save Address
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
