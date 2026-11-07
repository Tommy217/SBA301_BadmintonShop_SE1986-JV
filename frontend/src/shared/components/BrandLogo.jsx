import { Link } from 'react-router';
import './BrandLogo.css';

export default function BrandLogo({ compact = false, to = '/' }) {
  return (
    <Link
      to={to}
      className={'brand-logo' + (compact ? ' brand-logo-compact' : '')}
      aria-label="Badminton Shop — Trang chủ"
    >
      <span className="brand-logo-symbol" aria-hidden="true">
        <svg
          width="30"
          height="30"
          viewBox="0 0 32 32"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinecap="round"
          strokeLinejoin="round"
          focusable="false"
        >
          <path d="m10 21-5-12 7-4 8-2 6 6-7 15M5 9l11 15M12 5l6 18M20 3l-1 20M26 9 10 21" />
          <path d="m10 21 9 3-3 5c-1 2-4 2-6 0s-2-5 0-8Z" fill="currentColor" stroke="none" />
        </svg>
      </span>
      <span className="brand-logo-copy">
        <strong>
          BADMINTON<span>SHOP</span>
        </strong>
        {!compact && <small>ĐỒNG HÀNH MỖI TRẬN ĐẤU</small>}
      </span>
    </Link>
  );
}
