package com.bookstore.dto.payment;

import com.bookstore.model.OrderStatus;
import com.bookstore.dto.order.AddressDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * PaymentResponse — Returned after successful payment
 * ============================================================
 *
 * This IS the purchase confirmation screen data (FS-012).
 * The frontend uses this response directly to render the
 * "Your order is confirmed!" page.
 *
 * EXAMPLE JSON:
 * {
 *   "orderId": "uuid",
 *   "status": "CONFIRMED",
 *   "paymentConfirmedAt": "2026-08-30T10:00:00Z",
 *   "cancellationDeadline": "2026-09-01T10:00:00Z",
 *   "orderTotal": 1598.00,
 *   "giftPointsRedeemed": 50,
 *   "amountCharged": 1548.00,
 *   "giftPointsBalance": 100,
 *   "items": [...],
 *   "deliveryAddress": {...}
 * }
 */
public record PaymentResponse(
        UUID orderId,
        OrderStatus status,
        Instant paymentConfirmedAt,
        Instant cancellationDeadline,   // paymentConfirmedAt + 48 hours
        BigDecimal orderTotal,          // total before gift point deduction
        int giftPointsRedeemed,
        BigDecimal amountCharged,       // what was actually charged to card
        int giftPointsBalance,          // user's remaining gift point balance
        List<OrderItemInfo> items,
        AddressDto deliveryAddress
) {
    /**
     * One item in the purchase confirmation.
     * Includes tentative delivery date per item (FS-012 AC-6).
     */
    public record OrderItemInfo(
            UUID bookId,
            String title,
            String author,
            int quantity,
            BigDecimal lineTotal,
            LocalDate tentativeDeliveryDate
    ) {}
}
