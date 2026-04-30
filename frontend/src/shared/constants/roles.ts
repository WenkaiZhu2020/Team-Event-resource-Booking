export type Role = 'USER' | 'ORGANIZER' | 'RESOURCE_MANAGER' | 'ADMIN' | 'INTERNAL_SERVICE';

export function hasAnyRole(userRoles: string[], requiredRoles: Role[]) {
  return requiredRoles.some((role) => userRoles.includes(role));
}
