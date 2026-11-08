import { useState } from 'react';
import { NavLink } from 'react-router';

const styles = {
  sidebar: {
    position: 'sticky',
    top: 0,
    zIndex: 99,
    display: 'flex',
    flexDirection: 'column',
    width: '260px',
    minWidth: '260px',
    height: '100vh',
    padding: '24px 16px',
    overflowY: 'auto',
    backgroundColor: '#0f172a',
    borderRight: '1px solid #1e293b',
    color: '#e2e8f0',
  },
  title: {
    marginBottom: '16px',
    paddingLeft: '12px',
    fontSize: '11px',
    fontWeight: 700,
    letterSpacing: '1px',
    textTransform: 'uppercase',
    color: '#94a3b8',
  },
  nav: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  getLinkStyle: (isActive, isHovered) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '10px 14px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: isActive ? 600 : 500,
    color: isActive ? '#ffffff' : isHovered ? '#f1f5f9' : '#94a3b8',
    backgroundColor: isActive
      ? '#2563eb'
      : isHovered
      ? '#1e293b'
      : 'transparent',
    textDecoration: 'none',
    transition: 'all 0.2s ease',
  }),
};

export default function AdminSidebar() {
  const [hoveredNav, setHoveredNav] = useState(null);

  return (
    <aside style={styles.sidebar}>
      <div style={styles.title}>QUẢN TRỊ VIÊN</div>

      <nav style={styles.nav}>
        <NavLink
          to="/admin/employees"
          style={({ isActive }) =>
            styles.getLinkStyle(isActive, hoveredNav === 'employees')
          }
          onMouseEnter={() => setHoveredNav('employees')}
          onMouseLeave={() => setHoveredNav(null)}
        >
          <span>Nhân viên</span>
        </NavLink>

        <NavLink
          to="/admin/customers"
          style={({ isActive }) =>
            styles.getLinkStyle(isActive, hoveredNav === 'customers')
          }
          onMouseEnter={() => setHoveredNav('customers')}
          onMouseLeave={() => setHoveredNav(null)}
        >
          <span>Khách hàng</span>
        </NavLink>
      </nav>
    </aside>
  );
}
