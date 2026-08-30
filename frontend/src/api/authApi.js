// authApi.js — All authentication-related API calls.
// Endpoints: POST /api/auth/register, POST /api/auth/login

import axiosClient from './axiosClient';

/**
 * Register a new user account.
 * @param {string} name       - Full name
 * @param {string} email      - Email address
 * @param {string} password   - Password
 * @returns Promise<{ token, user }>
 */
export const register = (name, email, password) =>
  axiosClient.post('/api/auth/register', { name, email, password });

/**
 * Log in with existing credentials.
 * @param {string} email
 * @param {string} password
 * @returns Promise<{ token, user: { id, email, giftPointBalance } }>
 */
export const login = (email, password) =>
  axiosClient.post('/api/auth/login', { email, password });
