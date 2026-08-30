// CartSummary.jsx — Shows the cart total and "Proceed to Checkout" button.
//
// Props:
//   cartTotal (number)   — total price from the API
//   onCheckout (function) — called when the user clicks the CTA button
//   disabled   (boolean) — disables the button while an API call is in-flight

export default function CartSummary({ cartTotal, onCheckout, disabled }) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-6 sticky top-20">
      <h2 className="font-bold text-lg text-gray-900 mb-4">Order Summary</h2>

      <div className="flex justify-between text-sm text-gray-700 mb-2">
        <span>Subtotal</span>
        <span className="font-medium">₹{cartTotal?.toFixed(2)}</span>
      </div>
      <div className="flex justify-between text-xs text-gray-400 mb-4">
        <span>Shipping</span>
        <span>Calculated at checkout</span>
      </div>

      <div className="border-t pt-4 flex justify-between font-bold text-gray-900 mb-6">
        <span>Total</span>
        <span>₹{cartTotal?.toFixed(2)}</span>
      </div>

      <button
        onClick={onCheckout}
        disabled={disabled}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold
                   py-3 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Proceed to Checkout →
      </button>
    </div>
  );
}
