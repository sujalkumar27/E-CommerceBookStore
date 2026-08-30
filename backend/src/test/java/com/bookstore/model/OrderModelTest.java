package com.bookstore.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================
 * OrderModelTest — Unit Tests for Order Business Logic
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 * The Order entity has two computed methods with business logic:
 *
 *   isCancellable():
 *     Returns true only if:
 *       - paymentConfirmedAt is set (not null)
 *       - status is NOT already CANCELLED
 *       - current time is within 48 hours of paymentConfirmedAt
 *
 *   getCancellationDeadline():
 *     Returns paymentConfirmedAt + 48 hours, or null if not paid yet.
 *
 * WHY THESE ARE IMPORTANT:
 * The 48-hour window rule (D-002) is a key business rule.
 * It is implemented purely in the model — no database, no Spring needed.
 * This is exactly the kind of logic that should be unit tested in isolation.
 *
 * HOW THESE TESTS WORK:
 * We create Order objects directly (no mocks, no Spring).
 * We set paymentConfirmedAt to different times and check isCancellable().
 */
class OrderModelTest {

    // ─────────────────────────────────────────────────────────
    // isCancellable()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isCancellable returns true when paid recently (within 48 hours)")
    void isCancellable_trueWhenWithin48Hours() {
        Order order = new Order();
        // Payment was confirmed 1 hour ago → still within 48-hr window
        order.setPaymentConfirmedAt(Instant.now().minusSeconds(3_600));
        order.setStatus(OrderStatus.CONFIRMED);

        assertThat(order.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("isCancellable returns false when 48-hour window has expired")
    void isCancellable_falseAfter48Hours() {
        Order order = new Order();
        // Payment was confirmed 49 hours ago → window expired
        order.setPaymentConfirmedAt(Instant.now().minusSeconds(49 * 3_600));
        order.setStatus(OrderStatus.CONFIRMED);

        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("isCancellable returns false when order is already CANCELLED")
    void isCancellable_falseWhenAlreadyCancelled() {
        Order order = new Order();
        // Payment was just now — normally cancellable, BUT status is already CANCELLED
        order.setPaymentConfirmedAt(Instant.now().minusSeconds(60));
        order.setStatus(OrderStatus.CANCELLED);

        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("isCancellable returns false when paymentConfirmedAt is null (not yet paid)")
    void isCancellable_falseWhenPaymentNotConfirmed() {
        Order order = new Order();
        // paymentConfirmedAt defaults to null — order not yet paid
        order.setStatus(OrderStatus.PENDING);

        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("isCancellable returns true when payment confirmed exactly 47 hours ago")
    void isCancellable_trueAtBoundaryJustBeforeExpiry() {
        Order order = new Order();
        // 47 hours 59 minutes ago — just inside the 48-hour window
        order.setPaymentConfirmedAt(Instant.now().minusSeconds(47 * 3_600 + 59 * 60));
        order.setStatus(OrderStatus.CONFIRMED);

        assertThat(order.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("isCancellable returns true regardless of SHIPPED status within window")
    void isCancellable_trueWhenShippedButWithinWindow() {
        Order order = new Order();
        // D-002 says: cancellable regardless of status, only the time window matters
        order.setPaymentConfirmedAt(Instant.now().minusSeconds(1_800)); // 30 minutes ago
        order.setStatus(OrderStatus.SHIPPED);

        assertThat(order.isCancellable()).isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // getCancellationDeadline()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCancellationDeadline returns paymentConfirmedAt + 48 hours")
    void getCancellationDeadline_returnsCorrectDeadline() {
        Instant confirmedAt = Instant.now().minusSeconds(3_600); // 1 hour ago
        Order order = new Order();
        order.setPaymentConfirmedAt(confirmedAt);

        Instant expectedDeadline = confirmedAt.plusSeconds(172_800); // +48 hours

        assertThat(order.getCancellationDeadline()).isEqualTo(expectedDeadline);
    }

    @Test
    @DisplayName("getCancellationDeadline returns null when paymentConfirmedAt is null")
    void getCancellationDeadline_nullWhenNotPaid() {
        Order order = new Order();
        // paymentConfirmedAt is null → no deadline yet

        assertThat(order.getCancellationDeadline()).isNull();
    }
}
