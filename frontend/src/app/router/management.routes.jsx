import { ROLES } from '@/shared/constants/roles';
import ManagementLayout from '@/app/layouts/management/ManagementLayout';
import ProtectedRoute from './ProtectedRoute';
import SupportTicketManagementPage from '@/modules/support/pages/SupportTicketManagementPage';
import SupportTicketManagementDetailPage from '@/modules/support/pages/SupportTicketManagementDetailPage';
import DeliveryManagementPage from '@/modules/delivery/pages/DeliveryManagementPage';
import DeliveryManagementDetailPage from '@/modules/delivery/pages/DeliveryManagementDetailPage';
import DashboardPage from '@/modules/management/pages/DashboardPage';
import CatalogPage from '@/modules/management/pages/CatalogPage';
import ManagerOrdersPage from '@/modules/management/pages/ManagerOrdersPage';
import InvoiceManagementPage from '@/modules/management/pages/InvoiceManagementPage';
import InventoryManagementPage from '@/modules/management/pages/InventoryManagementPage';
import SettingsPage from '@/modules/management/pages/SettingsPage';

export const managementRoutes = [
  {
    element: <ProtectedRoute allowedRoles={[ROLES.MANAGER, ROLES.STAFF]} />,
    children: [
      {
        path: '/management',
        element: <ManagementLayout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'orders', element: <ManagerOrdersPage /> },
          { path: 'invoices', element: <InvoiceManagementPage /> },
          { path: 'inventory', element: <InventoryManagementPage /> },
          { path: 'catalog/:catalog', element: <CatalogPage /> },
          {
            path: 'support-tickets',
            element: <SupportTicketManagementPage />,
          },
          {
            path: 'support-tickets/:ticketId',
            element: <SupportTicketManagementDetailPage />,
          },
          {
            path: 'deliveries',
            element: <DeliveryManagementPage />,
          },
          {
            path: 'deliveries/:deliveryId',
            element: <DeliveryManagementDetailPage />,
          },
          {
            path: 'settings',
            element: <SettingsPage />,
          },
        ],
      },
    ],
  },
];
