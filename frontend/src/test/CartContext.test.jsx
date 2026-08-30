// CartContext.test.jsx — Unit tests for CartContext.
//
// Tests:
//   1. cartCount starts at 0
//   2. refreshCartCount() updates cartCount from the API
//   3. refreshCartCount() sets count to 0 on API error (guest/unauthenticated)

import { render, screen, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CartProvider, useCart } from '../context/CartContext.jsx';

// Mock the cartApi module — we don't want real HTTP calls in unit tests
vi.mock('../api/cartApi.js', () => ({
  getCart: vi.fn(),
}));

import { getCart } from '../api/cartApi.js';

function TestConsumer() {
  const { cartCount, refreshCartCount } = useCart();
  return (
    <div>
      <span data-testid="count">{cartCount}</span>
      <button onClick={refreshCartCount}>Refresh</button>
    </div>
  );
}

describe('CartContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('cartCount starts at 0', () => {
    render(<CartProvider><TestConsumer /></CartProvider>);
    expect(screen.getByTestId('count').textContent).toBe('0');
  });

  it('refreshCartCount() sums item quantities from API response', async () => {
    // The API returns a cart with two items: qty 2 and qty 3 → total 5
    getCart.mockResolvedValue({ data: { items: [{ quantity: 2 }, { quantity: 3 }] } });
    render(<CartProvider><TestConsumer /></CartProvider>);
    await act(async () => { screen.getByText('Refresh').click(); });
    await waitFor(() => expect(screen.getByTestId('count').textContent).toBe('5'));
  });

  it('refreshCartCount() sets count to 0 on API error', async () => {
    getCart.mockRejectedValue(new Error('401'));
    render(<CartProvider><TestConsumer /></CartProvider>);
    await act(async () => { screen.getByText('Refresh').click(); });
    await waitFor(() => expect(screen.getByTestId('count').textContent).toBe('0'));
  });
});

