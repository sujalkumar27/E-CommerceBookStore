# Frontend Architecture Guide — E-Commerce Bookstore

**Document:** Frontend Architecture & Developer Guide  
**Project:** AI-Assisted E-Commerce Bookstore  
**Stack:** React 18 · Vite · Tailwind CSS · Axios · React Router v6  
**Status:** Approved for implementation (Phase 6C)  
**Derives from:** `docs/technical-design.md §5`, `docs/feature-specifications.md`

---

## 1. Why This Document Exists

Before writing a single line of React, a developer must understand:

- What pages (routes) exist and which ones need login
- What components are shared vs. page-specific
- How the frontend talks to the backend (API calls)
- Where state (logged-in user, cart count) is stored
- How authentication tokens are stored and sent
- What happens when an API call fails

This document answers all of the above.

---

## 2. Tech Stack — What We Use and Why

| Tool | Version | Why |
|---|---|---|
| **React** | 18 | Component-based UI, the most widely-used frontend library |
| **Vite** | 5.x | Extremely fast build tool (replaces Create React App) |
| **React Router** | v6 | Client-side routing — no full-page reloads when navigating |
| **Axios** | 1.x | HTTP client — cleaner than `fetch`, supports interceptors |
| **Tailwind CSS** | 3.x | Utility-first CSS — fast to style, no separate CSS files needed |
| **Context API** | built-in | Lightweight global state for auth token and cart count |

> **No Redux.** The app is small enough that React Context handles all global state cleanly.

---

## 3. Folder Structure

```
frontend/
├── index.html                  ← Vite entry HTML (single page)
├── vite.config.js              ← Vite config (proxy, aliases)
├── tailwind.config.js          ← Tailwind CSS config
├── package.json
└── src/
    ├── main.jsx                ← App bootstrap (ReactDOM.render)
    ├── App.jsx                 ← Root router — defines all routes
    │
    ├── api/                    ← All backend API calls (Axios)
    │   ├── axiosClient.js      ← Configured Axios instance (base URL, auth header)
    │   ├── authApi.js          ← register(), login(), logout()
    │   ├── bookApi.js          ← getBooks(), getBookById(), getCategories()
    │   ├── cartApi.js          ← getCart(), addItem(), updateItem(), removeItem()
    │   ├── orderApi.js         ← getOrders(), getOrderById(), cancelOrder(), buyAgain()
    │   ├── addressApi.js       ← getAddresses(), addAddress(), updateAddress(), deleteAddress()
    │   ├── paymentApi.js       ← initiatePayment()
    │   └── recommendationApi.js← getRecommendations()
    │
    ├── context/                ← Global state shared across all components
    │   ├── AuthContext.jsx     ← token, user, login(), logout(), isLoggedIn
    │   └── CartContext.jsx     ← cartCount — the number shown in the header badge
    │
    ├── components/             ← Reusable UI building blocks (not full pages)
    │   ├── layout/
    │   │   ├── Navbar.jsx      ← Top nav: logo, search bar, cart badge, login/logout
    │   │   └── Footer.jsx      ← Simple footer
    │   ├── catalogue/
    │   │   ├── BookCard.jsx    ← One book card in the catalogue grid
    │   │   ├── BookGrid.jsx    ← Grid of BookCard components
    │   │   ├── SearchBar.jsx   ← Search input with submit handler
    │   │   ├── FilterPanel.jsx ← Category, publisher, price, availability filters
    │   │   └── Pagination.jsx  ← Page navigation controls
    │   ├── cart/
    │   │   ├── CartItem.jsx    ← One row in the cart (book info + qty controls)
    │   │   └── CartSummary.jsx ← Cart total + "Proceed to Checkout" button
    │   ├── checkout/
    │   │   ├── AddressSelector.jsx   ← List of saved addresses + select/add
    │   │   ├── PaymentForm.jsx       ← Card type selector + simulated card number
    │   │   └── GiftPointsInput.jsx   ← Points balance display + redeem input
    │   ├── order/
    │   │   ├── OrderCard.jsx         ← Summary row in order history list
    │   │   └── OrderItemRow.jsx      ← One book row inside an order
    │   ├── common/
    │   │   ├── ProtectedRoute.jsx    ← Redirects guest users to /login
    │   │   ├── LoadingSpinner.jsx    ← Shown while API calls are in-flight
    │   │   ├── ErrorMessage.jsx      ← Displays error banners from API responses
    │   │   └── EmptyState.jsx        ← "No results found" / "Cart is empty" etc.
    │   └── recommendations/
    │       └── RecommendationPanel.jsx ← Horizontal strip of recommended books
    │
    └── pages/                  ← Full page components (one per route)
        ├── CataloguePage.jsx         ← / (home)
        ├── ProductDetailPage.jsx     ← /books/:id
        ├── LoginPage.jsx             ← /login
        ├── RegisterPage.jsx          ← /register
        ├── CartPage.jsx              ← /cart (auth required)
        ├── CheckoutPage.jsx          ← /checkout (auth required, multi-step)
        ├── PurchaseConfirmationPage.jsx ← /checkout/confirmation (auth required)
        ├── OrderHistoryPage.jsx      ← /orders (auth required)
        ├── OrderDetailPage.jsx       ← /orders/:id (auth required)
        └── AddressManagementPage.jsx ← /account/addresses (auth required)
```

---

## 4. Routing — Every Page and Who Can See It

Defined in [`App.jsx`](../frontend/src/App.jsx). Uses **React Router v6**.

```jsx
// App.jsx (simplified)
<BrowserRouter>
  <Navbar />
  <Routes>
    {/* Public routes — guests can access */}
    <Route path="/"           element={<CataloguePage />} />
    <Route path="/books/:id"  element={<ProductDetailPage />} />
    <Route path="/login"      element={<LoginPage />} />
    <Route path="/register"   element={<RegisterPage />} />

    {/* Protected routes — require login */}
    <Route path="/cart"       element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
    <Route path="/checkout"   element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
    <Route path="/checkout/confirmation" element={<ProtectedRoute><PurchaseConfirmationPage /></ProtectedRoute>} />
    <Route path="/orders"     element={<ProtectedRoute><OrderHistoryPage /></ProtectedRoute>} />
    <Route path="/orders/:id" element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>} />
    <Route path="/account/addresses" element={<ProtectedRoute><AddressManagementPage /></ProtectedRoute>} />
  </Routes>
  <Footer />
</BrowserRouter>
```

### How `ProtectedRoute` Works

```jsx
// components/common/ProtectedRoute.jsx
function ProtectedRoute({ children }) {
  const { isLoggedIn } = useAuth();  // reads from AuthContext
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;  // redirect guest → login
  }
  return children;
}
```

**Key behaviour (D-001):**
- Guest can browse `/` and `/books/:id` without logging in
- Any attempt to visit `/cart`, `/checkout`, `/orders` etc. → redirected to `/login`
- After successful login, user is returned to the page they were trying to visit

---

## 5. Authentication — How Login Works End to End

This is the most important thing to understand before building the frontend.

### 5.1 The Flow

```
User fills in login form
        ↓
POST /api/auth/login  { email, password }
        ↓
Backend returns:  { token: "eyJ...", user: { id, email, giftPointBalance } }
        ↓
Frontend stores token in localStorage  ← persists across browser refresh
Frontend stores user in AuthContext    ← in-memory, available to all components
        ↓
Every subsequent API request:
  axios.defaults.headers['Authorization'] = `Bearer ${token}`
        ↓
Backend's JwtAuthFilter validates the token on every request
Backend identifies the user from the token
        ↓
Logout: delete token from localStorage, clear AuthContext
```

### 5.2 AuthContext

```jsx
// context/AuthContext.jsx
const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user,  setUser]  = useState(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = (token, user) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    setToken(token);
    setUser(user);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ token, user, login, logout, isLoggedIn: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
```

### 5.3 Why localStorage?

The JWT token must survive a browser refresh. `useState` alone loses its value when the page reloads. `localStorage` is persistent storage in the browser.

**Security note:** For this capstone, localStorage is acceptable. In production, `httpOnly` cookies are more secure (JavaScript cannot access them), but they require extra backend configuration.

---

## 6. The API Client — How We Talk to the Backend

All backend calls go through a single configured Axios instance.

### 6.1 `axiosClient.js`

```js
// api/axiosClient.js
import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach the JWT token to every request automatically
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: if the server returns 401, the token expired → logout
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';  // force redirect
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
```

**Why interceptors?**
Instead of manually adding `Authorization: Bearer <token>` to every API call and manually handling 401s, we do it once in the interceptor and every function in every API module gets it for free.

### 6.2 Example API Module

```js
// api/bookApi.js
import axiosClient from './axiosClient';

export const getBooks = (params) =>
  axiosClient.get('/api/books', { params });
  // params: { search, categoryId, publisher, minPrice, maxPrice, available, page, size }

export const getBookById = (id) =>
  axiosClient.get(`/api/books/${id}`);

export const getCategories = () =>
  axiosClient.get('/api/categories');
```

```js
// api/cartApi.js
import axiosClient from './axiosClient';

export const getCart    = ()               => axiosClient.get('/api/cart');
export const addItem    = (bookId, qty)    => axiosClient.post('/api/cart/items', { bookId, quantity: qty });
export const updateItem = (itemId, qty)    => axiosClient.put(`/api/cart/items/${itemId}`, { quantity: qty });
export const removeItem = (itemId)         => axiosClient.delete(`/api/cart/items/${itemId}`);
```

---

## 7. State Management — What Lives Where

There are only **two pieces of global state** in this app:

| State | Context | What it contains |
|---|---|---|
| **Auth** | `AuthContext` | `token` (string), `user` (email, id, giftPointBalance), `login()`, `logout()`, `isLoggedIn` (bool) |
| **Cart count** | `CartContext` | `cartCount` (integer) — the badge number shown on the cart icon in the Navbar |

Everything else (list of books, current order, addresses etc.) is **local component state** loaded via `useEffect`. This is the simplest correct approach for this app size.

### Why CartContext?

The cart item count shown in the `Navbar` badge must update whenever the user adds/removes items — even if the Navbar is not a child of the CartPage. A context (shared state accessible anywhere) is the right tool.

```jsx
// context/CartContext.jsx
export function CartProvider({ children }) {
  const [cartCount, setCartCount] = useState(0);

  const refreshCartCount = async () => {
    try {
      const res = await getCart();
      setCartCount(res.data.items.length);
    } catch {
      setCartCount(0);
    }
  };

  return (
    <CartContext.Provider value={{ cartCount, refreshCartCount }}>
      {children}
    </CartContext.Provider>
  );
}
```

---

## 8. Every Page — What It Does and Which APIs It Calls

### 8.1 `CataloguePage` `/`

**Purpose:** Browse and search all books. Public (guests allowed).

| Action | API Call |
|---|---|
| Page load | `GET /api/books?page=0&size=20` |
| Load categories for sidebar | `GET /api/categories` |
| User types in search | `GET /api/books?search=...` |
| User clicks category filter | `GET /api/books?categoryId=...` |
| User sets price range | `GET /api/books?minPrice=...&maxPrice=...` |
| User ticks "In stock only" | `GET /api/books?available=true` |
| User clicks a book card | Navigate to `/books/:id` |
| Logged-in user clicks "Add to cart" | `POST /api/cart/items` |
| Guest clicks "Add to cart" | Redirect to `/login` |

### 8.2 `ProductDetailPage` `/books/:id`

**Purpose:** Full details of one book. Public.

| Action | API Call |
|---|---|
| Page load | `GET /api/books/:id` |
| Add to cart button | `POST /api/cart/items` (auth required) |
| Click related book | Navigate to `/books/:relatedId` |

**Delivery date display:**  
`tentativeDeliveryDays` is returned by the API. Calculate:  
`new Date(Date.now() + tentativeDeliveryDays * 86400000).toLocaleDateString()`

### 8.3 `LoginPage` `/login`

| Action | API Call |
|---|---|
| Submit form | `POST /api/auth/login` |
| On success | Call `login(token, user)` from AuthContext, navigate to previous page or `/` |
| On 401 | Show "Invalid email or password." (generic — never say which field is wrong) |

### 8.4 `RegisterPage` `/register`

| Action | API Call |
|---|---|
| Submit form | `POST /api/auth/register` |
| On success (201) | Call `login(token, user)`, navigate to `/` |
| On 409 | Show "An account with this email already exists." |
| On 400 field errors | Show field-level validation messages |

### 8.5 `CartPage` `/cart` *(auth required)*

**Purpose:** Review cart, adjust quantities, proceed to checkout.

| Action | API Call |
|---|---|
| Page load | `GET /api/cart` |
| Increase/decrease qty | `PUT /api/cart/items/:itemId` |
| Remove item | `DELETE /api/cart/items/:itemId` |
| Page load (recommendations) | `GET /api/recommendations` |
| "Proceed to Checkout" | Navigate to `/checkout` |

### 8.6 `CheckoutPage` `/checkout` *(auth required)*

Multi-step form (no separate pages — handled with step state inside one component).

**Step 1 — Select Address**

| Action | API Call |
|---|---|
| Load saved addresses | `GET /api/addresses` |
| Add new address | `POST /api/addresses` |
| Select address | Local state only |

**Step 2 — Payment Method**

- Dropdown: Credit Card / Debit Card
- Simulated card number input (any value, not validated against a real gateway)
- Local state only — no API call until step 3

**Step 3 — Gift Points + Order Summary**

| Action | API Call |
|---|---|
| Show gift point balance | Read `user.giftPointBalance` from AuthContext |
| Confirm and Pay | `POST /api/payment/initiate` |
| On success | Navigate to `/checkout/confirmation` passing response data |
| On 402 (payment failed) | Show "Payment failed. Please try again." with retry option |

### 8.7 `PurchaseConfirmationPage` `/checkout/confirmation` *(auth required)*

**Purpose:** Show the order summary after successful payment.

- Displays: Order ID, items, quantities, delivery dates, total, delivery address, cancellation deadline
- Data comes from the `PaymentResponse` passed via router state (no extra API call needed)
- "Continue Shopping" → navigate to `/`
- "View Order History" → navigate to `/orders`

### 8.8 `OrderHistoryPage` `/orders` *(auth required)*

| Action | API Call |
|---|---|
| Page load | `GET /api/orders` |
| Click order | Navigate to `/orders/:id` |
| "Buy Again" button | `POST /api/orders/:id/buy-again` → refresh cart count |
| "Cancel" button (within 48 hrs) | `POST /api/orders/:id/cancel` |

### 8.9 `OrderDetailPage` `/orders/:id` *(auth required)*

| Action | API Call |
|---|---|
| Page load | `GET /api/orders/:id` |
| Cancel order | `POST /api/orders/:id/cancel` |
| Buy Again | `POST /api/orders/:id/buy-again` |

### 8.10 `AddressManagementPage` `/account/addresses` *(auth required)*

| Action | API Call |
|---|---|
| Load addresses | `GET /api/addresses` |
| Add address | `POST /api/addresses` |
| Edit address | `PUT /api/addresses/:id` |
| Delete address | `DELETE /api/addresses/:id` |

---

## 9. The Checkout Flow — Step by Step

This is the most complex user journey. Here is the exact sequence:

```
User on CartPage
  ↓ clicks "Proceed to Checkout"
  ↓
CheckoutPage — Step 1: Address
  User picks a saved address  OR  fills in a new one
  ↓ clicks "Continue"
  ↓
CheckoutPage — Step 2: Payment
  User selects "Credit Card" or "Debit Card"
  User enters any card number (simulated — not validated)
  ↓ clicks "Continue"
  ↓
CheckoutPage — Step 3: Review + Gift Points
  System shows: cart items, total, chosen address
  User optionally enters gift points to redeem (capped at their balance AND order total)
  User sees: amount to pay = total - gift points discount
  ↓ clicks "Confirm & Pay"
  ↓
POST /api/payment/initiate {
  deliveryAddressId: "uuid",
  paymentMethod: "CREDIT_CARD",
  giftPointsToRedeem: 50
}
  ↓
  IF 90% chance: 200 OK — payment success
    → navigate to /checkout/confirmation (pass response as router state)
  IF 10% chance: 402 — payment failure
    → show error toast "Payment failed. Please try again."
    → stay on step 3 so user can retry
```

---

## 10. API Response Shapes — What the Backend Returns

These are the exact shapes you will use to build the UI. Do not guess — use these.

### Cart Response (`GET /api/cart`, `POST /api/cart/items`, etc.)
```json
{
  "items": [
    {
      "id": "uuid",
      "book": {
        "id": "uuid",
        "title": "Clean Code",
        "author": "Robert Martin",
        "price": 799.00,
        "available": true,
        "coverImageUrl": "https://..."
      },
      "quantity": 2,
      "lineTotal": 1598.00
    }
  ],
  "cartTotal": 1598.00
}
```

### Book Summary (in catalogue list)
```json
{
  "id": "uuid",
  "title": "Clean Code",
  "author": "Robert Martin",
  "publisher": "Prentice Hall",
  "category": { "id": "uuid", "name": "Technology" },
  "price": 799.00,
  "available": true,
  "coverImageUrl": "https://...",
  "publishedYear": 2008
}
```

### Book Detail (`GET /api/books/:id`)
```json
{
  "id": "uuid",
  "title": "Clean Code",
  "author": "Robert Martin",
  "isbn": "978-0132350884",
  "description": "A handbook of agile software craftsmanship...",
  "price": 799.00,
  "available": true,
  "coverImageUrl": "https://...",
  "publishedYear": 2008,
  "publisher": "Prentice Hall",
  "category": { "id": "uuid", "name": "Technology", "deliveryOffsetDays": 5 },
  "tentativeDeliveryDays": 5,
  "relatedBooks": [ ...up to 6 BookSummary objects... ]
}
```

### Payment Response (`POST /api/payment/initiate`)
```json
{
  "orderId": "uuid",
  "status": "CONFIRMED",
  "paymentConfirmedAt": "2026-08-30T10:00:00Z",
  "cancellationDeadline": "2026-09-01T10:00:00Z",
  "orderTotal": 1598.00,
  "giftPointsRedeemed": 50,
  "amountCharged": 1548.00,
  "remainingGiftPointBalance": 100,
  "items": [
    {
      "bookId": "uuid",
      "title": "Clean Code",
      "author": "Robert Martin",
      "quantity": 2,
      "lineTotal": 1598.00,
      "tentativeDeliveryDate": "2026-09-04"
    }
  ],
  "deliveryAddress": {
    "id": "uuid",
    "fullName": "Alice Smith",
    "line1": "123 Main St",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001"
  }
}
```

### Error Response (all error cases)
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with ID: abc-123",
  "timestamp": "2026-08-30T10:00:00Z",
  "path": "/api/books/abc-123",
  "fieldErrors": null
}
```

---

## 11. Error Handling — What to Show the User

Every API call can fail. Here is the standard pattern for handling errors in a component:

```jsx
const [loading, setLoading] = useState(false);
const [error,   setError]   = useState(null);
const [data,    setData]    = useState(null);

useEffect(() => {
  setLoading(true);
  getBooks()
    .then(res  => setData(res.data))
    .catch(err => setError(err.response?.data?.message || 'Something went wrong.'))
    .finally(() => setLoading(false));
}, []);

if (loading) return <LoadingSpinner />;
if (error)   return <ErrorMessage message={error} />;
```

**Error messages to show:**

| HTTP Status | User-facing message |
|---|---|
| 400 | Show `fieldErrors` inline on the form field |
| 401 | "Please log in to continue." (axiosClient interceptor handles this) |
| 403 | "You don't have permission to do that." |
| 404 | "Not found." |
| 402 | "Payment failed. Please try again." |
| 409 | Show the `message` from the error body (e.g. "already cancelled") |
| 500 | "Something went wrong. Please try again." |

---

## 12. Environment Variables

Create a `.env` file in the `frontend/` directory (never commit it):

```
VITE_API_BASE_URL=http://localhost:8080
```

Access in code:
```js
import.meta.env.VITE_API_BASE_URL
```

> **Why `VITE_` prefix?** Vite only exposes env variables that start with `VITE_` to the browser. Others are kept server-side only (security feature).

---

## 13. Vite Proxy (Dev Only)

To avoid CORS issues during local development, configure Vite to proxy `/api` calls to the backend. This means the browser thinks all API calls are going to `localhost:5173` (same origin), so CORS is never triggered.

```js
// vite.config.js
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

With this, `axiosClient.baseURL` can simply be `/` (empty) in dev — the proxy handles the routing to port 8080.

---

## 14. Important API URL Note — `/api/cart` (not `/api/basket`)

The `docs/technical-design.md` still refers to `/api/basket`. **This was renamed to `/api/cart` during implementation.** Always use:

| ✅ Correct | ❌ Old name (do not use) |
|---|---|
| `GET /api/cart` | `GET /api/basket` |
| `POST /api/cart/items` | `POST /api/basket/items` |
| `PUT /api/cart/items/:id` | `PUT /api/basket/items/:id` |
| `DELETE /api/cart/items/:id` | `DELETE /api/basket/items/:id` |

---

## 15. Running the Full Stack Locally

```bash
# Terminal 1 — Start backend
.\start-backend.bat       # OR: cd backend && mvn spring-boot:run

# Terminal 2 — Start frontend (once frontend is created)
cd frontend
npm install
npm run dev               # Starts at http://localhost:5173
```

Backend runs at `http://localhost:8080`  
Frontend runs at `http://localhost:5173`

---

## 16. Summary — What to Build in Order

| Order | What | Pages / Files |
|---|---|---|
| 1 | Scaffold Vite + React project | `vite.config.js`, `package.json`, `tailwind.config.js` |
| 2 | API client | `api/axiosClient.js`, all API modules |
| 3 | Auth context + Cart context | `context/AuthContext.jsx`, `context/CartContext.jsx` |
| 4 | App router + ProtectedRoute | `App.jsx`, `components/common/ProtectedRoute.jsx` |
| 5 | Shared components | `Navbar`, `Footer`, `LoadingSpinner`, `ErrorMessage` |
| 6 | Catalogue + Search | `CataloguePage`, `BookCard`, `FilterPanel`, `SearchBar` |
| 7 | Product detail | `ProductDetailPage` |
| 8 | Login + Register | `LoginPage`, `RegisterPage` |
| 9 | Cart | `CartPage`, `CartItem`, `CartSummary` |
| 10 | Checkout | `CheckoutPage` (multi-step), `AddressSelector`, `PaymentForm`, `GiftPointsInput` |
| 11 | Confirmation | `PurchaseConfirmationPage` |
| 12 | Orders | `OrderHistoryPage`, `OrderDetailPage` |
| 13 | Addresses | `AddressManagementPage` |
| 14 | Recommendations | `RecommendationPanel` (used in CartPage and post-login) |
