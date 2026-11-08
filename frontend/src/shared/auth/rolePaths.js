import { ROLES } from '@/shared/constants/roles';

export const ROLE_HOME_PATHS = {
  [ROLES.ADMIN]: '/admin/employees',
  [ROLES.MANAGER]: '/management',
  [ROLES.STAFF]: '/management',
  [ROLES.CUSTOMER]: '/',
};

// Home page after login: ADMIN first, then MANAGER/STAFF, otherwise the store.
export function getRoleHomePath(role) {
  const normalized = typeof role === 'string' ? role.toUpperCase() : null;
  return ROLE_HOME_PATHS[normalized] || '/';
}
