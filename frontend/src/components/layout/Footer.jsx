// Footer.jsx — Simple site footer displayed on every page.

export default function Footer() {
  return (
    <footer className="bg-white border-t border-gray-200 mt-12">
      <div className="max-w-7xl mx-auto px-4 py-6 flex flex-col sm:flex-row items-center justify-between gap-2 text-sm text-gray-500">
        <span>© {new Date().getFullYear()} BookStore. All rights reserved.</span>
        <span>Built with ☕ Spring Boot &amp; ⚛️ React</span>
      </div>
    </footer>
  );
}
