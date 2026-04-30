import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShellLayout } from '../layouts/AppShellLayout';
import { AuthLayout } from '../layouts/AuthLayout';
import { AccountPage } from '../pages/AccountPage';
import { ApprovalsPage } from '../pages/ApprovalsPage';
import { AuthPage } from '../pages/AuthPage';
import { BookingsPage } from '../pages/BookingsPage';
import { DashboardPage } from '../pages/DashboardPage';
import { EventsPage } from '../pages/EventsPage';
import { NotificationsPage } from '../pages/NotificationsPage';
import { OAuthCallbackPage } from '../pages/OAuthCallbackPage';
import { ResourcesPage } from '../pages/ResourcesPage';
import { SystemPage } from '../pages/SystemPage';
import { ProtectedRoute } from './ProtectedRoute';

export function AppRouter() {
  return (
    <Routes>
      <Route path="/auth" element={<AuthLayout />}>
        <Route path="login" element={<AuthPage mode="login" />} />
        <Route path="register" element={<AuthPage mode="register" />} />
        <Route path="google/callback" element={<OAuthCallbackPage />} />
      </Route>

      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppShellLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="system" element={<SystemPage />} />
        <Route path="account" element={<AccountPage />} />
        <Route path="events" element={<EventsPage />} />
        <Route path="resources" element={<ResourcesPage />} />
        <Route path="bookings" element={<BookingsPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="approvals" element={<ApprovalsPage />} />
      </Route>

      <Route path="/login" element={<Navigate to="/auth/login" replace />} />
      <Route path="/oauth2/callback" element={<Navigate to="/auth/google/callback" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
