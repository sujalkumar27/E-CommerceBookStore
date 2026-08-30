package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * Order — A Completed Purchase Record
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "orders" table.
 * One Order = one complete checkout by a customer.
 * Created when payment is confirmed (simulated).
 *
 * KEY FIELDS EXPLAINED:
 *
 *   paymentConfirmedAt:
 *     The exact timestamp when payment was confirmed.
 *     This is the START of the 48-hour cancellation window (D-002).
 *     If null → payment has not been confirmed yet (PENDING status).
 *
 *   giftPointsRedeemed:
 *     How many gift points the customer used on this order.
 *     1 point = ₹1 discount (D-003).
 *     Deducted from user.giftPointBalance when order is confirmed.
 *
 *   totalAmount:
 *     The FINAL amount charged to the customer's card.
 *     = sum of all item prices - gift points discount
 *
 *   deliveryAddress (snapshot):
 *     We store a REFERENCE to the address at order time.
 *     If the customer later edits/deletes that address, the order
 *     still shows where it was supposed to be delivered.
 *
 * DATABASE TABLE: orders
 * ┌──────────────────────┬──────────────────────────────────────┐
 * │ Column               │ Description                          │
 * ├──────────────────────┼──────────────────────────────────────┤
 * │ id                   │ UUID primary key                     │
 * │ user_id              │ FK → users.id                       │
 * │ status               │ PENDING/CONFIRMED/SHIPPED/DELIVERED/ │
 * │                      │ CANCELLED                            │
 * │ total_amount         │ Final amount charged (after points)  │
 * │ gift_points_redeemed │ Points used (1pt = ₹1)              │
 * │ delivery_address_id  │ FK → addresses.id                   │
 * │ payment_method       │ CREDIT_CARD or DEBIT_CARD            │
 * │ payment_confirmed_at │ When payment was confirmed           │
 * │ created_at           │ When order was created               │
 * └──────────────────────┴──────────────────────────────────────┘
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** The customer who placed this order */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Current order status.
     * @Enumerated(STRING) stores "CONFIRMED" not "1" — more readable in DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    /** Final amount charged after gift point discount */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** How many gift points were redeemed. 1 point = ₹1. Default 0. */
    @Column(name = "gift_points_redeemed", nullable = false)
    private int giftPointsRedeemed = 0;

    /** The delivery address selected at checkout */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private Address deliveryAddress;

    /** CREDIT_CARD or DEBIT_CARD */
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    /**
     * When payment was confirmed (simulated).
     * NULL until payment completes.
     * The 48-hour cancellation window starts from this timestamp (D-002).
     */
    @Column(name = "payment_confirmed_at")
    private Instant paymentConfirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * All line items in this order.
     *
     * @OneToMany = one Order has many OrderItems
     * cascade = PERSIST: when we save the Order, its items are saved too
     * orphanRemoval = if an item is removed from this list, delete it from DB
     * mappedBy = "order": the OrderItem class has a field called "order" pointing back here
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Convenience method — is this order still within the 48-hour cancellation window?
     * Called by OrderService to decide if cancellation is allowed (D-002).
     *
     * @return true if cancellation is still allowed
     */
    public boolean isCancellable() {
        if (paymentConfirmedAt == null) return false;
        if (status == OrderStatus.CANCELLED) return false;
        // 48 hours = 172800 seconds
        return Instant.now().isBefore(paymentConfirmedAt.plusSeconds(172_800));
    }

    /**
     * The cancellation deadline = paymentConfirmedAt + 48 hours.
     * Returned in API responses so the frontend can show a countdown.
     */
    public Instant getCancellationDeadline() {
        if (paymentConfirmedAt == null) return null;
        return paymentConfirmedAt.plusSeconds(172_800);
    }
}
