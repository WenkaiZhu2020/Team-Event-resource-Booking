import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { AuthPanel } from './AuthPanel';

describe('AuthPanel', () => {
  it('renders google button and submits credentials', () => {
    const handleSubmit = vi.fn((event: React.FormEvent<HTMLFormElement>) => event.preventDefault());
    const handleGoogleLogin = vi.fn();

    render(
      <AuthPanel
        authMode="login"
        email="user@example.com"
        password="Password123"
        loading={false}
        googleEnabled
        onModeChange={vi.fn()}
        onEmailChange={vi.fn()}
        onPasswordChange={vi.fn()}
        onSubmit={handleSubmit}
        onGoogleLogin={handleGoogleLogin}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Continue with Google' }));
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }).closest('form')!);

    expect(handleGoogleLogin).toHaveBeenCalledTimes(1);
    expect(handleSubmit).toHaveBeenCalledTimes(1);
  });
});
