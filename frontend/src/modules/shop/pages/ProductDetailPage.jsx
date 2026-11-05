import { useState } from 'react';
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
  const [choice, setChoice] = useState('');
  const [notice, setNotice] = useState('');
  const [preview, setPreview] = useState(false);
  async function add(id, buy) {
    if (!getToken(user)) {
      navigate('/login', {
        state: {
          from: {
            pathname: buy ? '/order' : '/cart',
            search: buy ? '?variant=' + encodeURIComponent(id) : '',
          },
          message: 'Vui lòng đăng nhập để mua hàng.',
        },
      });
      return;
    }
    if (buy) {
      navigate('/order?variant=' + encodeURIComponent(id));
      return;
    }
    try {
      await act(() => cartApi.addItem(id));
      setNotice('Đã thêm sản phẩm vào giỏ hàng.');
    } catch {
      /* Error shown by provider. */
    }
  }


  const product = data.products.find((p) => String(p.id) === productId && p.is_active && !isDemoProduct(p) &&
    !(data.brands || []).some((b) => b.id === p.brand_id && !b.is_active) &&
    !(data.categories || []).some((c) => c.id === p.category_id && !c.is_active));
  if (!product) return <div className="empty-state">Không tìm thấy sản phẩm. <Link to="/products">Về danh sách</Link></div>;
  const variants = data.product_variants.filter((v) => v.product_id === product.id && v.is_active);
  const variant = variants.find((v) => String(v.id) === choice) || variants.find((v) => v.stock_quantity > 0) || variants[0];
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
        <Form.Label htmlFor="product-variant">Màu sắc / Kích cỡ</Form.Label>
        <Form.Select id="product-variant" value={variant?.id ?? ''} disabled={!variants.length} onChange={(e) => setChoice(e.target.value)}>
          {!variants.length && <option value="">Chưa có biến thể</option>}
          {variants.map((v) => <option key={v.id} value={v.id}>{v.color} / {v.size}</option>)}
        </Form.Select>
        <p className="text-secondary mt-3">{variant?.stock_quantity ? 'Còn ' + variant.stock_quantity + ' sản phẩm' : 'Hết hàng'}</p>
        <div className="d-flex flex-wrap gap-2 mt-4">
          <Button variant="outline-primary" disabled={busy || !variant?.stock_quantity} onClick={() => add(variant.id, false)}>Thêm vào giỏ</Button>
          <Button disabled={busy || !variant?.stock_quantity} onClick={() => add(variant.id, true)}>Đặt hàng</Button>
        </div>
      </Col>
    </Row>
    <section className="mt-5"><h2 className="h4">Thông số sản phẩm</h2>
      {specs.length ? <Table striped responsive className="mt-3"><tbody>{specs.map(({ key, label }) => <tr key={key}><th scope="row">{label}</th><td>{optionLabels[spec[key]] || spec[key]}</td></tr>)}</tbody></Table> : <p className="text-secondary">Thông số đang được cập nhật.</p>}
    </section>
    <Modal show={preview} onHide={() => setPreview(false)} centered size="lg"><Modal.Header closeButton><Modal.Title className="h5">{product.name}</Modal.Title></Modal.Header><Modal.Body className="product-preview"><img src={productImage(data, product, variant?.id)} onError={productImageFallback} alt={product.name} /></Modal.Body></Modal>
  </div>;
}
