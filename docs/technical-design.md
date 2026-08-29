# Technical Design — E-Commerce Bookstore

**Document:** Technical Design  
**Project:** AI-Assisted E-Commerce Bookstore  
**Status:** Draft — Awaiting Developer Review  
**Derives from:** `docs/implementation-plan.md`, `docs/feature-specifications.md`  
**Lifecycle stage:** Stage 3 — Design  

---

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Browser (React 18 SPA)                  │
│  Vite · Tailwind CSS · Axios · React Router v6          │
│                                                         │
│  Pages: /login  /register  /  /books/:id                │
│         /basket  /checkout  /orders  /orders/:id        │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS REST/JSON
                       │ Authorization: Bearer <JWT>
┌──────────────────────▼──────────────────────────────────┐
│            Spring Boot 3.x REST API (Java 21)            │
│                                                         │
│  @RestControllers  →  @Services  →  @Repositories       │
│  Spring Security JWT filter chain                       │
│  Global exception handler (@ControllerAdvice)           │
│  DataLoader (seed.json → DB on startup)                 │
└──────────────────────┬──────────────────────────────────┘
                       │ JPA / Hibernate
┌──────────────────────▼──────────────────────────────────┐
│                   PostgreSQL 16                          │
│  7 tables · 1 enum · GIN full-text index                │
└─────────────────────────────────────────────────────────┘

Offline:
  seed/seed.py  →  Open Library API  →  src/main/resources/data/seed.json
```

---

## 2. Database Schema

### 2.1 Entity-Relationship Overview

```
users ──< addresses
users ──< orders >── addresses
orders ──< order_items >── books >── categories
```

### 2.2 Full DDL

```sql
-- ============================================================
-- E-Commerce Bookstore — PostgreSQL Database Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- gen_random_uuid()

-- ------------------------------------------------------------
-- Enum
-- ------------------------------------------------------------
CREATE TYPE order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE users (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    email               TEXT           NOT NULL UNIQUE,
    password_hash       TEXT           NOT NULL,
    gift_point_balance  INTEGER        NOT NULL DEFAULT 0 CHECK (gift_point_balance >= 0),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);
COMMENT ON COLUMN users.gift_point_balance IS '1 point = ₹1; redeemable up to 100% of order total; no expiry.';

-- ------------------------------------------------------------
-- addresses
-- ------------------------------------------------------------
CREATE TABLE addresses (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name   TEXT        NOT NULL,
    line1       TEXT        NOT NULL,
    line2       TEXT,
    city        TEXT        NOT NULL,
    state       TEXT        NOT NULL,
    pincode     TEXT        NOT NULL,
    is_default  BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON COLUMN addresses.is_default IS 'At most one true per user — enforced at application layer.';

-- ------------------------------------------------------------
-- categories
-- ------------------------------------------------------------
CREATE TABLE categories (
    id                   UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 TEXT    NOT NULL UNIQUE,
    delivery_offset_days INTEGER NOT NULL DEFAULT 5 CHECK (delivery_offset_days > 0)
);
COMMENT ON COLUMN categories.delivery_offset_days IS 'Fiction=3, Non-Fiction=5, Academic/Textbooks=7, default=5.';

INSERT INTO categories (name, delivery_offset_days) VALUES
    ('Fiction',            3),
    ('Non-Fiction',        5),
    ('Academic/Textbooks', 7)
ON CONFLICT (name) DO NOTHING;

-- ------------------------------------------------------------
-- books
-- ------------------------------------------------------------
CREATE TABLE books (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    title           TEXT           NOT NULL,
    author          TEXT           NOT NULL,
    isbn            TEXT           UNIQUE,
    category_id     UUID           NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    publisher       TEXT,
    price           NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    stock           INTEGER        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    cover_image_url TEXT,
    description     TEXT,
    published_year  INTEGER        CHECK (published_year BETWEEN 1000 AND 9999),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    search_vector   TSVECTOR
        GENERATED ALWAYS AS (
            setweight(to_tsvector('english', coalesce(title,     '')), 'A') ||
            setweight(to_tsvector('english', coalesce(author,    '')), 'B') ||
            setweight(to_tsvector('english', coalesce(publisher, '')), 'C')
        ) STORED
);
COMMENT ON COLUMN books.search_vector IS 'title(A), author(B), publisher(C). Category search via JOIN.';

-- ------------------------------------------------------------
-- orders
-- ------------------------------------------------------------
CREATE TABLE orders (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status               order_status   NOT NULL DEFAULT 'PENDING',
    total_amount         NUMERIC(10,2)  NOT NULL CHECK (total_amount >= 0),
    gift_points_redeemed INTEGER        NOT NULL DEFAULT 0 CHECK (gift_points_redeemed >= 0),
    delivery_address_id  UUID           NOT NULL REFERENCES addresses(id) ON DELETE RESTRICT,
    payment_method       TEXT           NOT NULL,
    payment_confirmed_at TIMESTAMPTZ,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT gift_points_within_total CHECK (gift_points_redeemed <= total_amount)
);
COMMENT ON COLUMN orders.payment_confirmed_at IS 'Cancellation allowed within 48 hours of this timestamp (D-002).';

-- ------------------------------------------------------------
-- order_items
-- ------------------------------------------------------------
CREATE TABLE order_items (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id                 UUID          NOT NULL REFERENCES books(id)  ON DELETE RESTRICT,
    quantity                INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price              NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    tentative_delivery_date DATE          NOT NULL
);
COMMENT ON COLUMN order_items.unit_price IS 'Price snapshot at order time — independent of future catalogue changes.';
COMMENT ON COLUMN order_items.tentative_delivery_date IS 'order date + categories.delivery_offset_days at order creation.';

-- ------------------------------------------------------------
-- Indexes
-- ------------------------------------------------------------
CREATE INDEX idx_books_category_id    ON books(category_id);
CREATE INDEX idx_books_publisher      ON books(publisher);
CREATE INDEX idx_books_search_vector  ON books USING GIN (search_vector);
CREATE INDEX idx_orders_user_id       ON orders(user_id);
CREATE INDEX idx_orders_cancellable   ON orders(payment_confirmed_at)
    WHERE status != 'CANCELLED' AND payment_confirmed_at IS NOT NULL;
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_book_id  ON order_items(book_id);
CREATE INDEX idx_addresses_user_id    ON addresses(user_id);
```

### 2.3 Key Design Decisions

| Decision | Rationale |
|---|---|
| `search_vector` as `GENERATED ALWAYS AS … STORED` | Auto-maintained on every write; no trigger needed |
| `unit_price` snapshot on `order_items` | Historical prices must not be affected by future catalogue updates |
| `payment_confirmed_at` drives 48-hr cancellation | Explicit per D-002; no magic status-based rules |
| `ON DELETE RESTRICT` on orders → users/books | Preserve full order history |
| `ON DELETE CASCADE` on addresses → users | Owned data; clean removal |
| `gift_points_within_total` CHECK constraint | DB-enforced: redeemed points cannot exceed order total |

---

## 3. Backend Design

### 3.1 Package Structure

```
com.bookstore/
├── config/
│   ├── SecurityConfig.java          # Spring Security filter chain
│   ├── JwtConfig.java               # JWT secret, expiry from env vars
│   └── CorsConfig.java              # Restrict to frontend origin
├── security/
│   ├── JwtUtil.java                 # Generate + validate tokens
│   └── JwtAuthFilter.java           # OncePerRequestFilter
├── controller/
│   ├── AuthController.java
│   ├── BookController.java
│   ├── CategoryController.java
│   ├── BasketController.java
│   ├── AddressController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── GiftPointController.java
│   └── RecommendationController.java
├── service/
│   ├── AuthService.java
│   ├── BookService.java
│   ├── BasketService.java
│   ├── AddressService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── RecommendationService.java
├── repository/
│   ├── UserRepository.java
│   ├── BookRepository.java
│   ├── CategoryRepository.java
│   ├── BasketRepository.java        # or in-memory/session-scoped
│   ├── AddressRepository.java
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
├── model/
│   ├── User.java
│   ├── Address.java
│   ├── Category.java
│   ├── Book.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java             # enum
├── dto/
│   ├── auth/   RegisterRequest, LoginRequest, AuthResponse
│   ├── book/   BookSummaryDto, BookDetailDto
│   ├── basket/ BasketDto, AddItemRequest, UpdateItemRequest
│   ├── order/  OrderSummaryDto, OrderDetailDto, CancelResponse
│   └── payment/PaymentRequest, PaymentResponse
├── exception/
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── ForbiddenException.java
│   └── BusinessRuleException.java
├── loader/
│   └── SeedDataLoader.java          # ApplicationRunner reads seed.json
└── BookstoreApplication.java
```

### 3.2 Security Design

- **JWT secret** read from environment variable `JWT_SECRET` (never hardcoded)
- **BCrypt** password hashing (`strength = 12`)
- **JWT expiry:** 24 hours (configurable via `JWT_EXPIRY_HOURS` env var)
- **`JwtAuthFilter`** runs on every request; sets `SecurityContextHolder` on valid token
- **CORS:** backend allows only the frontend origin (configurable via `FRONTEND_ORIGIN` env var)
- **Service binding:** Spring Boot binds to `127.0.0.1` in local dev; container networking in Docker
- **Error responses:** `GlobalExceptionHandler` returns generic messages to client; full stack trace logged server-side only (never returned to client)
- **No sensitive data in logs:** user passwords, tokens, and card details must never appear in log statements

### 3.3 Basket Storage Strategy

The basket is stored **server-side** in the database using a `basket_items` table (not session-scoped in-memory) so that it persists across sessions:

```sql
CREATE TABLE basket_items (
    id        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id   UUID          NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    quantity  INTEGER       NOT NULL CHECK (quantity > 0),
    added_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (user_id, book_id)
);
```

On `POST /api/payment/initiate` success, all `basket_items` for the user are deleted within the same transaction that creates the order.

### 3.4 Recommendation Algorithm

`RecommendationService.getRecommendations(userId)`:

1. Fetch all `book_id` values from `order_items` joined through `orders` for this user → `orderedBookIds`
2. From those books, collect distinct `category_id` values → `purchasedCategoryIds`
3. From those books, collect distinct `author` values → `purchasedAuthors`
4. Query: books **not in** `orderedBookIds`, ordered by:
   - Signal priority: SAME_CATEGORY → SAME_AUTHOR → NEWEST
   - Within each signal: ordered by `created_at DESC`
5. De-duplicate across signals; take top 8
6. If `orderedBookIds` is empty (no history): return 8 newest books only

### 3.5 Payment Simulation

`PaymentService.initiatePayment(request, userId)`:

1. Validate basket is non-empty
2. Validate `deliveryAddressId` belongs to user
3. Validate `giftPointsToRedeem` ≤ user balance and ≤ order total
4. **Simulate payment** — random 90% success / 10% failure (or always-success in test profile)
5. On failure: return `402 Payment Required`
6. On success (within a single DB transaction):
   - Create `Order` record with `status = CONFIRMED`, `payment_confirmed_at = now()`
   - Create `OrderItem` records; compute `tentative_delivery_date = now().date + category.deliveryOffsetDays`
   - Deduct `giftPointsToRedeem` from `users.gift_point_balance`
   - Delete all `basket_items` for user
   - Return `201` with full order summary

### 3.6 Order Cancellation

`OrderService.cancelOrder(orderId, userId)`:

1. Fetch order; verify `order.userId == userId` (else `403`)
2. Check `order.status != CANCELLED` (else `409`)
3. Check `now() < order.paymentConfirmedAt + 48h` (else `400`)
4. Set `order.status = CANCELLED`; save
5. Return `200` with updated order summary

> Gift-point restoration and payment reversal are **not** implemented in this capstone (per FS-013 assumption).

---

## 4. REST API Contract

### 4.1 Common Error Envelope

All `4xx`/`5xx` responses:

```json
{
  "status":    400,
  "error":     "Bad Request",
  "message":   "Human-readable detail",
  "timestamp": "2026-08-29T10:00:00Z",
  "path":      "/api/payment/initiate"
}
```

### 4.2 Auth Header

```
Authorization: Bearer <JWT>
```

Missing/expired → `401 Unauthorized`  
Valid but wrong user for the resource → `403 Forbidden`

---

### Group 1 — AUTH `/api/auth`

#### `POST /api/auth/register`
- **Auth:** No
- **Request:** `{ "email": "string", "password": "string (min 8 chars)" }`
- **Response 201:** `{ "token": "JWT", "user": { "id", "email", "createdAt" } }`
- **Errors:** `400` invalid fields · `409` email already exists

#### `POST /api/auth/login`
- **Auth:** No
- **Request:** `{ "email": "string", "password": "string" }`
- **Response 200:** `{ "token": "JWT", "user": { "id", "email" } }`
- **Errors:** `400` missing fields · `401` invalid credentials (generic message only)

#### `POST /api/auth/logout`
- **Auth:** Yes
- **Request:** None
- **Response 200:** `{ "message": "Logged out successfully." }`
- **Notes:** Stateless JWT — client discards token

---

### Group 2 — BOOKS `/api/books`

#### `GET /api/books`
- **Auth:** No
- **Query params:** `search`, `categoryId`, `publisher`, `minPrice`, `maxPrice`, `available`, `page` (default 0), `size` (default 20)
- **Response 200:** `{ "content": [BookSummary], "page", "size", "totalElements", "totalPages" }`

**BookSummary shape:**
```json
{
  "id", "title", "author", "publisher",
  "category": { "id", "name" },
  "price", "available", "coverImageUrl", "publishedYear"
}
```

#### `GET /api/books/{id}`
- **Auth:** No
- **Response 200:** Full book + `relatedBooks` (up to 6, same category) + `tentativeDeliveryDays`
- **Errors:** `404`

---

### Group 3 — CATEGORIES `/api/categories`

#### `GET /api/categories`
- **Auth:** No
- **Response 200:** `[ { "id", "name", "deliveryOffsetDays" } ]`

---

### Group 4 — BASKET `/api/basket`

All require auth. Guest → `401`.

#### `GET /api/basket` → `200` basket object
#### `POST /api/basket/items` — `{ "bookId", "quantity" }` → `200` updated basket
#### `PUT /api/basket/items/{itemId}` — `{ "quantity" }` → `200` updated basket
#### `DELETE /api/basket/items/{itemId}` → `200` updated basket

**Basket shape:**
```json
{
  "id", "items": [
    { "id", "book": { "id","title","author","price","available","coverImageUrl" },
      "quantity", "lineTotal" }
  ],
  "basketTotal"
}
```

---

### Group 5 — ADDRESSES `/api/addresses`

All require auth.

#### `GET /api/addresses` → `200` array of addresses
#### `POST /api/addresses` — `{ fullName, line1, line2?, city, state, pincode }` → `201`
#### `PUT /api/addresses/{id}` → `200` | `403` | `404`
#### `DELETE /api/addresses/{id}` → `204` | `403` | `404`

---

### Group 6 — ORDERS `/api/orders`

All require auth.

#### `GET /api/orders` → `200` array of OrderSummary
#### `GET /api/orders/{id}` → `200` full OrderDetail | `403` | `404`
#### `POST /api/orders/{id}/cancel` → `200` | `400` (window expired) | `403` | `404` | `409` (already cancelled)
#### `POST /api/orders/{id}/buy-again` → `200` updated basket | `403` | `404`

**OrderSummary shape:**
```json
{
  "id", "createdAt", "paymentConfirmedAt", "status",
  "itemCount", "orderTotal", "cancellationDeadline", "cancellable"
}
```

---

### Group 7 — PAYMENT `/api/payment`

#### `POST /api/payment/initiate`
- **Auth:** Yes
- **Request:** `{ "deliveryAddressId": "UUID", "paymentMethod": "CREDIT_CARD|DEBIT_CARD", "giftPointsToRedeem": 0 }`
- **Response 201:** Full order + purchase confirmation data
- **Response 402:** `{ "message": "Payment failed. Please try again." }`
- **Errors:** `400` empty basket / invalid points · `404` address not found

---

### Group 8 — GIFT POINTS `/api/gift-points`

#### `GET /api/gift-points/balance`
- **Auth:** Yes
- **Response 200:** `{ "balance": 150 }`

---

### Group 9 — RECOMMENDATIONS `/api/recommendations`

#### `GET /api/recommendations`
- **Auth:** Yes
- **Response 200:** `{ "books": [ ...up to 8 BookSummary + "recommendationSignal": "SAME_CATEGORY|SAME_AUTHOR|NEWEST" ] }`

---

## 5. Frontend Design

### 5.1 Route Map

| Route | Component | Auth required |
|---|---|---|
| `/` | `CataloguePage` | No |
| `/books/:id` | `ProductDetailPage` | No |
| `/login` | `LoginPage` | No |
| `/register` | `RegisterPage` | No |
| `/basket` | `BasketPage` | Yes |
| `/checkout` | `CheckoutPage` | Yes |
| `/checkout/confirmation` | `PurchaseConfirmationPage` | Yes |
| `/orders` | `OrderHistoryPage` | Yes |
| `/orders/:id` | `OrderDetailPage` | Yes |
| `/account/addresses` | `AddressManagementPage` | Yes |

### 5.2 State Management

| State | Location | Notes |
|---|---|---|
| Auth (token, user) | `AuthContext` (React Context + localStorage) | Token persisted across page refresh |
| Basket item count (header badge) | `BasketContext` (React Context) | Sync'd with server on login |
| Server data | Local component state + `useEffect` | No Redux; too small to warrant it |

### 5.3 Protected Route

```jsx
// Wraps any route requiring auth; redirects to /login if no token
<ProtectedRoute>
  <BasketPage />
</ProtectedRoute>
```

### 5.4 Checkout Flow (page sequence)

```
BasketPage
  → CheckoutPage (step 1: select address)
  → CheckoutPage (step 2: payment method + card details)
  → CheckoutPage (step 3: apply gift points + order summary)
  → [POST /api/payment/initiate]
  → PurchaseConfirmationPage
```

---

## 6. Seed Script Design (`seed/seed.py`)

```
seed.py
  → Fetch subject lists from Open Library API (e.g. /subjects/fiction.json)
  → For each book record: extract title, author, isbn, publisher, subject, published year, cover id
  → Map subject → internal category (Fiction / Non-Fiction / Academic / default)
  → Generate a seeded price (e.g. ₹199 – ₹999 based on category)
  → Write output to backend/src/main/resources/data/seed.json
```

**Minimum fields per book in seed.json:**

```json
{
  "title": "string",
  "author": "string",
  "isbn": "string | null",
  "category": "Fiction | Non-Fiction | Academic/Textbooks",
  "publisher": "string | null",
  "price": 299.00,
  "stock": 50,
  "coverImageUrl": "string | null",
  "description": "string | null",
  "publishedYear": 2021
}
```

**`SeedDataLoader.java`** (Spring `ApplicationRunner`):
- Reads `seed.json` on startup
- Upserts categories first, then books (skip if ISBN already exists)
- Idempotent: safe to run on every startup

---

## 7. Configuration & Secrets

All secrets are provided via environment variables. **Nothing is hardcoded.**

| Variable | Used by | Example |
|---|---|---|
| `DB_URL` | Spring Boot datasource | `jdbc:postgresql://localhost:5432/bookstore` |
| `DB_USER` | Spring Boot datasource | `bookstore_user` |
| `DB_PASSWORD` | Spring Boot datasource | *(secret)* |
| `JWT_SECRET` | JwtConfig | *(min 256-bit random string)* |
| `JWT_EXPIRY_HOURS` | JwtConfig | `24` |
| `FRONTEND_ORIGIN` | CorsConfig | `http://localhost:5173` |

Local dev: use a `.env` file (added to `.gitignore`) loaded by Docker Compose.

---

## 8. Docker Compose (Local Dev)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: bookstore
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://db:5432/bookstore
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      FRONTEND_ORIGIN: http://localhost:5173
    depends_on:
      - db

  frontend:
    build: ./frontend
    ports:
      - "5173:5173"
    environment:
      VITE_API_BASE_URL: http://localhost:8080
```

> Note: In production or CI, services bind to specific internal addresses — `0.0.0.0` is used only within the container network boundary (not exposed directly to the host network beyond the mapped ports).

---

## 9. Testing Strategy

### Backend
| Test type | Tool | Scope |
|---|---|---|
| Unit tests | JUnit 5 + Mockito | `AuthService`, `OrderService`, `PaymentService`, `RecommendationService` |
| Integration tests | `@SpringBootTest` + `@DataJpaTest` | Repository queries, cancellation window logic |
| Controller tests | `MockMvc` | All REST endpoints: valid + invalid inputs |

### Frontend
| Test type | Tool | Scope |
|---|---|---|
| Component tests | Vitest + React Testing Library | `CataloguePage`, `BasketPage`, `CheckoutPage` |
| Auth flow | RTL | Login redirect, protected route behaviour |

### Security tests (per project rules)
- No hardcoded secrets (grep check in CI)
- JWT: expired token → 401, tampered token → 401
- Ownership: user A cannot access user B's orders/basket/addresses → 403

---

## 10. Traceability Matrix

| Feature Spec | API Endpoints | DB Tables | Service |
|---|---|---|---|
| FS-001 Auth | `/api/auth/*` | `users` | `AuthService` |
| FS-002 Catalogue | `GET /api/books`, `GET /api/categories` | `books`, `categories` | `BookService` |
| FS-003 Product Detail | `GET /api/books/{id}` | `books`, `categories` | `BookService` |
| FS-004 Search/Filter | `GET /api/books?search=…` | `books` (GIN index) | `BookService` |
| FS-005 Basket | `/api/basket/*` | `basket_items` | `BasketService` |
| FS-006 Order History | `GET /api/orders`, `POST /api/orders/{id}/buy-again` | `orders`, `order_items` | `OrderService` |
| FS-007 Addresses | `/api/addresses/*` | `addresses` | `AddressService` |
| FS-008 Delivery Date | Computed in `PaymentService` | `categories.delivery_offset_days` | `PaymentService` |
| FS-009 Checkout/Payment | `POST /api/payment/initiate` | `orders`, `order_items`, `basket_items` | `PaymentService` |
| FS-010 Gift Points | `GET /api/gift-points/balance` + payment | `users.gift_point_balance` | `PaymentService` |
| FS-011 Recommendations | `GET /api/recommendations` | `order_items`, `books` | `RecommendationService` |
| FS-012 Purchase Confirm | Response of `POST /api/payment/initiate` | — | `PaymentService` |
| FS-013 Cancellation | `POST /api/orders/{id}/cancel` | `orders.payment_confirmed_at` | `OrderService` |
| FS-014 Seed Pipeline | `SeedDataLoader` startup | `books`, `categories` | `SeedDataLoader` |
