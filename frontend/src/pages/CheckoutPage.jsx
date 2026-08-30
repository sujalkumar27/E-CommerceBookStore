// CheckoutPage.jsx — Multi-step checkout: Address → Payment → Review & Pay.
//
// URL: /checkout (auth required)
//
// STEP 1: Address — user picks a saved address or adds a new one
// STEP 2: Payment — card type + simulated card number
// STEP 3: Review + Gift Points — shows order summary, redeem points, confirm payment
//
// On success (200): navigate to /checkout/confirmation with the response as router state
// On 402 (payment failure): show error, stay on step 3 for retry

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart }       from '../api/cartApi.js';
import { getAddresses, addAddress } from '../api/addressApi.js';
import { initiatePayment }          from '../api/paymentApi.js';
import { useAuth }  from '../context/AuthContext.jsx';
import { useCart }  from '../context/CartContext.jsx';
import AddressSelector  from '../components/checkout/AddressSelector.jsx';
import PaymentForm      from '../components/checkout/PaymentForm.jsx';
import GiftPointsInput  from '../components/checkout/GiftPointsInput.jsx';
import LoadingSpinner   from '../components/common/LoadingSpinner.jsx';
import ErrorMessage     from '../components/common/ErrorMessage.jsx';

const STEPS = ['Address', 'Payment', 'Review & Pay'];

export default function CheckoutPage() {
  const { user, updateUser } = useAuth();
  const { refreshCartCount } = useCart();
  const navigate = useNavigate();

  const [step,      setStep]      = useState(0);   // 0, 1, or 2
  const [addresses, setAddresses] = useState([]);
  const [cart,      setCart]      = useState(null);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState(null);
  const [paying,    setPaying]    = useState(false);

  // Step 1 state
  const [selectedAddressId, setSelectedAddressId] = useState(null);

  // Step 2 state
  const [payment, setPayment] = useState({ paymentMethod: 'CREDIT_CARD', cardNumber: '' });

  // Step 3 state
  const [giftPoints, setGiftPoints] = useState(0);

  // ── Load addresses + cart on mount ─────────────────────────────────────────
  useEffect(() => {
    setLoading(true);
    Promise.all([getAddresses(), getCart()])
      .then(([addrRes, cartRes]) => {
        setAddresses(addrRes.data);
        setCart(cartRes.data);
        if (addrRes.data.length > 0) {
          setSelectedAddressId(addrRes.data[0].id); // pre-select first address
        }
      })
      .catch(() => setError('Could not load checkout data. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  // ── Add new address (called from AddressSelector) ──────────────────────────
  const handleAddNewAddress = async (addressData) => {
    try {
      const res = await addAddress(addressData);
      setAddresses((prev) => [...prev, res.data]);
      setSelectedAddressId(res.data.id);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save address.');
    }
  };

  // ── Step navigation ────────────────────────────────────────────────────────
  const handleNext = () => {
    if (step === 0 && !selectedAddressId) {
      setError('Please select or add a delivery address.');
      return;
    }
    if (step === 1 && !payment.cardNumber.trim()) {
      setError('Please enter a card number.');
      return;
    }
    setError(null);
    setStep((s) => s + 1);
  };

  // ── Confirm and Pay ────────────────────────────────────────────────────────
  const handlePay = async () => {
    setPaying(true);
    setError(null);
    try {
      const res = await initiatePayment(
        selectedAddressId,
        payment.paymentMethod,
        giftPoints
      );
      // Update gift point balance in AuthContext so Navbar/GiftPointsInput stays fresh
      if (res.data.remainingGiftPointBalance !== undefined) {
        updateUser({ giftPointBalance: res.data.remainingGiftPointBalance });
      }
      await refreshCartCount();
      // Pass payment response to confirmation page via router state (no extra API call needed)
      navigate('/checkout/confirmation', { state: { order: res.data } });
    } catch (err) {
      if (err.response?.status === 402) {
        setError('Payment failed. Please try again.');
      } else {
        setError(err.response?.data?.message || 'Something went wrong. Please try again.');
      }
    } finally {
      setPaying(false);
    }
  };

  if (loading) return <div className="max-w-2xl mx-auto px-4 py-12"><LoadingSpinner message="Preparing checkout…" /></div>;

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-10">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Checkout</h1>

      {/* Step indicator */}
      <div className="flex items-center mb-8">
        {STEPS.map((label, i) => (
          <div key={i} className="flex items-center flex-1 last:flex-none">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold
              ${i < step ? 'bg-green-500 text-white' :
                i === step ? 'bg-blue-600 text-white' :
                'bg-gray-200 text-gray-500'}`}>
              {i < step ? '✓' : i + 1}
            </div>
            <span className={`ml-2 text-sm hidden sm:block ${i === step ? 'font-semibold text-gray-900' : 'text-gray-500'}`}>
              {label}
            </span>
            {i < STEPS.length - 1 && (
              <div className={`flex-1 h-0.5 mx-3 ${i < step ? 'bg-green-400' : 'bg-gray-200'}`} />
            )}
          </div>
        ))}
      </div>

      {error && <ErrorMessage message={error} />}

      {/* ── Step 0: Address ─────────────────────────────────────────────────── */}
      {step === 0 && (
        <AddressSelector
          addresses={addresses}
          selectedAddressId={selectedAddressId}
          onSelect={setSelectedAddressId}
          onAddNew={handleAddNewAddress}
        />
      )}

      {/* ── Step 1: Payment ─────────────────────────────────────────────────── */}
      {step === 1 && (
        <PaymentForm
          paymentMethod={payment.paymentMethod}
          cardNumber={payment.cardNumber}
          onChange={setPayment}
        />
      )}

      {/* ── Step 2: Review + Gift Points ─────────────────────────────────────── */}
      {step === 2 && (
        <div className="space-y-6">
          {/* Order summary */}
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <h3 className="font-semibold text-gray-800 mb-3">Order Summary</h3>
            {cart?.items?.map((item) => (
              <div key={item.id} className="flex justify-between text-sm py-1.5 border-b border-gray-100 last:border-0">
                <span className="line-clamp-1 flex-1 pr-4">{item.book.title} × {item.quantity}</span>
                <span className="font-medium flex-shrink-0">₹{item.lineTotal?.toFixed(2)}</span>
              </div>
            ))}
            <div className="flex justify-between font-bold text-gray-900 mt-3 pt-3 border-t">
              <span>Total</span>
              <span>₹{cart?.cartTotal?.toFixed(2)}</span>
            </div>
          </div>

          {/* Delivery address recap */}
          {(() => {
            const addr = addresses.find((a) => a.id === selectedAddressId);
            return addr ? (
              <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 text-sm">
                <p className="font-semibold text-gray-700 mb-1">🚚 Delivering to:</p>
                <p className="text-gray-600">{addr.fullName}</p>
                <p className="text-gray-600">{addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}</p>
                <p className="text-gray-600">{addr.city}, {addr.state} – {addr.pincode}</p>
              </div>
            ) : null;
          })()}

          {/* Gift points */}
          <GiftPointsInput
            balance={user?.giftPointBalance || 0}
            orderTotal={cart?.cartTotal || 0}
            pointsToRedeem={giftPoints}
            onChange={setGiftPoints}
          />

          {/* Amount to pay */}
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 flex justify-between items-center">
            <span className="font-semibold text-gray-800">Amount to Pay</span>
            <span className="text-xl font-bold text-blue-700">
              ₹{((cart?.cartTotal || 0) - giftPoints).toFixed(2)}
            </span>
          </div>
        </div>
      )}

      {/* Navigation buttons */}
      <div className="flex justify-between mt-8">
        {step > 0 ? (
          <button
            onClick={() => { setError(null); setStep((s) => s - 1); }}
            className="px-5 py-2.5 border border-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50"
          >
            ← Back
          </button>
        ) : (
          <div />
        )}

        {step < 2 ? (
          <button
            onClick={handleNext}
            className="px-6 py-2.5 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700"
          >
            Continue →
          </button>
        ) : (
          <button
            onClick={handlePay}
            disabled={paying}
            className="px-8 py-3 bg-green-600 text-white rounded-xl font-bold text-sm hover:bg-green-700 disabled:opacity-60"
          >
            {paying ? 'Processing…' : 'Confirm & Pay'}
          </button>
        )}
      </div>
    </div>
  );
}
