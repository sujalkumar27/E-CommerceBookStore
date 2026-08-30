// GiftPointsInput.test.jsx — Tests for the GiftPointsInput checkout component.
//
// Tests:
//   1. Shows the user's balance
//   2. Shows "no gift points" message when balance is 0
//   3. Caps input at the user's balance
//   4. Caps input at the order total (can't redeem more than the order costs)
//   5. Calls onChange with updated value

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import GiftPointsInput from '../components/checkout/GiftPointsInput.jsx';

describe('GiftPointsInput', () => {
  it('shows the user balance', () => {
    render(<GiftPointsInput balance={150} orderTotal={500} pointsToRedeem={0} onChange={vi.fn()} />);
    expect(screen.getByText(/150 pts/)).toBeInTheDocument();
  });

  it('shows "no gift points" message when balance is 0', () => {
    render(<GiftPointsInput balance={0} orderTotal={500} pointsToRedeem={0} onChange={vi.fn()} />);
    expect(screen.getByText(/no gift points to redeem/i)).toBeInTheDocument();
  });

  it('shows input when balance > 0', () => {
    render(<GiftPointsInput balance={100} orderTotal={500} pointsToRedeem={0} onChange={vi.fn()} />);
    expect(screen.getByRole('spinbutton')).toBeInTheDocument();
  });

  it('max attribute is capped at balance when balance < orderTotal', () => {
    render(<GiftPointsInput balance={50} orderTotal={500} pointsToRedeem={0} onChange={vi.fn()} />);
    const input = screen.getByRole('spinbutton');
    expect(input.max).toBe('50'); // capped at balance
  });

  it('max attribute is capped at order total when balance > orderTotal', () => {
    render(<GiftPointsInput balance={1000} orderTotal={200} pointsToRedeem={0} onChange={vi.fn()} />);
    const input = screen.getByRole('spinbutton');
    expect(input.max).toBe('200'); // capped at floor(orderTotal)
  });

  it('calls onChange when input changes', () => {
    const handler = vi.fn();
    render(<GiftPointsInput balance={100} orderTotal={500} pointsToRedeem={0} onChange={handler} />);
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '30' } });
    expect(handler).toHaveBeenCalledWith(30);
  });
});

