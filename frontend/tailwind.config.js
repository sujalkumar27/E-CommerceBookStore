/** @type {import('tailwindcss').Config} */
// Tailwind scans all JSX/JS files in src/ and generates only the CSS classes
// that are actually used — keeping the final CSS bundle small.
export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      // Custom brand colours for the bookstore
      colors: {
        brand: {
          50:  '#eff6ff',
          100: '#dbeafe',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          900: '#1e3a5f',
        },
      },
    },
  },
  plugins: [],
};
