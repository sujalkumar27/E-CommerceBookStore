// ErrorMessage.jsx — Displays a styled error banner.
// Used in every page after a failed API call.
//
// Props:
//   message  (string)   — the error text to display
//   onRetry  (function) — if provided, shows a "Try Again" button

export default function ErrorMessage({ message, onRetry }) {
  return (
    <div className="bg-red-50 border border-red-300 text-red-700 rounded-lg px-4 py-3 flex items-start gap-3 my-4">
      {/* Warning icon */}
      <span className="text-xl leading-none">⚠️</span>
      <div className="flex-1">
        <p className="text-sm font-medium">{message || 'Something went wrong. Please try again.'}</p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="mt-2 text-sm font-semibold text-red-700 underline hover:text-red-900"
          >
            Try Again
          </button>
        )}
      </div>
    </div>
  );
}
