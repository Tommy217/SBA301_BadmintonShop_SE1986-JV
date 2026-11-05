import { Outlet, useLocation } from 'react-router';
import { Fragment } from 'react';
import { useAuth } from '@/app/providers/auth/AuthContext';
import ShopProvider from '@/app/providers/shop/ShopProvider';
import { getToken } from '@/shared/auth/session';
import PublicFooter from '@/shared/components/PublicFooter';
import PublicHeader from '@/app/layouts/public/PublicHeader';
import ManagementSidebar from './ManagementSidebar';
import '@/shared/styles/app.css';
import '@/modules/management/management.css';

export default function ManagementLayout() {
  const { user } = useAuth();
  const pathname = useLocation().pathname;
  const isDeliveryPage = pathname.startsWith('/management/deliveries');
  const StoreProvider =
    pathname.startsWith('/management/catalog/') ||
    pathname.startsWith('/management/inventory') ||
    pathname.startsWith('/management/invoices')
      ? Fragment
      : ShopProvider;
  const mainClassName = isDeliveryPage
    ? 'management-delivery min-vh-100 p-3 p-md-4 p-xl-5'
    : 'shop-main management-content';

  return (
    <StoreProvider key={getToken(user) || 'management'}>
      <div className="shop-app management-app">
        <PublicHeader management />
        <div className="shop-layout">
          <ManagementSidebar />
          <main className={mainClassName}>
            <Outlet />
          </main>
        </div>
        <PublicFooter />
      </div>
    </StoreProvider>
  );
}
