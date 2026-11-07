import './shop-pagination.css';

export default function ShopPagination({ page, totalPages, total, noun, onChange, disabled = false }) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, index) => index + 1)
    .filter((number) => number === 1 || number === totalPages || Math.abs(number - page) <= 1);

  return <div className="shop-pagination">
    <small>Trang {page} / {totalPages} · {total} {noun}</small>
    <nav aria-label={'Phân trang ' + noun}>
      <button type="button" disabled={disabled || page <= 1} onClick={() => onChange(page - 1)} aria-label="Trang trước">‹</button>
      {pages.map((number, index) => <span className="shop-pagination-slot" key={number}>
        {index > 0 && number - pages[index - 1] > 1 && <span className="shop-pagination-gap" aria-hidden="true">…</span>}
        <button type="button" className={number === page ? 'is-active' : ''} aria-current={number === page ? 'page' : undefined} aria-label={'Trang ' + number} disabled={disabled} onClick={() => onChange(number)}>{number}</button>
      </span>)}
      <button type="button" disabled={disabled || page >= totalPages} onClick={() => onChange(page + 1)} aria-label="Trang sau">›</button>
    </nav>
  </div>;
}
