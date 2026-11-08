import BrandLogo from '@/shared/components/BrandLogo';

import { Navbar, Button, Dropdown } from 'react-bootstrap';
import { Link } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { useShop } from '@/app/providers/shop/ShopContext';
import { getRole } from '@/shared/auth/session';
import { getRoleHomePath } from '@/shared/auth/rolePaths';
import './PublicHeader.css';

function HeaderIcon({ type }) {
  return (
    <svg
      width="22"
      height="22"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {type === 'cart' ? (
        <>
          <path d="M3 3h2l2.4 11.2a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 7H6" />
          <circle cx="10" cy="20" r="1" />
          <circle cx="18" cy="20" r="1" />
        </>
      ) : (
        <>
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21v-2a8 8 0 0 1 16 0v2" />
        </>
      )}
    </svg>
  );
}

export default function PublicHeader({ management = false }) {
  const shop = useShop();
  const { user, logout } = useAuth();
  const homePath = getRoleHomePath(getRole(user));
  const accountPath = homePath === '/' ? '/orders' : homePath;
  const accountLabel =
    homePath.startsWith('/admin')
      ? 'Quản lý tài khoản'
      : homePath === '/management'
        ? 'Dashboard quản lý'
        : 'Đơn hàng của tôi';
  const count = (shop?.data?.cart_items || []).reduce((sum, item) => sum + item.quantity, 0);

  return (
    <header className="store-header">
      <div className="shop-topbar">
        {management
          ? 'Badminton Shop · Không gian quản lý cửa hàng'
          : 'Đồng hành cùng đam mê cầu lông · Miễn phí giao hàng toàn quốc'}
      </div>
      <Navbar className="store-navbar">
        <BrandLogo to={homePath} />
        <div className="header-actions">
          <Button
            as={Link}
            to={management ? '/management/orders' : '/cart'}
            variant="light"
            className="header-cart"
            aria-label={management ? 'Đơn hàng khách hàng' : 'Giỏ hàng, ' + count + ' sản phẩm'}
          >
            <span className="header-icon-wrap">
              <HeaderIcon type="cart" />
              {!management && count > 0 && (
                <span className="header-cart-count">{count > 99 ? '99+' : count}</span>
              )}
            </span>
            <span className="header-action-label">{management ? 'Đơn hàng' : 'Giỏ hàng'}</span>
          </Button>
          <span className="header-action-divider" aria-hidden="true" />
          {user ? (
            <Dropdown align="end" className="header-account">
              <Dropdown.Toggle
                variant="light"
                className="header-account-toggle"
                aria-label="Mở menu tài khoản"
              >
                <span className="header-avatar">
                  {(user.fullName || user.email).trim().charAt(0).toUpperCase()}
                </span>
                <span className="header-user-copy">
                  <small>Xin chào,</small>
                  <strong>{user.fullName || user.email}</strong>
                </span>
              </Dropdown.Toggle>
              <Dropdown.Menu className="header-account-menu">
                <Dropdown.Item as={Link} to="/profile">
                  Hồ sơ cá nhân
                </Dropdown.Item>
                <Dropdown.Item as={Link} to={accountPath}>
                  {accountLabel}
                </Dropdown.Item>
                <Dropdown.Divider />
                <Dropdown.Item onClick={logout}>Đăng xuất</Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          ) : (
            <Button as={Link} to="/login" className="header-login" variant="primary">
              <HeaderIcon type="user" />
              <span>Đăng nhập</span>
            </Button>
          )}
        </div>
      </Navbar>
    </header>
  );
}
