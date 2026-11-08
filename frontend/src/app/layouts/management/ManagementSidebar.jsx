import { Nav } from 'react-bootstrap';
import { NavLink } from 'react-router';
import Icon from '@/shared/components/Icon';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { ROLES } from '@/shared/constants/roles';
import '@/app/layouts/public/PublicSidebar.css';

const CATALOG_LINKS = [
  ['products', 'Sản phẩm'],
  ['brands', 'Thương hiệu'],
  ['categories', 'Danh mục'],
  ['vouchers', 'Voucher'],
].map(([key, label]) => ({ to: `/management/catalog/${key}`, label, icon: 'card' }));

const LINKS = [
  { to: '/management', label: 'Dashboard tổng quan', icon: 'receipt' },
  { to: '/management/orders', label: 'Đơn hàng khách hàng', icon: 'bag' },
  { to: '/management/inventory', label: 'Quản lý kho', icon: 'card' },
  { to: '/management/invoices', label: 'Hóa đơn', icon: 'receipt' },
  ...CATALOG_LINKS,
  { to: '/management/support-tickets', label: 'Support tickets', icon: 'shield' },
  { to: '/management/deliveries', label: 'Quản lý giao hàng', icon: 'truck' },
];

export default function ManagementSidebar() {
  const { user } = useAuth();
  const role = user?.role?.toUpperCase();
  const isManager = role === ROLES.MANAGER || role === ROLES.ADMIN;

  const links = [
    ...LINKS,
    ...(isManager ? [{ to: '/management/settings', label: 'Cài đặt hệ thống', icon: 'gear' }] : []),
  ];

  return (
    <aside className="shop-sidebar manager-sidebar">
      <div className="eyebrow">QUẢN LÝ CỬA HÀNG</div>
      <Nav className="flex-column gap-2">
        {links.map(({ to, label, icon }) => (
          <Nav.Link as={NavLink} end={to === '/management'} to={to} key={to}>
            <span className="sidebar-icon">
              <Icon name={icon} />
            </span>
            <span>{label}</span>
          </Nav.Link>
        ))}
      </Nav>
      <div className="sidebar-tip">
        <strong>Cùng cửa hàng phát triển</strong>
        <p className="small mt-2 mb-0">Theo dõi doanh thu và chăm sóc từng đơn hàng.</p>
      </div>
    </aside>
  );
}
