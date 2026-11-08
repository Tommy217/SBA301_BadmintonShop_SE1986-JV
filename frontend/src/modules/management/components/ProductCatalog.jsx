import { Alert, Badge, Button, Nav } from 'react-bootstrap';
import { useSearchParams } from 'react-router';
import { useCatalog } from './CatalogContext';
import { formatMoney } from '@/shared/utils/format';
import CatalogSection from './CatalogSection';
import { optionLabels } from '@/modules/management/utils/managementUtils';
import { productSections } from '@/modules/management/utils/catalogNavigation';

export default function ProductManagementPage() {
  const { data } = useCatalog();
  const [params, setParams] = useSearchParams();
  const id = params.get('product');
  const product = data.products.find((p) => String(p.id) === id);
  const tab = Object.hasOwn(productSections, params.get('tab')) ? params.get('tab') : 'info';
  function select(row) {
    setParams({ product: row.id, tab: params.get('tab') || 'info' });
  }
  if (!product)
    return (
      <>
        {id && (
          <Alert variant="warning">
            Sản phẩm không còn tồn tại. Chọn sản phẩm khác trong danh sách.
          </Alert>
        )}
        {params.get('tab') && (
          <Alert variant="info">
            Chọn “Quản lý chi tiết” trên sản phẩm để quản lý {productSections[tab].toLowerCase()}{' '}
            của sản phẩm đó.
          </Alert>
        )}
        <CatalogSection
          catalog="products"
          onManage={select}
          onSaved={(result, savedId) => {
            if (!savedId && result.saved) select(result.saved);
          }}
        />
      </>
    );
  const variants = data.product_variants.filter((v) => String(v.product_id) === id);
  const images = (data.product_images || [])
    .filter((i) => String(i.product_id) === id)
    .sort((a, b) => a.sort_order - b.sort_order);
  const specTable = { racket: 'racket_specs', shoes: 'shoe_specs', apparel: 'apparel_specs' }[
    product.product_type
  ];
  const specs = (data[specTable] || []).filter((s) => String(s.product_id) === id);
  const historyCount = (data.product_price_history || []).filter((h) =>
    variants.some((v) => v.id === h.variant_id),
  ).length;
  const counts = {
    variants: variants.length,
    images: images.length,
    specs: specs.length,
    history: historyCount,
  };
  const scope = { product_id: product.id };
  return (
    <section className="manager-page">
      <div className="manager-title">
        <div>
          <div className="eyebrow">QUẢN LÝ SẢN PHẨM</div>
          <h1>{product.name}</h1>
          <p>
            {product.brand} · {optionLabels[product.product_type]}
          </p>
        </div>
        <Button className="app-back-button" variant="outline-primary" onClick={() => setParams({})}>
          ← Danh sách sản phẩm
        </Button>
      </div>
      <div className="manager-entity-summary">
        {images[0]?.url ? (
          <img src={images[0].url} alt={product.name} />
        ) : (
          <span className="text-secondary">Chưa có ảnh</span>
        )}
        <div>
          <Badge bg={product.is_active ? 'success' : 'secondary'}>
            {product.is_active ? 'Đang kinh doanh' : 'Tạm ẩn'}
          </Badge>
          <strong>{formatMoney(product.base_price)}</strong>
          <p>
            {variants.length} biến thể · Tổng tồn:{' '}
            {variants.reduce((sum, v) => sum + Number(v.stock_quantity), 0)} · {images.length} hình
            ảnh
          </p>
        </div>
      </div>
      <Nav variant="tabs" className="manager-entity-tabs" aria-label="Các phần quản lý sản phẩm">
        {Object.entries(productSections).map(([key, label]) => (
          <Nav.Item key={key}>
            <Nav.Link
              as="button"
              type="button"
              active={tab === key}
              onClick={() => setParams({ product: id, tab: key })}
            >
              {label}
              {counts[key] !== undefined && (
                <Badge bg="light" text="secondary" className="ms-2">
                  {counts[key]}
                </Badge>
              )}
            </Nav.Link>
          </Nav.Item>
        ))}
      </Nav>
      {tab === 'info' && (
        <CatalogSection
          key={id + '-info'}
          catalog="products"
          scope={{ id: product.id }}
          embedded
          allowCreate={false}
        />
      )}
      {tab === 'variants' && (
        <CatalogSection key={id + '-variants'} catalog="product_variants" scope={scope} embedded />
      )}
      {tab === 'images' && (
        <>
          <p className="small text-secondary">
            Ảnh mới tự gắn vào sản phẩm này. Bạn có thể gắn riêng cho một biến thể và sắp xếp thứ tự
            hiển thị.
          </p>
          <CatalogSection key={id + '-images'} catalog="product_images" scope={scope} embedded />
        </>
      )}
      {tab === 'specs' && (
        <CatalogSection
          key={id + '-specs'}
          catalog={specTable}
          scope={scope}
          embedded
          allowCreate={!specs.length}
        />
      )}
      {tab === 'history' && (
        <CatalogSection
          key={id + '-history'}
          catalog="product_price_history"
          scope={{ variant_id: variants.map((v) => v.id) }}
          embedded
          allowCreate={false}
        />
      )}
    </section>
  );
}
