// PaymentForm.jsx — Step 2 of checkout: select card type and enter card number.
//
// NOTE: This is a SIMULATED payment form. No real payment processing occurs.
//       Any card number is accepted (the backend ignores it).
//
// Props:
//   paymentMethod  (string)   — "CREDIT_CARD" or "DEBIT_CARD"
//   cardNumber     (string)   — entered card number (display only)
//   onChange       (function) — called with { paymentMethod, cardNumber } on change

export default function PaymentForm({ paymentMethod, cardNumber, onChange }) {
  const handle = (field, value) => onChange({ paymentMethod, cardNumber, [field]: value });

  return (
    <div>
      <h3 className="text-base font-semibold text-gray-800 mb-4">Payment Method</h3>

      {/* Card type selection */}
      <div className="flex gap-4 mb-5">
        {['CREDIT_CARD', 'DEBIT_CARD'].map((type) => (
          <label
            key={type}
            className={`flex items-center gap-2 px-4 py-3 border rounded-xl cursor-pointer flex-1 transition-colors
              ${paymentMethod === type
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-blue-300'
              }`}
          >
            <input
              type="radio"
              name="paymentMethod"
              value={type}
              checked={paymentMethod === type}
              onChange={() => handle('paymentMethod', type)}
              className="accent-blue-600"
            />
            <span className="text-sm font-medium text-gray-700">
              {type === 'CREDIT_CARD' ? '💳 Credit Card' : '🏦 Debit Card'}
            </span>
          </label>
        ))}
      </div>

      {/* Simulated card number field */}
      <div>
        <label className="block text-xs font-medium text-gray-700 mb-1">
          Card Number <span className="text-gray-400">(simulated — any value)</span>
        </label>
        <input
          type="text"
          value={cardNumber}
          onChange={(e) => handle('cardNumber', e.target.value)}
          placeholder="•••• •••• •••• ••••"
          maxLength={19}
          className="w-full border border-gray-300 rounded-lg px-4 py-3 text-sm
                     font-mono tracking-widest focus:ring-2 focus:ring-blue-400 focus:outline-none"
        />
        <p className="text-xs text-gray-400 mt-1">
          ℹ️ This is a simulation. Enter any value — no real card is charged.
        </p>
      </div>
    </div>
  );
}
