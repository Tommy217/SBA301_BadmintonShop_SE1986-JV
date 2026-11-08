import { Outlet } from 'react-router';
import AdminFooter from './AdminFooter';
import AdminHeader from './AdminHeader';
import AdminSidebar from './AdminSidebar';
import '@/shared/styles/app.css';
import '@/modules/management/management.css';

export default function AdminLayout() {
  return (
    <div className="shop-app management-app">
      <AdminHeader />
      <div className="shop-layout">
        <AdminSidebar />
        <main className="shop-main management-content">
          <Outlet />
        </main>
      </div>
      <AdminFooter />
    </div>
  );
}
