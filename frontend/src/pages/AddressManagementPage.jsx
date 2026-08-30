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

const emptyForm = { fullName: '', line1: '', line2: '', city: '', state: '', pincode: '' };

// ── Reusable Address Form (used for Add and Edit) ──────────────────────────
function AddressForm({ initial = emptyForm, onSubmit, onCancel, submitLabel = 'Save' }) {
  const [form,   setForm]   = useState(initial);
  const [errors, setErrors] = useState({});

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const validate = () => {
    const e = {};
    if (!form.fullName.trim())          e.fullName = 'Required.';
    if (!form.line1.trim())             e.line1    = 'Required.';
    if (!form.city.trim())              e.city     = 'Required.';
    if (!form.state.trim())             e.state    = 'Required.';
    if (!/^\d{6}$/.test(form.pincode))  e.pincode  = '6-digit pincode required.';
    return e;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    onSubmit(form);
  };

  const fields = [
    { name: 'fullName', label: 'Full Name',          span: 2 },
    { name: 'line1',    label: 'Address Line 1',     span: 2 },
    { name: 'line2',    label: 'Line 2 (optional)',  span: 2 },
    { name: 'city',     label: 'City',               span: 1 },
    { name: 'state',    label: 'State',              span: 1 },
    { name: 'pincode',  label: 'Pincode',            span: 1 },
  ];

  return (
    <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-3 mt-4">
      {fields.map(({ name, label, span }) => (
        <div key={name} className={span === 2 ? 'col-span-2' : ''}>
          <label className="block text-xs font-medium text-gray-700 mb-1">{label}</label>
          <input
            type="text"
            name={name}
            value={form[name]}
            onChange={handle}
            className={`w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-400 focus:outline-none
              ${errors[name] ? 'border-red-400' : 'border-gray-300'}`}
          />
          {errors[name] && <p className="text-xs text-red-500 mt-0.5">{errors[name]}</p>}
        </div>
      ))}
      <div className="col-span-2 flex gap-2 mt-1">
        <button type="submit" className="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-semibold hover:bg-blue-700">
          {submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className="border border-gray-300 px-5 py-2 rounded-lg text-sm hover:bg-gray-50">
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
