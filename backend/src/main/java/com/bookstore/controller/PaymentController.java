package com.bookstore.controller;

import com.bookstore.dto.payment.PaymentRequest;
import com.bookstore.dto.payment.PaymentResponse;
import com.bookstore.model.User;
import com.bookstore.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================
 * PaymentController — Checkout and Gift Points Endpoints
 * ============================================================
 *
 * BASE URL: /api/payment and /api/gift-points
 * All endpoints require authentication.
 *
 * ENDPOINTS:
 *   POST /api/payment/initiate      → checkout: simulate payment + create order
 *   GET  /api/gift-points/balance   → get current gift point balance
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payment/initiate
     *
     * The main checkout endpoint. Does everything in one call:
     *   - Validates basket, address, gift points
     *   - Simulates payment (90% success)
     *   - Creates the order
     *   - Clears the basket
     *   - Returns purchase confirmation data
     *
     * RESPONSE 201: PaymentResponse (full order + confirmation details)
     * RESPONSE 400: empty basket, invalid gift points
     * RESPONSE 402: simulated payment failure (try again)
     * RESPONSE 404: address not found
     */
    @PostMapping("/api/payment/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
    }

    /**
     * GET /api/gift-points/balance
     *
     * Returns the logged-in user's current gift point balance.
     * 1 point = ₹1. Points do not expire (D-003).
     *
     * RESPONSE 200: { "balance": 150 }
     */
    @GetMapping("/api/gift-points/balance")
    public ResponseEntity<Map<String, Integer>> getGiftPointBalance(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("balance", user.getGiftPointBalance()));
    }
}
