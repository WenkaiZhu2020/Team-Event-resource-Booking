import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { PageErrorBoundary } from '../../components/PageErrorBoundary';
import { useAppContext } from '../state/AppContext';

export function AppShellLayout() {
  const location = useLocation();
  const { currentUser, initials, navigationItems, rolesLabel, logout, approvals, notifications, resources, myEvents } = useAppContext();

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
          <p className="sidebar-copy">
            Live operations workspace for events, shared resources, approvals, notifications, and booking flow control.
          </p>
        </div>
        <nav className="nav-list" aria-label="Primary">
          {navigationItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <span>{item.label}</span>
              {item.badge && item.badge > 0 ? <small>{item.badge}</small> : null}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-insight panel-glass">
          <p className="eyebrow">Workspace status</p>
          <div className="sidebar-stats">
            <article>
              <strong>{resources.length}</strong>
              <span>resources</span>
            </article>
            <article>
              <strong>{myEvents.length}</strong>
              <span>owned events</span>
            </article>
            <article>
              <strong>{approvals.length}</strong>
              <span>pending reviews</span>
            </article>
            <article>
              <strong>{notifications.length}</strong>
              <span>notifications</span>
            </article>
          </div>
        </div>
      </aside>

      <section className="content">
        <header className="topbar page-enter">
          <div>
            <p className="eyebrow">Frontend workspace</p>
            <h2>{resolveTitle(location.pathname)}</h2>
            <p className="topbar-copy">{resolveSubtitle(location.pathname)}</p>
          </div>
          <div className="account-chip">
            <span>{initials}</span>
            <div className="account-copy">
              <strong>{currentUser?.email}</strong>
              <small>{rolesLabel}</small>
            </div>
            <div className="account-status">
              <small>Active role set</small>
              <strong>{rolesLabel}</strong>
            </div>
            <button type="button" className="secondary-button" onClick={logout}>Sign out</button>
          </div>
        </header>
        <div className="page-stage page-enter">
          <PageErrorBoundary key={location.pathname}>
            <Outlet />
          </PageErrorBoundary>
        </div>
      </section>
    </main>
  );
}

function resolveTitle(pathname: string) {
  switch (pathname) {
    case '/dashboard':
      return 'Dashboard';
    case '/account':
      return 'Account';
    case '/system':
      return 'System Monitor';
    case '/events':
      return 'Events';
    case '/resources':
      return 'Resources';
    case '/bookings':
      return 'Bookings';
    case '/notifications':
      return 'Notifications';
    case '/approvals':
      return 'Approvals';
    default:
      return 'Service Console';
  }
}

function resolveSubtitle(pathname: string) {
  switch (pathname) {
    case '/dashboard':
      return 'Operational totals, demand signals, and current activity indicators.';
    case '/account':
      return 'Identity details and communication preferences for the signed-in user.';
    case '/system':
      return 'Live workspace signals, service topology, event flow, and concurrency control mechanisms.';
    case '/events':
      return 'Publishing, registration, and organizer-owned event activity.';
    case '/resources':
      return 'Shared rooms, facilities, and equipment with bookable policies.';
    case '/bookings':
      return 'Personal reservation flow, statuses, and cancellation actions.';
    case '/notifications':
      return 'Delivery history and unread operational updates.';
    case '/approvals':
      return 'Approval work queue for manager and admin decision handling.';
    default:
      return 'Role-aware operational controls across the full platform.';
  }
}
