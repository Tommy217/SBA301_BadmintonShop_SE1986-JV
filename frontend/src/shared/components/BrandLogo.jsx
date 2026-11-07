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
        <img src="/brand-mark.svg?v=4" width="48" height="48" alt="" />
      </span>
      <span className="brand-logo-copy">
        <strong>badminton<span>shop.</span></strong>
        {!compact && <small>ĐỒNG HÀNH MỖI TRẬN ĐẤU</small>}
      </span>
    </Link>
  );
}
