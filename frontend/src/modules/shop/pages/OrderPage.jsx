import { productImage, productImageFallback } from '../utils/productImages';
import { calculateVoucher } from '@/modules/shop/utils/voucherRules';
import Icon from '@/shared/components/Icon';
import QuantityControl from '@/shared/components/QuantityControl';
import '@/modules/shop/shop.css';
import { useRef, useState } from 'react';
import { Alert, Button, Card, Col, Form, Row } from 'react-bootstrap';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatMoney } from '@/shared/utils/format';
import { orderApi } from '@/modules/shop/api/shop.api';

export default function OrderPage() {
  const { data, busy, act } = useShop();
  const [params] = useSearchParams();
  const directVariant = params.get('variant');
  const fromCart = !directVariant && params.get('source') === 'cart';
  const [quantity, setQuantity] = useState(() => {
    const requested = Number(params.get('quantity'));
    const stock = data.product_variants.find((v) => String(v.id) === directVariant && v.is_active)?.stock_quantity || 1;
    return Number.isSafeInteger(requested) && requested > 0 ? Math.min(requested, stock) : 1;
  });
  const [voucherCode, setVoucherCode] = useState('');
  const [appliedCode, setAppliedCode] = useState('');
  const [voucherError, setVoucherError] = useState('');
  const [paymentMethod] = useState('prepaid');
  const [form, setForm] = useState({
    receiver_name: '',
    receiver_phone: '',
    shipping_address: '',
    note: '',
  });
  const lock = useRef(false);
  const navigate = useNavigate();
  const variant = data.product_variants.find((v) => String(v.id) === directVariant && v.is_active);
  const items = directVariant
    ? variant
      ? [{ id: 'direct', variant_id: variant.id, quantity }]
      : []
    : fromCart
      ? data.cart_items.filter((i) => i.is_selected)
      : [];
  const subtotal = items.reduce(
    (sum, item) =>
      sum + data.product_variants.find((v) => v.id === item.variant_id).price * item.quantity,
    0,
  );
  const shipping = 0;

  let discount = 0,
    calculationError = '';
  try {
    discount = calculateVoucher(data, appliedCode, items, data.carts[0]?.user_id).discount;
  } catch (e) {
    calculationError = e.message;
  }
  function applyVoucher() {
    try {
      calculateVoucher(data, voucherCode, items, data.carts[0]?.user_id);
      setAppliedCode(voucherCode.trim());
      setVoucherError('');
    } catch (e) {
      setAppliedCode('');
      setVoucherError(e.message);
    }
  }
  async function submit(event) {
    event.preventDefault();
    if (lock.current) return;
    lock.current = true;
    try {
      const result = await act(() =>
        orderApi.checkout({
          source: directVariant ? 'direct' : 'cart',
          variant_id: directVariant,
          quantity,
          cart_item_ids: fromCart ? items.map((item) => item.id) : undefined,
          payment_method: paymentMethod,
          voucher_code: appliedCode,
          ...form,
        }),
      );
      const order = result.orders.at(-1);
      navigate('/payments?order=' + encodeURIComponent(order.id));
    } catch {
      /* Provider displays the error. */
    } finally {
      lock.current = false;
    }
  }

  if (!items.length)
    return (
      <div className="empty-state">
        <h1 className="h4">Chưa có sản phẩm để đặt hàng</h1>
        <p>Bấm “Đặt hàng” ở sản phẩm hoặc chọn sản phẩm và tiến hành đặt hàng từ giỏ.</p>
        <Button as={Link} to="/products" className="me-2">
          Xem sản phẩm
        </Button>
        <Button as={Link} to="/cart" variant="outline-primary">
          Về giỏ hàng
        </Button>
      </div>
    );

  return (
    <div className="commerce-page order-page">
      <div className="eyebrow">XÁC NHẬN MUA SẮM</div>
      <h1 className="h2 fw-bold mb-2">Đặt hàng</h1>
      <p className="text-secondary mb-4">
        {directVariant
          ? 'Đặt trực tiếp sản phẩm đã chọn.'
          : 'Đặt các sản phẩm bạn đã chọn trong giỏ hàng.'}{' '}
        Đơn hàng chỉ được tạo khi bạn xác nhận bên dưới.
      </p>
      <Form onSubmit={submit}>
        <Row className="g-4">
          <Col lg={7}>
            <Card className="delivery-panel">
              <Card.Body className="p-4">
                <h2 className="section-title">
                  <Icon name="pin" />
                  Thông tin giao hàng
                </h2>
                {[
                  ['receiver_name', 'Họ và tên người nhận'],
                  ['receiver_phone', 'Số điện thoại'],
                  ['shipping_address', 'Địa chỉ giao hàng'],
                ].map(([key, label]) => (
                  <Form.Group key={key} controlId={key} className="mb-3">
                    <Form.Label>{label} *</Form.Label>
                    <Form.Control
                      required
                      disabled={busy}
                      minLength={key === 'shipping_address' ? 10 : undefined}
                      maxLength={
                        key === 'shipping_address' ? 500 : key === 'receiver_phone' ? 20 : 150
                      }
                      type={key === 'receiver_phone' ? 'tel' : 'text'}
                      pattern={key === 'receiver_phone' ? '(0|\\+84)[0-9]{9}' : undefined}
                      placeholder={
                        key === 'shipping_address'
                          ? 'Số nhà, đường, phường/xã, tỉnh/thành phố'
                          : label
                      }
                      value={form[key]}
                      onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                    />
                  </Form.Group>
                ))}
                <Form.Group controlId="note">
                  <Form.Label>Ghi chú (không bắt buộc)</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    maxLength={500}
                    disabled={busy}
                    value={form.note}
                    onChange={(e) => setForm({ ...form, note: e.target.value })}
                  />
                </Form.Group>
                <h2 className="section-title mt-4">
                  <Icon name="card" />
                  Hình thức thanh toán
                </h2>
                <Form.Check
                  type="radio"
                  name="paymentMethod"
                  id="prepaid"
                  className="payment-choice is-chosen my-3"
                  label={
                    <span className="payment-choice-copy">
                      <Icon name="card" />
                      <span>
                        <strong>Thanh toán trực tuyến (payOS VietQR 24/7)</strong>
                        <small>Quét mã VietQR chuyển khoản nhanh bằng ứng dụng ngân hàng bất kỳ</small>
                      </span>
                    </span>
                  }
                  checked
                  disabled={busy}
                  readOnly
                />
                <Alert variant="info" className="small mt-3 mb-0">
                  Đơn hàng áp dụng thanh toán trực tuyến 100%. Sau khi bấm xác nhận, bạn sẽ được chuyển đến trang quét mã VietQR để hoàn tất thanh toán.
                </Alert>
              </Card.Body>
            </Card>
          </Col>
          <Col lg={5}>
            <Card className="summary-card commerce-summary">
              <Card.Body className="p-4">
                <h2 className="section-title">
                  <Icon name="receipt" />
                  Sản phẩm đặt mua
                </h2>
                {items.map((item) => {
                  const v = data.product_variants.find((v) => v.id === item.variant_id);
                  const p = data.products.find((p) => p.id === v.product_id);
                  return (
                    <div key={item.id} className="order-product">
                      <div className="summary-row">
                        <img
                          className="order-product-image"
                          src={productImage(data, p, v.id)} onError={(event) => productImageFallback(event, p)}
                          alt={p.name}
                        />
                        <div className="order-product-copy">
                          {p.name}
                          <small className="d-block text-secondary">
                            {v.color} / {v.size} × {item.quantity}
                          </small>
                        </div>
                        <strong className="text-nowrap">
                          {formatMoney(v.price * item.quantity)}
                        </strong>
                      </div>
                      {directVariant && (
                        <div className="order-quantity-row">
                          <span>Số lượng</span>
                          <QuantityControl
                            value={quantity}
                            max={v.stock_quantity}
                            disabled={busy}
                            onChange={setQuantity}
                            label={p.name}
                          />
                        </div>
                      )}
                    </div>
                  );
                })}
                <hr />
                <div className="summary-row">
                  <span>Tạm tính</span>
                  <span>{formatMoney(subtotal)}</span>
                </div>
                <div className="summary-row">
                  <span>Phí vận chuyển</span>
                  <span>{formatMoney(shipping)}</span>
                </div>
                <Form.Group className="my-3" controlId="checkout-voucher">
                  <Form.Label>Mã giảm giá</Form.Label>
                  <div className="d-flex gap-2">
                    <Form.Control
                      placeholder="Nhập mã voucher"
                      value={voucherCode}
                      onChange={(e) => setVoucherCode(e.target.value.toUpperCase())}
                    />
                    <Button type="button" variant="outline-primary" onClick={applyVoucher}>
                      Áp dụng
                    </Button>
                  </div>
                  {(voucherError || calculationError) && (
                    <p className="text-danger small mt-2">{voucherError || calculationError}</p>
                  )}
                  {appliedCode && !calculationError && (
                    <p className="text-success small mt-2">
                      Đã áp dụng {appliedCode}{' '}
                      <Button
                        type="button"
                        size="sm"
                        variant="link"
                        onClick={() => {
                          setAppliedCode('');
                          setVoucherCode('');
                        }}
                      >
                        Bỏ mã
                      </Button>
                    </p>
                  )}
                </Form.Group>
                <div className="summary-row">
                  <span>Giảm giá</span>
                  <span>−{formatMoney(discount)}</span>
                </div>
                <div className="commerce-total">
                  <span>Tổng thanh toán</span>
                  <strong>{formatMoney(subtotal - discount + shipping)}</strong>
                </div>
                <Button
                  type="submit"
                  disabled={
                    busy ||
                    !!calculationError ||
                    (directVariant && (!quantity || quantity > variant.stock_quantity))
                  }
                  className="w-100 mt-3 icon-button"
                >
                  <Icon name="bag" />
                  {busy ? 'Đang tạo đơn…' : 'Xác nhận đặt hàng'}
                </Button>
                <Link
                  className="d-block text-center small mt-3"
                  to={directVariant ? '/products' : '/cart'}
                >
                  {directVariant ? 'Quay lại sản phẩm' : 'Quay lại giỏ hàng'}
                </Link>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Form>
    </div>
  );
}
