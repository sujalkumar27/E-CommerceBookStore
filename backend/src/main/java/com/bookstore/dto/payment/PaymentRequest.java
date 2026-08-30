package com.bookstore.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * ============================================================
 * PaymentRequest — What the client sends to initiate payment
 * ============================================================
 *
 * USED FOR:
 *   POST /api/payment/initiate
 *
 * EXAMPLE:
 * {
 *   "deliveryAddressId": "uuid-of-saved-address",
 *   "paymentMethod": "CREDIT_CARD",
 *   "giftPointsToRedeem": 50
 * }
 *
 * PAYMENT METHODS SUPPORTED (per BRD §13.2):
 *   CREDIT_CARD  — credit card payment (simulated)
 *   DEBIT_CARD   — debit card payment (simulated)
 *
 * GIFT POINTS (D-003):
 *   Optional. Default 0 (no points redeemed).
 *   1 point = ₹1 discount. Cannot exceed user balance or order total.
 */
public record PaymentRequest(

        @NotNull(message = "Delivery address is required")
        UUID deliveryAddressId,

        @NotBlank(message = "Payment method is required")
        String paymentMethod,   // "CREDIT_CARD" or "DEBIT_CARD"

        @Min(value = 0, message = "Gift points cannot be negative")
        int giftPointsToRedeem  // default 0

) {}
