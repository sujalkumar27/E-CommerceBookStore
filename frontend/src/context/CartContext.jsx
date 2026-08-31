// CartContext.jsx — Tracks the number of items in the cart for the Navbar badge.
//
// WHY A SEPARATE CONTEXT FOR JUST ONE NUMBER?
//   The Navbar (top of every page) shows a badge: "Cart (3)".
//   The Navbar is not a child of CartPage, so we can't pass the count as a prop.
//   A context lets CartPage, ProductDetailPage, and CataloguePage all call
//   refreshCartCount() after adding/removing items, and the Navbar badge updates
//   instantly — no page reload needed.
//
// WHAT THIS CONTEXT PROVIDES:
//   - cartCount          : number of items currently in the cart
//   - refreshCartCount() : re-fetches the cart from the backend and updates the count
//
// FIX: CartProvider now calls refreshCartCount() on mount so the badge shows the
//   real count immediately for logged-in users instead of always starting at 0.

import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { getCart } from '../api/cartApi';

const CartContext = createContext(undefined);

export function CartProvider({ children }) {
  const [cartCount, setCartCount] = useState(0);

  /**
   * Fetches the cart from the backend and updates cartCount.
   * Call this after any add / update / remove cart action.
   * useCallback ensures the function reference is stable (safe to use in useEffect deps).
   */
  const refreshCartCount = useCallback(async () => {
    try {
      const res = await getCart();
      // Count total quantity across all items (not just number of distinct books)
      const total = res.data.items.reduce((sum, item) => sum + item.quantity, 0);
      setCartCount(total);
    } catch {
      // If the user is not logged in, getCart will 401 — just show 0 for guests.
      setCartCount(0);
    }
  }, []);

  // Populate cart count on first render so the badge is correct immediately after
  // login or a page refresh (without this, the badge always starts at 0).
  useEffect(() => {
    refreshCartCount();
  }, [refreshCartCount]);

  return (
    <CartContext.Provider value={{ cartCount, refreshCartCount }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (ctx === undefined) {
    throw new Error('useCart must be used inside <CartProvider>');
  }
  return ctx;
}
