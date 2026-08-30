// CartItemRow.test.jsx — Tests for the CartItemRow component.
//
// Tests:
//   1. Renders book title, author, quantity, and line total
//   2. Minus button calls onUpdate with quantity-1
//   3. Plus button calls onUpdate with quantity+1
//   4. Minus button is disabled when quantity is 1
//   5. Remove button calls onRemove with the item ID

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import CartItemRow from '../components/cart/CartItemRow.jsx';

const mockItem = {
  id: 'item-1',
  quantity: 2,
  lineTotal: 1598.00,
  book: {
    id: 'book-1',
    title: 'Clean Code',
    author: 'Robert Martin',
    price: 799.00,
    coverImageUrl: null,
  },
};

const renderRow = (overrides = {}) => {
  const onUpdate = vi.fn();
  const onRemove = vi.fn();
  render(
    <MemoryRouter>
      <CartItemRow
        item={{ ...mockItem, ...overrides }}
        onUpdate={onUpdate}
        onRemove={onRemove}
        disabled={false}
      />
    </MemoryRouter>
  );
  return { onUpdate, onRemove };
};

describe('CartItemRow', () => {
  it('renders the book title and author', () => {
    renderRow();
    expect(screen.getByText('Clean Code')).toBeInTheDocument();
    expect(screen.getByText('Robert Martin')).toBeInTheDocument();
  });

  it('shows the current quantity', () => {
    renderRow();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('shows the line total', () => {
    renderRow();
    expect(screen.getByText('₹1598.00')).toBeInTheDocument();
  });

  it('plus button calls onUpdate with quantity+1', () => {
    const { onUpdate } = renderRow();
    fireEvent.click(screen.getByRole('button', { name: '+' }));
    expect(onUpdate).toHaveBeenCalledWith('item-1', 3);
  });

  it('minus button calls onUpdate with quantity-1', () => {
    const { onUpdate } = renderRow();
    fireEvent.click(screen.getByRole('button', { name: '−' }));
    expect(onUpdate).toHaveBeenCalledWith('item-1', 1);
  });

  it('minus button is disabled when quantity is 1', () => {
    renderRow({ quantity: 1 });
    expect(screen.getByRole('button', { name: '−' })).toBeDisabled();
  });

  it('remove button calls onRemove with the item ID', () => {
    const { onRemove } = renderRow();
    fireEvent.click(screen.getByText('Remove'));
    expect(onRemove).toHaveBeenCalledWith('item-1');
  });
});

