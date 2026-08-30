package com.bookstore.controller;

import com.bookstore.dto.cart.AddItemRequest;
import com.bookstore.dto.cart.CartDto;
import com.bookstore.dto.cart.UpdateItemRequest;
import com.bookstore.model.User;
import com.bookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ============================================================
 * CartController — HTTP Endpoints for the Shopping Cart
 * ============================================================
 *
 * WHAT THIS IS:
 * Handles all cart-related HTTP requests.
 * ALL endpoints here require authentication (JWT token).
 * If a guest (no token) calls these → Spring Security returns 401.
 *
 * BASE URL: /api/cart
 *
 * ENDPOINTS:
 *   GET    /api/cart              → get current cart
 *   POST   /api/cart/items        → add a book to cart
 *   PUT    /api/cart/items/{id}   → update quantity
 *   DELETE /api/cart/items/{id}   → remove item
 *
 * HOW @AuthenticationPrincipal WORKS:
 * Spring Security reads the JWT token, looks up the User in the DB,
 * and injects it directly into the method. No manual token parsing needed.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** GET /api/cart — returns the logged-in user's full cart */
    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getCart(user));
    }

    /**
     * POST /api/cart/items
     * Add a book to the cart (or increase quantity if already present).
     * Body: { "bookId": "uuid", "quantity": 2 }
     */
    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(user, request));
    }

    /**
     * PUT /api/cart/items/{itemId}
     * Update the quantity of an existing cart item.
     * Body: { "quantity": 3 }
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(user, itemId, request));
    }

    /**
     * DELETE /api/cart/items/{itemId}
     * Remove an item from the cart.
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(user, itemId));
    }
}
