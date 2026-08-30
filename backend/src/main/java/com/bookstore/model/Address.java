package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================
 * Address — A Customer's Saved Delivery Address
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "addresses" table.
 * A customer can save multiple addresses (D-009) and pick one at checkout.
 *
 * RELATIONSHIP:
 *   One User → many Addresses  (a user can have many saved addresses)
 *
 * DATABASE TABLE: addresses
 * ┌──────────────┬──────────────────────────────────────────────┐
 * │ Column       │ Description                                  │
 * ├──────────────┼──────────────────────────────────────────────┤
 * │ id           │ UUID primary key                             │
 * │ user_id      │ FK → users.id                               │
 * │ full_name    │ Recipient name e.g. "Alice Smith"            │
 * │ line1        │ Street address line 1                        │
 * │ line2        │ Apartment / suite (optional)                 │
 * │ city         │ City name                                    │
 * │ state        │ State name                                   │
 * │ pincode      │ PIN/ZIP code                                 │
 * │ is_default   │ Is this the user's default address?         │
 * │ created_at   │ When this address was saved                  │
 * └──────────────┴──────────────────────────────────────────────┘
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who owns this address.
     * ON DELETE CASCADE — if user is deleted, their addresses go too.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String line1;

    /** Apartment number, suite, landmark — optional */
    private String line2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    /** Mobile/phone number for delivery contact — 10-digit Indian mobile number */
    @Column(nullable = false)
    private String phone;

    /**
     * Whether this is the user's preferred/default delivery address.
     * At most one address per user should be true.
     * Enforced at application layer (not DB constraint).
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
