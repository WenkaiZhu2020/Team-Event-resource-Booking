import type { PropsWithChildren } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAppContext } from '../state/AppContext';
import { hasAnyRole, type Role } from '../../shared/constants/roles';

interface ProtectedRouteProps {
  roles?: Role[];
}

export function ProtectedRoute({ roles, children }: PropsWithChildren<ProtectedRouteProps>) {
  const location = useLocation();
  const { authenticated, currentUser } = useAppContext();

  if (!authenticated) {
    return <Navigate to="/auth/login" replace state={{ redirectTo: location.pathname + location.search }} />;
  }

  if (roles && roles.length > 0 && !hasAnyRole(currentUser?.roles ?? [], roles)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
