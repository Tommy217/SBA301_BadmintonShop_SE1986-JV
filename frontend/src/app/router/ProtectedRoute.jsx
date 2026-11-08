import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { getRole, getToken } from '@/shared/auth/session';
import { getRoleHomePath } from '@/shared/auth/rolePaths';

// Requires a logged-in user; when allowedRoles is given, the user must have an allowed role.
export default function ProtectedRoute({ allowedRoles }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!getToken(user)?.trim()) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location, message: 'Vui lòng đăng nhập để sử dụng chức năng này.' }}
      />
    );
  }

  const userRole = getRole(user);
  const hasPermission =
    !allowedRoles?.length || (userRole && allowedRoles.map((r) => r.toUpperCase()).includes(userRole));

  return hasPermission ? <Outlet /> : <Navigate to={getRoleHomePath(userRole)} replace />;
}
