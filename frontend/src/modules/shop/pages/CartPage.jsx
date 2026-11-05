import { productImage, productImageFallback } from '../utils/productImages';
import { Button, Card, Col, Form, Row } from 'react-bootstrap';
import { Link } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatMoney } from '@/shared/utils/format';
import Icon from '@/shared/components/Icon';
import QuantityControl from '@/shared/components/QuantityControl';
import '@/modules/shop/shop.css';
import { cartApi } from '../api/shop.api';

export default function CartPage() {
  const { data, busy, act } = useShop();
  const change = (request) => act(request).catch(() => {});
  const selected = data.cart_items.filter((i) => i.is_selected);
  const subtotal = selected.reduce(
    (sum, i) => sum + data.product_variants.find((v) => v.id === i.variant_id).price * i.quantity,
    0,
  );
  const shipping = !subtotal || subtotal >= 2000000 ? 0 : 30000;
  return (
    <div className="commerce-page cart-page">
      <div className="commerce-heading">
        <div>
          <div className="eyebrow">MUA SẮM CỦA BẠN</div>
          <h1>Giỏ hàng</h1>
          <p>Những lựa chọn cho trận đấu tiếp theo.</p>
        </div>
        <span className="item-count">{data.cart_items.length} sản phẩm</span>
      </div>
      {!data.cart_items.length ? (
        <div className="empty-state">
          <span className="empty-icon">
            <Icon name="cart" size={32} />
          </span>
          <h2 className="h5 mt-3">Giỏ hàng đang trống</h2>
          <p>Khám phá trang bị phù hợp và thêm vào giỏ hàng của bạn.</p>
          <Button as={Link} to="/products" className="icon-button">
            <Icon name="bag" />
            Khám phá sản phẩm
          </Button>
        </div>
      ) : (
        <Row className="g-4">
          <Col lg={8}>
            <div className="cart-selection">
              <Form.Check
                label="Chọn tất cả sản phẩm"
                checked={selected.length === data.cart_items.length}
                disabled={busy}
                onChange={(e) => change(() => cartApi.selectAll(e.target.checked))}
              />
              <small>
                Đã chọn {selected.length}/{data.cart_items.length}
              </small>
            </div>
            <div className="cart-items">
              {data.cart_items.map((i) => {
                const v = data.product_variants.find((v) => v.id === i.variant_id);
                const p = data.products.find((p) => p.id === v.product_id);
                return (
                  <article
                    key={i.id}
                    className={'cart-product' + (i.is_selected ? ' is-selected' : '')}
                  >
                    <div className="cart-product-top">
                      <Form.Check
                        aria-label={'Chọn ' + p.name}
                        checked={i.is_selected}
                        disabled={busy}
                        onChange={(e) =>
                          change(() =>
                            cartApi.updateItem({ id: i.id, is_selected: e.target.checked }),
                          )
                        }
                      />
                      <img
                        className="cart-image"
                        src={productImage(data, p, v.id)} onError={(event) => productImageFallback(event, p)}
                        alt={p.name}
                      />
                      <div className="cart-product-copy">
                        <small>{p.brand}</small>
                        <h2>{p.name}</h2>
                        <span className="variant-label">
                          {v.color} / {v.size}
                        </span>
                        <p>{formatMoney(v.price)}</p>
                      </div>
                      <Button
                        variant="light"
                        className="remove-item"
                        aria-label={'Xóa ' + p.name + ' khỏi giỏ'}
                        title="Xóa khỏi giỏ"
                        disabled={busy}
                        onClick={() => change(() => cartApi.removeItem(i.id))}
                      >
                        <Icon name="trash" size={18} />
                      </Button>
                    </div>
                    <div className="cart-product-bottom">
                      <QuantityControl
                        value={i.quantity}
                        max={v.stock_quantity}
                        label={p.name}
                        disabled={busy}
                        onChange={(quantity) =>
                          change(() => cartApi.updateItem({ id: i.id, quantity }))
                        }
                      />
                      <strong>{formatMoney(v.price * i.quantity)}</strong>
                    </div>
                  </article>
                );
              })}
            </div>
            <Button
              as={Link}
              to="/products"
              variant="outline-secondary"
              className="icon-button mt-4"
            >
              <Icon name="bag" size={18} />
              Tiếp tục mua sắm
            </Button>
          </Col>
          <Col lg={4}>
            <Card className="summary-card commerce-summary">
              <Card.Body>
                <h2 className="section-title">
                  <Icon name="receipt" />
                  Tóm tắt giỏ hàng
                </h2>
                <div className="summary-row">
                  <span>Tạm tính ({selected.length} sản phẩm)</span>
                  <strong>{formatMoney(subtotal)}</strong>
                </div>
                <div className="summary-row">
                  <span>Phí vận chuyển</span>
                  <span>{shipping ? formatMoney(shipping) : 'Miễn phí'}</span>
                </div>
                <div className="commerce-total">
                  <span>Tổng cộng</span>
                  <strong>{formatMoney(subtotal + shipping)}</strong>
                </div>
                <Button
                  as={selected.length ? Link : undefined}
                  to="/order?source=cart"
                  disabled={busy || !selected.length}
                  className="w-100 icon-button"
                >
                  <Icon name="bag" />
                  Tiến hành đặt hàng
                </Button>
                <div className="commerce-note">
                  <Icon name="truck" size={18} />
                  <span>Miễn phí vận chuyển cho đơn từ 2.000.000đ.</span>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}
