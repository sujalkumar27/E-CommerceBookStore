# Implementation Plan — E-Commerce Bookstore

**Document:** Implementation Plan  
**Project:** AI-Assisted E-Commerce Bookstore  
**Status:** Draft — Awaiting Developer Review  
**Derives from:** `docs/feature-specifications.md`  
**Lifecycle stage:** Stage 2 — Plan  

---

## 1. Technology Stack

All choices below are proposed. The developer must confirm before Stage 3.

| Layer | Technology | Rationale |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 3.x | Industry-standard, mature ecosystem, strong REST support |
| **Database** | PostgreSQL 16 | Relational, suits order/user/product model; open source |
| **ORM** | Spring Data JPA / Hibernate | Standard Spring persistence layer |
| **Authentication** | Spring Security + JWT (JSON Web Tokens) | Stateless auth, fits REST API |
| **Frontend** | React 18 + Vite | Modern SPA, fast build tooling |
| **UI Components** | Tailwind CSS | Utility-first, minimal bundle |
| **HTTP Client** | Axios | Standard React HTTP client |
| **Seed Script** | Python 3.12 + `requests` | Fetch Open Library data, write JSON seed file |
| **Build Tool (BE)** | Maven | Standard Spring Boot build |
| **Containerisation** | Docker + Docker Compose | Local dev and consistent environment |
| **Testing (BE)** | JUnit 5 + Mockito | Standard Spring Boot testing |
| **Testing (FE)** | Vitest + React Testing Library | Vite-native test runner |

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Browser (React SPA)               │
│  Pages: Catalogue · Product · Basket · Checkout ·   │
│         Orders · Confirmation · Login · Register     │
└────────────────────┬────────────────────────────────┘
                     │ HTTPS / REST JSON
┌────────────────────▼────────────────────────────────┐
│              Spring Boot REST API                    │
│  Controllers → Services → Repositories              │
│  Spring Security (JWT filter)                       │
└────────────────────┬────────────────────────────────┘
                     │ JPA
┌────────────────────▼────────────────────────────────┐
│                  PostgreSQL                          │
│  Tables: users · addresses · books · categories ·   │
│          orders · order_items · gift_points          │
└─────────────────────────────────────────────────────┘

Offline (run once):
  Python seed script → Open Library API → seed.json → DB on startup
```

---

## 3. Project Structure

### 3.1 Backend (`/backend`)

```
backend/
├── src/main/java/com/bookstore/
│   ├── config/          # Security, CORS, JWT config
│   ├── controller/      # REST controllers (one per domain)
│   ├── service/         # Business logic
│   ├── repository/      # Spring Data JPA repositories
│   ├── model/           # JPA entity classes
│   ├── dto/             # Request/Response DTOs
│   ├── exception/       # Global exception handler
│   └── BookstoreApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── data/seed.json   # Written by seed script, read on startup
└── pom.xml
```

### 3.2 Frontend (`/frontend`)

```
frontend/
├── src/
│   ├── api/             # Axios API client modules
│   ├── components/      # Reusable UI components
│   ├── pages/           # Route-level page components
│   ├── context/         # Auth context, Basket context
│   ├── hooks/           # Custom React hooks
│   └── main.jsx
├── index.html
├── vite.config.js
└── package.json
```

### 3.3 Seed Script (`/seed`)

```
seed/
├── seed.py              # Fetches Open Library data, writes seed.json
├── requirements.txt
└── README.md
```

---

## 4. Domain Model (Entities)

| Entity | Key Fields | Notes |
|---|---|---|
| `User` | id, email, passwordHash, giftPointBalance, createdAt | Registered user |
| `Address` | id, userId, fullName, line1, line2, city, state, pincode, isDefault | Multiple per user |
| `Category` | id, name, deliveryOffsetDays | Drives delivery date (D-005) |
| `Book` | id, title, author, isbn, categoryId, publisher, price, stock, coverImageUrl, description, publishedYear | Core catalogue entity |
| `Order` | id, userId, status, totalAmount, giftPointsRedeemed, deliveryAddressId, paymentConfirmedAt, createdAt | `paymentConfirmedAt` drives 48-hr cancel window |
| `OrderItem` | id, orderId, bookId, quantity, unitPrice, tentativeDeliveryDate | Delivery date computed at order time |

---

## 5. API Surface (Summary)

Full API design is produced in Stage 3. This is a route-group overview only.

| Group | Base Path | Key Operations |
|---|---|---|
| Auth | `/api/auth` | POST register, POST login, POST logout |
| Catalogue | `/api/books` | GET list (search + filter), GET /{id} |
| Categories | `/api/categories` | GET list |
| Basket | `/api/basket` | GET, POST add item, PUT update qty, DELETE remove item |
| Addresses | `/api/addresses` | GET list, POST add, PUT update, DELETE remove |
| Orders | `/api/orders` | GET list, GET /{id}, POST create, POST /{id}/cancel |
| Gift Points | `/api/gift-points` | GET balance |
| Recommendations | `/api/recommendations` | GET (auth required) |
| Payment | `/api/payment` | POST initiate (simulated) |

---

## 6. Implementation Phases

Work is divided into 6 phases. Each phase produces a working, testable increment.

---

### Phase 1 — Project Scaffold & Infrastructure

**Goal:** Both the backend and frontend projects run locally with Docker Compose.

| Task | Spec |
|---|---|
| Initialise Spring Boot project with Maven | — |
| Configure PostgreSQL via Docker Compose | — |
| Configure Spring Security skeleton (permit all initially) | FS-001 |
| Initialise React + Vite frontend project | — |
| Configure Axios base client with auth token header | FS-001 |
| Configure CORS on backend | — |
| Write `docker-compose.yml` for local dev | — |

**Exit criteria:** `docker-compose up` starts backend + frontend + DB; health endpoint returns 200.

---

### Phase 2 — User Authentication (FS-001)

**Goal:** Registration, login, logout with JWT.

| Task | Spec |
|---|---|
| `User` entity + repository | FS-001 |
| Register endpoint: validate input, hash password (BCrypt), store user | FS-001 |
| Login endpoint: verify credentials, return JWT | FS-001 |
| JWT filter: validate token on protected routes | FS-001 |
| Frontend: Register page | FS-001 |
| Frontend: Login page | FS-001 |
| Frontend: Auth context (store token, expose `isLoggedIn`) | FS-001 |
| Frontend: Protected route wrapper (redirect guest to login) | FS-001, D-001 |

**Exit criteria:** User can register, log in, and access a protected test route. Guest is redirected from protected routes.

---

### Phase 3 — Catalogue, Search & Product Detail (FS-002, FS-003, FS-004, FS-014)

**Goal:** Full catalogue browsing, search, filtering, and product detail with related products.

| Task | Spec |
|---|---|
| `Category`, `Book` entities + repositories | FS-002 |
| Write Python seed script (Open Library → seed.json) | FS-014 |
| DataLoader: read seed.json → populate DB on startup | FS-014 |
| GET `/api/books` with query params: search, category, brand, minPrice, maxPrice, available | FS-004 |
| GET `/api/books/{id}` — book detail + related books (same category) | FS-003 |
| GET `/api/categories` | FS-002 |
| Frontend: Catalogue page (grid, category sidebar, brand filter) | FS-002 |
| Frontend: Search bar + filter panel | FS-004 |
| Frontend: Product detail page (info, tentative delivery date, related products) | FS-003, FS-008 |

**Exit criteria:** Guest can browse, search, and filter books. Product detail shows related books and delivery date.

---

### Phase 4 — Basket & Recommendations (FS-005, FS-011)

**Goal:** Authenticated basket management with recommendations panel.

| Task | Spec |
|---|---|
| Session-scoped basket (server-side or client-side with JWT) | FS-005 |
| POST `/api/basket` — add item | FS-005 |
| PUT `/api/basket/{itemId}` — update quantity | FS-005 |
| DELETE `/api/basket/{itemId}` — remove item | FS-005 |
| GET `/api/basket` — fetch current basket | FS-005 |
| GET `/api/recommendations` — blended logic (category + author + newest) | FS-011 |
| Frontend: Basket page (items, totals, recommendations panel) | FS-005, FS-011 |
| Frontend: "Add to basket" on catalogue + detail page | FS-005 |
| Frontend: Guest redirect to login on add-to-basket | FS-001, D-001 |

**Exit criteria:** Logged-in user can add, update, remove basket items. Recommendations appear in basket. Guest is blocked.

---

### Phase 5 — Orders, Addresses, Checkout & Payment (FS-006, FS-007, FS-009, FS-010, FS-012, FS-013)

**Goal:** Full purchase flow from basket to confirmation, plus order history and cancellation.

| Task | Spec |
|---|---|
| `Address` entity + CRUD endpoints | FS-007 |
| `Order`, `OrderItem` entities + repositories | FS-009 |
| POST `/api/payment` — simulate success/failure, create order on success | FS-009 |
| Gift point balance check + deduction at payment | FS-010 |
| Tentative delivery date calculation at order-item creation | FS-008 |
| GET `/api/orders` — order history | FS-006 |
| GET `/api/orders/{id}` — order detail | FS-006 |
| POST `/api/orders/{id}/cancel` — cancel within 48-hr window | FS-013 |
| Buy Again: add order items back to basket | FS-006 |
| Frontend: Address management page | FS-007 |
| Frontend: Checkout flow (address → payment method → gift points → confirm) | FS-009, FS-010 |
| Frontend: Payment confirmation screen | FS-009 |
| Frontend: Purchase confirmation screen | FS-012 |
| Frontend: Order history page + order detail | FS-006 |
| Frontend: Cancel order button (within window) | FS-013 |
| Frontend: Recommendations on basket page (already built in Phase 4) | FS-011 |

**Exit criteria:** Full purchase flow completes end-to-end. Order history and cancellation work correctly.

---

### Phase 6 — Polish, Testing & Verification

**Goal:** Tests pass, no security issues, application is demo-ready.

| Task | Spec |
|---|---|
| Backend unit tests: services (auth, order, basket, recommendations) | All |
| Backend integration tests: key API endpoints | All |
| Frontend component tests: catalogue, basket, checkout | All |
| Security review: no secrets in code, JWT validated, inputs sanitised | — |
| Error handling: generic error messages to client, detailed logs server-side | — |
| Structured logging review (no PII/passwords in logs) | — |
| End-to-end smoke test: full customer journey | All |
| README: setup instructions, seed script usage | — |

**Exit criteria:** All tests pass. Full customer journey works without errors.

---

## 7. Dependency & Sequencing

```
Phase 1 (Scaffold)
    └── Phase 2 (Auth)
            └── Phase 3 (Catalogue)       ← no auth dependency for browse
                    └── Phase 4 (Basket)  ← requires auth + catalogue
                            └── Phase 5 (Orders/Checkout)
                                    └── Phase 6 (Testing)
```

Phases 1–3 can be partially parallelised (scaffold + seed script are independent).

---

## 8. Open Decisions Required Before Phase 3

The following must be confirmed before Phase 3 begins:

| # | Decision needed | Default assumption if not answered |
|---|---|---|
| Q-A | Number of related products to show | 6 |
| Q-B | Number of recommendations to show | 8 |
| Q-C | Delivery offset days per category | Fiction=3, Non-Fiction=5, Academic=7, Default=5 |
| Q-H | Open Library fields to import | title, author, ISBN, subject/category, publisher, cover, year, description + seeded price |
| Q-I | Password reset required? | No — out of scope for this capstone |

---

## 9. Security Checklist (per project rules)

Applied throughout all phases:

- [ ] Passwords hashed with BCrypt (never stored plaintext)
- [ ] JWT tokens validated on every protected request
- [ ] No secrets or credentials hardcoded — all via environment variables
- [ ] All services bind to `127.0.0.1` (not `0.0.0.0`)
- [ ] Parameterised queries only (no string-concatenated SQL)
- [ ] Generic error messages returned to client; full detail logged server-side only
- [ ] No PII or tokens written to logs
- [ ] TLS 1.2+ for any external HTTP calls (Open Library seed script)
- [ ] CORS restricted to the frontend origin

---

## 10. Deliverables Summary

| Phase | Deliverable |
|---|---|
| 1 | Runnable scaffold: `docker-compose up` works |
| 2 | Auth: register, login, JWT, protected routes |
| 3 | Catalogue: seed data, browse, search, filter, product detail |
| 4 | Basket: add/update/remove, recommendations |
| 5 | Full purchase flow: checkout, payment, orders, cancellation |
| 6 | Tested, secure, demo-ready application |
