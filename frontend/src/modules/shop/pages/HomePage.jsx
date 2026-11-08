import ProductCard from '../components/ProductCard';
import ShopPagination from '../components/ShopPagination';
import ProductOptionsModal from '../components/ProductOptionsModal';
import { productApi } from '@/modules/management/api/catalog.api';
import { productImage, productImageFallback } from '../utils/productImages';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { cartApi } from '../api/shop.api';
import { getToken } from '@/shared/auth/session';
import Icon from '@/shared/components/Icon';
import '@/modules/shop/shop.css';
import { useEffect, useState } from 'react';
import { Alert, Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
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
  const [sort, setSort] = useState('default');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [priceRange, setPriceRange] = useState({});
  const [priceError, setPriceError] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    productApi.getPage({ page, search, type, sort, ...priceRange }, { signal: controller.signal })
      .then((value) => { if (!controller.signal.aborted) { setResult(value); setLoadError(''); setLoading(false); } })
      .catch((error) => { if (!controller.signal.aborted) { setLoadError(error.message); setLoading(false); } });
    return () => controller.abort();
  }, [page, search, type, sort, priceRange, retry]);
  function changePage(next) { setLoading(true); setPage(next); }
  function applyPrice(event) {
    event.preventDefault();
    if (minPrice !== '' && maxPrice !== '' && Number(minPrice) > Number(maxPrice)) {
      setPriceError('Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu.');
      return;
    }
    setPriceError('');
    setLoading(true);
    setPage(0);
    setPriceRange({ minPrice: minPrice || undefined, maxPrice: maxPrice || undefined });
  }
  function resetFilters() {
    setSearch(''); setType(''); setSort('default');
    setMinPrice(''); setMaxPrice(''); setPriceRange({}); setPriceError('');
    setLoading(true); setPage(0);
  }
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
        <Col md={6}>
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
        <Col md={3}>
          <Form.Select aria-label="Danh mục" value={type} onChange={(e) => { setLoading(true); setType(e.target.value); setPage(0); }}>
            <option value="">Tất cả danh mục</option>
            <option value="racket">Vợt cầu lông</option>
            <option value="shoes">Giày cầu lông</option>
            <option value="apparel">Trang phục</option>
          </Form.Select>
        </Col>
        <Col md={3}>
          <Form.Select aria-label="Sắp xếp sản phẩm" value={sort} onChange={(e) => { setLoading(true); setSort(e.target.value); setPage(0); }}>
            <option value="default">Sắp xếp mặc định</option>
            <option value="price_asc">Giá: thấp đến cao</option>
            <option value="price_desc">Giá: cao đến thấp</option>
            <option value="name_asc">Tên: A–Z</option>
            <option value="name_desc">Tên: Z–A</option>
          </Form.Select>
        </Col>
      </Row>
      <Form onSubmit={applyPrice} className="catalog-price-filter mb-4">
        <div>
          <Form.Label htmlFor="catalog-min-price">Giá từ (₫)</Form.Label>
          <Form.Control id="catalog-min-price" type="number" min="0" step="1" placeholder="Không giới hạn" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} />
        </div>
        <div>
          <Form.Label htmlFor="catalog-max-price">Đến (₫)</Form.Label>
          <Form.Control id="catalog-max-price" type="number" min="0" step="1" placeholder="Không giới hạn" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} />
        </div>
        <Button type="submit">Áp dụng giá</Button>
        <Button type="button" variant="outline-secondary" onClick={resetFilters}>Xóa bộ lọc</Button>
        {priceError && <p className="text-danger mb-0 catalog-price-error" role="alert">{priceError}</p>}
        {(priceRange.minPrice !== undefined || priceRange.maxPrice !== undefined) && <p className="text-secondary mb-0 catalog-price-error" role="status">
          Khoảng giá đang áp dụng: {Number(priceRange.minPrice || 0).toLocaleString('vi-VN')} ₫ – {priceRange.maxPrice === undefined ? 'Không giới hạn' : `${Number(priceRange.maxPrice).toLocaleString('vi-VN')} ₫`}
        </p>}
      </Form>
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice('')}>
          {notice} <Link to="/cart">Xem giỏ hàng</Link>
        </Alert>
      )}
      {loadError && <Alert variant="danger">{loadError} <Button onClick={() => { setLoading(true); setRetry((n) => n + 1); }}>Thử lại</Button></Alert>}
      {loading && <p role="status">Đang tải sản phẩm…</p>}
      {!loading && !loadError && !products.length && <div className="empty-state">Không tìm thấy sản phẩm phù hợp.</div>}
      <Row className="g-3">
        {!loading && !loadError && products.map((product) => <Col xs={6} md={6} lg={4} xl={3} key={product.id}>
          <ProductCard product={product} data={data} busy={busy} onPreview={setPreview} onPurchase={setPurchase} />
        </Col>)}
      </Row>
      {!loadError && <ShopPagination page={page + 1} totalPages={result?.totalPages || 0} total={result?.totalElements || 0} noun="sản phẩm" disabled={loading} onChange={(next) => changePage(next - 1)} />}
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
