package com.bookstore.dto.basket;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BasketDto — The Full Basket Response Sent to the Frontend
 * ============================================================
 *
 * WHAT THIS IS:
 * The shape of the basket data returned to the frontend for all
 * basket operations (GET, add item, update quantity, remove item).
 * Every basket endpoint returns this same shape.
 *
 * EXAMPLE JSON:
 * {
 *   "items": [
 *     {
 *       "id": "uuid-of-basket-item",
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
 *   "basketTotal": 1598.00
 * }
 *
 * WHY lineTotal AND basketTotal?
 * The frontend doesn't need to multiply price × quantity itself.
 * We compute it server-side for accuracy (avoids floating-point issues
 * in JavaScript).
 */
public record BasketDto(
        List<BasketItemDto> items,
        BigDecimal basketTotal        // sum of all lineTotals
) {
    /**
     * BasketItemDto — one line item in the basket.
     */
    public record BasketItemDto(
            UUID id,                  // basket item UUID (used for update/delete)
            BookInfo book,            // book details
            int quantity,             // how many copies
            BigDecimal lineTotal      // price × quantity
    ) {}

    /**
     * BookInfo — minimal book details needed in the basket view.
     * Full detail is not needed here — just enough to display the item.
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
