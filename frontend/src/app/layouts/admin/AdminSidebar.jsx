import { Nav } from 'react-bootstrap';
import { NavLink } from 'react-router';
import Icon from '@/shared/components/Icon';
import '@/app/layouts/public/PublicSidebar.css';

const LINKS = [
  { to: '/admin/employees', label: 'Nhân viên', icon: 'shield' },
  { to: '/admin/customers', label: 'Khách hàng', icon: 'bag' },
];

export default function AdminSidebar() {
  return (
    <aside className="shop-sidebar manager-sidebar">
      <div className="eyebrow">HỆ THỐNG QUẢN TRỊ</div>
      <Nav className="flex-column gap-2">
        {LINKS.map(({ to, label, icon }) => (
          <Nav.Link as={NavLink} to={to} key={to}>
            <span className="sidebar-icon">
              <Icon name={icon} />
            </span>
            <span>{label}</span>
          </Nav.Link>
        ))}
      </Nav>
      <div className="sidebar-tip">
        <strong>Phân quyền an toàn</strong>
        <p className="small mt-2 mb-0">Quản trị viên có toàn quyền kiểm soát nhân sự và người dùng.</p>
      </div>
    </aside>
  );
}
