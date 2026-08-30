package com.bookstore.controller;

import com.bookstore.dto.cart.CartDto;
import com.bookstore.dto.order.OrderDto;
import com.bookstore.model.User;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * OrderController — Order History, Cancellation, Buy Again
 * ============================================================
 *
 * BASE URL: /api/orders
 * All endpoints require authentication.
 *
 * ENDPOINTS:
 *   GET  /api/orders              → order history list
 *   GET  /api/orders/{id}         → single order detail
 *   POST /api/orders/{id}/cancel  → cancel order (within 48 hrs)
 *   POST /api/orders/{id}/buy-again → re-add items to cart
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** GET /api/orders — full order history, newest first */
    @GetMapping
    public ResponseEntity<List<OrderDto.OrderSummaryDto>> getOrders(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getOrderHistory(user));
    }

    /** GET /api/orders/{id} — single order full detail */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.OrderDetailDto> getOrderById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(user, id));
    }

    /**
     * POST /api/orders/{id}/cancel
     * Cancel an order within 48 hours of payment confirmation.
     * Returns 400 if window has expired, 409 if already cancelled.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDto.OrderSummaryDto> cancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(user, id));
    }

    /**
     * POST /api/orders/{id}/buy-again
     * Add all items from a past order back to the current cart.
     * Returns the updated cart.
     */
    @PostMapping("/{id}/buy-again")
    public ResponseEntity<CartDto> buyAgain(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.buyAgain(user, id));
    }
}
