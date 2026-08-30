// Pagination.test.jsx — Tests for the Pagination component.
//
// Tests:
//   1. Does not render when totalPages <= 1
//   2. Renders correct number of page buttons
//   3. Calls onPageChange with the correct page index
//   4. Disables the "Prev" button on the first page
//   5. Disables the "Next" button on the last page

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Pagination from '../components/catalogue/Pagination.jsx';

describe('Pagination', () => {
  it('renders nothing when totalPages is 1', () => {
    const { container } = render(
      <Pagination currentPage={0} totalPages={1} onPageChange={vi.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders 3 page buttons for totalPages=3', () => {
    render(<Pagination currentPage={0} totalPages={3} onPageChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: '1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '3' })).toBeInTheDocument();
  });

  it('calls onPageChange with correct index when page button clicked', () => {
    const handler = vi.fn();
    render(<Pagination currentPage={0} totalPages={3} onPageChange={handler} />);
    fireEvent.click(screen.getByRole('button', { name: '2' }));
    expect(handler).toHaveBeenCalledWith(1); // 0-based index for page 2
  });

  it('"← Prev" is disabled on the first page', () => {
    render(<Pagination currentPage={0} totalPages={3} onPageChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /prev/i })).toBeDisabled();
  });

  it('"Next →" is disabled on the last page', () => {
    render(<Pagination currentPage={2} totalPages={3} onPageChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
  });

  it('"Next →" calls onPageChange with currentPage+1', () => {
    const handler = vi.fn();
    render(<Pagination currentPage={0} totalPages={3} onPageChange={handler} />);
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(handler).toHaveBeenCalledWith(1);
  });
});

