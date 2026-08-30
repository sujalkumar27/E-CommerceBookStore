// EmptyState.jsx — Shows a friendly "nothing here" message.
// Used for: empty cart, no search results, no orders, no addresses, etc.
//
// Props:
//   icon     (string)    — emoji icon (default 📭)
//   title    (string)    — main heading
//   subtitle (string)    — secondary line (optional)
//   action   (ReactNode) — optional CTA button

export default function EmptyState({ icon = '📭', title, subtitle, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center text-gray-500">
      <div className="text-6xl mb-4">{icon}</div>
      <h3 className="text-lg font-semibold text-gray-700 mb-1">{title}</h3>
      {subtitle && <p className="text-sm mb-4">{subtitle}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
