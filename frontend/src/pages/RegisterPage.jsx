// RegisterPage.jsx — New account registration form.
//
// URL: /register
// Public: yes
//
// FLOW:
//   1. User fills in name, email, password, confirm password
//   2. POST /api/auth/register
//   3. On 201: call login(token, user), navigate to /
//   4. On 409: "An account with this email already exists."
//   5. On 400: show field-level validation errors from backend

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/authApi.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';

export default function RegisterPage() {
  const { login, isLoggedIn } = useAuth();
  const { refreshCartCount }  = useCart();
  const navigate = useNavigate();

  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError]   = useState(null);
  const [loading, setLoading] = useState(false);

  if (isLoggedIn) { navigate('/'); return null; }

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  // Client-side validation before sending the request
  const validate = () => {
    const e = {};
    if (!form.name.trim())          e.name     = 'Name is required.';
    if (!form.email.includes('@'))  e.email    = 'Enter a valid email.';
    if (form.password.length < 8)   e.password = 'Password must be at least 8 characters.';
    if (form.password !== form.confirmPassword) e.confirmPassword = 'Passwords do not match.';
    return e;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setFieldErrors(errs); return; }

    setFieldErrors({});
    setError(null);
    setLoading(true);
    try {
      const res = await register(form.name, form.email, form.password);
      login(res.data.token, res.data.user);
      await refreshCartCount();
      navigate('/');
    } catch (err) {
      if (err.response?.status === 409) {
        setError('An account with this email already exists.');
      } else if (err.response?.status === 400 && err.response.data?.fieldErrors) {
        // Map backend field errors to the form
        const fe = {};
        err.response.data.fieldErrors.forEach(({ field, message }) => { fe[field] = message; });
        setFieldErrors(fe);
      } else {
        setError('Something went wrong. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const fields = [
    { name: 'name',            label: 'Full Name',        type: 'text',     autocomplete: 'name' },
    { name: 'email',           label: 'Email',            type: 'email',    autocomplete: 'email' },
    { name: 'password',        label: 'Password',         type: 'password', autocomplete: 'new-password' },
    { name: 'confirmPassword', label: 'Confirm Password', type: 'password', autocomplete: 'new-password' },
  ];

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="bg-white border border-gray-200 rounded-2xl shadow-sm p-8 w-full max-w-md">
        <div className="text-center mb-8">
          <div className="text-4xl mb-3">📚</div>
          <h1 className="text-2xl font-bold text-gray-900">Create an account</h1>
          <p className="text-sm text-gray-500 mt-1">Join BookStore today</p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-300 text-red-700 text-sm rounded-lg px-4 py-3 mb-5">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {fields.map(({ name, label, type, autocomplete }) => (
            <div key={name}>
              <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
              <input
                type={type}
                name={name}
                value={form[name]}
                onChange={handle}
                required
                autoComplete={autocomplete}
                className={`w-full border rounded-lg px-4 py-2.5 text-sm
                            focus:ring-2 focus:ring-blue-400 focus:outline-none
                            ${fieldErrors[name] ? 'border-red-400' : 'border-gray-300'}`}
              />
              {fieldErrors[name] && (
                <p className="text-xs text-red-500 mt-1">{fieldErrors[name]}</p>
              )}
            </div>
          ))}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold
                       py-3 rounded-xl transition-colors disabled:opacity-60"
          >
            {loading ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 font-semibold hover:underline">Sign In</Link>
        </p>
      </div>
    </div>
  );
}
