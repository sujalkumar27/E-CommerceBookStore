// LoginPage.test.jsx — Component tests for the LoginPage.
//
// Tests:
//   1. Renders the email and password fields
//   2. Shows a helpful error message on 401 (bad credentials)
//   3. Shows the same helpful error message on 403 (Spring Security rejection)
//   3. Calls login() and navigates on successful submission

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../pages/LoginPage.jsx';

// ── Mocks ────────────────────────────────────────────────────────────────────
vi.mock('../api/authApi.js', () => ({
  login: vi.fn(),
}));
import { login as loginApi } from '../api/authApi.js';

// Mock useAuth — we control what isLoggedIn returns
const mockLogin = vi.fn();
vi.mock('../context/AuthContext.jsx', () => ({
  useAuth: () => ({ isLoggedIn: false, login: mockLogin }),
}));

// Mock useCart — refreshCartCount is a no-op in these tests
vi.mock('../context/CartContext.jsx', () => ({
  useCart: () => ({ refreshCartCount: vi.fn().mockResolvedValue(undefined) }),
}));

// Mock useNavigate so we can assert navigation
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderPage = () => render(<MemoryRouter><LoginPage /></MemoryRouter>);

  it('renders email and password fields', () => {
    renderPage();
    expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
  });

  it('shows credential error on 401 response', async () => {
    loginApi.mockRejectedValue({ response: { status: 401 } });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText('you@example.com'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByPlaceholderText('••••••••'),         { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/incorrect email or password/i)).toBeInTheDocument();
    });
  });

  it('shows credential error on 403 response (Spring Security rejection)', async () => {
    loginApi.mockRejectedValue({ response: { status: 403 } });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText('you@example.com'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByPlaceholderText('••••••••'),         { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/incorrect email or password/i)).toBeInTheDocument();
    });
  });

  it('calls login() and navigates on success', async () => {
    loginApi.mockResolvedValue({ data: { token: 'tok', user: { email: 'a@b.com', giftPointBalance: 0 } } });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText('you@example.com'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByPlaceholderText('••••••••'),         { target: { value: 'password1' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('tok', { email: 'a@b.com', giftPointBalance: 0 });
      expect(mockNavigate).toHaveBeenCalled();
    });
  });
});

