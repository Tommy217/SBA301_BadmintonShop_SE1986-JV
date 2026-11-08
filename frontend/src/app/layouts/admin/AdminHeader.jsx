import { Button, Navbar } from 'react-bootstrap';
import { useNavigate } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import BrandLogo from '@/shared/components/BrandLogo';
import '@/app/layouts/public/PublicHeader.css';

export default function AdminHeader() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initial = (user?.fullName || user?.email || 'A').charAt(0).toUpperCase();

  return (
    <header className="store-header">
      <div className="shop-topbar">
        Badminton Shop · Cổng quản trị hệ thống
      </div>
      <Navbar className="store-navbar">
        <BrandLogo to="/admin/employees" />

        <div className="header-actions">
          <div className="d-flex align-items-center gap-2">
            <div className="header-avatar">{initial}</div>
            <div className="header-user-copy">
              <small>Quản trị viên</small>
              <strong>{user?.fullName || user?.email || 'Administrator'}</strong>
            </div>
          </div>
          <span className="header-action-divider" aria-hidden="true" />
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={handleLogout}
          >
            Đăng xuất
          </Button>
        </div>
      </Navbar>
    </header>
  );
}
