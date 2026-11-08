import { useAuth } from '@/app/providers/auth/AuthContext';
import { Navigate, Outlet, useLocation } from 'react-router';
import PublicHeader from './PublicHeader';
import PublicSidebar from './PublicSidebar';
import PublicFooter from '@/shared/components/PublicFooter';
import ShopProvider from '@/app/providers/shop/ShopProvider';
import '@/shared/styles/app.css';
import { getRole, getToken } from '@/shared/auth/session';
import { getRoleHomePath } from '@/shared/auth/rolePaths';
export default function PublicLayout() {
  const { user } = useAuth();
  const { pathname } = useLocation();
  const homePath = getRoleHomePath(getRole(user));

  if (pathname === '/' && homePath !== '/') return <Navigate to={homePath} replace />;

  return (
    <ShopProvider key={getToken(user) || 'guest'}>
      <div className="shop-app">
        <PublicHeader />
        <div className="shop-layout">
          <PublicSidebar />
          <main
            className={
              ['/payments', '/payments/qr'].includes(pathname)
                ? 'shop-main payment-content'
                : 'shop-main'
            }
          >
            <Outlet />
          </main>
        </div>
        <PublicFooter />
      </div>
    </ShopProvider>
  );
}
