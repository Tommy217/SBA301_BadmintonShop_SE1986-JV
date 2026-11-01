import { Alert, Badge, Button, Card } from 'react-bootstrap';
import { Link, useLocation } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { carriers, deliveryStatuses } from '@/modules/delivery/constants';

export default function OrdersPage() {
  const { data } = useShop(),
    { state } = useLocation();
  return (
    <>
      <div className="eyebrow">LỊCH SỬ MUA SẮM</div>
      <h1 className="h2 fw-bold mb-4">Đơn hàng của tôi</h1>
      {state?.placed && (
        <Alert variant="success">Đặt hàng thành công! Đơn hàng của bạn đang chờ xác nhận.</Alert>
      )}
      {!data.orders.length && (
        <div className="empty-state">
          <p>Bạn chưa có đơn hàng nào.</p>
          <Button as={Link} to="/products">
            Mua sắm ngay
          </Button>
        </div>
      )}
      {[...data.orders].reverse().map((o) => {
        const delivery = (data.deliveries || []).find((d) => d.order_id === o.id);
        const carrier = delivery ? carriers[delivery.carrier] : null;
        const statusInfo = delivery ? deliveryStatuses[delivery.status] : null;

        return (
          <Card key={o.id} className="border-0 mb-4 shadow-sm">
            <Card.Body className="p-4">
              <div className="d-flex justify-content-between align-items-start gap-2">
                <div>
                  <h2 className="h6 fw-bold mb-1">{o.order_code}</h2>
                  <small className="text-secondary">{formatDateTime(o.created_at)}</small>
                </div>
                <div className="text-end">
                  {delivery ? (
                    <>
                      <Badge
                        bg={statusInfo?.variant || 'info'}
                        text={statusInfo?.variant === 'warning' ? 'dark' : undefined}
                        className="d-block mb-1"
                      >
                        {statusInfo?.label || 'Đang giao'} ({carrier?.shortName || 'ĐVVC'})
                      </Badge>
                      <small className="text-secondary font-monospace d-block">
                        {delivery.tracking_number}
                      </small>
                    </>
                  ) : (
                    <Badge bg="info" text="dark">
                      Chờ xác nhận
                    </Badge>
                  )}
                </div>
              </div>
              <hr />
              {data.order_items
                .filter((i) => i.order_id === o.id)
                .map((i) => (
                  <div className="summary-row" key={i.id}>
                    <span>
                      {i.product_name}
                      <small className="d-block text-secondary">
                        {i.variant_info} × {i.quantity}
                      </small>
                    </span>
                    <strong>{formatMoney(i.line_total)}</strong>
                  </div>
                ))}
              <hr />
              <p className="mb-1">
                {o.receiver_name} · {o.receiver_phone}
              </p>
              <p className="small text-secondary">{o.shipping_address}</p>
              {o.note && <p className="small">Ghi chú: {o.note}</p>}
              <div className="summary-row small">
                <span>Phí vận chuyển</span>
                <span>{formatMoney(o.shipping_fee)}</span>
              </div>
              <div className="summary-row">
                <span>
                  VietQR payOS · {o.payment_status === 'paid' ? 'Đã thanh toán' : 'Chưa thanh toán'}
                </span>
                <strong className="text-primary">{formatMoney(o.total_amount)}</strong>
              </div>

              <div className="d-flex flex-wrap gap-2 mt-3 pt-3 border-top">
                <Button
                  as={Link}
                  to={`/orders/${encodeURIComponent(o.id)}/tracking`}
                  variant="outline-primary"
                  size="sm"
                  className="d-inline-flex align-items-center gap-1"
                >
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2" />
                    <path d="M15 18H9" />
                    <path d="M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14" />
                    <circle cx="17" cy="18" r="2" />
                    <circle cx="7" cy="18" r="2" />
                  </svg>
                  <span>
                    {delivery
                      ? `Theo dõi vận chuyển (${carrier?.shortName || 'ĐVVC'})`
                      : 'Theo dõi đơn hàng'}
                  </span>
                </Button>

                {o.payment_status === 'unpaid' && (
                    <Button
                      as={Link}
                      to={'/payments?order=' + encodeURIComponent(o.id)}
                      variant="outline-success"
                      size="sm"
                    >
                      Tiếp tục thanh toán
                    </Button>
                  )}
              </div>
            </Card.Body>
          </Card>
        );
      })}
    </>
  );
}
