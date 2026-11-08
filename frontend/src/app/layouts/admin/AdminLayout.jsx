import { Outlet } from 'react-router';
import AdminFooter from './AdminFooter';
import AdminHeader from './AdminHeader';
import AdminSidebar from './AdminSidebar';

const styles = {
  layout: {
    display: 'flex',
    minHeight: '100vh',
    width: '100%',
    backgroundColor: '#f8fafc',
  },
  main: {
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
    minWidth: 0,
  },
  content: {
    flex: 1,
    padding: '28px 36px',
    overflowY: 'auto',
  },
};

export default function AdminLayout() {
  return (
    <div style={styles.layout}>
      <AdminSidebar />
      <div style={styles.main}>
        <AdminHeader />
        <main style={styles.content}>
          <Outlet />
        </main>
        <AdminFooter />
      </div>
    </div>
  );
}
