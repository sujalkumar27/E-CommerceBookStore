package com.bookstore.controller;

import com.bookstore.dto.basket.AddItemRequest;
import com.bookstore.dto.basket.BasketDto;
import com.bookstore.dto.basket.UpdateItemRequest;
import com.bookstore.model.User;
import com.bookstore.service.BasketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ============================================================
 * BasketController — HTTP Endpoints for the Shopping Basket
 * ============================================================
 *
 * WHAT THIS IS:
 * Handles all basket-related HTTP requests.
 * ALL endpoints here require authentication (JWT token).
 * If a guest (no token) calls these → Spring Security returns 401.
 *
 * BASE URL: /api/basket
 *
 * ENDPOINTS:
 *   GET    /api/basket              → get current basket
 *   POST   /api/basket/items        → add a book to basket
 *   PUT    /api/basket/items/{id}   → update quantity
 *   DELETE /api/basket/items/{id}   → remove item
 *
 * HOW @AuthenticationPrincipal WORKS:
 * Spring Security reads the JWT token from the request header,
 * looks up the user in the database, and makes the User object
 * available as a method parameter. We just add @AuthenticationPrincipal
 * User user and Spring injects the logged-in user automatically.
 * No manual token parsing needed in the controller.
 */
@RestController
@RequestMapping("/api/basket")
@RequiredArgsConstructor
public class BasketController {

    private final BasketService basketService;

    /**
     * GET /api/basket
     * Returns the current user's full basket with item details and totals.
     *
     * RESPONSE 200:
     * { "items": [...], "basketTotal": 1598.00 }
     */
    @GetMapping
    public ResponseEntity<BasketDto> getBasket(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(basketService.getBasket(user));
    }

    /**
     * POST /api/basket/items
     * Add a book to the basket (or increase quantity if already present).
     *
     * REQUEST BODY:
     * { "bookId": "uuid", "quantity": 2 }
     *
     * RESPONSE 200: updated full basket
     * RESPONSE 400: invalid request (missing bookId, quantity < 1)
     * RESPONSE 401: not logged in
     * RESPONSE 404: book not found
     */
    @PostMapping("/items")
    public ResponseEntity<BasketDto> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(basketService.addItem(user, request));
    }

    /**
     * PUT /api/basket/items/{itemId}
     * Update the quantity of an existing basket item.
     *
     * REQUEST BODY:
     * { "quantity": 3 }
     *
     * RESPONSE 200: updated full basket
     * RESPONSE 403: item belongs to another user
     * RESPONSE 404: item not found
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<BasketDto> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(basketService.updateItem(user, itemId, request));
    }

    /**
     * DELETE /api/basket/items/{itemId}
     * Remove an item from the basket.
     *
     * RESPONSE 200: updated full basket (without the removed item)
     * RESPONSE 403: item belongs to another user
     * RESPONSE 404: item not found
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<BasketDto> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(basketService.removeItem(user, itemId));
    }
}
