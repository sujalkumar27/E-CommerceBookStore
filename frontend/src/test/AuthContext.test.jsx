// AuthContext.test.jsx — Unit tests for AuthContext provider.
//
// Tests:
//   1. isLoggedIn starts false when localStorage is empty
//   2. login() sets isLoggedIn to true and persists to localStorage
//   3. logout() clears state and localStorage
//   4. updateUser() merges fields without wiping other user data

import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { AuthProvider, useAuth } from '../context/AuthContext.jsx';

// A simple test component that reads from AuthContext and displays values
function TestConsumer() {
  const { isLoggedIn, user, login, logout, updateUser } = useAuth();
  return (
    <div>
      <span data-testid="logged-in">{isLoggedIn ? 'yes' : 'no'}</span>
      <span data-testid="email">{user?.email || 'none'}</span>
      <span data-testid="points">{user?.giftPointBalance ?? 'none'}</span>
      <button onClick={() => login('tok123', { email: 'test@test.com', giftPointBalance: 100 })}>
        Login
      </button>
      <button onClick={logout}>Logout</button>
      <button onClick={() => updateUser({ giftPointBalance: 50 })}>Update Points</button>
    </div>
  );
}

describe('AuthContext', () => {
  // Clear localStorage before each test for isolation
  beforeEach(() => {
    localStorage.clear();
  });

  it('isLoggedIn is false initially when no token in localStorage', () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    expect(screen.getByTestId('logged-in').textContent).toBe('no');
    expect(screen.getByTestId('email').textContent).toBe('none');
  });

  it('login() sets isLoggedIn to true and stores in localStorage', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    await act(async () => {
      screen.getByText('Login').click();
    });
    expect(screen.getByTestId('logged-in').textContent).toBe('yes');
    expect(screen.getByTestId('email').textContent).toBe('test@test.com');
    expect(localStorage.getItem('token')).toBe('tok123');
    expect(JSON.parse(localStorage.getItem('user')).email).toBe('test@test.com');
  });

  it('logout() clears state and removes localStorage', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    await act(async () => { screen.getByText('Login').click(); });
    await act(async () => { screen.getByText('Logout').click(); });
    expect(screen.getByTestId('logged-in').textContent).toBe('no');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('updateUser() merges new fields into existing user', async () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    await act(async () => { screen.getByText('Login').click(); });
    expect(screen.getByTestId('points').textContent).toBe('100');
    await act(async () => { screen.getByText('Update Points').click(); });
    // giftPointBalance updated, email still intact
    expect(screen.getByTestId('points').textContent).toBe('50');
    expect(screen.getByTestId('email').textContent).toBe('test@test.com');
  });
});

