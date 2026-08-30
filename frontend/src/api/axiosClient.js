// axiosClient.js — The single configured Axios instance used by every API module.
//
// WHY ONE INSTANCE?
//   Instead of calling axios.get() directly everywhere (and repeating the base URL,
//   the Authorization header, and error handling in every file), we create ONE
//   configured instance here and import it everywhere else.
//
// INTERCEPTORS:
//   - Request interceptor:  automatically attaches "Authorization: Bearer <token>"
//     to every outgoing request if the user is logged in.
//   - Response interceptor: if the server returns 401 (token expired / missing),
//     we clear localStorage and redirect to /login so the user can log back in.

import axios from 'axios';

// Create an Axios instance with sensible defaults.
// baseURL is empty string — in dev the Vite proxy forwards /api → localhost:8080.
// In production you would set VITE_API_BASE_URL to the real backend URL.
const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000, // 15 s — never hang forever
});

// ── Request interceptor ──────────────────────────────────────────────────────
// Runs before every request is sent.
// Reads the JWT from localStorage and adds it as the Authorization header.
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// ── Response interceptor ─────────────────────────────────────────────────────
// Runs after every response (success or error).
// On 401: the token is expired or invalid → force logout + redirect to /login.
axiosClient.interceptors.response.use(
  (response) => response, // pass successful responses through unchanged
  (error) => {
    if (error.response?.status === 401) {
      // Clear auth data and redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error); // propagate so the caller can show an error
  }
);

export default axiosClient;
