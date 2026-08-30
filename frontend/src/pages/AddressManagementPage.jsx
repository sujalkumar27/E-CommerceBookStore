// AddressManagementPage.jsx — View, add, edit, and delete delivery addresses.
//
// URL: /account/addresses (auth required)
//
// ACTIONS:
//   - Load all addresses: GET /api/addresses
//   - Add new address:    POST /api/addresses
//   - Edit address:       PUT /api/addresses/:id (inline editing)
//   - Delete address:     DELETE /api/addresses/:id

import { useState, useEffect } from 'react';
import { getAddresses, addAddress, updateAddress, deleteAddress } from '../api/addressApi.js';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorMessage   from '../components/common/ErrorMessage.jsx';
import EmptyState     from '../components/common/EmptyState.jsx';

const emptyForm = { fullName: '', phone: '', line1: '', line2: '', city: '', state: '', pincode: '' };

// Validate a single field
const vf = (name, value) => {
  if (name === 'fullName') return !value.trim() ? 'Required.' : '';
  if (name === 'phone')    return !/^[6-9]\d{9}$/.test(value) ? 'Enter a valid 10-digit mobile number.' : '';
  if (name === 'line1')    return !value.trim() ? 'Required.' : '';
  if (name === 'city')     return !value.trim() ? 'Required.' : '';
  if (name === 'state')    return !value.trim() ? 'Required.' : '';
  if (name === 'pincode')  return !/^\d{6}$/.test(value) ? '6-digit pincode required.' : '';
  return '';
};

// ── Reusable Address Form (used for Add and Edit) ──────────────────────────
function AddressForm({ initial = emptyForm, onSubmit, onCancel, submitLabel = 'Save' }) {
  const [form,    setForm]    = useState({ ...emptyForm, ...initial });
  const [errors,  setErrors]  = useState({});
  const [touched, setTouched] = useState({});

  const handle = (e) => {
    const { name, value } = e.target;
    if (name === 'phone'   && value.replace(/\D/g,'').length > 10) return;
    if (name === 'pincode' && value.replace(/\D/g,'').length > 6)  return;
    setForm((f) => ({ ...f, [name]: value }));
    setTouched((t) => ({ ...t, [name]: true }));
    setErrors((err) => ({ ...err, [name]: vf(name, value) }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const required = ['fullName', 'phone', 'line1', 'city', 'state', 'pincode'];
    const errs = {};
    required.forEach((k) => { errs[k] = vf(k, form[k] || ''); });
    const hasErrors = Object.values(errs).some(Boolean);
    setErrors(errs);
    setTouched(Object.fromEntries(required.map((k) => [k, true])));
    if (hasErrors) return;
    onSubmit(form);
  };

  const inp = (name) =>
    `w-full border-2 rounded-xl px-3 py-2 text-sm focus:outline-none transition-colors
     ${touched[name] && errors[name] ? 'border-red-400 focus:border-red-500' : 'border-gray-200 focus:border-blue-500'}`;

  const fields = [
    { name: 'fullName', label: 'Full Name *',                    span: 2, type: 'text', placeholder: 'Recipient name' },
    { name: 'phone',    label: 'Mobile Number * (10 digits)',    span: 2, type: 'tel',  placeholder: '9876543210', maxLength: 10 },
    { name: 'line1',    label: 'Address Line 1 *',               span: 2, type: 'text', placeholder: 'House/flat, street' },
    { name: 'line2',    label: 'Line 2 (optional)',              span: 2, type: 'text', placeholder: 'Landmark, apartment' },
    { name: 'city',     label: 'City *',                         span: 1, type: 'text', placeholder: 'Mumbai' },
    { name: 'state',    label: 'State *',                        span: 1, type: 'text', placeholder: 'Maharashtra' },
    { name: 'pincode',  label: 'Pincode * (6 digits)',           span: 1, type: 'text', placeholder: '400001', maxLength: 6 },
  ];

  return (
    <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-3 mt-4">
      {fields.map(({ name, label, span, type, placeholder, maxLength }) => (
        <div key={name} className={span === 2 ? 'col-span-2' : ''}>
          <label className="block text-xs font-semibold text-gray-600 mb-1">{label}</label>
          <input
            type={type} name={name} value={form[name] || ''} onChange={handle}
            placeholder={placeholder} maxLength={maxLength}
            className={inp(name)}
          />
          {touched[name] && errors[name] && <p className="text-xs text-red-500 mt-0.5">⚠ {errors[name]}</p>}
        </div>
      ))}
      <div className="col-span-2 flex gap-2 mt-1">
        <button type="submit" className="bg-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-bold hover:bg-blue-700">
          {submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className="border-2 border-gray-200 px-5 py-2.5 rounded-xl text-sm hover:bg-gray-50">
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

// ── Main Page ──────────────────────────────────────────────────────────────
export default function AddressManagementPage() {
  const [addresses,  setAddresses]  = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingId,  setEditingId]  = useState(null);  // UUID of the address being edited
  const [toast,      setToast]      = useState(null);

  const showToast = (msg, isError = false) => {
    setToast({ msg, isError });
    setTimeout(() => setToast(null), 3500);
  };

  const loadAddresses = () => {
    setLoading(true);
    getAddresses()
      .then((res) => setAddresses(res.data || []))
      .catch(() => setError('Could not load addresses.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadAddresses(); }, []);

  const handleAdd = async (data) => {
    try {
      const res = await addAddress(data);
      setAddresses((prev) => [...prev, res.data]);
      setShowAddForm(false);
      showToast('✅ Address saved.');
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not save.'), true);
    }
  };

  const handleUpdate = async (id, data) => {
    try {
      const res = await updateAddress(id, data);
      setAddresses((prev) => prev.map((a) => (a.id === id ? res.data : a)));
      setEditingId(null);
      showToast('✅ Address updated.');
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not update.'), true);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this address?')) return;
    try {
      await deleteAddress(id);
      setAddresses((prev) => prev.filter((a) => a.id !== id));
      showToast('✅ Address deleted.');
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not delete.'), true);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-10">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-20 right-4 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium
          ${toast.isError ? 'bg-red-600 text-white' : 'bg-green-600 text-white'}`}>
          {toast.msg}
        </div>
      )}

      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900">My Addresses</h1>
        <button
          onClick={() => setShowAddForm((v) => !v)}
          className="text-sm font-semibold bg-blue-600 text-white px-4 py-2 rounded-xl hover:bg-blue-700"
        >
          {showAddForm ? '− Cancel' : '+ Add Address'}
        </button>
      </div>

      {/* Add form */}
      {showAddForm && (
        <div className="bg-white border border-gray-200 rounded-2xl p-5 mb-5">
          <h2 className="font-semibold text-gray-800 mb-1">New Address</h2>
          <AddressForm
            onSubmit={handleAdd}
            onCancel={() => setShowAddForm(false)}
            submitLabel="Save Address"
          />
        </div>
      )}

      {loading && <LoadingSpinner message="Loading addresses…" />}
      {!loading && error && <ErrorMessage message={error} onRetry={loadAddresses} />}
      {!loading && !error && addresses.length === 0 && !showAddForm && (
        <EmptyState
          icon="🏠"
          title="No addresses saved"
          subtitle="Add a delivery address to get started."
          action={
            <button
              onClick={() => setShowAddForm(true)}
              className="bg-blue-600 text-white px-6 py-2 rounded-xl text-sm font-semibold hover:bg-blue-700"
            >
              Add Address
            </button>
          }
        />
      )}

      {/* Address list */}
      <div className="space-y-4">
        {addresses.map((addr) => (
          <div key={addr.id} className="bg-white border border-gray-200 rounded-2xl p-5">
            {editingId === addr.id ? (
              // Edit form inline
              <>
                <h3 className="font-semibold text-gray-800 mb-1">Edit Address</h3>
                <AddressForm
                  initial={addr}
                  onSubmit={(data) => handleUpdate(addr.id, data)}
                  onCancel={() => setEditingId(null)}
                  submitLabel="Update"
                />
              </>
            ) : (
              // Display mode
              <div className="flex items-start justify-between">
                <div className="text-sm">
                  <p className="font-semibold text-gray-900">{addr.fullName}</p>
                  {addr.phone && <p className="text-gray-500 text-xs mb-0.5">📱 {addr.phone}</p>}
                  <p className="text-gray-600">{addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}</p>
                  <p className="text-gray-600">{addr.city}, {addr.state} – {addr.pincode}</p>
                </div>
                <div className="flex gap-2 ml-4 flex-shrink-0">
                  <button
                    onClick={() => setEditingId(addr.id)}
                    className="text-xs text-blue-600 border border-blue-300 px-3 py-1.5 rounded-lg hover:bg-blue-50"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(addr.id)}
                    className="text-xs text-red-600 border border-red-300 px-3 py-1.5 rounded-lg hover:bg-red-50"
                  >
                    Delete
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
