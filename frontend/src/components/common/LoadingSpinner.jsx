// LoadingSpinner.jsx — Shown while an API call is in-flight.
// Accepts an optional `message` prop for context ("Loading books...", etc.)

export default function LoadingSpinner({ message = 'Loading…' }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-gray-500">
      {/* Spinning circle built entirely with Tailwind utility classes */}
      <div className="w-10 h-10 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mb-4" />
      <p className="text-sm">{message}</p>
    </div>
  );
}
