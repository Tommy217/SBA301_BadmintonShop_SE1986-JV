import { Button } from 'react-bootstrap';
import { Link } from 'react-router';
import Icon from '@/shared/components/Icon';
import { formatMoney } from '@/shared/utils/format';
import { productImage, productImageFallback } from '../utils/productImages';
import '../product-cards.css';

export default function ProductCard({ product, data, busy, onPreview, onPurchase }) {
  const variants = data.product_variants.filter((v) => String(v.product_id) === String(product.id) && v.is_active);
  const available = variants.filter((v) => v.stock_quantity > 0);
  const displayVariant = available[0] || variants[0];
  const prices = (available.length ? available : variants).map((v) => Number(v.price));
  const price = prices.length ? Math.min(...prices) : product.base_price;
  const stock = available.reduce((sum, v) => sum + Number(v.stock_quantity), 0);
  const typeLabel = { racket: 'Vợt cầu lông', shoes: 'Giày cầu lông', apparel: 'Trang phục' }[product.product_type];
  return <article className={'catalog-product' + (!stock ? ' is-sold-out' : '')}>
    <div className="catalog-product-media">
      {product.brand && <span className="catalog-product-brand">{product.brand}</span>}
      <button type="button" className="catalog-product-photo" aria-label={'Phóng to ảnh ' + product.name} onClick={() => onPreview({ product, variantId: displayVariant?.id })}>
        <img src={productImage(data, product, displayVariant?.id)} onError={productImageFallback} alt={product.name} loading="lazy" />
        <span className="catalog-product-zoom" aria-hidden="true"><Icon name="search" size={15} /></span>
      </button>
      {!stock && <span className="catalog-product-unavailable">Tạm hết hàng</span>}
    </div>
    <div className="catalog-product-body">
      <span className="catalog-product-category">{typeLabel || 'Phụ kiện cầu lông'}</span>
      <h3 className="catalog-product-title"><Link to={'/products/' + product.id}>{product.name}</Link></h3>
      <div className="catalog-product-price">{new Set(prices).size > 1 && <small>Từ</small>}<strong>{formatMoney(price)}</strong></div>
      <div className="catalog-product-meta"><span className={'catalog-product-stock' + (!stock ? ' is-empty' : '')}>{stock ? 'Còn hàng' : 'Hết hàng'}</span><span>{variants.length} phân loại</span></div>
      <div className="catalog-product-actions">
        <Button type="button" variant="light" className="catalog-product-cart" disabled={busy || !stock} aria-label={'Thêm ' + product.name + ' vào giỏ'} title="Thêm vào giỏ" onClick={() => onPurchase({ product, buy: false })}><Icon name="cart" size={18} /></Button>
        <Button type="button" className="catalog-product-buy" disabled={busy || !stock} onClick={() => onPurchase({ product, buy: true })}>{stock ? 'Mua ngay' : 'Tạm hết hàng'}<span aria-hidden="true">↗</span></Button>
      </div>
    </div>
  </article>;
}
