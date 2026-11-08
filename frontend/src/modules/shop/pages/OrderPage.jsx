import { productImage, productImageFallback } from '../utils/productImages';
import { calculateVoucher } from '@/modules/shop/utils/voucherRules';
import Icon from '@/shared/components/Icon';
import QuantityControl from '@/shared/components/QuantityControl';
import '@/modules/shop/shop.css';
import '@/modules/shop/order-checkout.css';
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { httpClient } from '@/shared/api/httpClient';
import VoucherPicker from '../components/VoucherPicker';
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
    const stock =
      data.product_variants.find((v) => String(v.id) === directVariant && v.is_active)
        ?.stock_quantity || 1;
    return Number.isSafeInteger(requested) && requested > 0 ? Math.min(requested, stock) : 1;
  });
  const { user } = useAuth();
  const userId = user?.id;
  const [wallet, setWallet] = useState(null);
  const [walletError, setWalletError] = useState('');
  const [walletAttempt, setWalletAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    httpClient
      .get('/shop/vouchers', { signal: controller.signal })
      .then((result) => {
        if (active) {
          setWallet({ ...result, userId });
          setWalletError('');
        }
      })
      .catch((error) => {
        if (active && error.name !== 'CanceledError') setWalletError(error.message);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [userId, walletAttempt]);
  const voucherData = {
    ...data,
    vouchers: wallet?.userId === userId ? wallet.vouchers : [],
    voucher_usages: wallet?.userId === userId ? wallet.voucher_usages : [],
  };
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
    discount = calculateVoucher(voucherData, appliedCode, items, userId).discount;
  } catch (e) {
    calculationError = e.message;
  }
  function applyVoucher(code) {
    try {
      calculateVoucher(voucherData, code, items, userId);
      setAppliedCode(code);
      setVoucherError('');
    } catch (e) {
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
      <header className="checkout-heading">
        <div>
          <Link className="checkout-back" to={directVariant ? '/products' : '/cart'}>
            ← {directVariant ? 'Tiếp tục mua sắm' : 'Quay lại giỏ hàng'}
          </Link>
          <p className="checkout-eyebrow">SẴN SÀNG CHO TRẬN ĐẤU MỚI</p>
          <h1>Hoàn tất đơn hàng</h1>
          <p>Chỉ còn một bước nữa để những món đồ yêu thích đến tay bạn.</p>
        </div>
        <div className="checkout-assurance">
          <Icon name="shield" size={24} />
          <span>
            <strong>An tâm mua sắm</strong>
            <small>Thông tin của bạn được bảo mật</small>
          </span>
        </div>
      </header>
      <ol className="checkout-steps" aria-label="Tiến trình đặt hàng">
        <li className="is-complete">
          <span>
            <Icon name="check" size={16} />
          </span>
          Chọn sản phẩm
        </li>
        <li className="is-current" aria-current="step">
          <span>2</span>Thông tin đặt hàng
        </li>
        <li>
          <span>3</span>Thanh toán
        </li>
      </ol>
      <Form onSubmit={submit}>
        <Row className="g-4">
          <Col lg={7}>
            <Card className="delivery-panel checkout-details">
              <Card.Body className="p-4">
                <h2 className="section-title">
                  <Icon name="pin" />
                  Thông tin giao hàng
                </h2>
                <p className="checkout-section-note">
                  Điền thông tin người nhận để chúng mình giao hàng chính xác.
                </p>
                <div className="checkout-fields">
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
                </div>
                <Form.Group controlId="note">
                  <Form.Label>Ghi chú (không bắt buộc)</Form.Label>
                  <Form.Control
                    as="textarea"
                    placeholder="Ví dụ: Gọi cho mình trước khi giao hàng…"
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
                <label className="checkout-payment-option my-3" htmlFor="prepaid">
                  <input
                    type="radio"
                    name="paymentMethod"
                    id="prepaid"
                    className="checkout-payment-radio"
                    checked
                    disabled={busy}
                    readOnly
                  />
                  <span className="payment-choice-copy">
                    <Icon name="card" />
                    <span>
                      <strong>VietQR qua payOS</strong>
                      <small>
                        Quét mã VietQR chuyển khoản nhanh bằng ứng dụng ngân hàng bất kỳ
                      </small>
                    </span>
                  </span>
                </label>
                <Alert variant="info" className="small mt-3 mb-0">
                  Sau khi xác nhận đơn hàng, chọn tiếp tục thanh toán để nhận mã VietQR.
                </Alert>
              </Card.Body>
            </Card>
          </Col>
          <Col lg={5}>
            <Card className="summary-card commerce-summary checkout-summary">
              <Card.Body className="p-4">
                <h2 className="section-title">
                  <Icon name="receipt" />
                  Đơn hàng của bạn
                  <span className="checkout-count">
                    {items.reduce((sum, item) => sum + item.quantity, 0)} sản phẩm
                  </span>
                </h2>
                {items.map((item) => {
                  const v = data.product_variants.find((v) => v.id === item.variant_id);
                  const p = data.products.find((p) => p.id === v.product_id);
                  return (
                    <div key={item.id} className="order-product">
                      <div className="summary-row">
                        <img
                          className="order-product-image"
                          src={productImage(data, p, v.id)}
                          onError={(event) => productImageFallback(event, p)}
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
                <VoucherPicker
                  data={voucherData}
                  items={items}
                  userId={userId}
                  appliedCode={appliedCode}
                  onSelect={applyVoucher}
                  onRemove={() => {
                    setAppliedCode('');
                    setVoucherError('');
                  }}
                  loading={!wallet && !walletError}
                  error={walletError}
                  onRetry={() => {
                    setWalletError('');
                    setWalletAttempt((n) => n + 1);
                  }}
                  disabled={busy}
                />
                {(voucherError || calculationError) && (
                  <p className="text-danger small">{voucherError || calculationError}</p>
                )}
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
                <p className="checkout-submit-note">
                  <Icon name="shield" size={15} /> Thanh toán an toàn qua ứng dụng ngân hàng
                </p>
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
