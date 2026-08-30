import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite configuration for the Bookstore frontend.
// Key feature: the proxy forwards all /api requests from the browser (port 5173)
// to the Spring Boot backend (port 8080) during local development.
// This avoids CORS issues entirely — the browser sees one origin (5173).
export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1', // bind to localhost only (security: never 0.0.0.0)
    port: 5173,
    proxy: {
      // Any request starting with /api is forwarded to the backend
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    // Vitest configuration (unit tests)
    globals: true,           // allows describe/it/expect without importing
    environment: 'jsdom',    // simulates a browser DOM
    setupFiles: ['./src/test/setup.js'],
  },
});
