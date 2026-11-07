import useOrderUpdates from '@/modules/shop/hooks/useOrderUpdates';
import { getOrderStatus } from '@/modules/shop/utils/orderStatus';
import { useState } from 'react';
import { Alert, Badge, Button, Card, Col, Row, Spinner, Table } from 'react-bootstrap';
import { Link, useParams } from 'react-router';
import { carriers, carrierServices, deliveryStatuses } from '@/modules/delivery/constants';
import { useShop } from '@/app/providers/shop/ShopContext';
import { useOrderShipment } from '@/modules/delivery/hooks/useDelivery';
import { toDelivery } from '@/modules/delivery/api/delivery.mapper';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import '@/modules/delivery/delivery.css';
import { deliveryApi } from '@/modules/delivery/api/delivery.api';

export default function OrderTrackingPage() {
  const { orderId } = useParams();
  const { data } = useShop();
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState('');
  const [error, setError] = useState('');

  const order = (data.orders || []).find((item) => String(item.id) === orderId);
  const { data: delivery, setData: setDelivery, reload: reloadShipment } = useOrderShipment(orderId, Boolean(order));
  useOrderUpdates(reloadShipment);
  const orderItems = (data.order_items || []).filter((item) => String(item.order_id) === orderId);

  if (!order) {
    return (
      <div className="py-5 text-center">
        <div className="eyebrow">THEO DÕI ĐƠN HÀNG</div>
        <h1 className="h3 fw-bold mb-2">Không tìm thấy đơn hàng</h1>
        <p className="text-secondary mb-4">
          Đơn hàng không tồn tại hoặc bạn không có quyền truy cập.
        </p>
        <Button as={Link} to="/orders" variant="primary">
          Xem danh sách đơn hàng
        </Button>
      </div>
    );
  }

  const carrier = delivery
    ? carriers[delivery.carrier] || {
        name: delivery.carrier?.toUpperCase() || 'Đối tác vận chuyển',
        shortName: delivery.carrier?.toUpperCase() || 'ĐVVC',
        badgeColor: '#333',
        badgeBg: '#eee',
        hotline: '1900 xxxx',
      }
    : null;

  const statusInfo = delivery
    ? deliveryStatuses[delivery.status] || {
        label: delivery.status || 'Chưa xác định',
        variant: 'secondary',
      }
    : getOrderStatus(order.status);

  async function handleRefresh() {
    if (!delivery) return;
    setIsSyncing(true);
    setError('');
    setSyncMessage('');
    try {
      setDelivery(toDelivery(await deliveryApi.syncOrderShipment(orderId)));
      setSyncMessage(`Đã làm mới thông tin hành trình mới nhất từ ${carrier?.name || 'ĐVVC'}.`);
    } catch (err) {
      setError(err.message || 'Không thể làm mới hành trình giao hàng.');
    } finally {
      setIsSyncing(false);
    }
  }

  // Xác định tiến trình của Stepper 4 bước
  // 1: Đặt hàng thành công
  // 2: ĐVVC đã lấy hàng
  // 3: Đang giao hàng
  // 4: Giao thành công
  let currentStep = 1;
  if (delivery) {
    if (delivery.status === 'pending_pickup') currentStep = 2;
    else if (delivery.status === 'in_transit') currentStep = 2;
    else if (delivery.status === 'out_for_delivery') currentStep = 3;
    else if (delivery.status === 'delivered') currentStep = 4;
    else if (delivery.status === 'delivery_failed') currentStep = 3;
  }

  const trackingLogs = delivery?.tracking_logs || [];

  return (
    <div className="py-2">
      <div className="eyebrow">THEO DÕI HÀNH TRÌNH ĐƠN HÀNG</div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <div className="d-flex align-items-center gap-2 mb-1">
            <h1 className="h3 fw-bold mb-0">Đơn hàng #{order.order_code}</h1>
            <Badge bg={getOrderStatus(order.status).variant} text={order.status === 'pending' ? 'dark' : undefined}>{getOrderStatus(order.status).label}</Badge>
            {carrier && (
              <span
                className="carrier-badge-pill"
                style={{
                  backgroundColor: carrier.badgeBg,
                  color: carrier.badgeColor,
                }}
              >
                {carrier.name}
              </span>
            )}
          </div>
          <p className="text-secondary mb-0">
            {delivery ? (
              <>
                Mã vận đơn:{' '}
                <strong className="font-monospace text-dark">{delivery.tracking_number || 'Đang chờ cấp mã'}</strong> ·
                Cập nhật: {formatDateTime(delivery.last_synced_at)}
              </>
            ) : (
              getOrderStatus(order.status).description
            )}
          </p>
        </div>
        <div className="d-flex gap-2">
          <Button as={Link} to="/orders" variant="outline-secondary">
            ← Danh sách đơn hàng
          </Button>
        </div>
      </div>

      {syncMessage && (
        <Alert variant="success" dismissible onClose={() => setSyncMessage('')} className="mb-4">
          {syncMessage}
        </Alert>
      )}

      {error && (
        <Alert variant="danger" dismissible onClose={() => setError('')} className="mb-4">
          {error}
        </Alert>
      )}

      {/* Stepper 4 bước trực quan */}
      <Card className="border-0 shadow-sm mb-4">
        <Card.Body className="p-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <span className="fw-bold text-dark">Tiến trình vận chuyển</span>
            <Badge
              bg={statusInfo.variant}
              text={statusInfo.variant === 'warning' ? 'dark' : undefined}
              className="px-3 py-2"
            >
              {statusInfo.label}
            </Badge>
          </div>

          <div className="tracking-stepper">
            <div
              className="stepper-progress"
              style={{
                width: `${Math.min(100, Math.max(0, ((currentStep - 1) / 3) * 100))}%`,
              }}
            />

            {/* Bước 1 */}
            <div
              className={`stepper-step ${currentStep >= 1 ? (currentStep > 1 ? 'completed' : 'active') : ''}`}
            >
              <div className="stepper-circle">{currentStep > 1 ? '✓' : '1'}</div>
              <div className="stepper-label">Đặt hàng thành công</div>
              <div className="stepper-date">{formatDateTime(order.created_at)}</div>
            </div>

            {/* Bước 2 */}
            <div
              className={`stepper-step ${currentStep >= 2 ? (currentStep > 2 ? 'completed' : 'active') : ''}`}
            >
              <div className="stepper-circle">{currentStep > 2 ? '✓' : '2'}</div>
              <div className="stepper-label">ĐVVC đã nhận hàng</div>
              <div className="stepper-date">
                {delivery?.shipped_at
                  ? formatDateTime(delivery.shipped_at)
                  : currentStep >= 2
                    ? 'Đang xử lý'
                    : 'Chờ lấy'}
              </div>
            </div>

            {/* Bước 3 */}
            <div
              className={`stepper-step ${currentStep >= 3 ? (currentStep > 3 ? 'completed' : 'active') : ''}`}
            >
              <div className="stepper-circle">{currentStep > 3 ? '✓' : '3'}</div>
              <div className="stepper-label">Đang giao hàng</div>
              <div className="stepper-date">
                {delivery?.status === 'out_for_delivery'
                  ? 'Đang phát'
                  : currentStep > 3
                    ? 'Đã qua'
                    : 'Dự kiến'}
              </div>
            </div>

            {/* Bước 4 */}
            <div className={`stepper-step ${currentStep === 4 ? 'completed' : ''}`}>
              <div className="stepper-circle">{currentStep === 4 ? '✓' : '4'}</div>
              <div className="stepper-label">Đã nhận hàng</div>
              <div className="stepper-date">
                {delivery?.delivered_at ? formatDateTime(delivery.delivered_at) : 'Chưa hoàn tất'}
              </div>
            </div>
          </div>

          {delivery?.carrier_status_text && (
            <div className="carrier-info-box mt-3 text-center">
              <span className="small text-secondary d-block">
                Trạng thái mới nhất từ {carrier?.shortName}:
              </span>
              <strong className="text-primary fs-6">{delivery.carrier_status_text}</strong>
            </div>
          )}
        </Card.Body>
      </Card>

      <Row className="g-4">
        {/* Cột trái: Timeline hành trình bưu cục chi tiết */}
        <Col lg={7}>
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center justify-content-between mb-3 border-bottom pb-3">
                <h2 className="h5 fw-bold mb-0">Nhật ký hành trình thực tế</h2>
                {delivery && (
                  <Button
                    variant="outline-primary"
                    size="sm"
                    onClick={handleRefresh}
                    disabled={isSyncing}
                    className="d-inline-flex align-items-center gap-1"
                  >
                    {isSyncing ? (
                      <Spinner animation="border" size="sm" />
                    ) : (
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
                      </svg>
                    )}
                    <span>Làm mới hành trình</span>
                  </Button>
                )}
              </div>

              {!delivery ? (
                <div className="p-4 text-center text-secondary">
                  <p className="mb-2">Kiện hàng của bạn đang được đóng gói tại shop.</p>
                  <small>
                    Ngay sau khi bàn giao cho đối tác vận chuyển, mã vận đơn và lộ trình sẽ hiển thị
                    tại đây.
                  </small>
                </div>
              ) : trackingLogs.length === 0 ? (
                <div className="p-4 text-center text-secondary">
                  Đang chờ cập nhật thông tin bưu cục đầu tiên từ đối tác vận chuyển.
                </div>
              ) : (
                <div className="tracking-timeline">
                  {trackingLogs.map((log, index) => {
                    const isLatest = index === 0;
                    const logClass =
                      log.status === 'delivered'
                        ? 'delivered'
                        : log.status === 'delivery_failed'
                          ? 'failed'
                          : isLatest
                            ? 'active'
                            : '';

                    return (
                      <div className={`timeline-item ${logClass}`} key={log.id || index}>
                        <div className="timeline-dot" />
                        <div className="timeline-content">
                          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
                            <span className="timeline-time">{formatDateTime(log.timestamp)}</span>
                            {isLatest && (
                              <span className="badge bg-primary-subtle text-primary border border-primary-subtle small">
                                Trạm hiện tại
                              </span>
                            )}
                          </div>
                          <div className="timeline-title">{log.title}</div>
                          {log.location && (
                            <div className="timeline-location">
                              <svg
                                width="14"
                                height="14"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                              >
                                <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
                                <circle cx="12" cy="10" r="3" />
                              </svg>
                              <span>{log.location}</span>
                            </div>
                          )}
                          {log.note && <div className="timeline-note">{log.note}</div>}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </Card.Body>
          </Card>

          {/* Card: Sản phẩm trong đơn hàng */}
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              <h2 className="h5 fw-bold mb-3 border-bottom pb-2">
                Sản phẩm đã đặt ({orderItems.length})
              </h2>

              <Table responsive className="align-middle mb-0">
                <tbody>
                  {orderItems.map((item) => (
                    <tr key={item.id}>
                      <td className="ps-0">
                        <div className="fw-semibold">{item.product_name}</div>
                        <div className="small text-secondary">
                          {item.variant_info || 'Tiêu chuẩn'} × {item.quantity}
                        </div>
                      </td>
                      <td className="text-end pe-0 fw-semibold">{formatMoney(item.line_total)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>

        {/* Cột phải: Thông tin giao nhận và đối tác */}
        <Col lg={5}>
          {/* Card: Thông tin đối tác vận chuyển */}
          {carrier && delivery && (
            <Card className="border-0 shadow-sm mb-4">
              <Card.Body className="p-4">
                <h3 className="h6 fw-bold mb-3 border-bottom pb-2">Đơn vị vận chuyển bên thứ 3</h3>

                <div className="d-flex align-items-center justify-content-between mb-3">
                  <div>
                    <span className="fw-bold fs-6 d-block">{carrier.name}</span>
                    <small className="text-secondary">
                      Gói: {carrierServices[delivery.service_type] || 'Tiêu chuẩn'}
                    </small>
                  </div>
                  <span
                    className="badge"
                    style={{
                      backgroundColor: carrier.badgeBg,
                      color: carrier.badgeColor,
                      fontSize: '13px',
                      padding: '6px 12px',
                    }}
                  >
                    {carrier.shortName}
                  </span>
                </div>

                <div className="mb-3">
                  <span className="small text-secondary d-block">Mã tra cứu vận đơn:</span>
                  <span className="fs-6 font-monospace fw-bold text-primary">
                    {delivery.tracking_number || 'Đang chờ cấp mã'}
                  </span>
                </div>

                <div className="d-grid gap-2 mb-3">
                  {delivery.tracking_number && (delivery.tracking_url || carrier.trackingUrl) && (
                    <a
                      href={delivery.tracking_url || carrier.trackingUrl(delivery.tracking_number)}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="btn btn-sm btn-outline-primary d-flex align-items-center justify-content-center gap-2"
                    >
                      <span>Tra cứu trực tiếp trên web {carrier.shortName}</span>
                      <svg
                        width="13"
                        height="13"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3" />
                      </svg>
                    </a>
                  )}
                </div>

                <div className="small text-secondary">
                  Hotline hãng:{' '}
                  <a
                    href={`tel:${carrier.hotline.replace(/\s+/g, '')}`}
                    className="fw-bold text-decoration-none text-primary"
                  >
                    {carrier.hotline}
                  </a>
                </div>
              </Card.Body>
            </Card>
          )}

          {/* Card: Bưu tá giao hàng (nếu đang đi giao) */}
          {delivery?.shipper_name && (
            <Card className="border-0 shadow-sm mb-4 bg-primary-subtle border-primary-subtle">
              <Card.Body className="p-3">
                <div className="small text-primary fw-bold mb-1">Bưu tá đang phát hàng cho bạn</div>
                <div className="fw-semibold fs-6 text-dark">{delivery.shipper_name}</div>
                {delivery.shipper_phone && (
                  <div className="mt-2">
                    <a
                      href={`tel:${delivery.shipper_phone.replace(/\./g, '')}`}
                      className="btn btn-sm btn-primary d-inline-flex align-items-center gap-2"
                    >
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                      </svg>
                      <span>Gọi bưu tá: {delivery.shipper_phone}</span>
                    </a>
                  </div>
                )}
              </Card.Body>
            </Card>
          )}

          {/* Card: Thông tin nhận hàng & Thanh toán */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body className="p-4">
              <h3 className="h6 fw-bold mb-3 border-bottom pb-2">
                Thông tin giao nhận & Thanh toán
              </h3>

              <div className="mb-2">
                <span className="text-secondary small d-block">Người nhận:</span>
                <span className="fw-semibold">
                  {order.receiver_name} · {order.receiver_phone}
                </span>
              </div>

              <div className="mb-3">
                <span className="text-secondary small d-block">Địa chỉ nhận hàng:</span>
                <span className="small text-dark">{order.shipping_address}</span>
              </div>

              {order.note && (
                <div className="mb-3">
                  <span className="text-secondary small d-block">Ghi chú đơn hàng:</span>
                  <span className="small text-secondary fst-italic">{order.note}</span>
                </div>
              )}

              <hr />

              <div className="d-flex justify-content-between mb-2">
                <span className="small text-secondary">Phí vận chuyển:</span>
                <span className="small fw-semibold">{formatMoney(order.shipping_fee)}</span>
              </div>

              <div className="d-flex justify-content-between mb-2">
                <span className="small text-secondary">Hình thức thanh toán:</span>
                <span className="small fw-semibold">
                  Thanh toán trực tuyến (payOS VietQR)
                </span>
              </div>

              <div className="d-flex justify-content-between align-items-center pt-2 border-top mt-2">
                <div>
                  <span className="fw-bold d-block">Trạng thái thanh toán:</span>
                  <small className="text-secondary">
                    {order.payment_status === 'paid' ? 'Đã thanh toán đủ' : 'Chờ quét mã VietQR'}
                  </small>
                </div>
                <span className={`fs-5 fw-bold ${order.payment_status === 'paid' ? 'text-success' : 'text-danger'}`}>
                  {order.payment_status === 'paid' ? 'ĐÃ THANH TOÁN' : formatMoney(order.total_amount)}
                </span>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
