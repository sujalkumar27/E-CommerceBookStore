// NotFoundPage.jsx — Displayed when the user navigates to a URL that does not
// match any defined route (the catch-all <Route path="*"> in App.jsx).
//
// WHY THIS EXISTS:
//   Without a catch-all route, React renders a blank white screen for any unknown URL.
//   This page gives users a clear message and a link back to the catalogue.

import { Link } from 'react-router-dom';
import { useEffect } from 'react';

export default function NotFoundPage() {
  // Update the browser tab title so the user knows they've hit a dead end
  useEffect(() => {
    document.title = '404 – Page Not Found | BookStore';
  }, []);

  return (
    <div className="max-w-lg mx-auto px-4 py-24 text-center">
      <div className="text-7xl mb-6">📚</div>
      <h1 className="text-4xl font-black text-gray-900 mb-3">404</h1>
      <p className="text-xl font-semibold text-gray-700 mb-2">Page Not Found</p>
      <p className="text-gray-500 mb-8">
        The page you&apos;re looking for doesn&apos;t exist or has been moved.
      </p>
      <Link
        to="/"
        className="inline-block bg-blue-600 text-white font-bold px-8 py-3 rounded-xl hover:bg-blue-700 transition-colors"
      >
        ← Back to BookStore
      </Link>
    </div>
  );
}
