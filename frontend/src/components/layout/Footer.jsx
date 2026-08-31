// Footer.jsx — Simple site footer displayed on every page.
//
// FIX: Changed background from bg-white (same as body) to bg-gray-50 with a
//   visible top border, so the footer is visually distinct from page content.

export default function Footer() {
  return (
    <footer className="bg-gray-50 border-t border-gray-300 mt-12">
      <div className="max-w-7xl mx-auto px-4 py-6 flex flex-col sm:flex-row items-center justify-between gap-2 text-sm text-gray-500">
        <span>© {new Date().getFullYear()} BookStore. All rights reserved.</span>
        <span>Built with ☕ Spring Boot &amp; ⚛️ React</span>
      </div>
    </footer>
  );
}
