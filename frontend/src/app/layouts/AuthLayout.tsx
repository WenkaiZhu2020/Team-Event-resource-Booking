import { Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <main className="auth-shell">
      <section className="auth-column">
        <div className="auth-copy page-enter">
          <div className="auth-hero-copy">
            <p className="eyebrow">Team Resource</p>
            <h1>Management Console</h1>
            <p className="helper-copy">
              Event operations, shared inventory, approvals, and booking pressure in one live workspace.
            </p>
          </div>
          <div className="auth-visual" aria-hidden="true">
            <div className="auth-visual-map">
              <span className="map-node node-a" />
              <span className="map-node node-b" />
              <span className="map-node node-c" />
              <span className="map-route route-a" />
              <span className="map-route route-b" />
            </div>
            <div className="auth-preview-card card-main">
              <span>Approval queue</span>
              <strong>18</strong>
              <small>4 urgent reviews</small>
            </div>
            <div className="auth-preview-card card-side">
              <span>Room A-12</span>
              <strong>Reserved</strong>
              <small>15:00 - 17:00</small>
            </div>
            <div className="auth-preview-card card-mini">
              <span>Events</span>
              <strong>Live</strong>
            </div>
            <div className="auth-signal-row">
              <span />
              <span />
              <span />
              <span />
              <span />
            </div>
          </div>
        </div>
        <div className="page-enter auth-panel-wrap">
          <Outlet />
        </div>
      </section>
    </main>
  );
}
