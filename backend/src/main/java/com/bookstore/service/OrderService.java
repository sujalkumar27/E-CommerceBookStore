package com.bookstore.service;

import com.bookstore.dto.order.AddressDto;
import com.bookstore.dto.order.OrderDto;
import com.bookstore.exception.BusinessRuleException;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Order;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.dto.cart.CartDto;
import com.bookstore.dto.cart.AddItemRequest;
import com.bookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * OrderService — Order History, Cancellation, Buy Again
 * ============================================================
 *
 * WHAT THIS DOES:
 * Handles all order management for customers:
 *   - View order history (FS-006)
 *   - View single order detail (FS-006)
 *   - Cancel an order within 48 hours (FS-013, D-002)
 *   - Buy Again — re-add order items to cart (FS-006)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final AddressService addressService;

    /** Get all orders for the logged-in user, newest first */
    @Transactional(readOnly = true)
    public List<OrderDto.OrderSummaryDto> getOrderHistory(User user) {
        return orderRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toSummaryDto).toList();
    }

    /** Get full detail of one order — ownership check */
    @Transactional(readOnly = true)
    public OrderDto.OrderDetailDto getOrderById(User user, UUID orderId) {
        Order order = findOwnedOrder(user, orderId);
        return toDetailDto(order);
    }

    /**
     * Cancel an order within the 48-hour window (D-002, FS-013).
     *
     * RULES:
     *   - Order must belong to this user
     *   - Order must not already be cancelled
     *   - Current time must be within 48 hours of paymentConfirmedAt
     *
     * NOTE: No payment reversal or gift point restoration in this capstone.
     */
    public OrderDto.OrderSummaryDto cancelOrder(User user, UUID orderId) {
        Order order = findOwnedOrder(user, orderId);

        // Already cancelled?
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("This order is already cancelled");
        }

        // Within 48-hour window? (D-002)
        if (!order.isCancellable()) {
            throw new BusinessRuleException(
                    "The 48-hour cancellation window has expired. This order cannot be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toSummaryDto(order);
    }

    /**
     * Buy Again — add all items from a past order back to the cart (FS-006, BR-007).
     * Returns the updated cart.
     */
    public CartDto buyAgain(User user, UUID orderId) {
        Order order = findOwnedOrder(user, orderId);

        // Add each item from the order back to the cart
        for (var item : order.getItems()) {
            cartService.addItem(user, new AddItemRequest(
                    item.getBook().getId(),
                    item.getQuantity()
            ));
        }

        return cartService.getCart(user);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /** Find an order and verify it belongs to the logged-in user */
    private Order findOwnedOrder(User user, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This order does not belong to you");
        }
        return order;
    }

    /** Convert Order → OrderSummaryDto (for list view) */
    private OrderDto.OrderSummaryDto toSummaryDto(Order order) {
        return new OrderDto.OrderSummaryDto(
                order.getId(),
                order.getCreatedAt(),
                order.getPaymentConfirmedAt(),
                order.getStatus(),
                order.getItems().size(),
                order.getTotalAmount(),
                order.getCancellationDeadline(),
                order.isCancellable()
        );
    }

    /** Convert Order → OrderDetailDto (for single order view) */
    private OrderDto.OrderDetailDto toDetailDto(Order order) {
        AddressDto addressDto = addressService.toDto(order.getDeliveryAddress());

        List<OrderDto.OrderItemDto> items = order.getItems().stream()
                .map(oi -> new OrderDto.OrderItemDto(
                        oi.getId(),
                        new OrderDto.BookInfo(
                                oi.getBook().getId(),
                                oi.getBook().getTitle(),
                                oi.getBook().getAuthor(),
                                oi.getBook().getCoverImageUrl()
                        ),
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getLineTotal(),
                        oi.getTentativeDeliveryDate()
                )).toList();

        return new OrderDto.OrderDetailDto(
                order.getId(),
                order.getCreatedAt(),
                order.getPaymentConfirmedAt(),
                order.getStatus(),
                order.getCancellationDeadline(),
                order.isCancellable(),
                order.getPaymentMethod(),
                order.getGiftPointsRedeemed(),
                order.getTotalAmount(),
                order.getTotalAmount().subtract(
                        java.math.BigDecimal.valueOf(order.getGiftPointsRedeemed())),
                addressDto,
                items
        );
    }
}
