package com.bookstore.dto.order;

import com.bookstore.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * OrderDto — Order data returned in API responses
 * ============================================================
 *
 * TWO VARIANTS:
 *
 * OrderSummaryDto — used in the order history LIST
 *   Shows: id, date, status, item count, total, cancellable flag
 *   Does NOT include individual items (keeps list responses lean)
 *
 * OrderDetailDto — used for a SINGLE order detail view
 *   Shows everything: full item list, delivery address, payment info
 */
public class OrderDto {

    /**
     * Summary shown in order history list.
     * GET /api/orders → returns List<OrderSummaryDto>
     */
    public record OrderSummaryDto(
            UUID id,
            Instant createdAt,
            Instant paymentConfirmedAt,
            OrderStatus status,
            int itemCount,
            BigDecimal orderTotal,
            Instant cancellationDeadline,  // paymentConfirmedAt + 48 hours
            boolean cancellable            // server-computed: is cancellation still allowed?
    ) {}

    /**
     * Full detail for one order.
     * GET /api/orders/{id} → returns OrderDetailDto
     */
    public record OrderDetailDto(
            UUID id,
            Instant createdAt,
            Instant paymentConfirmedAt,
            OrderStatus status,
            Instant cancellationDeadline,
            boolean cancellable,
            String paymentMethod,
            int giftPointsRedeemed,
            BigDecimal orderTotal,
            BigDecimal amountPaid,         // totalAmount - gift points discount
            AddressDto deliveryAddress,
            List<OrderItemDto> items
    ) {}

    /**
     * One line item within an order.
     * Included inside OrderDetailDto.
     */
    public record OrderItemDto(
            UUID id,
            BookInfo book,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            LocalDate tentativeDeliveryDate
    ) {}

    /**
     * Minimal book info included in order items.
     * We show just enough to identify the book — no need for full detail.
     */
    public record BookInfo(
            UUID id,
            String title,
            String author,
            String coverImageUrl
    ) {}
}
