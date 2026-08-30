package com.bookstore.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * CartDto — The Full Cart Response Sent to the Frontend
 * ============================================================
 *
 * WHAT THIS IS:
 * The shape of the cart data returned to the frontend for all
 * cart operations (GET, add item, update quantity, remove item).
 * Every cart endpoint returns this same shape.
 *
 * EXAMPLE JSON:
 * {
 *   "items": [
 *     {
 *       "id": "uuid-of-cart-item",
 *       "book": {
 *         "id": "uuid-of-book",
 *         "title": "Clean Code",
 *         "author": "Robert Martin",
 *         "price": 799.00,
 *         "available": true,
 *         "coverImageUrl": "https://..."
 *       },
 *       "quantity": 2,
 *       "lineTotal": 1598.00
 *     }
 *   ],
 *   "cartTotal": 1598.00
 * }
 */
public record CartDto(
        List<CartItemDto> items,
        BigDecimal cartTotal       // sum of all lineTotals
) {
    /**
     * CartItemDto — one line item in the cart.
     */
    public record CartItemDto(
            UUID id,               // cart item UUID (used for update/delete)
            BookInfo book,         // book details
            int quantity,          // how many copies
            BigDecimal lineTotal   // price × quantity
    ) {}

    /**
     * BookInfo — minimal book details needed in the cart view.
     */
    public record BookInfo(
            UUID id,
            String title,
            String author,
            BigDecimal price,
            boolean available,
            String coverImageUrl
    ) {}
}
