// orderApi.js — API calls for order management.
// Endpoints:
//   GET  /api/orders            - list all orders for the logged-in user
//   GET  /api/orders/:id        - get one order's full detail
//   POST /api/orders/:id/cancel    - cancel an order (within 48 h window)
//   POST /api/orders/:id/buy-again - re-add all items from a past order to cart

import axiosClient from './axiosClient';

/** Get all orders for the current user (newest first). */
export const getOrders = () =>
  axiosClient.get('/api/orders');

/**
 * Get full detail for one order.
 * @param {string} id - Order UUID
 */
export const getOrderById = (id) =>
  axiosClient.get(`/api/orders/${id}`);

/**
 * Cancel an order (only allowed within 48 hours of placing it).
 * @param {string} id - Order UUID
 */
export const cancelOrder = (id) =>
  axiosClient.post(`/api/orders/${id}/cancel`);

/**
 * Re-add all items from a past order to the current cart.
 * @param {string} id - Order UUID
 */
export const buyAgain = (id) =>
  axiosClient.post(`/api/orders/${id}/buy-again`);
