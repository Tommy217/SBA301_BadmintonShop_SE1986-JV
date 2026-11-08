import { Navigate } from 'react-router';
import { ROLES } from '@/shared/constants/roles';
import ProtectedRoute from './ProtectedRoute';
import AdminLayout from '@/app/layouts/admin/AdminLayout';
import EmployeesPage from '@/modules/admin/pages/EmployeesPage';
import CustomersPage from '@/modules/admin/pages/CustomersPage';

export const adminRoutes = [
  {
    element: <ProtectedRoute allowedRoles={[ROLES.ADMIN]} />,
    children: [
      {
        path: '/admin',
        element: <AdminLayout />,
        children: [
          { index: true, element: <Navigate to="/admin/employees" replace /> },
          { path: 'employees', element: <EmployeesPage /> },
          { path: 'customers', element: <CustomersPage /> },
        ],
      },
    ],
  },
];
