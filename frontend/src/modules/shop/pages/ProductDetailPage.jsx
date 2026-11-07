import { useRef, useState } from 'react';
import { Alert, Col, Form, Modal, Row, Table } from 'react-bootstrap';
import { Link, useNavigate, useParams } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { useShop } from '@/app/providers/shop/ShopContext';
import { getToken } from '@/shared/auth/session';
import { formatMoney } from '@/shared/utils/format';
import { catalogs } from '@/modules/management/constants';
import { optionLabels } from '@/modules/management/utils/managementUtils';
import { productImage, productImageFallback } from '../utils/productImages';
import { isDemoProduct } from '../utils/catalogProducts';
import { cartApi } from '../api/shop.api';
import '@/modules/shop/shop.css';

export default function ProductDetailPage() {
  const { productId } = useParams();
  const { user } = useAuth();
  const { data, busy, act } = useShop();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);
  const lock = useRef(false);
  const [color, setColor] = useState('');
  const [size, setSize] = useState('');
  const [notice, setNotice] = useState('');
  const [preview, setPreview] = useState(false);
  async function add(id, buy, quantity = 1) {
    if (!getToken(user)) {
      navigate('/login', {
        state: {
          from: {
            pathname: buy ? '/order' : '/cart',
            search: buy ? '?variant=' + encodeURIComponent(id) + '&quantity=' + quantity : '',
          },
          message: 'Vui lòng đăng nhập để mua hàng.',
        },
      });
      return;
    }
    if (buy) {
      navigate('/order?variant=' + encodeURIComponent(id) + '&quantity=' + quantity);
      return;
    }
    try {
      await act(() => cartApi.addItem(id, quantity));
      setNotice('Đã thêm sản phẩm vào giỏ hàng.');
    } catch {
      /* Provider displays the error. */
    }
  }


  const product = data.products.find((p) => String(p.id) === productId && p.is_active && !isDemoProduct(p) &&
    !(data.brands || []).some((b) => b.id === p.brand_id && !b.is_active) &&
    !(data.categories || []).some((c) => c.id === p.category_id && !c.is_active));
  if (!product) return <div className="empty-state">Không tìm thấy sản phẩm. <Link to="/products">Về danh sách</Link></div>;
  const variants = data.product_variants.filter((v) => v.product_id === product.id && v.is_active);
  const colorOf = (v) => v.color || 'Không phân màu';
  const sizeOf = (v) => v.size || 'Một kích cỡ';
  const colors = [...new Set(variants.map(colorOf))];
  const sizes = [...new Set(variants.map(sizeOf))];
  const available = variants.filter((v) => v.stock_quantity > 0);
  const variant = variants.find((v) => colorOf(v) === color && sizeOf(v) === size);
  function chooseColor(value) {
    setColor(value);
    setQuantity(1);
    if (!available.some((v) => colorOf(v) === value && sizeOf(v) === size)) setSize('');
  }
  const selectedQuantity = Math.min(quantity, variant?.stock_quantity || 1);
  async function purchase(buy) {
    if (busy || lock.current || !variant || variant.stock_quantity <= 0) return;
    lock.current = true;
    try { await add(variant.id, buy, selectedQuantity); }
    finally { lock.current = false; }
  }
  const specTable = { racket: 'racket_specs', shoes: 'shoe_specs', apparel: 'apparel_specs' }[product.product_type];
  const spec = (data[specTable] || []).find((row) => row.product_id === product.id);
  const specs = (catalogs[specTable]?.fields || []).filter((field) => field.key !== 'product_id' && spec?.[field.key] != null && spec[field.key] !== '');
  return <div className="commerce-page product-detail-page">
    <Link to="/products" className="detail-back-button"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m12 5-7 7 7 7M5 12h14" /></svg><span>Danh sách sản phẩm</span></Link>
    {notice && <Alert variant="success" dismissible onClose={() => setNotice('')}>{notice} <Link to="/cart">Xem giỏ hàng</Link></Alert>}
    <Row className="g-4">
      <Col md={6}><button className="detail-image-button" type="button" onClick={() => setPreview(true)} aria-label={'Phóng to ảnh ' + product.name}><img src={productImage(data, product, variant?.id)} onError={productImageFallback} alt={product.name} /></button></Col>
      <Col md={6} className="detail-purchase-panel">
        <span className="detail-brand">{product.brand}</span>
        <h1 className="detail-title">{product.name}</h1>
        <p className="detail-price">{formatMoney(variant?.price ?? product.base_price)}</p>
        <p className="product-detail-description">{product.description || 'Chưa có mô tả sản phẩm.'}</p>
        <div className="purchase-selection" aria-live="polite">{variant ? 'Đã chọn: ' + color + ' / ' + size + ' · Còn ' + variant.stock_quantity + ' sản phẩm' : available.length ? 'Vui lòng chọn màu sắc và kích cỡ' : 'Sản phẩm tạm hết hàng'}</div>
        <fieldset className="purchase-group" disabled={busy}>
          <legend>Màu sắc {color && <span>{color}</span>}</legend>
          <div className="purchase-options">{colors.map((value) => {
            const enabled = available.some((v) => colorOf(v) === value);
            const imageVariant = variants.find((v) => colorOf(v) === value);
            return <button type="button" key={value} className={'purchase-chip purchase-color' + (color === value ? ' is-selected' : '')} aria-pressed={color === value} disabled={!enabled} onClick={() => chooseColor(value)}>
              <img src={productImage(data, product, imageVariant?.id)} onError={productImageFallback} alt="" /><span>{value}{!enabled && <small>Hết hàng</small>}</span>
            </button>;
          })}</div>
        </fieldset>
        <fieldset className="purchase-group" disabled={busy}>
          <legend>{product.product_type === 'racket' ? 'Trọng lượng / Cỡ cán' : 'Kích cỡ'} {size && <span>{size}</span>}</legend>
          <div className="purchase-options">{sizes.map((value) => <button type="button" key={value} className={'purchase-chip' + (size === value ? ' is-selected' : '')} aria-pressed={size === value} disabled={!available.some((v) => sizeOf(v) === value && (!color || colorOf(v) === color))} onClick={() => { setSize(value); setQuantity(1); }}>{value}</button>)}</div>
        </fieldset>
        {product.product_type === 'racket' && <p className="purchase-hint">U: nhóm trọng lượng vợt · G: kích cỡ cán vợt</p>}
        <div className="purchase-quantity-row">
          <Form.Label htmlFor="detail-quantity">Số lượng</Form.Label>
          <div className="purchase-stepper">
            <button type="button" aria-label="Giảm số lượng" disabled={busy || !variant || selectedQuantity <= 1} onClick={() => setQuantity(selectedQuantity - 1)}>−</button>
            <input id="detail-quantity" type="number" min="1" max={variant?.stock_quantity || 1} step="1" value={selectedQuantity} disabled={busy || !variant || variant.stock_quantity <= 0} onChange={(event) => setQuantity(Math.min(variant?.stock_quantity || 1, Math.max(1, Math.floor(Number(event.target.value) || 1))))} />
            <button type="button" aria-label="Tăng số lượng" disabled={busy || !variant || selectedQuantity >= variant.stock_quantity} onClick={() => setQuantity(selectedQuantity + 1)}>+</button>
          </div>
        </div>
        <div className="purchase-total detail-total"><span>Tạm tính · {selectedQuantity} sản phẩm</span><strong>{formatMoney((variant?.price ?? product.base_price) * selectedQuantity)}</strong></div>
        <div className="detail-purchase-actions">
          <button type="button" className="detail-cart-button" disabled={busy || !variant || variant.stock_quantity <= 0} onClick={() => purchase(false)}>Thêm vào giỏ</button>
          <button type="button" className="purchase-confirm" disabled={busy || !variant || variant.stock_quantity <= 0} onClick={() => purchase(true)}>Đặt hàng</button>
        </div>
      </Col>
    </Row>
    <section className="detail-specs"><h2 className="h4">Thông số sản phẩm</h2>
      {specs.length ? <Table striped responsive className="mt-3"><tbody>{specs.map(({ key, label }) => <tr key={key}><th scope="row">{label}{key === 'shaft_diameter' ? ' (mm)' : ''}</th><td>{optionLabels[spec[key]] || spec[key]}</td></tr>)}</tbody></Table> : <p className="text-secondary">Thông số đang được cập nhật.</p>}
      <h3 className="h5 mt-4">Thông số các biến thể</h3>
      <Table responsive className="detail-variants-table"><thead><tr><th>Màu sắc</th><th>{product.product_type === 'racket' ? 'Trọng lượng - Cỡ cán' : 'Kích cỡ'}</th><th>SKU</th><th>Giá</th><th>Tồn kho</th></tr></thead><tbody>{variants.map((v) => <tr key={v.id}><td>{v.color || 'Không phân màu'}</td><td>{v.size || 'Một kích cỡ'}</td><td>{v.sku}</td><td>{formatMoney(v.price)}</td><td>{v.stock_quantity > 0 ? v.stock_quantity : 'Hết hàng'}</td></tr>)}</tbody></Table>
    </section>
    <Modal show={preview} onHide={() => setPreview(false)} centered size="lg"><Modal.Header closeButton><Modal.Title className="h5">{product.name}</Modal.Title></Modal.Header><Modal.Body className="product-preview"><img src={productImage(data, product, variant?.id)} onError={productImageFallback} alt={product.name} /></Modal.Body></Modal>
  </div>;
}
