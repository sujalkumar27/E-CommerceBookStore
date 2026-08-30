package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * ============================================================
 * User — The Customer Account Entity
 * ============================================================
 *
 * WHAT THIS IS:
 * This Java class represents the "users" table in the database.
 * Every registered customer has one row in that table, and one
 * instance of this class in memory when they are loaded.
 *
 * HOW JPA WORKS HERE:
 * @Entity tells Hibernate: "this class maps to a database table"
 * @Table(name="users") tells Hibernate: "the table is called 'users'"
 * Each @Column field maps to one column in that table.
 *
 * WHY implements UserDetails?
 * Spring Security requires users to implement UserDetails so it
 * can check passwords, load authorities (roles), and manage sessions.
 * We implement it directly on the User entity to keep things simple.
 *
 * DATABASE TABLE: users
 * ┌──────────────────────┬──────────────────────────────────┐
 * │ Column               │ Description                      │
 * ├──────────────────────┼──────────────────────────────────┤
 * │ id                   │ Unique identifier (UUID)         │
 * │ email                │ Login email (must be unique)     │
 * │ password_hash        │ BCrypt hashed password           │
 * │ gift_point_balance   │ Redeemable points (1pt = ₹1)    │
 * │ created_at           │ When the account was created     │
 * └──────────────────────┴──────────────────────────────────┘
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor  // Lombok: generates a no-argument constructor (required by JPA)
public class User implements UserDetails {

    /**
     * Primary key — a UUID (Universally Unique Identifier).
     * UUIDs look like: "550e8400-e29b-41d4-a716-446655440000"
     * They are better than auto-increment numbers for security
     * (users can't guess other users' IDs by incrementing).
     *
     * @GeneratedValue(AUTO) tells Hibernate to generate the UUID automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user's display name — shown in the UI (e.g. in the Navbar greeting).
     * Not used for login — email is the login identifier.
     */
    @Column(nullable = false)
    private String name;

    /**
     * The user's email address — used as their username for login.
     * unique = true → database enforces no two users share the same email.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The BCrypt hash of the user's password.
     * We NEVER store the actual password — only this scrambled version.
     * Example: "$2b$12$X9Kd3mN..." (looks nothing like the real password)
     *
     * Column name "password_hash" maps to the DB column of the same name.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * How many gift points this user has available.
     * 1 gift point = ₹1 discount.
     * Starts at 0 for new users.
     * Is reduced when the user redeems points at checkout.
     */
    @Column(name = "gift_point_balance", nullable = false)
    private int giftPointBalance = 0;

    /**
     * When this account was created.
     * Instant = a precise moment in time (timezone-independent).
     * updatable = false → once set, this can never be changed.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Convenience constructor — used in AuthService when registering a new user.
     *
     * @param name         - the user's display name
     * @param email        - the user's email
     * @param passwordHash - the BCrypt hash of their password (NOT the plain password)
     */
    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // =========================================================
    // UserDetails interface methods — required by Spring Security
    // =========================================================

    /**
     * Returns the user's "username" — Spring Security uses email as username.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Returns the stored password hash — Spring Security uses this to verify login.
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Returns the user's roles/permissions.
     * All customers have the same role — no admin distinction.
     * We return an empty list (no special roles needed for this app).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    /** Account is always active — we don't support account locking */
    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }
}
