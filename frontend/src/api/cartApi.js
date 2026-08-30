// cartApi.js — API calls for cart management.
// Endpoints:
//   GET    /api/cart                    - get current user's cart
//   POST   /api/cart/items              - add a book to cart
//   PUT    /api/cart/items/:itemId      - update quantity of one item
//   DELETE /api/cart/items/:itemId      - remove one item

import axiosClient from './axiosClient';

/** Get the current user's cart (items + totals). */
export const getCart = () =>
  axiosClient.get('/api/cart');

/**
 * Add a book to the cart.
 * @param {string} bookId   - UUID of the book
 * @param {number} quantity - How many to add
 */
export const addItem = (bookId, quantity = 1) =>
  axiosClient.post('/api/cart/items', { bookId, quantity });

/**
 * Update the quantity of a cart item.
 * @param {string} itemId   - UUID of the CartItem row
 * @param {number} quantity - New quantity
 */
export const updateItem = (itemId, quantity) =>
  axiosClient.put(`/api/cart/items/${itemId}`, { quantity });

/**
 * Remove a cart item entirely.
 * @param {string} itemId - UUID of the CartItem row
 */
export const removeItem = (itemId) =>
  axiosClient.delete(`/api/cart/items/${itemId}`);
