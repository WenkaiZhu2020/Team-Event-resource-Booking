import { Link, Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <main className="auth-shell">
      <section className="auth-column">
        <div className="auth-copy page-enter">
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
          <p className="helper-copy">
            Events, resources, approvals, notifications, and reporting.
          </p>
          <div className="auth-feature-grid">
            <article>
              <strong>Role-aware access</strong>
              <span>JWT-backed navigation and approval visibility.</span>
            </article>
            <article>
              <strong>Conflict-safe booking</strong>
              <span>Shared resource reservation with approval and waitlist handling.</span>
            </article>
            <article>
              <strong>Operational insight</strong>
              <span>Dashboard metrics and notification history for walkthrough-ready demos.</span>
            </article>
          </div>
          <div className="auth-links">
            <Link to="/auth/login">Login</Link>
            <Link to="/auth/register">Register</Link>
          </div>
        </div>
        <div className="page-enter auth-panel-wrap">
          <Outlet />
        </div>
      </section>
    </main>
  );
}
