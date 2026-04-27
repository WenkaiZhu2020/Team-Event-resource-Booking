import type { FormEvent } from 'react';

interface AuthPanelProps {
  authMode: 'login' | 'register';
  email: string;
  password: string;
  loading: boolean;
  onModeChange: (mode: 'login' | 'register') => void;
  onEmailChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function AuthPanel(props: AuthPanelProps) {
  const {
    authMode,
    email,
    password,
    loading,
    onModeChange,
    onEmailChange,
    onPasswordChange,
    onSubmit
  } = props;

  return (
    <section className="panel auth-panel">
      <div>
        <p className="eyebrow">Session</p>
        <h3>{authMode === 'login' ? 'Sign in' : 'Create account'}</h3>
      </div>

      <div className="segmented-control" aria-label="Authentication mode">
        <button
          type="button"
          className={authMode === 'login' ? 'selected' : ''}
          onClick={() => onModeChange('login')}
        >
          Login
        </button>
        <button
          type="button"
          className={authMode === 'register' ? 'selected' : ''}
          onClick={() => onModeChange('register')}
        >
          Register
        </button>
      </div>

      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Email
          <input type="email" value={email} onChange={(event) => onEmailChange(event.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => onPasswordChange(event.target.value)} minLength={8} required />
        </label>
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? 'Working...' : authMode === 'login' ? 'Sign in' : 'Create account'}
        </button>
      </form>
    </section>
  );
}
