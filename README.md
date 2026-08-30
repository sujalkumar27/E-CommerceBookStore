# 📚 BookStore — E-Commerce Bookstore

A full-stack e-commerce application for browsing, searching, and purchasing books.

**Backend:** Spring Boot 3.3.4 · Java 17 · PostgreSQL  
**Frontend:** React 18 · Vite 6 · Tailwind CSS · Axios  
**Tests:** 49 backend unit tests · 37 backend integration tests · 41 frontend tests

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Running the Backend](#running-the-backend)
4. [Running the Frontend](#running-the-frontend)
5. [Seed Data](#seed-data)
6. [Running Tests](#running-tests)
7. [API Reference](#api-reference)
8. [Project Structure](#project-structure)

---

## Architecture Overview

```
Browser (React SPA)
    │  HTTP/JSON over Vite dev proxy  (dev)
    │  HTTPS  (production)
    ▼
Spring Boot REST API  (port 8080)
    │  JDBC / JPA (Hibernate 6)
    ▼
PostgreSQL 14+  (port 5432)
```

- **Auth:** JWT (JJWT 0.12.6) — tokens are stateless, validated on every request by `JwtAuthFilter`
- **CORS:** Configured in `CorsConfig.java` to allow `http://localhost:5173`
- **Security:** BCrypt password hashing, SecureRandom for payment simulation, no hardcoded secrets

---

## Prerequisites

| Tool | Version | Download |
|---|---|---|
| Java JDK | 17 | [Adoptium](https://adoptium.net/) |
| Maven | 3.9+ | Bundled with IntelliJ or [download](https://maven.apache.org/) |
| PostgreSQL | 14+ | [postgresql.org](https://www.postgresql.org/) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) |

---

## Running the Backend

### 1. Set up the database

Connect to PostgreSQL and run:

```sql
CREATE USER bookstore_user WITH PASSWORD 'bookstore_pass';
CREATE DATABASE bookstore OWNER bookstore_user;
GRANT ALL PRIVILEGES ON DATABASE bookstore TO bookstore_user;
```

### 2. Configure secrets

Copy `.env.example` to `.env` in the project root and fill in values:

```bash
cp .env.example .env
```

```env
DB_URL=jdbc:postgresql://localhost:5432/bookstore
DB_USERNAME=bookstore_user
DB_PASSWORD=bookstore_pass
JWT_SECRET=<your-256-bit-base64-encoded-secret>
```

> **Generate a JWT secret:**  
> `node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"`

### 3. Start the backend

```powershell
.\start-backend.bat
```

Or manually:

```powershell
cd backend
mvn spring-boot:run
```

The API will start at **http://localhost:8080**.  
On first run, `SeedDataLoader` automatically imports 113 books across 8 categories from `src/main/resources/data/seed.json`.

---

## Running the Frontend

```powershell
cd frontend
npm install
npm run dev
```

The app opens at **http://localhost:5173**.  
All `/api/*` requests are proxied to `http://localhost:8080` — no CORS issues in dev.

---

## Seed Data

The backend seeds 113 books and 8 categories automatically on first startup.

To re-fetch fresh book data from the Open Library API:

```bash
cd seed
pip install requests
python fetch_books.py
```

This regenerates `backend/src/main/resources/data/seed.json`.

---

## Running Tests

### Backend tests

```powershell
cd backend
mvn test
```

**49 unit tests + 37 integration tests = 86 tests total.**  
Integration tests use an H2 in-memory database (configured in `application-test.yml`) — no PostgreSQL needed.

### Frontend tests

```powershell
cd frontend
npm run test
```

**41 tests** covering: `AuthContext`, `CartContext`, `LoginPage`, `BookCard`, `CartItemRow`, `Pagination`, `LoadingSpinner`, `ErrorMessage`, `GiftPointsInput`.

---

## API Reference

All endpoints are prefixed with `/api`.

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register new user → returns JWT |
| POST | `/api/auth/login` | None | Login → returns JWT |

### Books

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/books` | None | List books with filters (search, categoryId, minPrice, maxPrice, available, page, size) |
| GET | `/api/books/:id` | None | Get book detail + related books |
| GET | `/api/categories` | None | List all categories |

### Cart

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/cart` | ✅ | Get current cart |
| POST | `/api/cart/items` | ✅ | Add item `{ bookId, quantity }` |
| PUT | `/api/cart/items/:id` | ✅ | Update quantity `{ quantity }` |
| DELETE | `/api/cart/items/:id` | ✅ | Remove item |

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/orders` | ✅ | List user's orders |
| GET | `/api/orders/:id` | ✅ | Get order detail |
| POST | `/api/orders/:id/cancel` | ✅ | Cancel order (within 48 h) |
| POST | `/api/orders/:id/buy-again` | ✅ | Re-add order items to cart |

### Payment

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/payment/initiate` | ✅ | Initiate payment `{ deliveryAddressId, paymentMethod, giftPointsToRedeem }` |

### Addresses

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/addresses` | ✅ | List saved addresses |
| POST | `/api/addresses` | ✅ | Add address |
| PUT | `/api/addresses/:id` | ✅ | Update address |
| DELETE | `/api/addresses/:id` | ✅ | Delete address |

### Recommendations

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/recommendations` | ✅ | Get personalised book recommendations |

---

## Project Structure

```
BookStoreApplication/
├── AGENTS.md                          ← AI-assisted dev lifecycle rules
├── .env.example                       ← Secret template (copy to .env)
├── .gitignore
├── start-backend.bat                  ← One-click backend start
├── postman/                           ← Postman collection for manual API testing
├── docs/
│   ├── business-requirements.md
│   ├── feature-specifications.md
│   ├── implementation-plan.md
│   ├── technical-design.md
│   ├── architecture-guide.md
│   └── frontend-guide.md
├── seed/
│   └── fetch_books.py                 ← Re-fetch book data from Open Library
├── frontend/                          ← React + Vite SPA
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   └── src/
│       ├── api/                       ← Axios API modules
│       ├── context/                   ← AuthContext, CartContext
│       ├── components/                ← Reusable UI components
│       ├── pages/                     ← One component per route
│       └── test/                      ← Vitest + React Testing Library tests
└── backend/                           ← Spring Boot application
    ├── pom.xml
    └── src/
        ├── main/java/com/bookstore/
        │   ├── config/                ← SecurityConfig, CorsConfig
        │   ├── security/              ← JwtUtil, JwtAuthFilter
        │   ├── model/                 ← JPA entities
        │   ├── repository/            ← Spring Data JPA repositories
        │   ├── service/               ← Business logic
        │   ├── controller/            ← REST endpoints
        │   ├── dto/                   ← Request/response DTOs
        │   ├── exception/             ← Global exception handler
        │   └── loader/                ← SeedDataLoader
        └── test/java/com/bookstore/
            ├── model/                 ← OrderModelTest
            ├── security/              ← JwtUtilTest
            ├── service/               ← AuthService, BookService, CartService, OrderService, RecommendationService
            └── controller/            ← AuthController, BookController, CartController, OrderController
```
