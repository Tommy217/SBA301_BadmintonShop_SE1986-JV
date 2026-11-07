import ProductOptionsModal from '../components/ProductOptionsModal';
import { productApi } from '@/modules/management/api/catalog.api';
import { productImage, productImageFallback } from '../utils/productImages';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { cartApi } from '../api/shop.api';
import { getToken } from '@/shared/auth/session';
import Icon from '@/shared/components/Icon';
import '@/modules/shop/shop.css';
import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Modal, Pagination, Row } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatMoney } from '@/shared/utils/format';
export default function HomePage({ catalog = false }) {
  const { user } = useAuth();
  const { data, busy, act } = useShop(),
    [search, setSearch] = useState(''),
    [type, setType] = useState(''),
    [notice, setNotice] = useState(''),
    [preview, setPreview] = useState(null);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    productApi.getPage({ page, search, type }, { signal: controller.signal })
      .then((value) => { if (!controller.signal.aborted) { setResult(value); setLoadError(''); setLoading(false); } })
      .catch((error) => { if (!controller.signal.aborted) { setLoadError(error.message); setLoading(false); } });
    return () => controller.abort();
  }, [page, search, type, retry]);
  function changePage(next) { setLoading(true); setPage(next); }
  const navigate = useNavigate();
  const [purchase, setPurchase] = useState(null);
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
      throw new Error('Không thể thêm sản phẩm vào giỏ.');
    }
  }

  const products = result?.content || [];
  return (
    <div className="commerce-page catalog-page">
      {!catalog && (
        <>
          <section className="shop-hero mb-4">
            <div>
              <div className="eyebrow">FOR YOUR NEXT MATCH</div>
              <h1>
                Nâng tầm từng
                <br />
                đường cầu.
              </h1>
              <p>
                Vợt, giày và trang phục cầu lông.
                <br />
                Cùng bạn tự tin trong mỗi trận đấu.
              </p>
              <Button as={Link} to="/products">
                <Icon name="bag" size={18} /> Khám phá sản phẩm
              </Button>
            </div>
            <div className="hero-art" aria-hidden="true">
              <img src="/products/racket.svg" alt="" />
              <span>
                PLAY
                <br />
                YOUR BEST.
              </span>
            </div>
          </section>
          <Row className="g-3 mb-5">
            {[
              ['shield', 'Sản phẩm chính hãng'],
              ['check', 'Chọn đúng phong cách'],
              ['truck', 'Mua sắm thật dễ dàng'],
            ].map(([icon, text]) => (
              <Col md={4} key={icon}>
                <div className="shop-perk">
                  <Icon name={icon} />
                  <span>{text}</span>
                </div>
              </Col>
            ))}
          </Row>
        </>
      )}
      <div className="eyebrow">BỘ SƯU TẬP CẦU LÔNG</div>
      <h2 className="h3 fw-bold">{catalog ? 'Tất cả sản phẩm' : 'Sẵn sàng cho trận đấu mới'}</h2>
      <p className="text-secondary">Tìm người bạn đồng hành trên sân của bạn.</p>
      <Row className="g-3 mb-4 catalog-toolbar">
        <Col md={8}>
          <div className="catalog-search">
            <Icon name="search" />
            <Form.Control
              aria-label="Tìm sản phẩm"
              placeholder="Tìm tên sản phẩm…"
              value={search}
              onChange={(e) => { setLoading(true); setSearch(e.target.value); setPage(0); }}
            />
          </div>
        </Col>
        <Col md={4}>
          <Form.Select aria-label="Danh mục" value={type} onChange={(e) => { setLoading(true); setType(e.target.value); setPage(0); }}>
            <option value="">Tất cả danh mục</option>
            <option value="racket">Vợt cầu lông</option>
            <option value="shoes">Giày cầu lông</option>
            <option value="apparel">Trang phục</option>
          </Form.Select>
        </Col>
      </Row>
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice('')}>
          {notice} <Link to="/cart">Xem giỏ hàng</Link>
        </Alert>
      )}
      {loadError && <Alert variant="danger">{loadError} <Button onClick={() => { setLoading(true); setRetry((n) => n + 1); }}>Thử lại</Button></Alert>}
      {loading && <p role="status">Đang tải sản phẩm…</p>}
      {!loading && !loadError && !products.length && <div className="empty-state">Không tìm thấy sản phẩm phù hợp.</div>}
      <Row className="g-4">
        {!loading && !loadError && products.map((p) => {
          const options = data.product_variants.filter((v) => v.product_id === p.id && v.is_active),
            v = options.find((v) => v.stock_quantity > 0) || options[0];
          return (
            <Col xs={12} sm={6} xl={3} key={p.id}>
              <Card className="product-card h-100">
                <div className={'product-art art-' + p.product_type}>
                  <Badge bg="light" text="primary">
                    {p.brand}
                  </Badge>
                  <button type="button" className="product-image-button" onClick={() => setPreview({ product: p, variantId: v?.id })} aria-label={'Phóng to ảnh ' + p.name}>
                    <img src={productImage(data, p, v?.id)} onError={productImageFallback} alt={p.name} loading="lazy" />
                    <span className="product-image-hint"><Icon name="search" size={16} /> Xem ảnh lớn</span>
                  </button>
                </div>
                <Card.Body className="d-flex flex-column">
                  <Card.Title as="h3" className="h6 fw-bold mt-2"><Link className="product-name-link" to={'/products/' + p.id}>{p.name}</Link></Card.Title>
                  <div className="product-price-row">
                    <strong className="text-primary">{formatMoney(v?.price ?? p.base_price)}</strong>
                    <Badge bg={v?.stock_quantity ? 'success' : 'secondary'}>{v?.stock_quantity ? 'Còn ' + v.stock_quantity + ' sản phẩm' : 'Hết hàng'}</Badge>
                  </div>
                  <div className="product-actions mt-auto">
                    <Button variant="outline-primary" className="icon-button" disabled={busy || !v?.stock_quantity} aria-label={'Thêm ' + p.name + ' vào giỏ'} title="Thêm vào giỏ" onClick={() => setPurchase({ product: p, buy: false })}><Icon name="cart" size={18} /></Button>
                    <Button className="icon-button" disabled={busy || !v?.stock_quantity} onClick={() => setPurchase({ product: p, buy: true })}><Icon name="bag" size={18} />Đặt hàng</Button>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          );
        })}
      </Row>
      {result?.totalPages > 1 && !loadError && <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mt-4">
        <small className="text-secondary">Trang {page + 1} / {result.totalPages} · {result.totalElements} sản phẩm</small>
        <Pagination className="mb-0" aria-label="Phân trang sản phẩm">
          <Pagination.Prev disabled={loading || page === 0} onClick={() => changePage(page - 1)} />
          {Array.from({ length: result.totalPages }, (_, index) => <Pagination.Item key={index} active={page === index} disabled={loading} onClick={() => changePage(index)}>{index + 1}</Pagination.Item>)}
          <Pagination.Next disabled={loading || page >= result.totalPages - 1} onClick={() => changePage(page + 1)} />
        </Pagination>
      </div>}
      {purchase && <ProductOptionsModal product={purchase.product} data={data} buy={purchase.buy} busy={busy} onHide={() => setPurchase(null)} onConfirm={add} />}
      <Modal show={Boolean(preview)} onHide={() => setPreview(null)} centered size="lg">
        <Modal.Header closeButton><Modal.Title className="h5">{preview?.product.name}</Modal.Title></Modal.Header>
        <Modal.Body className="product-preview">
          {preview && <img src={productImage(data, preview.product, preview.variantId)} onError={productImageFallback} alt={preview.product.name} />}
        </Modal.Body>
      </Modal>
    </div>
  );
}
