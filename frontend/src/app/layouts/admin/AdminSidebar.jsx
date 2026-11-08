import { NavLink } from 'react-router';

export default function AdminSidebar() {
  return (
    <aside className="admin-sidebar">
      <div className="admin-sidebar-title">QUẢN TRỊ VIÊN</div>

      <nav className="admin-nav">
        <NavLink to="/admin/employees" className="admin-nav-link">
          <span className="admin-nav-icon">👔</span>
          <span>Nhân viên</span>
        </NavLink>
        <NavLink to="/admin/customers" className="admin-nav-link">
          <span className="admin-nav-icon">👥</span>
          <span>Khách hàng</span>
        </NavLink>
      </nav>
    </aside>
  );
}
