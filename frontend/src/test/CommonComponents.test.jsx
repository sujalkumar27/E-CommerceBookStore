// LoadingSpinner.test.jsx and ErrorMessage.test.jsx — Tests for common components.
//
// LoadingSpinner tests:
//   1. Renders default "Loading…" text
//   2. Accepts a custom message prop
//
// ErrorMessage tests:
//   1. Renders the error message
//   2. Shows a "Try Again" button when onRetry is provided
//   3. Calls onRetry when "Try Again" is clicked
//   4. Does NOT render "Try Again" when onRetry is absent

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorMessage   from '../components/common/ErrorMessage.jsx';

// ── LoadingSpinner ───────────────────────────────────────────────────────────
describe('LoadingSpinner', () => {
  it('shows default "Loading…" message', () => {
    render(<LoadingSpinner />);
    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('shows a custom message when provided', () => {
    render(<LoadingSpinner message="Loading books…" />);
    expect(screen.getByText('Loading books…')).toBeInTheDocument();
  });
});

// ── ErrorMessage ─────────────────────────────────────────────────────────────
describe('ErrorMessage', () => {
  it('renders the error message text', () => {
    render(<ErrorMessage message="Something failed." />);
    expect(screen.getByText('Something failed.')).toBeInTheDocument();
  });

  it('shows "Try Again" button when onRetry is provided', () => {
    render(<ErrorMessage message="Error" onRetry={vi.fn()} />);
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });

  it('calls onRetry when "Try Again" is clicked', () => {
    const handler = vi.fn();
    render(<ErrorMessage message="Error" onRetry={handler} />);
    fireEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(handler).toHaveBeenCalledOnce();
  });

  it('does not render "Try Again" when onRetry is not provided', () => {
    render(<ErrorMessage message="Error" />);
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull();
  });
});

