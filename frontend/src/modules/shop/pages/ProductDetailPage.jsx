import { useRef, useState } from 'react';
import { Alert, Badge, Button, Col, Form, Modal, Row, Table } from 'react-bootstrap';
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
  const [choice, setChoice] = useState('');
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
  const variant = variants.find((v) => String(v.id) === choice);
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
    <Link to="/products" className="d-inline-block mb-4">← Danh sách sản phẩm</Link>
    {notice && <Alert variant="success" dismissible onClose={() => setNotice('')}>{notice} <Link to="/cart">Xem giỏ hàng</Link></Alert>}
    <Row className="g-4">
      <Col md={6}><button className="detail-image-button" type="button" onClick={() => setPreview(true)} aria-label={'Phóng to ảnh ' + product.name}><img src={productImage(data, product, variant?.id)} onError={productImageFallback} alt={product.name} /></button></Col>
      <Col md={6}>
        <Badge bg="light" text="primary">{product.brand}</Badge>
        <h1 className="h3 mt-3">{product.name}</h1>
        <p className="h3 text-primary my-4">{formatMoney(variant?.price ?? product.base_price)}</p>
        <p className="product-detail-description">{product.description || 'Chưa có mô tả sản phẩm.'}</p>
        <Form.Label htmlFor="product-variant">{product.product_type === 'racket' ? 'Màu sắc / Trọng lượng - Cỡ cán' : 'Màu sắc / Kích cỡ'}</Form.Label>
        <Form.Select id="product-variant" value={variant?.id ?? ''} disabled={busy || !variants.length} onChange={(e) => { setChoice(e.target.value); setQuantity(1); }}>
          <option value="">{variants.length ? 'Chọn màu sắc / kích cỡ' : 'Chưa có biến thể'}</option>
          {variants.map((v) => <option key={v.id} value={v.id} disabled={v.stock_quantity <= 0}>{v.color} / {v.size}{v.stock_quantity <= 0 ? ' — Hết hàng' : ''}</option>)}
        </Form.Select>
        <p className="text-secondary mt-3">{variant ? (variant.stock_quantity > 0 ? 'Còn ' + variant.stock_quantity + ' sản phẩm' : 'Hết hàng') : 'Vui lòng chọn phân loại sản phẩm.'}</p>
        <div className="purchase-quantity-row">
          <Form.Label htmlFor="detail-quantity">Số lượng</Form.Label>
          <div className="purchase-stepper">
            <button type="button" aria-label="Giảm số lượng" disabled={busy || !variant || selectedQuantity <= 1} onClick={() => setQuantity(selectedQuantity - 1)}>−</button>
            <input id="detail-quantity" type="number" min="1" max={variant?.stock_quantity || 1} step="1" value={selectedQuantity} disabled={busy || !variant || variant.stock_quantity <= 0} onChange={(event) => setQuantity(Math.min(variant?.stock_quantity || 1, Math.max(1, Math.floor(Number(event.target.value) || 1))))} />
            <button type="button" aria-label="Tăng số lượng" disabled={busy || !variant || selectedQuantity >= variant.stock_quantity} onClick={() => setQuantity(selectedQuantity + 1)}>+</button>
          </div>
        </div>
        {variant && <p className="mt-3">Tạm tính: <strong>{formatMoney(variant.price * selectedQuantity)}</strong></p>}
        <div className="d-flex flex-wrap gap-2 mt-4">
          <Button variant="outline-primary" disabled={busy || !variant || variant.stock_quantity <= 0} onClick={() => purchase(false)}>Thêm vào giỏ</Button>
          <Button disabled={busy || !variant || variant.stock_quantity <= 0} onClick={() => purchase(true)}>Đặt hàng</Button>
        </div>
      </Col>
    </Row>
    <section className="mt-5"><h2 className="h4">Thông số sản phẩm</h2>
      {specs.length ? <Table striped responsive className="mt-3"><tbody>{specs.map(({ key, label }) => <tr key={key}><th scope="row">{label}{key === 'shaft_diameter' ? ' (mm)' : ''}</th><td>{optionLabels[spec[key]] || spec[key]}</td></tr>)}</tbody></Table> : <p className="text-secondary">Thông số đang được cập nhật.</p>}
      <h3 className="h5 mt-4">Thông số các biến thể</h3>
      <Table striped responsive><thead><tr><th>Màu sắc</th><th>{product.product_type === 'racket' ? 'Trọng lượng - Cỡ cán' : 'Kích cỡ'}</th><th>SKU</th><th>Giá</th><th>Tồn kho</th></tr></thead><tbody>{variants.map((v) => <tr key={v.id}><td>{v.color || 'Không phân màu'}</td><td>{v.size || 'Một kích cỡ'}</td><td>{v.sku}</td><td>{formatMoney(v.price)}</td><td>{v.stock_quantity > 0 ? v.stock_quantity : 'Hết hàng'}</td></tr>)}</tbody></Table>
    </section>
    <Modal show={preview} onHide={() => setPreview(false)} centered size="lg"><Modal.Header closeButton><Modal.Title className="h5">{product.name}</Modal.Title></Modal.Header><Modal.Body className="product-preview"><img src={productImage(data, product, variant?.id)} onError={productImageFallback} alt={product.name} /></Modal.Body></Modal>
  </div>;
}
