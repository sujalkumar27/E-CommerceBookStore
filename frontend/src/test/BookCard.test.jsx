// BookCard.test.jsx — Tests for the BookCard component.
//
// Tests:
//   1. Renders book title, author, and price
//   2. Shows "Out of Stock" when available is false
//   3. "Add to Cart" button calls onAddToCart with the book ID
//   4. Shows "Login to Buy" when user is not logged in

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import BookCard from '../components/catalogue/BookCard.jsx';

const mockBook = {
  id: 'book-1',
  title: 'Clean Code',
  author: 'Robert Martin',
  price: 799.00,
  available: true,
  coverImageUrl: null,
  category: { name: 'Technology' },
};

const renderCard = (overrides = {}, isLoggedIn = true) =>
  render(
    <MemoryRouter>
      <BookCard
        book={{ ...mockBook, ...overrides }}
        onAddToCart={vi.fn()}
        isLoggedIn={isLoggedIn}
      />
    </MemoryRouter>
  );

describe('BookCard', () => {
  it('renders book title and author', () => {
    renderCard();
    expect(screen.getByText('Clean Code')).toBeInTheDocument();
    expect(screen.getByText('Robert Martin')).toBeInTheDocument();
  });

  it('shows ₹799.00 price', () => {
    renderCard();
    expect(screen.getByText('₹799.00')).toBeInTheDocument();
  });

  it('shows "In Stock" for available books', () => {
    renderCard({ available: true });
    expect(screen.getByText('In Stock')).toBeInTheDocument();
  });

  it('shows "Out of Stock" for unavailable books', () => {
    renderCard({ available: false });
    expect(screen.getByText('Out of Stock')).toBeInTheDocument();
  });

  it('calls onAddToCart with the book id when "Add to Cart" is clicked', () => {
    const handler = vi.fn();
    render(
      <MemoryRouter>
        <BookCard book={mockBook} onAddToCart={handler} isLoggedIn={true} />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: /add to cart/i }));
    expect(handler).toHaveBeenCalledWith('book-1');
  });

  it('shows "Login to Buy" when user is not logged in', () => {
    renderCard({}, false);
    expect(screen.getByRole('button', { name: /login to buy/i })).toBeInTheDocument();
  });
});

