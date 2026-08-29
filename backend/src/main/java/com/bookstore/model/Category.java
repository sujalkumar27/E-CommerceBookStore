package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * ============================================================
 * Category — A Book Category (Fiction, Non-Fiction, etc.)
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "categories" table in the database.
 * Every book belongs to exactly one category.
 *
 * WHY DELIVERY OFFSET DAYS IS HERE:
 * Business decision D-005 says delivery date = order date + offset.
 * The offset depends on the category — Fiction ships faster than
 * Academic textbooks. Storing it here means we can change delivery
 * times per category without touching code.
 *
 * CATEGORIES SEEDED:
 * The Python script creates categories like "Fiction", "Technology",
 * "History", etc. The SeedDataLoader creates them in the DB on startup.
 *
 * DATABASE TABLE: categories
 * ┌──────────────────────┬──────────────────────────────────────┐
 * │ Column               │ Description                          │
 * ├──────────────────────┼──────────────────────────────────────┤
 * │ id                   │ Unique identifier (UUID)             │
 * │ name                 │ Category name e.g. "Fiction"         │
 * │ delivery_offset_days │ Days to add to order date for ETA    │
 * └──────────────────────┴──────────────────────────────────────┘
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The display name of the category, e.g. "Fiction", "Technology".
     * unique = true → two categories cannot have the same name.
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * How many days after the order date the book should arrive.
     * Used in FS-008 (Tentative Delivery Date).
     *
     * Default offsets per design (D-005):
     *   Fiction          → 3 days
     *   Non-Fiction      → 5 days
     *   Technology       → 5 days
     *   History          → 5 days
     *   Academic         → 7 days
     *   (everything else)→ 5 days
     */
    @Column(name = "delivery_offset_days", nullable = false)
    private int deliveryOffsetDays = 5; // default: 5 days

    /**
     * Convenience constructor used by SeedDataLoader.
     *
     * @param name               - category name e.g. "Fiction"
     * @param deliveryOffsetDays - delivery days offset for this category
     */
    public Category(String name, int deliveryOffsetDays) {
        this.name = name;
        this.deliveryOffsetDays = deliveryOffsetDays;
    }
}
