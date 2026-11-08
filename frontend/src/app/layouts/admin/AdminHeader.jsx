import { Button } from 'react-bootstrap';
import { useNavigate } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import BrandLogo from '@/shared/components/BrandLogo';

const styles = {
  header: {
    position: 'sticky',
    top: 0,
    zIndex: 100,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    minHeight: '64px',
    padding: '12px 24px',
    backgroundColor: '#ffffff',
    borderBottom: '1px solid #e2e8f0',
    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)',
  },
  brand: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  },
  userContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  avatar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '36px',
    height: '36px',
    borderRadius: '50%',
    backgroundColor: '#0ea5e9',
    color: '#ffffff',
    fontSize: '14px',
    fontWeight: 'bold',
  },
  userInfo: {
    display: 'flex',
    flexDirection: 'column',
  },
  userName: {
    fontSize: '14px',
    fontWeight: '600',
    lineHeight: 1.2,
    color: '#1e293b',
  },
  userEmail: {
    fontSize: '12px',
    color: '#64748b',
  },
  logoutBtn: {
    marginLeft: '12px',
    borderRadius: '6px',
    fontSize: '13px',
  },
};

export default function AdminHeader() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initial = (user?.fullName || user?.email || 'A').charAt(0).toUpperCase();

  return (
    <header style={styles.header}>
      <div style={styles.brand}>
        <BrandLogo />
      </div>

      <div style={styles.userContainer}>
        <div style={styles.avatar}>{initial}</div>
        <div style={styles.userInfo}>
          <span style={styles.userName}>{user?.fullName || 'Administrator'}</span>
          <span style={styles.userEmail}>{user?.email || 'admin@badminton.vn'}</span>
        </div>
        <Button
          variant="outline-danger"
          size="sm"
          style={styles.logoutBtn}
          onClick={handleLogout}
        >
          Đăng xuất
        </Button>
      </div>
    </header>
  );
}
