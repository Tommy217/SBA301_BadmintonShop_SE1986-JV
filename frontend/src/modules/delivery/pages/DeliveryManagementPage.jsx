import { useMemo, useState } from 'react';
import { Alert, Badge, Button, Card, Form, Spinner, Table } from 'react-bootstrap';
import { Link, useLocation } from 'react-router';
import { carriers, carrierServices, deliveryStatuses } from '@/modules/delivery/constants';
import { useShipments } from '@/modules/delivery/hooks/useDelivery';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { deliveryApi } from '@/modules/delivery/api/delivery.api';


const EMPTY_LIST = [];

export default function DeliveryManagementPage() {
  const { data: shipmentPage, isLoading, error, reload } = useShipments();
  const { state } = useLocation();
  const [query, setQuery] = useState('');
  const [carrierFilter, setCarrierFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [syncingId, setSyncingId] = useState(null);
  const [syncMessage, setSyncMessage] = useState(
    state?.updated ? "Đã cập nhật vận đơn thành công." : "",
  );
  const [syncFailed, setSyncFailed] = useState(false);

  const deliveries = shipmentPage?.content || EMPTY_LIST;
  const normalizedQuery = query.trim().toLocaleLowerCase('vi');

  const filteredDeliveries = useMemo(
    () =>
      [...deliveries]
        .filter((item) => {
          const matchesCarrier = carrierFilter === 'all' || item.carrier === carrierFilter;
          const matchesStatus = statusFilter === 'all' || item.status === statusFilter;
          const searchableText = [
            item.tracking_number,
            item.order_code,
            item.receiver_name,
            item.receiver_phone,
            item.shipping_address,
            item.carrier_status_text,
          ]
            .filter(Boolean)
            .join(' ')
            .toLocaleLowerCase('vi');
          return (
            matchesCarrier &&
            matchesStatus &&
            (!normalizedQuery || searchableText.includes(normalizedQuery))
          );
        })
        .sort(
          (a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0),
        ),
    [carrierFilter, deliveries, normalizedQuery, statusFilter],
  );

  const countByStatus = (status) => deliveries.filter((d) => d.status === status).length;

  async function handleSync(delivery) {
    setSyncingId(delivery.id);
    setSyncMessage('');
    setSyncFailed(false);
    try {
      // chua gui duoc sang doi tac thi gui lai, nguoc lai doc trang thai moi nhat
      if (delivery.resendable) {
        await deliveryApi.resend(delivery.id);
        setSyncMessage(`Đã gửi lại yêu cầu vận đơn cho đơn ${delivery.order_code}.`);
      } else {
        await deliveryApi.sync(delivery.id);
        const carrierInfo = carriers[delivery.carrier];
        setSyncMessage(
          `Đã đồng bộ trạng thái mới nhất từ ${carrierInfo?.name || 'đối tác vận chuyển'} cho mã vận đơn ${delivery.tracking_number}.`,
        );
      }
      reload();
    } catch (err) {
      setSyncFailed(true);
      setSyncMessage(err.message || 'Không thể đồng bộ với đối tác vận chuyển.');

    } finally {
      setSyncingId(null);
    }
  }

  return (
    <>
      <div className="eyebrow">QUẢN LÝ VẬN HÀNH & GIAO VẬN</div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <h1 className="h2 fw-bold mb-2">Vận đơn & Giao hàng (3rd-Party)</h1>
          <p className="text-secondary mb-0">
            Theo dõi hành trình bưu cục và đồng bộ trạng thái trực tiếp từ các đối tác giao hàng bên
            thứ 3.

          </p>
        </div>
        <div className="d-flex gap-2 align-items-center">
          <Badge bg="light" text="dark" className="fs-6 border">
            {deliveries.length} vận đơn
          </Badge>
        </div>
      </div>

      {syncMessage && (
        <Alert variant={syncFailed ? 'danger' : 'success'} dismissible onClose={() => setSyncMessage('')} className="mb-4">
          {syncMessage}
        </Alert>
      )}

      {error && (
        <Alert variant="danger" className="mb-4">
          {error.message || 'Không tải được danh sách vận đơn.'}{' '}
          <Button variant="link" size="sm" className="p-0 align-baseline" onClick={reload}>
            Thử lại
          </Button>
        </Alert>
      )}

      {/* Thẻ thống kê 4 trạng thái chính */}
      <div className="row g-3 mb-4">
        {[
          ['Chờ lấy hàng', countByStatus('pending_pickup'), 'warning'],
          ['Đang trung chuyển', countByStatus('in_transit'), 'info'],
          ['Đang phát hàng', countByStatus('out_for_delivery'), 'primary'],
          ['Giao thành công', countByStatus('delivered'), 'success'],
        ].map(([label, count, variant]) => (
          <div className="col-6 col-xl-3" key={label}>
            <Card className="border-0 shadow-sm h-100">
              <Card.Body className="p-3">
                <div className="small text-secondary mb-1">{label}</div>
                <div className={`fs-3 fw-bold text-${variant}`}>{count}</div>
              </Card.Body>
            </Card>
          </div>
        ))}
      </div>

      <Card className="border-0 shadow-sm">
        <Card.Body className="p-3 p-md-4">
          {/* Bộ lọc & Tìm kiếm */}
          <div className="row g-3 mb-4">
            <div className="col-lg-6">
              <Form.Label
                htmlFor="delivery-search"
                className="fw-semibold small"
              >
                Tìm kiếm vận đơn
              </Form.Label>
              <Form.Control
                id="delivery-search"
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Mã vận đơn, mã đơn, khách hàng, số điện thoại, địa chỉ..."
              />
            </div>
            <div className="col-sm-6 col-lg-3">
              <Form.Label
                htmlFor="carrier-filter"
                className="fw-semibold small"
              >
                Đối tác vận chuyển
              </Form.Label>
              <Form.Select
                id="carrier-filter"
                value={carrierFilter}
                onChange={(e) => setCarrierFilter(e.target.value)}
              >
                <option value="all">Tất cả đối tác</option>
                {Object.entries(carriers).map(([key, val]) => (
                  <option key={key} value={key}>
                    {val.name}
                  </option>
                ))}
              </Form.Select>
            </div>
            <div className="col-sm-6 col-lg-3">
              <Form.Label htmlFor="status-filter" className="fw-semibold small">
                Trạng thái ĐVVC
              </Form.Label>
              <Form.Select
                id="status-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="all">Tất cả trạng thái</option>
                {Object.entries(deliveryStatuses).map(([key, val]) => (
                  <option key={key} value={key}>
                    {val.label}
                  </option>
                ))}
              </Form.Select>
            </div>
          </div>

          {isLoading && !shipmentPage ? (
            <div className="py-5 text-center">
              <Spinner animation="border" size="sm" className="me-2" />
              Đang tải vận đơn...
            </div>
          ) : filteredDeliveries.length === 0 ? (
            <div className="empty-state py-5 text-center">
              <h2 className="h5 fw-bold mb-2">
                Không tìm thấy vận đơn phù hợp
              </h2>
              <p className="text-secondary mb-3">
                Thử thay đổi từ khóa tìm kiếm hoặc bỏ chọn các bộ lọc trạng thái
                / đối tác.
              </p>
              {(query || carrierFilter !== 'all' || statusFilter !== 'all') && (
                <Button
                  variant="outline-secondary"
                  size="sm"
                  onClick={() => {
                    setQuery('');
                    setCarrierFilter('all');
                    setStatusFilter('all');
                  }}
                >
                  Xóa bộ lọc
                </Button>
              )}
            </div>
          ) : (
            <Table responsive hover className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Mã vận đơn</th>
                  <th>Đơn hàng & Người nhận</th>
                  <th>Đối tác & Gói dịch vụ</th>
                  <th>Phí vận chuyển</th>
                  <th>Trạng thái ĐVVC</th>
                  <th>Đồng bộ gần nhất</th>
                  <th className="text-end">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredDeliveries.map((delivery) => {
                  const carrier = carriers[delivery.carrier] || {
                    name: delivery.carrier?.toUpperCase() || 'ĐVVC',
                    shortName: delivery.carrier?.toUpperCase() || 'ĐVVC',
                    badgeColor: '#333',
                    badgeBg: '#eee',
                  };
                  const statusInfo = deliveryStatuses[delivery.status] || {
                    label: delivery.status || 'Chưa xác định',
                    variant: 'secondary',
                  };
                  const isSyncing = syncingId === delivery.id;

                  return (
                    <tr key={delivery.id}>
                      {/* Cột 1: Mã vận đơn */}
                      <td>
                        <Link
                          to={`/management/deliveries/${encodeURIComponent(delivery.id)}`}
                          className="fw-bold text-decoration-none"
                        >
                          {delivery.tracking_number || 'Chưa có mã'}
                        </Link>
                        <div
                          className="small text-secondary text-truncate"
                          style={{ maxWidth: 220 }}
                          title={delivery.carrier_status_text}
                        >
                          {delivery.carrier_status_text || 'Chờ cập nhật'}
                        </div>
                      </td>

                      {/* Cột 2: Đơn hàng & Người nhận */}
                      <td>
                        <div className="fw-semibold">
                          {delivery.receiver_name}
                        </div>
                        <div className="small text-secondary">
                          Đơn:{" "}
                          <span className="font-monospace">
                            {delivery.order_code}
                          </span>
                        </div>
                        <div
                          className="small text-muted text-truncate"
                          style={{ maxWidth: 200 }}
                          title={delivery.shipping_address}
                        >
                          {delivery.shipping_address}
                        </div>
                      </td>

                      {/* Cột 3: Đối tác vận chuyển */}
                      <td>
                        <span
                          className="badge"
                          style={{
                            backgroundColor: carrier.badgeBg,
                            color: carrier.badgeColor,
                            fontWeight: 600,
                            fontSize: '12px',
                            padding: '6px 10px',
                          }}
                        >
                          {carrier.shortName}
                        </span>
                        <div className="small text-secondary mt-1">
                          {carrierServices[delivery.service_type] || 'Tiêu chuẩn'}

                        </div>
                      </td>

                      {/* Cột 4: Phí vận chuyển (không thu hộ COD) */}
                      <td>
                        <div className="fw-semibold">
                          {formatMoney(delivery.shipping_fee)}
                          <small className="d-block text-secondary fw-normal">Đối tác báo</small>
                        </div>
                      </td>

                      {/* Cột 5: Trạng thái ĐVVC */}
                      <td>
                        <Badge
                          bg={statusInfo.variant}
                          text={statusInfo.variant === 'warning' ? 'dark' : undefined}

                          className="px-2 py-1"
                        >
                          {statusInfo.label}
                        </Badge>
                      </td>

                      {/* Cột 6: Đồng bộ gần nhất */}
                      <td className="small text-nowrap text-secondary">
                        {formatDateTime(delivery.last_synced_at)}
                      </td>

                      {/* Cột 7: Thao tác */}
                      <td className="text-end">
                        <div className="d-inline-flex gap-1 align-items-center">
                          <Button
                            variant="outline-secondary"
                            size="sm"
                            title={delivery.resendable ? 'Gửi lại yêu cầu cho ĐVVC' : 'Đồng bộ trạng thái từ ĐVVC'}
                            onClick={() => handleSync(delivery)}
                            disabled={isSyncing || (!delivery.partner_ref && !delivery.resendable)}
                            className="btn-sync-carrier"
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
                            <span className="d-none d-md-inline ms-1">
                              {delivery.resendable ? 'Gửi lại' : 'Đồng bộ'}
                            </span>
                          </Button>

                          <Link
                            to={`/management/deliveries/${encodeURIComponent(delivery.id)}`}
                            className="btn btn-sm btn-outline-primary text-nowrap"
                          >
                            Chi tiết
                          </Link>

                          {delivery.tracking_number && (delivery.tracking_url || carrier.trackingUrl) && (
                            <a
                              href={
                                delivery.tracking_url || carrier.trackingUrl(delivery.tracking_number)
                              }
                              target="_blank"
                              rel="noreferrer noopener"
                              className="btn btn-sm btn-light text-secondary"
                              title="Tra cứu trên website đối tác"
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
                                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3" />
                              </svg>
                            </a>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>
    </>
  );
}
