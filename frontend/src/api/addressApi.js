// addressApi.js — API calls for managing delivery addresses.
// Endpoints:
//   GET    /api/addresses        - list saved addresses
//   POST   /api/addresses        - add a new address
//   PUT    /api/addresses/:id    - update an existing address
//   DELETE /api/addresses/:id    - delete an address

import axiosClient from './axiosClient';

/** Get all saved delivery addresses for the current user. */
export const getAddresses = () =>
  axiosClient.get('/api/addresses');

/**
 * Add a new delivery address.
 * @param {Object} address - { fullName, line1, line2, city, state, pincode }
 */
export const addAddress = (address) =>
  axiosClient.post('/api/addresses', address);

/**
 * Update an existing address.
 * @param {string} id      - Address UUID
 * @param {Object} address - Updated fields
 */
export const updateAddress = (id, address) =>
  axiosClient.put(`/api/addresses/${id}`, address);

/**
 * Delete an address.
 * @param {string} id - Address UUID
 */
export const deleteAddress = (id) =>
  axiosClient.delete(`/api/addresses/${id}`);
