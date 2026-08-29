# Feature Specifications — E-Commerce Bookstore

**Document:** Feature Specifications  
**Project:** AI-Assisted E-Commerce Bookstore  
**Status:** Draft — Awaiting Developer Review  
**Derives from:** `docs/business-requirements.md`  
**Lifecycle stage:** Stage 1 — Specification  

---

## How to Read This Document

Each specification entry maps directly to one or more confirmed business
requirements (BRs) or resolved decisions (D-xxx) from the BRD.

Classification key (inherited from BRD §21):

| Tag | Meaning |
|---|---|
| ✅ Confirmed | Explicitly required by BRD |
| 🔵 Proposed | Necessary to make the feature work; not explicitly stated |
| ❓ Open | Still needs a business decision |

---

## FS-001 — User Registration and Login

**Traces to:** BR-001, §6.1, D-001

### Description
The application supports two user types: Guest and Registered. Authentication
is required before a customer can add items to the basket or proceed to checkout.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A new user can register with an email address and password | ✅ Confirmed |
| 2 | A registered user can log in with their email and password | ✅ Confirmed |
| 3 | A logged-in user can log out | 🔵 Proposed |
| 4 | A guest user can browse the catalogue without logging in | ✅ Confirmed (D-001) |
| 5 | A guest user cannot add items to the basket — a login prompt is shown | ✅ Confirmed (D-001) |
| 6 | A guest user cannot begin checkout — a login prompt is shown | ✅ Confirmed (D-001) |
| 7 | An invalid login attempt shows a generic error message (no credential details exposed) | 🔵 Proposed |
| 8 | Passwords are stored securely (hashed, never plaintext) | 🔵 Proposed |
| 9 | A session/token is issued on successful login and invalidated on logout | 🔵 Proposed |

### Out of Scope
- Social login (Google, Facebook, etc.)
- Password reset / forgot-password flow — ❓ Open (not in BRD; must be approved to add)
- Multi-factor authentication for standard customers

---

## FS-002 — Product Catalogue Browsing

**Traces to:** BR-002, BR-003, §7.1–7.6, D-001

### Description
The catalogue is the primary product-discovery experience. It is accessible to
both Guest and Registered users.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | The catalogue displays available books | ✅ Confirmed |
| 2 | Books are organised into categories; the customer can select a category to view its books | ✅ Confirmed (BR-002) |
| 3 | The customer can browse books by brand (publisher) | ✅ Confirmed (BR-003) |
| 4 | Each catalogue entry shows enough information for product selection (title, author, price, availability) | ✅ Confirmed (§8) |
| 5 | A book that is unavailable is indicated as such in the catalogue | ✅ Confirmed (§7.5) |
| 6 | Guest users can browse the catalogue | ✅ Confirmed (D-001) |
| 7 | The catalogue is populated from the offline Open Library seed script | ✅ Confirmed (A-001, A-003) |

### Data Fields (minimum per BRD §8)
- Title
- Author
- Category
- Brand / Publisher
- Price
- Availability status
- Cover image (🔵 Proposed — necessary for usable catalogue display)
- Tentative delivery date offset (derived from category — see FS-008)

### Out of Scope
- In-application catalogue management (admin CRUD) — explicitly excluded (§3.2)

---

## FS-003 — Product Detail Page

**Traces to:** §8, §7.4, BR-004

### Description
When a customer selects a product from the catalogue, a product detail page
is displayed with full product information and related products.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | Selecting a product opens a detail view | ✅ Confirmed |
| 2 | The detail view shows full product information sufficient for a purchase decision | ✅ Confirmed (§8) |
| 3 | The detail view shows a tentative delivery date | ✅ Confirmed (§7.5, D-005) |
| 4 | A list of related products is displayed (same category) | ✅ Confirmed (BR-004, D-006) |
| 5 | The customer can navigate to a related product from the detail view | ✅ Confirmed |
| 6 | A registered user can add the product to the basket from the detail view | ✅ Confirmed |
| 7 | A guest user sees a login prompt when attempting to add to basket | ✅ Confirmed (D-001) |

### Related Products Rule (D-006)
Related products are books in the **same category** as the currently viewed book.
Up to N related products are shown (N = ❓ Open — number not defined in BRD; suggest 6 as default, subject to approval).

---

## FS-004 — Product Search and Filtering

**Traces to:** §9.1, §9.2, D-007, D-008

### Description
The catalogue supports free-text search and structured filtering. Available
to both Guest and Registered users.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | The customer can enter a search term and receive matching results | ✅ Confirmed |
| 2 | Search covers: title, author, category, and brand/publisher | ✅ Confirmed (D-007) |
| 3 | Search is case-insensitive | 🔵 Proposed |
| 4 | The customer can filter results by category | ✅ Confirmed (D-008) |
| 5 | The customer can filter results by brand | ✅ Confirmed (D-008) |
| 6 | The customer can filter results by price range | ✅ Confirmed (D-008) |
| 7 | The customer can filter results by availability | ✅ Confirmed (D-008) |
| 8 | Filters and search can be combined | 🔵 Proposed |
| 9 | An empty search result shows an appropriate message | 🔵 Proposed |

### Out of Scope
- Full-text/fuzzy/semantic search
- Search autocomplete / typeahead

---

## FS-005 — Shopping Basket

**Traces to:** BR-005, §10.1–10.4, D-001

### Description
A logged-in customer can build a basket of selected products before proceeding
to checkout. Guest users cannot use the basket.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A registered user can add a product to the basket | ✅ Confirmed (BR-005) |
| 2 | The basket displays all added products with title, quantity, unit price, and line total | ✅ Confirmed (§10.2) |
| 3 | The basket displays the overall total | 🔵 Proposed |
| 4 | The customer can update the quantity of a basket item | ✅ Confirmed (§10.3) |
| 5 | The customer can remove an item from the basket | ✅ Confirmed (§10.3) |
| 6 | The basket persists for the duration of the logged-in session | 🔵 Proposed |
| 7 | The basket displays recommendations based on order history (see FS-011) | ✅ Confirmed (§10.4) |
| 8 | The customer can proceed from the basket to checkout | ✅ Confirmed |
| 9 | A guest user attempting to add to basket is redirected to login | ✅ Confirmed (D-001) |

### Out of Scope
- Guest basket persistence
- Saved baskets / wishlists

---

## FS-006 — Order History and Buy Again

**Traces to:** BR-006, BR-007, §11.2, §11.3, §6.4

### Description
An authenticated customer can view all previous orders and re-purchase
any previously ordered product with a single action.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A registered user can view a list of their past orders | ✅ Confirmed (BR-006) |
| 2 | Each order shows: order date, items, quantities, order total, and order status | 🔵 Proposed |
| 3 | The customer can select an individual order to view its details | 🔵 Proposed |
| 4 | The customer can use Buy Again to add a previously ordered product to the basket | ✅ Confirmed (BR-007) |
| 5 | Buy Again respects current product availability | 🔵 Proposed |
| 6 | Guest users cannot access order history | ✅ Confirmed (D-001) |

---

## FS-007 — Delivery Address Management

**Traces to:** BR-009, §12.1, D-009

### Description
A registered customer can maintain multiple saved delivery addresses and
select one at checkout.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A registered user can save one or more delivery addresses to their account | ✅ Confirmed (D-009) |
| 2 | During checkout, the customer selects a delivery address from their saved addresses | ✅ Confirmed (BR-009, D-009) |
| 3 | The customer can add a new delivery address | ✅ Confirmed (D-009) |
| 4 | The customer can edit an existing delivery address | 🔵 Proposed |
| 5 | The customer can delete a saved delivery address | 🔵 Proposed |
| 6 | At least one address must be selected before checkout can proceed | 🔵 Proposed |

### Open Questions
- ❓ Are delivery charges applicable? (BRD §12.3, Q-16 — unresolved)
- ❓ Are delivery charges location-specific or product-specific? (BRD §12.3 — unresolved)

> **Assumption:** No delivery charges for this capstone (simulated), unless a business decision adds them.

---

## FS-008 — Tentative Delivery Date

**Traces to:** §7.5, §12.2, D-005

### Description
Each product displays an estimated delivery date, calculated as a fixed
category-based offset from the order date.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | The product detail page shows a tentative delivery date | ✅ Confirmed |
| 2 | The delivery date is calculated as: order date + category delivery offset | ✅ Confirmed (D-005) |
| 3 | Each book category has a configured delivery offset in days | ✅ Confirmed (D-005) |
| 4 | The delivery date is displayed on the purchase confirmation screen | 🔵 Proposed |

### Delivery Offset Configuration (D-005)
Example offsets (exact values subject to approval):

| Category | Offset (days) |
|---|---|
| Fiction | 3 |
| Non-Fiction | 5 |
| Academic / Textbooks | 7 |
| Default (uncategorised) | 5 |

> These are proposed defaults. The developer must confirm or revise before implementation.

---

## FS-009 — Checkout and Payment

**Traces to:** BR-010, §13.1–13.5, D-004

### Description
After basket review, the customer proceeds through a checkout flow that
collects the delivery address, payment method, and optional gift-point
redemption, then submits a simulated payment.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | Only a logged-in user can begin checkout | ✅ Confirmed (D-001) |
| 2 | Checkout requires a delivery address to be selected | ✅ Confirmed (BR-009) |
| 3 | The customer selects a payment method: credit card or debit card | ✅ Confirmed (§13.2) |
| 4 | The customer can optionally redeem gift points before payment (see FS-010) | ✅ Confirmed (BR-011) |
| 5 | The customer initiates payment | ✅ Confirmed (BR-010) |
| 6 | Payment is fully simulated — no real gateway | ✅ Confirmed (D-004) |
| 7 | The simulated payment returns a success or failure response | ✅ Confirmed (D-004) |
| 8 | On payment success, the application shows a payment confirmation | ✅ Confirmed (BR-012) |
| 9 | On payment success, the application shows a purchase confirmation | ✅ Confirmed (BR-013) |
| 10 | On payment failure, the customer is informed and can retry | 🔵 Proposed |
| 11 | A successful payment creates an order record | 🔵 Proposed |
| 12 | The order record stores payment confirmation timestamp (used for cancellation window) | ✅ Confirmed (D-002) |

### Checkout Flow (proposed)
```
Basket Review
    → Select Delivery Address
    → Select Payment Method + Enter Card Details (simulated)
    → Apply Gift Points (optional)
    → Order Summary / Confirm
    → Simulated Payment
    → Payment Confirmation
    → Purchase Confirmation
```

### Out of Scope
- Real payment gateway (Stripe, Razorpay, etc.)
- Payment refunds
- EMI / instalment payment options

---

## FS-010 — Gift Point Redemption

**Traces to:** BR-011, §14.1–14.2, D-003

### Description
A registered customer can redeem gift points against an order total during
the payment step.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | The customer can view their current gift point balance during checkout | ✅ Confirmed |
| 2 | The customer can choose to apply some or all of their available points | ✅ Confirmed (D-003) |
| 3 | 1 gift point = ₹1 discount on the order total | ✅ Confirmed (D-003) |
| 4 | Points can cover up to 100% of the order total | ✅ Confirmed (D-003) |
| 5 | Points do not expire | ✅ Confirmed (D-003) |
| 6 | Redeemed points are deducted from the customer's balance on payment confirmation | 🔵 Proposed |
| 7 | The updated gift point balance is reflected in the customer's account | 🔵 Proposed |

### Open Questions
- ❓ How are gift points **earned**? (BRD Q-22 — unresolved)
- ❓ What happens to redeemed points when an order is cancelled? (BRD Q-26 — unresolved)

> **Assumption for this capstone:** Gift points are pre-seeded / manually assigned for testing purposes until the earning mechanism is defined.

---

## FS-011 — Recommendations

**Traces to:** BR-008, §15.1–15.2, §6.5, §10.4, D-010

### Description
The application displays personalised book recommendations to registered
customers, based on their order history.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | Recommendations are shown within the shopping basket | ✅ Confirmed (§10.4) |
| 2 | Recommendations are shown on the post-login / home screen | ✅ Confirmed (§6.5, §15.1) |
| 3 | Recommendations are based on categories of previously purchased books | ✅ Confirmed (D-010) |
| 4 | Recommendations include books by the same author as previously purchased books | ✅ Confirmed (D-010) |
| 5 | Recommendations include the newest catalogue additions | ✅ Confirmed (D-010) |
| 6 | Recommendations exclude books already owned by the customer (already ordered) | 🔵 Proposed |
| 7 | If the customer has no order history, display newest catalogue additions only | 🔵 Proposed |

### Open Questions
- ❓ How many recommendations should be displayed? (BRD Q-28 — unresolved; suggest 8, pending approval)

---

## FS-012 — Purchase Confirmation

**Traces to:** BR-013, §16, D-002

### Description
After a successful payment, the application presents a purchase confirmation
screen that summarises the completed order.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A purchase confirmation screen is shown after successful payment | ✅ Confirmed (BR-013) |
| 2 | The confirmation shows the order reference / ID | 🔵 Proposed |
| 3 | The confirmation shows the items purchased | 🔵 Proposed |
| 4 | The confirmation shows the total amount paid | 🔵 Proposed |
| 5 | The confirmation shows the selected delivery address | 🔵 Proposed |
| 6 | The confirmation shows the tentative delivery date per item | ✅ Confirmed (D-005) |
| 7 | The confirmation shows the cancellation deadline (payment time + 48 hrs) | 🔵 Proposed |

---

## FS-013 — Order Cancellation

**Traces to:** BR-014, §11.4, D-002

### Description
A registered customer can cancel an order within 48 hours of payment
confirmation, regardless of order status.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A customer can cancel an order from order history | ✅ Confirmed (BR-014) |
| 2 | Cancellation is allowed for 48 hours from payment confirmation timestamp | ✅ Confirmed (D-002) |
| 3 | Cancellation is allowed regardless of current order status, within the window | ✅ Confirmed (D-002) |
| 4 | After the 48-hour window, the cancel option is disabled / not shown | ✅ Confirmed |
| 5 | The customer is shown a cancellation confirmation | 🔵 Proposed |
| 6 | The order status is updated to "Cancelled" after cancellation | 🔵 Proposed |

### Open Questions
- ❓ What happens to the simulated payment after cancellation? (BRD Q-13 — unresolved)
- ❓ Are redeemed gift points restored on cancellation? (BRD Q-26 — unresolved)

> **Assumption for this capstone:** Cancellation simply marks the order as Cancelled. No payment reversal or point restoration is implemented until those questions are resolved.

---

## FS-014 — Catalogue Seed Pipeline

**Traces to:** A-001, A-002, A-003, §3.1

### Description
The book catalogue is populated by an offline Python script that fetches
book metadata from the Open Library API and writes it to a seed file loaded
by the application on startup. There is no in-application catalogue management.

### Acceptance Criteria

| # | Criteria | Classification |
|---|---|---|
| 1 | A standalone Python script fetches book metadata from Open Library | ✅ Confirmed (A-001) |
| 2 | The script produces a structured output file (JSON or equivalent) | ✅ Confirmed |
| 3 | The application loads catalogue data from the seed file on startup | ✅ Confirmed (A-003) |
| 4 | Re-running the seed script refreshes the catalogue | ✅ Confirmed (A-003) |
| 5 | No in-application UI is provided to manage the catalogue | ✅ Confirmed (§3.2) |

### Open Questions
- ❓ What exact data fields must be imported from Open Library? (BRD Q-31 — unresolved)

> **Proposed minimum fields:** title, author(s), ISBN, category/subject, publisher (brand), cover image URL, published year, description, price (seeded/generated).

---

## Summary Table

| Spec | Feature | Traces to BRs |
|---|---|---|
| FS-001 | User Registration & Login | BR-001, D-001 |
| FS-002 | Product Catalogue Browsing | BR-002, BR-003 |
| FS-003 | Product Detail Page | §8, BR-004, D-006 |
| FS-004 | Product Search & Filtering | D-007, D-008 |
| FS-005 | Shopping Basket | BR-005, D-001 |
| FS-006 | Order History & Buy Again | BR-006, BR-007 |
| FS-007 | Delivery Address Management | BR-009, D-009 |
| FS-008 | Tentative Delivery Date | D-005 |
| FS-009 | Checkout & Payment | BR-010, D-004 |
| FS-010 | Gift Point Redemption | BR-011, D-003 |
| FS-011 | Recommendations | BR-008, D-010 |
| FS-012 | Purchase Confirmation | BR-013, D-005 |
| FS-013 | Order Cancellation | BR-014, D-002 |
| FS-014 | Catalogue Seed Pipeline | A-001–A-003 |

---

## Pending Decisions (must be resolved before final specification)

| # | Question | Blocks |
|---|---|---|
| Q-A | How many related products to display? | FS-003 |
| Q-B | How many recommendations to display? | FS-011 |
| Q-C | Exact delivery offset (days) per category? | FS-008, FS-012 |
| Q-D | How are gift points earned? | FS-010 |
| Q-E | Gift points restored on cancellation? | FS-010, FS-013 |
| Q-F | Payment outcome on cancellation (simulated)? | FS-013 |
| Q-G | Are delivery charges applicable? | FS-007, FS-009 |
| Q-H | Exact Open Library data fields to import? | FS-014 |
| Q-I | Password reset / forgot-password flow required? | FS-001 |
