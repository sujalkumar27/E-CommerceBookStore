// RegisterPage.jsx — Registration form with book-themed split layout.
//
// FIXED: now sends { name, email, password } to match the backend RegisterRequest DTO.
// VALIDATION: real-time field validation with clear error messages.

import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/authApi.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';

// ── Validation helpers ────────────────────────────────────────────────────────
const validators = {
  name:            (v) => !v.trim() ? 'Full name is required.' : v.trim().length < 2 ? 'Name is too short.' : '',
  email:           (v) => !v.trim() ? 'Email is required.' : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? 'Enter a valid email address.' : '',
  password:        (v) => !v ? 'Password is required.' : v.length < 8 ? 'Password must be at least 8 characters.' : '',
  confirmPassword: (v, form) => v !== form.password ? 'Passwords do not match.' : '',
};

export default function RegisterPage() {
  const { login, isLoggedIn } = useAuth();
  const { refreshCartCount }  = useCart();
  const navigate = useNavigate();

  // Update browser tab title on mount
  useEffect(() => { document.title = 'Register | BookStore'; }, []);

  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [touched,     setTouched]     = useState({});
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [loading,     setLoading]     = useState(false);
  const [showPwd,     setShowPwd]     = useState(false);

  if (isLoggedIn) { navigate('/'); return null; }

  // Mark field as touched and validate it on every change
  const handle = (e) => {
    const { name, value } = e.target;
    const newForm = { ...form, [name]: value };
    setForm(newForm);
    setTouched((t) => ({ ...t, [name]: true }));
    const msg = validators[name]?.(value, newForm) || '';
    setFieldErrors((fe) => ({ ...fe, [name]: msg }));
    // Re-validate confirmPassword whenever password changes
    if (name === 'password' && touched.confirmPassword) {
      setFieldErrors((fe) => ({
        ...fe,
        confirmPassword: validators.confirmPassword(newForm.confirmPassword, newForm),
      }));
    }
  };

  // Validate all fields before submit
  const validateAll = () => {
    const errs = {};
    errs.name            = validators.name(form.name);
    errs.email           = validators.email(form.email);
    errs.password        = validators.password(form.password);
    errs.confirmPassword = validators.confirmPassword(form.confirmPassword, form);
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validateAll();
    const hasErrors = Object.values(errs).some(Boolean);
    setFieldErrors(errs);
    setTouched({ name: true, email: true, password: true, confirmPassword: true });
    if (hasErrors) return;

    setServerError(null);
    setLoading(true);
    try {
      const res = await register(form.name.trim(), form.email.trim(), form.password);
      login(res.data.token, res.data.user);
      await refreshCartCount();
      navigate('/');
    } catch (err) {
      const status = err.response?.status;
      if (status === 409) {
        // Duplicate email — the most common registration error
        setServerError('An account with this email already exists. Try logging in instead.');
      } else if (status === 400 && err.response.data?.fieldErrors) {
        // Backend returned field-level validation errors — map them onto the form
        const fe = {};
        Object.entries(err.response.data.fieldErrors).forEach(([field, msg]) => { fe[field] = msg; });
        setFieldErrors(fe);
        setTouched({ name: true, email: true, password: true, confirmPassword: true });
      } else if (status === 400) {
        // Generic bad request from backend without field errors
        setServerError(err.response.data?.message || 'Some of your details are invalid. Please check and try again.');
      } else if (status === 403) {
        // 403 from Spring Security — almost always means the backend rejected the CORS
        // preflight or the security config is blocking the request.
        // Give the user a clear message and log the detail for debugging.
        console.error('Register returned 403:', err.response);
        setServerError('Registration is currently unavailable. Please make sure the backend server is running and try again.');
      } else if (!err.response) {
        // Network error — backend is unreachable
        setServerError('Cannot reach the server. Please make sure the backend is running and try again.');
      } else {
        setServerError('Something went wrong. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Helper: show error for a field only if it was touched
  const err = (name) => touched[name] && fieldErrors[name] ? fieldErrors[name] : '';

  const inputCls = (name) =>
    `w-full border-2 rounded-xl px-4 py-3 text-sm focus:outline-none transition-colors bg-white
     ${err(name) ? 'border-red-400 focus:border-red-500' : 'border-gray-200 focus:border-blue-500'}`;

  return (
    <div className="min-h-screen flex">
      {/* ── Left decorative panel ─────────────────────────────────────────── */}
      <div className="hidden lg:flex lg:w-2/5 bg-gradient-to-br from-emerald-800 via-teal-900 to-slate-900 flex-col justify-between p-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-10 text-white text-8xl leading-tight select-none pointer-events-none overflow-hidden">
          {Array.from({ length: 50 }).map((_, i) => (
            <span key={i} className="inline-block mr-3">📖</span>
          ))}
        </div>
        <div className="relative z-10">
          <div className="text-white text-4xl font-black tracking-tight">📚 BookStore</div>
          <p className="text-emerald-200 mt-2 text-sm">India's favourite online bookstore</p>
        </div>
        <div className="relative z-10 space-y-4">
          {['🎁 Start with 0 gift points — earn more on every order',
            '🚚 Track all your deliveries in one place',
            '📦 Save multiple delivery addresses',
            '🔄 Re-order your favourites in one click',
          ].map((perk) => (
            <div key={perk} className="flex items-start gap-2 text-emerald-100 text-sm">{perk}</div>
          ))}
        </div>
        <p className="relative z-10 text-emerald-300 text-xs">Free to join. No credit card required.</p>
      </div>

      {/* ── Right form panel ─────────────────────────────────────────────── */}
      <div className="flex-1 flex items-center justify-center bg-gray-50 px-6 py-10">
        <div className="w-full max-w-md">
          <div className="lg:hidden text-center mb-6">
            <div className="text-4xl mb-2">📚</div>
            <h2 className="text-xl font-bold text-gray-900">BookStore</h2>
          </div>

          <h1 className="text-3xl font-bold text-gray-900 mb-1">Create your account</h1>
          <p className="text-gray-500 text-sm mb-7">Join thousands of readers today</p>

          {serverError && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-5 flex items-center gap-2">
              <span>⚠️</span> {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Full Name */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Full Name</label>
              <input type="text" name="name" value={form.name} onChange={handle}
                autoComplete="name" placeholder="Sujal Kumar"
                className={inputCls('name')} />
              {err('name') && <p className="text-xs text-red-500 mt-1 flex items-center gap-1">⚠ {err('name')}</p>}
            </div>

            {/* Email */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Email address</label>
              <input type="email" name="email" value={form.email} onChange={handle}
                autoComplete="email" placeholder="you@example.com"
                className={inputCls('email')} />
              {err('email') && <p className="text-xs text-red-500 mt-1 flex items-center gap-1">⚠ {err('email')}</p>}
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Password</label>
              <div className="relative">
                <input type={showPwd ? 'text' : 'password'} name="password" value={form.password} onChange={handle}
                  autoComplete="new-password" placeholder="Min. 8 characters"
                  className={inputCls('password') + ' pr-12'} />
                <button type="button" onClick={() => setShowPwd(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-sm">
                  {showPwd ? '🙈' : '👁️'}
                </button>
              </div>
              {err('password') && <p className="text-xs text-red-500 mt-1 flex items-center gap-1">⚠ {err('password')}</p>}
            </div>

            {/* Confirm Password */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Confirm Password</label>
              <input type="password" name="confirmPassword" value={form.confirmPassword} onChange={handle}
                autoComplete="new-password" placeholder="Re-enter your password"
                className={inputCls('confirmPassword')} />
              {err('confirmPassword') && <p className="text-xs text-red-500 mt-1 flex items-center gap-1">⚠ {err('confirmPassword')}</p>}
            </div>

            <button type="submit" disabled={loading}
              className="w-full bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white font-bold
                         py-3.5 rounded-xl transition-colors disabled:opacity-50 text-base shadow-sm mt-2">
              {loading ? '⏳ Creating account…' : 'Create Account →'}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-7">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 font-bold hover:underline">Sign in →</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
