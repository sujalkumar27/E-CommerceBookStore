# Architecture & Flow Guide — E-Commerce Bookstore

**Who is this for?**
This guide is written for someone who knows Java basics and some frontend,
but is new to backend development and the spec-driven approach.
Every concept is explained in plain English first, then shown in code terms.

---

## 1. The Big Picture — What Are We Building?

Think of this project as three separate programs that talk to each other:

```
┌──────────────────────────────────────────────────────────────────┐
│  1. FRONTEND  (React)                                            │
│     The website the customer sees in their browser.              │
│     Written in JavaScript/React.                                 │
│     Lives in the /frontend folder.                               │
└───────────────────────┬──────────────────────────────────────────┘
                        │
                        │  Sends HTTP requests (like fetching a webpage)
                        │  e.g. "give me all books" or "log me in"
                        │
┌───────────────────────▼──────────────────────────────────────────┐
│  2. BACKEND  (Spring Boot / Java)                                │
│     The brain of the application.                                │
│     Handles all the business logic:                              │
│       - Is this user allowed to do this?                         │
│       - Calculate the delivery date                              │
│       - Process the payment                                      │
│     Lives in the /backend folder.                                │
└───────────────────────┬──────────────────────────────────────────┘
                        │
                        │  Reads and writes data using SQL
                        │
┌───────────────────────▼──────────────────────────────────────────┐
│  3. DATABASE  (PostgreSQL)                                       │
│     Stores everything permanently:                               │
│       - Users, passwords, orders, books, addresses               │
│     Runs in Docker on your machine.                              │
└──────────────────────────────────────────────────────────────────┘
```

**Analogy:** Think of a restaurant.
- The **Frontend** is the menu and the dining room — what the customer sees.
- The **Backend** is the kitchen — where orders are processed.
- The **Database** is the pantry — where all ingredients (data) are stored.

---

## 2. What is Spec-Driven Development?

Normally, developers just start coding. That leads to confusion —
"What exactly should this button do?" or "Did we need this feature?"

The spec-driven approach forces us to think BEFORE coding:

```
Step 1 — Business Requirements   What does the business NEED?
              ↓
Step 2 — Feature Specifications  What exactly should each feature DO?
              ↓
Step 3 — Implementation Plan     In what ORDER do we build things?
              ↓
Step 4 — Technical Design        HOW exactly will we build it?
              ↓
Step 5 — Code                    NOW we write code (nothing is a surprise)
              ↓
Step 6 — Tests                   Does it do what we said it would?
              ↓
Step 7 — Verify                  Does it match the original requirements?
```

**Why does this matter?**
Every file we write traces back to a requirement. If someone asks
"why did you build this?" — the answer is always in the docs folder.

---

## 3. The Backend — How Spring Boot Works

### 3.1 The Layered Architecture

Spring Boot uses a layered approach. Think of it like a chain of responsibility:

```
HTTP Request from Browser
        │
        ▼
┌───────────────────┐
│   CONTROLLER      │  ← Receives the request, validates the input,
│                   │    calls the service, returns the response.
│  "The Receptionist"│   Does NOT contain business logic.
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│   SERVICE         │  ← Contains ALL the business logic.
│                   │    e.g. "Can this user cancel this order?"
│  "The Manager"    │   Calls the repository to get/save data.
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│   REPOSITORY      │  ← Talks to the database.
│                   │    Just fetches, saves, updates, deletes data.
│  "The Filing Clerk"│   No business logic here.
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│   DATABASE        │  ← Stores data permanently.
│   (PostgreSQL)    │
└───────────────────┘
```

### 3.2 A Real Example — "Get All Books"

Let's trace what happens when the customer opens the bookstore:

```
1. Browser sends:   GET http://localhost:8080/api/books

2. BookController   receives the request
                    reads query parameters (search? category? price?)
                    calls BookService.getBooks(filters)

3. BookService      applies business rules
                    calls BookRepository.findBooks(filters)

4. BookRepository   runs a SQL query against the database
                    returns a list of Book objects

5. BookService      returns the list to the controller

6. BookController   converts the list to JSON
                    sends it back to the browser as a response

7. Browser          React displays the books on screen
```

### 3.3 What is a REST API?

REST is a standard way for the frontend and backend to communicate.
They talk using **HTTP** — the same protocol your browser uses.

| Action | HTTP Method | Example |
|--------|-------------|---------|
| Get data | GET | `GET /api/books` — fetch all books |
| Create something | POST | `POST /api/auth/register` — create a new user |
| Update something | PUT | `PUT /api/addresses/123` — update an address |
| Delete something | DELETE | `DELETE /api/basket/items/456` — remove basket item |

The response is always **JSON** — a simple text format both sides understand:

```json
{
  "id": "abc-123",
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald",
  "price": 299.00
}
```

---

## 4. The Database — How Tables and Relationships Work

### 4.1 Tables are Like Excel Sheets

Each table stores one type of data. Each row is one record. Each column is one field.

**users table:**
| id | email | password_hash | gift_point_balance |
|----|-------|---------------|--------------------|
| uuid-1 | alice@email.com | $2b$12$... | 150 |
| uuid-2 | bob@email.com | $2b$12$... | 0 |

**books table:**
| id | title | author | price | stock |
|----|-------|--------|-------|-------|
| uuid-a | Harry Potter | J.K. Rowling | 499.00 | 20 |
| uuid-b | Clean Code | Robert Martin | 799.00 | 5 |

### 4.2 Relationships Between Tables

Tables link to each other using **foreign keys** (one table stores the ID of a row in another table).

```
users                    orders
──────                   ──────
id ◄─────────────────── user_id   (this order belongs to this user)
email                    total_amount
gift_point_balance       payment_confirmed_at

orders                   order_items
──────                   ───────────
id ◄─────────────────── order_id   (this item belongs to this order)
status                   book_id ──────────► books.id
total_amount             quantity
                         unit_price

books                    categories
──────                   ──────────
id                       id ◄────── books.category_id
title                    name
category_id ────────────►           delivery_offset_days
```

### 4.3 What is JPA / Hibernate?

Writing raw SQL is tedious. JPA (Java Persistence API) lets us work with
**Java objects** instead of SQL:

```java
// Without JPA — raw SQL (tedious):
"SELECT * FROM books WHERE category_id = ?"

// With JPA — plain Java (clean):
bookRepository.findByCategoryId(categoryId)
```

Hibernate is the tool that translates between Java objects and database tables automatically.

### 4.4 All 7 Tables at a Glance

| Table | What it stores |
|-------|----------------|
| `users` | Customer accounts (email, hashed password, gift points) |
| `addresses` | Saved delivery addresses (a user can have many) |
| `categories` | Book categories like Fiction, Non-Fiction (also stores delivery offset days) |
| `books` | The entire book catalogue |
| `basket_items` | Books currently in a user's shopping basket |
| `orders` | Completed purchase records |
| `order_items` | Individual books within each order |

---

## 5. Security — How Login and JWT Work

### 5.1 The Problem with Passwords

We NEVER store a user's actual password. If the database is ever hacked,
attackers would have everyone's passwords.

Instead we store a **hash** — a scrambled version that cannot be reversed:

```
"mypassword123"  →  BCrypt  →  "$2b$12$X9Kd3mN..."
```

When the user logs in, we hash what they type and compare to the stored hash.

### 5.2 What is a JWT Token?

After login, we give the user a **JWT (JSON Web Token)** — think of it like
a wristband at a concert. You show it once at the door (login), then use it
to prove you're allowed in everywhere else.

```
LOGIN REQUEST                          LOGIN RESPONSE
─────────────                          ──────────────
POST /api/auth/login                   {
{                                        "token": "eyJhbGciOiJ...",
  "email": "alice@email.com",            "user": { "id": "...", "email": "..." }
  "password": "mypassword123"          }
}
```

The token is then sent with EVERY protected request:

```
GET /api/basket
Authorization: Bearer eyJhbGciOiJ...
```

The backend reads the token, checks it's valid and not expired, and knows
which user is making the request — without needing to look up a session.

### 5.3 What Happens Without a Token?

```
Guest user tries:  GET /api/basket (no token)
Backend responds:  401 Unauthorized — "Please log in first"

Logged-in user tries:  GET /api/orders/someone-elses-order-id
Backend responds:  403 Forbidden — "This is not your order"
```

### 5.4 The Security Filter Chain

Every request passes through a security filter BEFORE hitting the controller:

```
Every HTTP request
        │
        ▼
┌───────────────────────────────────┐
│  JwtAuthFilter                    │
│  1. Read "Authorization" header   │
│  2. Extract and validate token    │
│  3. If valid: set current user    │
│  4. If invalid: pass through      │
│     (controller will reject it)   │
└───────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────┐
│  SecurityConfig rules             │
│  - /api/auth/**  → allow all      │
│  - GET /api/books/** → allow all  │
│  - everything else → need token   │
└───────────────────────────────────┘
        │
        ▼
   Controller (handles request)
```

---

## 6. Feature-by-Feature Flow

### Feature 1 — User Registration & Login (FS-001)

```
REGISTER FLOW:
──────────────
Browser                 Backend                      Database
  │                        │                             │
  │── POST /api/auth/register ──►                        │
  │   { email, password }  │                             │
  │                        │ 1. Validate email format    │
  │                        │ 2. Check email not taken ──►│
  │                        │                        ◄── │ (not found = good)
  │                        │ 3. Hash password            │
  │                        │ 4. Save new user ──────────►│
  │                        │ 5. Generate JWT token       │
  │◄── 201 { token, user } ─│                             │


LOGIN FLOW:
───────────
Browser                 Backend                      Database
  │                        │                             │
  │── POST /api/auth/login ─►                            │
  │   { email, password }  │                             │
  │                        │ 1. Find user by email ─────►│
  │                        │                        ◄── │ (returns user row)
  │                        │ 2. Compare hashed password  │
  │                        │ 3. Generate JWT token       │
  │◄── 200 { token, user } ─│                             │
```

### Feature 2 — Browse Books (FS-002, FS-004)

```
BROWSE BOOKS FLOW:
──────────────────
Browser                    Backend                    Database
  │                           │                          │
  │── GET /api/books ─────────►                          │
  │   ?search=harry           │                          │
  │   &categoryId=xxx         │ 1. Parse filters         │
  │   &minPrice=100           │ 2. Build query ──────────►│
  │                           │                     ◄── │ (list of books)
  │                           │ 3. Convert to JSON        │
  │◄── 200 { content:[...] } ──│                          │
  │                           │                          │
React renders the book grid   │                          │
```

### Feature 3 — Shopping Basket (FS-005)

```
ADD TO BASKET FLOW:
───────────────────
Browser                    Backend                    Database
  │                           │                          │
  │── POST /api/basket/items ─►                          │
  │   { bookId, quantity }    │                          │
  │   Authorization: Bearer.. │ 1. Validate token        │
  │                           │ 2. Get user from token   │
  │                           │ 3. Check book exists ───►│
  │                           │ 4. Add/update basket ───►│
  │◄── 200 { basket } ─────────│                          │
```

### Feature 4 — Checkout & Payment (FS-009)

```
FULL CHECKOUT FLOW (step by step):
───────────────────────────────────

Step 1: Customer reviews basket
Step 2: Customer selects delivery address
Step 3: Customer selects payment method (Credit/Debit card)
Step 4: Customer optionally applies gift points
Step 5: Customer clicks "Pay Now"

  Browser                   Backend                    Database
    │                          │                          │
    │── POST /api/payment/initiate ──►                    │
    │   { addressId,           │ 1. Validate basket       │
    │     paymentMethod,       │    is not empty          │
    │     giftPointsToRedeem } │ 2. Validate address      │
    │                          │    belongs to this user  │
    │                          │ 3. Validate gift points  │
    │                          │    ≤ user balance        │
    │                          │ 4. SIMULATE payment      │
    │                          │    (90% success)         │
    │                          │ 5. If success:           │
    │                          │    - Create order ──────►│
    │                          │    - Create order items─►│
    │                          │    - Deduct gift points─►│
    │                          │    - Clear basket ──────►│
    │◄── 201 { order details }──│                          │

Step 6: Show Purchase Confirmation screen
```

### Feature 5 — Order Cancellation (FS-013)

```
CANCEL ORDER FLOW:
──────────────────
Browser                    Backend                    Database
  │                           │                          │
  │── POST /api/orders/        │                          │
  │      {id}/cancel ─────────►                          │
  │   Authorization: Bearer.. │ 1. Find order by ID ────►│
  │                           │ 2. Check order belongs   │
  │                           │    to this user          │
  │                           │    (else → 403)          │
  │                           │ 3. Check not already     │
  │                           │    cancelled             │
  │                           │    (else → 409)          │
  │                           │ 4. Check within 48 hrs   │
  │                           │    of payment_confirmed_ │
  │                           │    at (else → 400)       │
  │                           │ 5. Set status=CANCELLED─►│
  │◄── 200 { order } ──────────│                          │
```

### Feature 6 — Recommendations (FS-011)

```
RECOMMENDATION LOGIC:
─────────────────────
When a logged-in user calls GET /api/recommendations:

1. Look at ALL books this user has ever ordered
   → collect their categories  (e.g. Fiction, Non-Fiction)
   → collect their authors     (e.g. J.K. Rowling)

2. Find books that match ANY of:
   Signal A: same CATEGORY as past purchases
   Signal B: same AUTHOR as past purchases
   Signal C: newest books added to catalogue

3. Remove books the user has already ordered

4. Return the top 8 results (blended from all 3 signals)

5. If user has NO order history → just return 8 newest books
```

---

## 7. The Frontend — How React Works With the Backend

### 7.1 React Pages and Routes

Each URL in the browser shows a different React "page" (component):

| URL | What the customer sees |
|-----|------------------------|
| `/` | Book catalogue (browse all books) |
| `/books/abc-123` | Detail page for one specific book |
| `/login` | Login form |
| `/register` | Registration form |
| `/basket` | Shopping basket |
| `/checkout` | Address + payment form |
| `/orders` | Order history list |
| `/orders/xyz-456` | Detail of one order |

### 7.2 How React Talks to the Backend

React uses **Axios** (an HTTP library) to call the backend API:

```javascript
// Example: fetch all books when the catalogue page loads
useEffect(() => {
  axios.get('/api/books?search=harry')
    .then(response => setBooks(response.data.content))
}, [])
```

### 7.3 Auth Context — Staying Logged In

When a user logs in, we save their JWT token in localStorage
(browser storage that survives page refresh):

```
User logs in
    → Backend returns JWT token
    → React saves token to localStorage
    → AuthContext makes token available everywhere in the app
    → Axios automatically adds "Authorization: Bearer <token>"
       to every request

User logs out
    → Token is deleted from localStorage
    → User is redirected to /login
```

### 7.4 Protected Routes

Some pages should only be accessible when logged in.
We wrap them in a `<ProtectedRoute>` component:

```jsx
// If user is NOT logged in, redirect to /login
// If user IS logged in, show the page
<ProtectedRoute>
  <BasketPage />
</ProtectedRoute>
```

---

## 8. The Project Folder Structure — What Lives Where

```
BookStoreApplication/
│
├── docs/                          ← All planning documents
│   ├── business-requirements.md  ← WHAT the business needs
│   ├── feature-specifications.md ← WHAT each feature does
│   ├── implementation-plan.md    ← HOW we build it (order + tech)
│   ├── technical-design.md       ← Database + API design
│   └── architecture-guide.md     ← This file (how it all works)
│
├── backend/                       ← Spring Boot Java application
│   ├── pom.xml                    ← Dependencies (like package.json for Java)
│   ├── Dockerfile                 ← How to build backend as a container
│   └── src/main/java/com/bookstore/
│       ├── BookstoreApplication.java   ← Entry point (start here)
│       ├── config/
│       │   ├── SecurityConfig.java     ← Who can access what
│       │   └── CorsConfig.java         ← Allow frontend to call backend
│       ├── security/
│       │   ├── JwtUtil.java            ← Create and validate tokens
│       │   └── JwtAuthFilter.java      ← Check token on every request
│       ├── controller/                 ← Receive HTTP requests
│       │   ├── AuthController.java     ← /api/auth/*
│       │   ├── BookController.java     ← /api/books/*
│       │   └── ...
│       ├── service/                    ← Business logic
│       │   ├── AuthService.java        ← Register, login logic
│       │   ├── BookService.java        ← Search, filter logic
│       │   └── ...
│       ├── repository/                 ← Talk to database
│       │   ├── UserRepository.java
│       │   ├── BookRepository.java
│       │   └── ...
│       ├── model/                      ← Java classes = database tables
│       │   ├── User.java               ← Matches "users" table
│       │   ├── Book.java               ← Matches "books" table
│       │   └── ...
│       ├── dto/                        ← What we send/receive over API
│       │   ├── auth/                   ← Login/Register request & response
│       │   ├── book/                   ← Book list & detail response
│       │   └── ...
│       ├── exception/                  ← Handle errors gracefully
│       │   └── GlobalExceptionHandler.java
│       └── loader/
│           └── SeedDataLoader.java     ← Load books from JSON on startup
│
├── frontend/                      ← React application
│   └── src/
│       ├── api/                   ← All Axios calls to backend
│       ├── components/            ← Reusable UI pieces (Button, Card...)
│       ├── pages/                 ← Full pages (CataloguePage, LoginPage...)
│       ├── context/               ← Shared state (auth token, basket count)
│       └── hooks/                 ← Reusable logic (useBooks, useAuth...)
│
├── seed/
│   └── seed.py                    ← Python script: fetch books from Open Library
│
├── docker-compose.yml             ← Run everything with one command
├── .env.example                   ← Template for your secret variables
└── .gitignore                     ← Files Git should never commit
```

---

## 9. The Build Order — Why We Build in This Sequence

We build in phases because each phase depends on the previous one:

```
Phase 1 — Scaffold
  Set up the project structure, Docker, database connection
  WHY FIRST: Everything else needs this foundation

Phase 2 — Authentication
  Register, login, JWT tokens, protected routes
  WHY SECOND: Almost every other feature needs to know "who is this user?"

Phase 3 — Catalogue
  Books, categories, search, filter, product detail
  WHY THIRD: You can't add to basket if there are no books to show

Phase 4 — Basket
  Add/remove/update items, recommendations
  WHY FOURTH: You need books (Phase 3) before you can add them to a basket

Phase 5 — Checkout & Orders
  Payment, order history, cancellation, addresses
  WHY FIFTH: You need a basket (Phase 4) before you can check out

Phase 6 — Polish & Tests
  Tests, error handling, security review
  WHY LAST: You test what you've built
```

---

## 10. Glossary — Terms You Will See in the Code

| Term | Plain English meaning |
|------|-----------------------|
| **Entity** | A Java class that maps directly to a database table |
| **Repository** | A Java interface that handles database queries |
| **Service** | A Java class containing business logic |
| **Controller** | A Java class that handles HTTP requests/responses |
| **DTO** | Data Transfer Object — what we send over the API (not the full entity) |
| **JWT** | JSON Web Token — a signed login token |
| **BCrypt** | A password hashing algorithm (one-way scrambling) |
| **REST** | A standard style for building web APIs |
| **JSON** | Text format for sending data between frontend and backend |
| **Spring Boot** | A Java framework that makes building backend APIs much easier |
| **JPA / Hibernate** | Tools that translate between Java objects and database tables |
| **Docker** | Runs the app in isolated containers so it works the same on any computer |
| **Endpoint** | One specific URL + HTTP method the backend responds to |
| **Foreign Key** | A column in one table that stores the ID of a row in another table |
| **Migration** | A script that changes the database structure in a controlled way |
| **Seed data** | Initial data loaded into the database (our book catalogue) |
| **Context (React)** | Shared state accessible from any component (like a global variable) |
| **Hook (React)** | A reusable piece of logic in a React function component |
| **Protected Route** | A page that redirects to login if the user is not authenticated |
