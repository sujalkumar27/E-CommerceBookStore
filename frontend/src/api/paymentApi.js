// paymentApi.js — Initiates a payment and creates the order in one step.
// Endpoint: POST /api/payment/initiate
//
// The backend:
//   1. Reads the user's cart
//   2. Deducts gift points if requested
//   3. Simulates a payment (90% success / 10% failure)
//   4. On success: creates an Order, clears the cart, returns PaymentResponse
//   5. On failure: returns 402 Payment Required

import axiosClient from './axiosClient';

/**
 * Initiate a payment.
 * @param {string} deliveryAddressId   - UUID of the selected delivery address
 * @param {string} paymentMethod       - "CREDIT_CARD" or "DEBIT_CARD"
 * @param {number} giftPointsToRedeem  - Points to redeem (0 if none)
 * @returns Promise<PaymentResponse>
 */
export const initiatePayment = (deliveryAddressId, paymentMethod, giftPointsToRedeem = 0) =>
  axiosClient.post('/api/payment/initiate', {
    deliveryAddressId,
    paymentMethod,
    giftPointsToRedeem,
  });
