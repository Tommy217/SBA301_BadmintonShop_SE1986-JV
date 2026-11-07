import { useState } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap';
import { Link, useParams } from 'react-router';
import { carriers, carrierServices, deliveryStatuses } from '@/modules/delivery/constants';
import { useShop } from '@/app/providers/shop/ShopContext';
import { useShipment } from '@/modules/delivery/hooks/useDelivery';
import useAutoHide from '@/modules/delivery/hooks/useAutoHide';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import '@/modules/delivery/delivery.css';
import { deliveryApi } from '@/modules/delivery/api/delivery.api';
import { toDelivery } from '@/modules/delivery/api/delivery.mapper';

export default function DeliveryManagementDetailPage() {
  const { deliveryId } = useParams();
  const { data } = useShop();
  const {
    data: delivery,
    isLoading,
    error: loadError,
    setData: setDelivery,
  } = useShipment(deliveryId);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState('');
  const [error, setError] = useState('');
  const [cancelReason, setCancelReason] = useState(null);
  // thong bao thanh cong (dong bo / gui lai / huy) tu an sau 3 giay; loi giu lai
  useAutoHide(syncMessage, setSyncMessage);

  if (isLoading && !delivery) {
    return (
      <div className="py-5 text-center">
        <Spinner animation="border" size="sm" className="me-2" />
        Đang tải vận đơn...
      </div>
    );
  }

  if (!delivery) {
    return (
      <>
        <div className="eyebrow">QUẢN LÝ VẬN HÀNH & GIAO VẬN</div>
        <h1 className="h2 fw-bold mb-2">Không tìm thấy vận đơn</h1>
        <p className="text-secondary mb-4">
          {loadError?.status === 404 || !loadError
            ? 'Vận đơn không tồn tại hoặc đã bị xóa khỏi hệ thống.'
            : loadError.message}
        </p>
        <Button as={Link} to="/management/deliveries" variant="outline-primary">
          Quay lại danh sách vận đơn
        </Button>
      </>
    );
  }

  const orderItems = (data.order_items || []).filter((item) => item.order_id === delivery.order_id);
  const carrier = carriers[delivery.carrier] || {
    name: delivery.carrier?.toUpperCase() || 'Đối tác vận chuyển',
    shortName: delivery.carrier?.toUpperCase() || 'ĐVVC',
    badgeColor: '#333',
    badgeBg: '#eee',
    hotline: '1900 xxxx',
  };
  const statusInfo = deliveryStatuses[delivery.status] || {
    label: delivery.status || 'Chưa xác định',
    variant: 'secondary',
  };

  // chua gui duoc sang doi tac thi gui lai, nguoc lai doc trang thai moi nhat
  async function handleSync() {
    setIsSyncing(true);
    setError('');
    setSyncMessage('');
    try {
      if (delivery.resendable) {
        setDelivery(toDelivery(await deliveryApi.resend(delivery.id)));
        setSyncMessage(`Đã gửi lại yêu cầu vận đơn cho ${carrier.name}.`);
      } else {
        setDelivery(toDelivery(await deliveryApi.sync(delivery.id)));
        setSyncMessage(`Đã đồng bộ thành công dữ liệu mới nhất từ cổng thông tin ${carrier.name}.`);
      }
    } catch (err) {
      setError(err.message || 'Không thể đồng bộ với đối tác vận chuyển.');
    } finally {
      setIsSyncing(false);
    }
  }

  async function handleCancel(event) {
    event.preventDefault();
    setIsSyncing(true);
    setError('');
    setSyncMessage('');
    try {
      setDelivery(toDelivery(await deliveryApi.cancel(delivery.id, cancelReason.trim())));
      setCancelReason(null);
      setSyncMessage('Đã huỷ vận đơn.');
    } catch (err) {
      setError(err.message || 'Không thể huỷ vận đơn.');
      setCancelReason(null);
    } finally {
      setIsSyncing(false);
    }
  }

  const trackingLogs = delivery.tracking_logs || [];

  return (
    <>
      <div className="eyebrow">CHI TIẾT VẬN ĐƠN & HÀNH TRÌNH</div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <div className="d-flex align-items-center gap-2 mb-2">
            <h1 className="h2 fw-bold mb-0">{delivery.tracking_number || 'Chưa có mã vận đơn'}</h1>
            <span
              className="carrier-badge-pill"
              style={{
                backgroundColor: carrier.badgeBg,
                color: carrier.badgeColor,
              }}
            >
              {carrier.name}
            </span>
          </div>
          <p className="text-secondary mb-0">
            Mã đơn hàng liên kết: <strong>{delivery.order_code}</strong> · Đồng bộ lần cuối:{' '}
            {formatDateTime(delivery.last_synced_at)}
          </p>
        </div>
        <div className="d-flex gap-2">
          <Button as={Link} to="/management/deliveries" variant="outline-secondary">
            ← Quay lại danh sách
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

      <Row className="g-4">
        {/* Cột trái: Timeline hành trình bưu cục và danh sách sản phẩm */}
        <Col lg={8}>
          {/* Card 1: Nhật ký Hành trình Bưu cục (3rd-Party Tracking Timeline) */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center justify-content-between mb-3 border-bottom pb-3">
                <div>
                  <h2 className="h5 fw-bold mb-1">Hành trình bưu cục thời gian thực</h2>
                  <p className="small text-secondary mb-0">
                    Dữ liệu được cập nhật từ API cổng vận chuyển của {carrier.name}
                  </p>
                </div>
                <Badge
                  bg={statusInfo.variant}
                  text={statusInfo.variant === 'warning' ? 'dark' : undefined}
                  className="fs-6 px-3 py-2"
                >
                  {statusInfo.label}
                </Badge>
              </div>

              {trackingLogs.length === 0 ? (
                <div className="text-center py-4 text-secondary">
                  Chưa có thông tin cập nhật bưu cục từ đối tác.
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
                                Mới nhất
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

          {/* Card 2: Sản phẩm trong kiện hàng */}
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              <h2 className="h5 fw-bold mb-3 border-bottom pb-2">Sản phẩm trong kiện hàng</h2>

              <Table responsive className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Phân loại</th>
                    <th className="text-center">Số lượng</th>
                    <th className="text-end">Đơn giá</th>
                    <th className="text-end">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {orderItems.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-center text-secondary py-3">
                        Không có dữ liệu chi tiết sản phẩm.
                      </td>
                    </tr>
                  ) : (
                    orderItems.map((item) => (
                      <tr key={item.id}>
                        <td className="fw-semibold">{item.product_name}</td>
                        <td className="text-secondary small">
                          {item.variant_info || 'Tiêu chuẩn'}
                        </td>
                        <td className="text-center">{item.quantity}</td>
                        <td className="text-end">{formatMoney(item.unit_price)}</td>
                        <td className="text-end fw-semibold">{formatMoney(item.line_total)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>

        {/* Cột phải: Thao tác ĐVVC, Thông tin giao nhận và tài chính */}
        <Col lg={4}>
          {/* Card 3: Thao tác & Đồng bộ với ĐVVC */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body className="p-4">
              <h3 className="h6 fw-bold mb-3">Thao tác với đối tác vận chuyển</h3>

              <div className="carrier-info-box mb-3">
                <div className="small text-secondary mb-1">
                  Trạng thái nguyên bản từ {carrier.shortName}:
                </div>
                <div className="fw-semibold text-dark mb-2">
                  {delivery.carrier_status_text || 'Chưa có thông báo'}
                </div>
                <div className="small text-secondary">
                  Cập nhật: {formatDateTime(delivery.last_synced_at)}
                </div>
              </div>

              <div className="d-grid gap-2">
                <Button
                  variant="primary"
                  onClick={handleSync}
                  disabled={isSyncing || (!delivery.partner_ref && !delivery.resendable)}
                  className="d-flex align-items-center justify-content-center gap-2"
                >
                  {isSyncing ? (
                    <>
                      <Spinner animation="border" size="sm" />
                      <span>Đang lấy dữ liệu từ ĐVVC...</span>
                    </>
                  ) : (
                    <>
                      <svg
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
                      </svg>
                      <span>
                        {delivery.resendable ? 'Gửi lại yêu cầu cho ĐVVC' : 'Đồng bộ từ ĐVVC ngay'}
                      </span>
                    </>
                  )}
                </Button>

                {delivery.cancellable && (
                  <Button
                    variant="outline-danger"
                    disabled={isSyncing}
                    onClick={() => setCancelReason('')}
                  >
                    Huỷ vận đơn
                  </Button>
                )}

                {delivery.tracking_number && (delivery.tracking_url || carrier.trackingUrl) && (
                  <a
                    href={delivery.tracking_url || carrier.trackingUrl(delivery.tracking_number)}
                    target="_blank"
                    rel="noreferrer noopener"
                    className="btn btn-outline-secondary d-flex align-items-center justify-content-center gap-2"
                  >
                    <span>Mở cổng tra cứu {carrier.shortName}</span>
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
                      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3" />
                    </svg>
                  </a>
                )}
              </div>

              <hr />

              <div className="small text-secondary">
                Hotline hỗ trợ đối tác:{' '}
                <a
                  href={`tel:${carrier.hotline.replace(/\s+/g, '')}`}
                  className="fw-bold text-decoration-none text-primary"
                >
                  {carrier.hotline}
                </a>
              </div>
            </Card.Body>
          </Card>

          {/* Card 4: Thông tin Giao nhận & Người nhận */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body className="p-4">
              <h3 className="h6 fw-bold mb-3 border-bottom pb-2">Thông tin người nhận</h3>

              <div className="mb-2">
                <span className="text-secondary small d-block">Khách hàng:</span>
                <span className="fw-semibold">{delivery.receiver_name}</span>
              </div>

              <div className="mb-2">
                <span className="text-secondary small d-block">Số điện thoại:</span>
                <a
                  href={`tel:${delivery.receiver_phone}`}
                  className="fw-semibold text-decoration-none"
                >
                  {delivery.receiver_phone}
                </a>
              </div>

              <div className="mb-2">
                <span className="text-secondary small d-block">Địa chỉ nhận hàng:</span>
                <span className="text-dark small">{delivery.shipping_address}</span>
              </div>

              {delivery.note && (
                <div className="mb-2">
                  <span className="text-secondary small d-block">Ghi chú giao:</span>
                  <span className="small fst-italic text-secondary">{delivery.note}</span>
                </div>
              )}

              {/* Thông tin shipper ĐVVC */}
              {delivery.shipper_name && (
                <div className="shipper-card mt-3">
                  <div className="small fw-bold text-primary mb-1">Bưu tá phụ trách giao hàng</div>
                  <div className="fw-semibold">{delivery.shipper_name}</div>
                  {delivery.shipper_phone && (
                    <div className="small text-secondary">
                      SĐT:{' '}
                      <a
                        href={`tel:${delivery.shipper_phone.replace(/\./g, '')}`}
                        className="fw-bold text-decoration-none"
                      >
                        {delivery.shipper_phone}
                      </a>
                    </div>
                  )}
                </div>
              )}
            </Card.Body>
          </Card>

          {/* Card 5: Tài chính & Tiền thu hộ COD */}
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              <h3 className="h6 fw-bold mb-3 border-bottom pb-2">Cước phí & Thu hộ</h3>

              <div className="d-flex justify-content-between mb-2">
                <span className="text-secondary small">Gói dịch vụ:</span>
                <span className="fw-semibold small">
                  {carrierServices[delivery.service_type] || 'Tiêu chuẩn'}
                </span>
              </div>

              <div className="d-flex justify-content-between mb-2">
                <span className="text-secondary small">Phí vận chuyển:</span>
                <span className="fw-semibold small">{formatMoney(delivery.shipping_fee)}</span>
              </div>

              <div className="d-flex justify-content-between mb-2">
                <span className="text-secondary small">Dự kiến giao:</span>
                <span className="fw-semibold small">
                  {formatDateTime(delivery.estimated_delivery_date)}
                </span>
              </div>

              <div className="d-flex justify-content-between align-items-center pt-2 border-top mt-2">
                <div>
                  <span className="fw-bold d-block">Tiền thu hộ COD:</span>
                  <small className="text-secondary">
                    {delivery.cod_amount > 0 ? 'Bưu tá ĐVVC sẽ thu' : 'Đã thanh toán online'}
                  </small>
                </div>
                <span className="fs-5 fw-bold text-danger">{formatMoney(delivery.cod_amount)}</span>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Modal show={cancelReason !== null} onHide={() => setCancelReason(null)} centered>
        <Form onSubmit={handleCancel}>
          <Modal.Header closeButton>
            <Modal.Title className="h5">Huỷ vận đơn</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Group controlId="cancel-reason">
              <Form.Label>Lý do huỷ</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                maxLength={300}
                required
                value={cancelReason || ''}
                onChange={(e) => setCancelReason(e.target.value)}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={() => setCancelReason(null)}>
              Đóng
            </Button>
            <Button type="submit" variant="danger" disabled={isSyncing || !cancelReason?.trim()}>
              Xác nhận huỷ
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </>
  );
}
