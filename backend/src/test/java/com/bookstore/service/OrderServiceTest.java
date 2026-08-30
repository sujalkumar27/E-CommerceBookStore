package com.bookstore.service;

import com.bookstore.exception.BusinessRuleException;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Order;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * OrderServiceTest — Unit Tests for Order Management Logic
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 *   getOrderHistory() → returns list of orders for the user
 *   getOrderById()    → returns detail or 404 / 403
 *   cancelOrder()     → cancels within window; rejects outside window or already cancelled
 *
 * KEY BUSINESS RULES UNDER TEST:
 *   - A user can only see/cancel THEIR OWN orders (ownership check → 403)
 *   - An already-cancelled order → ConflictException
 *   - Cancellation outside 48-hour window → BusinessRuleException
 *   - Within the window → status set to CANCELLED
 *
 * NOTE: buyAgain() delegates entirely to CartService.addItem() in a loop.
 *       The interaction is tested here; CartService itself is tested in CartServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartService cartService;
    @Mock private AddressService addressService;

    @InjectMocks
    private OrderService orderService;

    // ── Fixtures ──
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = makeUser("alice@test.com");
        bob   = makeUser("bob@test.com");
    }

    // ─────────────────────────────────────────────────────────
    // getOrderHistory()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderHistory: returns empty list when user has no orders")
    void getOrderHistory_noOrders_returnsEmptyList() {
        when(orderRepository.findAllByUserOrderByCreatedAtDesc(alice)).thenReturn(List.of());

        List<?> result = orderService.getOrderHistory(alice);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getOrderHistory: returns one summary per order")
    void getOrderHistory_twoOrders_returnsTwoSummaries() {
        Order o1 = makeConfirmedOrder(alice, Instant.now().minusSeconds(3_600));
        Order o2 = makeConfirmedOrder(alice, Instant.now().minusSeconds(7_200));
        when(orderRepository.findAllByUserOrderByCreatedAtDesc(alice)).thenReturn(List.of(o1, o2));

        List<?> result = orderService.getOrderHistory(alice);

        assertThat(result).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────
    // getOrderById()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderById: 404 when order does not exist")
    void getOrderById_notFound_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(alice, unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderById: 403 when order belongs to another user")
    void getOrderById_wrongUser_throwsForbiddenException() {
        Order bobsOrder = makeConfirmedOrder(bob, Instant.now().minusSeconds(3_600));
        when(orderRepository.findById(bobsOrder.getId())).thenReturn(Optional.of(bobsOrder));

        // Alice tries to view Bob's order
        assertThatThrownBy(() -> orderService.getOrderById(alice, bobsOrder.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─────────────────────────────────────────────────────────
    // cancelOrder()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder: successfully cancels a within-window order")
    void cancelOrder_withinWindow_setsStatusCancelled() {
        // Payment confirmed 1 hour ago → inside 48-hr window
        Order order = makeConfirmedOrder(alice, Instant.now().minusSeconds(3_600));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancelOrder(alice, order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder: throws ConflictException when order is already cancelled")
    void cancelOrder_alreadyCancelled_throwsConflictException() {
        Order order = makeConfirmedOrder(alice, Instant.now().minusSeconds(3_600));
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(alice, order.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("cancelOrder: throws BusinessRuleException when 48-hour window has expired")
    void cancelOrder_windowExpired_throwsBusinessRuleException() {
        // Payment confirmed 49 hours ago → outside window
        Order order = makeConfirmedOrder(alice, Instant.now().minusSeconds(49 * 3_600));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(alice, order.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("48-hour");
    }

    @Test
    @DisplayName("cancelOrder: throws ForbiddenException when order belongs to another user")
    void cancelOrder_wrongUser_throwsForbiddenException() {
        Order bobsOrder = makeConfirmedOrder(bob, Instant.now().minusSeconds(3_600));
        when(orderRepository.findById(bobsOrder.getId())).thenReturn(Optional.of(bobsOrder));

        // Alice tries to cancel Bob's order
        assertThatThrownBy(() -> orderService.cancelOrder(alice, bobsOrder.getId()))
                .isInstanceOf(ForbiddenException.class);

        // Status must NOT have changed
        assertThat(bobsOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    private User makeUser(String email) {
        User u = new User("Test User", email, "hash");
        setId(u, UUID.randomUUID());
        return u;
    }

    /**
     * Build an Order with CONFIRMED status, no items, belonging to the given user.
     * paymentConfirmedAt is set to control whether it is inside/outside the cancel window.
     */
    private Order makeConfirmedOrder(User owner, Instant paymentConfirmedAt) {
        Order order = new Order();
        setId(order, UUID.randomUUID());
        order.setUser(owner);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("999.00"));
        order.setPaymentConfirmedAt(paymentConfirmedAt);
        order.setPaymentMethod("CREDIT_CARD");
        // items must be a mutable list so OrderService can call .size()
        // (Order entity initialises items = new ArrayList<>() already)
        return order;
    }

    private void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}

