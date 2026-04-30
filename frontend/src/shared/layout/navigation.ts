import type { Role } from '../constants/roles';

export interface NavItem {
  label: string;
  path: string;
  roles?: Role[];
  badge?: number;
}

export const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard' },
  { label: 'Account', path: '/account' },
  { label: 'Events', path: '/events' },
  { label: 'Resources', path: '/resources' },
  { label: 'Bookings', path: '/bookings' },
  { label: 'Notifications', path: '/notifications' },
  { label: 'Approvals', path: '/approvals', roles: ['RESOURCE_MANAGER', 'ADMIN'] }
];
