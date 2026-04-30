import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAppContext } from '../state/AppContext';

export function AppShellLayout() {
  const location = useLocation();
  const { currentUser, initials, navigationItems, rolesLabel, logout } = useAppContext();

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Team Resource</p>
          <h1>Management Console</h1>
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
      </aside>

      <section className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Frontend workspace</p>
            <h2>{resolveTitle(location.pathname)}</h2>
          </div>
          <div className="account-chip">
            <span>{initials}</span>
            <div className="account-copy">
              <strong>{currentUser?.email}</strong>
              <small>{rolesLabel}</small>
            </div>
            <button type="button" className="secondary-button" onClick={logout}>Sign out</button>
          </div>
        </header>
        <Outlet />
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
