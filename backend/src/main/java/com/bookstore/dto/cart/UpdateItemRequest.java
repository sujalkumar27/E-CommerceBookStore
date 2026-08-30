package com.bookstore.dto.cart;

import jakarta.validation.constraints.Min;

/**
 * UpdateItemRequest — What the client sends to change quantity.
 * PUT /api/cart/items/{itemId}
 * Body: { "quantity": 3 }
 */
public record UpdateItemRequest(

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity

) {}
