// GiftPointsInput.jsx — Step 3 of checkout: optionally redeem gift points.
//
// BUSINESS RULES (enforced here on the client; backend also validates):
//   - Cannot redeem more points than the user's balance
//   - Cannot redeem more than the order total (you can't get cashback)
//   - Each point = ₹1 discount
//
// Props:
//   balance       (number)   — user's current gift point balance
//   orderTotal    (number)   — total cost of the order in ₹
//   pointsToRedeem (number)  — currently chosen value
//   onChange      (function) — called with new points value

export default function GiftPointsInput({ balance, orderTotal, pointsToRedeem, onChange }) {
  // The maximum redeemable is the lower of: user's balance OR order total
  const maxRedeemable = Math.min(balance, Math.floor(orderTotal));

  const handleChange = (e) => {
    const val = Math.max(0, Math.min(Number(e.target.value), maxRedeemable));
    onChange(val);
  };

  return (
    <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4">
      <h4 className="font-semibold text-gray-800 text-sm mb-2">🎁 Gift Points</h4>

      <div className="flex items-center justify-between text-sm mb-3">
        <span className="text-gray-600">Your Balance:</span>
        <span className="font-bold text-yellow-700">{balance} pts (₹{balance})</span>
      </div>

      {balance > 0 ? (
        <>
          <label className="block text-xs text-gray-600 mb-1">
            Points to Redeem (max {maxRedeemable}):
          </label>
          <input
            type="number"
            min={0}
            max={maxRedeemable}
            value={pointsToRedeem}
            onChange={handleChange}
            className="w-full border border-yellow-300 rounded-lg px-3 py-2 text-sm
                       focus:ring-2 focus:ring-yellow-400 focus:outline-none"
          />
          <div className="flex justify-between text-sm mt-2">
            <span className="text-gray-600">Discount:</span>
            <span className="font-semibold text-green-600">− ₹{pointsToRedeem}</span>
          </div>
        </>
      ) : (
        <p className="text-sm text-gray-500">You have no gift points to redeem.</p>
      )}
    </div>
  );
}
