import { Link, Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <main className="auth-shell">
      <section className="auth-column">
        <div className="auth-copy">
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
          <p className="helper-copy">
            Unified workspace for event coordination, resource booking, approvals, notifications, and analytics.
          </p>
          <div className="auth-links">
            <Link to="/auth/login">Login</Link>
            <Link to="/auth/register">Register</Link>
          </div>
        </div>
        <div>
          <Outlet />
        </div>
      </section>
    </main>
  );
}
