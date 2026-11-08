import { Button, Form } from 'react-bootstrap';
import Icon from './Icon';

const paginationStyles = {
  container: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '16px',
    marginTop: '20px',
    padding: '12px 18px',
    backgroundColor: '#ffffff',
    borderRadius: '12px',
    border: '1px solid #e2e8f0',
    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
  },
  resultCount: {
    fontSize: '13px',
    color: '#64748b',
  },
  nav: {
    display: 'flex',
    gap: '6px',
    alignItems: 'center',
  },
  getBtnStyle: (isActive) => ({
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: '34px',
    height: '34px',
    padding: '0 10px',
    borderRadius: '8px',
    fontSize: '13px',
    fontWeight: isActive ? 600 : 500,
    border: isActive ? '1px solid #2386c8' : '1px solid #e2e8f0',
    backgroundColor: isActive ? '#2386c8' : '#ffffff',
    color: isActive ? '#ffffff' : '#334155',
    boxShadow: isActive ? '0 2px 4px rgba(35, 134, 200, 0.25)' : 'none',
    transition: 'all 0.15s ease-in-out',
  }),
  dots: {
    color: '#94a3b8',
    padding: '0 4px',
    fontSize: '13px',
  },
};

export function ManagerSearch({ value, onChange, placeholder = 'Tìm kiếm…', label = 'Tìm kiếm' }) {
  return (
    <div className="manager-search">
      <Icon name="search" size={19} />
      <Form.Control
        type="search"
        aria-label={label}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}

export function ManagerPagination({ total, page, pageSize = 10, onChange }) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  const current = Math.min(page, pages);
  const start = Math.max(1, Math.min(current - 2, pages - 4));
  const numbers = Array.from({ length: Math.min(5, pages) }, (_, i) => start + i);

  return (
    <div className="manager-pagination" style={paginationStyles.container}>
      <span className="manager-result-count" style={paginationStyles.resultCount}>
        Hiển thị{' '}
        <strong className="text-dark">
          {total ? (current - 1) * pageSize + 1 : 0}–{Math.min(current * pageSize, total)}
        </strong>{' '}
        trong <strong className="text-dark">{total}</strong> kết quả
      </span>
      <nav aria-label="Phân trang danh sách" style={paginationStyles.nav}>
        <Button
          variant="light"
          disabled={current === 1}
          style={paginationStyles.getBtnStyle(false)}
          onClick={() => onChange(current - 1)}
          aria-label="Trang trước"
        >
          ‹
        </Button>
        {start > 1 && (
          <>
            <Button
              variant="light"
              style={paginationStyles.getBtnStyle(false)}
              onClick={() => onChange(1)}
            >
              1
            </Button>
            {start > 2 && <span style={paginationStyles.dots}>…</span>}
          </>
        )}
        {numbers.map((number) => (
          <Button
            key={number}
            variant={number === current ? 'primary' : 'light'}
            style={paginationStyles.getBtnStyle(number === current)}
            aria-label={'Trang ' + number}
            aria-current={number === current ? 'page' : undefined}
            onClick={() => onChange(number)}
          >
            {number}
          </Button>
        ))}
        {numbers.at(-1) < pages && (
          <>
            {numbers.at(-1) < pages - 1 && <span style={paginationStyles.dots}>…</span>}
            <Button
              variant="light"
              style={paginationStyles.getBtnStyle(false)}
              onClick={() => onChange(pages)}
            >
              {pages}
            </Button>
          </>
        )}
        <Button
          variant="light"
          disabled={current === pages}
          style={paginationStyles.getBtnStyle(false)}
          onClick={() => onChange(current + 1)}
          aria-label="Trang tiếp theo"
        >
          ›
        </Button>
      </nav>
    </div>
  );
}
