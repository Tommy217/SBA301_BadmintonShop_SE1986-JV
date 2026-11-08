import { Button, Card, Collapse } from 'react-bootstrap';
import { formatMoney } from '@/shared/utils/format';
import { productImage, productImageFallback } from '@/modules/shop/utils/productImages';
import { useShop } from '@/app/providers/shop/ShopContext';

function PaymentSummary({
  isExpanded,
  items,
  onToggle,
  shippingFee,
  subtotal,
  total,
  discount = 0,
}) {
  const { data } = useShop();
  const itemCount = items.reduce((count, item) => count + item.quantity, 0);
  function itemImage(item) {
    const orderItem = (data.order_items || []).find((row) => String(row.id) === String(item.id));
    const variant = (data.product_variants || []).find(
      (row) => String(row.id) === String(orderItem?.variant_id),
    );
    return variant
      ? productImage(data, { id: variant.product_id }, variant.id)
      : '/products/unavailable.svg';
  }

  return (
    <Card className="review-card">
      <Card.Body className="p-4">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <p className="payment-step mb-2">ĐƠN HÀNG CỦA BẠN</p>
            <h2 className="h5 fw-bold mb-0">Chi tiết đơn hàng</h2>
          </div>
          <span className="review-item-count">{itemCount} món</span>
        </div>

        <Button
          className="order-list-toggle w-100"
          variant="light"
          type="button"
          aria-expanded={isExpanded}
          aria-controls="payment-review-items"
          onClick={onToggle}
        >
          <span>{isExpanded ? 'Ẩn danh sách sản phẩm' : 'Xem danh sách sản phẩm'}</span>
          <span aria-hidden="true">{isExpanded ? '−' : '+'}</span>
        </Button>
        <Collapse in={isExpanded}>
          <div className="review-items" id="payment-review-items">
            {items.map((item) => (
              <div className="review-item" key={item.id}>
                <div className="review-item-image" aria-hidden="true">
                  <img src={itemImage(item)} onError={productImageFallback} alt="" />
                </div>
                <div className="review-item-copy">
                  <p className="fw-semibold mb-1">{item.name}</p>
                  <p className="small text-secondary mb-0">
                    {item.detail} · SL: {item.quantity}
                  </p>
                </div>
                <span className="review-item-price">{formatMoney(item.price * item.quantity)}</span>
              </div>
            ))}
          </div>
        </Collapse>

        <div className="review-totals mt-3">
          <div>
            <span>Tạm tính</span>
            <span>{formatMoney(subtotal)}</span>
          </div>
          <div>
            <span>Phí vận chuyển</span>
            <span>{formatMoney(shippingFee)}</span>
          </div>
          {Number(discount) > 0 && (
            <div className="review-discount">
              <span>Giảm giá</span>
              <span>−{formatMoney(discount)}</span>
            </div>
          )}
        </div>
        <div className="review-grand-total">
          <span>Tổng thanh toán</span>
          <strong>{formatMoney(total)}</strong>
        </div>
        <p className="review-caption small text-secondary mb-0 mt-3">
          Kiểm tra thông tin đơn hàng trước khi tiếp tục thanh toán.
        </p>
      </Card.Body>
    </Card>
  );
}

export default PaymentSummary;
