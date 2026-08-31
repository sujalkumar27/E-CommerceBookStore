// App.jsx — Root component that sets up routing and global context providers.
//
// PROVIDERS (outermost to innermost):
//   AuthProvider   → makes auth state (token, user, login, logout) available everywhere
//   CartProvider   → makes cartCount available everywhere (for the Navbar badge)
//   BrowserRouter  → enables client-side routing (no full-page reloads)
//
// ROUTES:
//   Public (no login needed):   /, /books/:id, /login, /register
//   Protected (login required): /cart, /checkout, /checkout/confirmation,
//                               /orders, /orders/:id, /account/addresses
//   Catch-all:                  * → NotFoundPage (prevents blank white screen)
//
//   ProtectedRoute wraps each auth-required page and redirects guests to /login.

import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider }  from './context/AuthContext.jsx';
import { CartProvider }  from './context/CartContext.jsx';

import Navbar            from './components/layout/Navbar.jsx';
import Footer            from './components/layout/Footer.jsx';
import ProtectedRoute    from './components/common/ProtectedRoute.jsx';

import CataloguePage              from './pages/CataloguePage.jsx';
import ProductDetailPage          from './pages/ProductDetailPage.jsx';
import LoginPage                  from './pages/LoginPage.jsx';
import RegisterPage               from './pages/RegisterPage.jsx';
import CartPage                   from './pages/CartPage.jsx';
import CheckoutPage               from './pages/CheckoutPage.jsx';
import PurchaseConfirmationPage   from './pages/PurchaseConfirmationPage.jsx';
import OrderHistoryPage           from './pages/OrderHistoryPage.jsx';
import OrderDetailPage            from './pages/OrderDetailPage.jsx';
import AddressManagementPage      from './pages/AddressManagementPage.jsx';
import NotFoundPage               from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    // AuthProvider must be the outermost because CartProvider's getCart() call
    // needs the token which is managed by AuthProvider.
    <AuthProvider>
      <CartProvider>
        <BrowserRouter>
          {/* Navbar is rendered on every page */}
          <Navbar />

          {/* min-h-screen ensures the footer is always pushed to the bottom */}
          <main className="min-h-screen">
            <Routes>
              {/* ── Public routes ──────────────────────────────────── */}
              <Route path="/"           element={<CataloguePage />} />
              <Route path="/books/:id"  element={<ProductDetailPage />} />
              <Route path="/login"      element={<LoginPage />} />
              <Route path="/register"   element={<RegisterPage />} />

              {/* ── Protected routes (require login) ───────────────── */}
              <Route
                path="/cart"
                element={<ProtectedRoute><CartPage /></ProtectedRoute>}
              />
              <Route
                path="/checkout"
                element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>}
              />
              <Route
                path="/checkout/confirmation"
                element={<ProtectedRoute><PurchaseConfirmationPage /></ProtectedRoute>}
              />
              <Route
                path="/orders"
                element={<ProtectedRoute><OrderHistoryPage /></ProtectedRoute>}
              />
              <Route
                path="/orders/:id"
                element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>}
              />
              <Route
                path="/account/addresses"
                element={<ProtectedRoute><AddressManagementPage /></ProtectedRoute>}
              />

              {/* ── 404 catch-all ─────────────────────────────────────── */}
              {/* Matches any URL that did not match a route above.        */}
              {/* Without this, React renders a blank white screen.        */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </main>

          <Footer />
        </BrowserRouter>
      </CartProvider>
    </AuthProvider>
  );
}
