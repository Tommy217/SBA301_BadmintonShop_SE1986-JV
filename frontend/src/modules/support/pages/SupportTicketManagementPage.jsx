import { ManagerSearch, ManagerPagination } from '@/shared/components/ManagerControls';
import { useMemo, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Modal, Row, Table } from 'react-bootstrap';
import { Link, useLocation } from 'react-router';
import { ticketStatuses, ticketTypes } from '@/modules/support/constants';
import { useShop } from '@/app/providers/shop/ShopContext';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { ROLES } from '@/shared/constants/roles';
import { supportApi } from '@/modules/support/api/support.api';
import { formatDateTime, formatMoney } from '@/shared/utils/format';

const EMPTY_LIST = [];

export default function SupportTicketManagementPage() {
  const { data, act } = useShop();
  const { user } = useAuth();
  const { state } = useLocation();
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');

  const currentThreshold = Number(data?.refund_approval_threshold) || 3000000;
  const role = user?.role?.toUpperCase();
  const isManager = role === ROLES.MANAGER || role === ROLES.ADMIN;

  const [showThresholdModal, setShowThresholdModal] = useState(false);
  const [newThresholdInput, setNewThresholdInput] = useState('');
  const [thresholdError, setThresholdError] = useState('');
  const [thresholdSuccess, setThresholdSuccess] = useState('');
  const [savingThreshold, setSavingThreshold] = useState(false);

  const tickets = data?.support_tickets || EMPTY_LIST;
  const orders = data?.orders || EMPTY_LIST;
  const normalizedQuery = query.trim().toLocaleLowerCase('vi');

  const filteredTickets = useMemo(() => {
    return [...tickets]
      .filter((ticket) => {
        const order = orders.find((item) => item.id === ticket.order_id);
        const matchesStatus =
          statusFilter === 'all' ||
          ticket.status === statusFilter ||
          (statusFilter === 'PENDING' && ['open', 'under_review'].includes(ticket.status));

        const matchesType =
          typeFilter === 'all' ||
          ticket.ticket_type === typeFilter ||
          (typeFilter === 'REFUND' && ticket.ticket_type === 'return');

        const searchableText = [
          ticket.ticket_code,
          ticket.reason,
          ticket.user_id,
          order?.order_code,
          order?.receiver_name,
          order?.receiver_phone,
        ]
          .filter(Boolean)
          .join(' ')
          .toLocaleLowerCase('vi');

        return matchesStatus && matchesType && (!normalizedQuery || searchableText.includes(normalizedQuery));
      })
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  }, [normalizedQuery, orders, statusFilter, typeFilter, tickets]);

  const countByStatus = (status) =>
    tickets.filter(
      (ticket) =>
        ticket.status === status ||
        (status === 'PENDING' && ['open', 'under_review'].includes(ticket.status)),
    ).length;

  return (
    <section className="manager-page manager-support-page">
      <div className="eyebrow">QUẢN LÝ VẬN HÀNH (UC15)</div>
      <div className="manager-title mb-4">
        <div>
          <h1 className="h2 fw-bold mb-1">Quản lý Trả hàng & Hoàn tiền</h1>
          <p className="text-secondary mb-0">
            Hàng đợi tiếp nhận, thẩm định sản phẩm và quy trình hoàn tiền theo chuẩn UC15.
          </p>
        </div>
        <div className="d-flex align-items-center gap-2 flex-wrap">
          <Badge bg="secondary" className="fs-6 py-2 px-3">
            Ngưỡng Quản lý duyệt: {formatMoney(currentThreshold)}
          </Badge>
          {isManager && (
            <Button
              variant="outline-primary"
              size="sm"
              onClick={() => {
                setNewThresholdInput(String(currentThreshold));
                setThresholdError('');
                setThresholdSuccess('');
                setShowThresholdModal(true);
              }}
            >
              ⚙️ Đổi ngưỡng duyệt
            </Button>
          )}
          <Badge bg="primary" className="fs-6 py-2 px-3">
            Tổng số: {tickets.length} phiếu
          </Badge>
        </div>
      </div>

      {state?.updated && (
        <Alert variant="success" dismissible>
          Đã cập nhật trạng thái phiếu hỗ trợ thành công.
        </Alert>
      )}

      {/* MODAL THIẾT LẬP NGƯỠNG PHÊ DUYỆT */}
      <Modal show={showThresholdModal} onHide={() => setShowThresholdModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">Thiết lập ngưỡng Quản lý phê duyệt</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="small text-secondary mb-3">
            Các khoản hoàn tiền <strong>nhỏ hơn</strong> ngưỡng này sẽ do Nhân viên CSKH trực tiếp duyệt. Các khoản từ ngưỡng này trở lên bắt buộc phải do Quản lý (Manager) thẩm định và phê duyệt.
          </p>
          {thresholdError && <Alert variant="danger" className="py-2 small">{thresholdError}</Alert>}
          {thresholdSuccess && <Alert variant="success" className="py-2 small">{thresholdSuccess}</Alert>}
          <Form.Group controlId="thresholdInput">
            <Form.Label className="fw-semibold">Ngưỡng phê duyệt hoàn tiền (VNĐ)</Form.Label>
            <Form.Control
              type="number"
              step="100000"
              min="100000"
              placeholder="VD: 3000000"
              value={newThresholdInput}
              disabled={savingThreshold}
              onChange={(e) => setNewThresholdInput(e.target.value)}
            />
            <Form.Text className="text-muted d-block mt-1">
              Xem trước: <strong>{formatMoney(Number(newThresholdInput) || 0)}</strong>
            </Form.Text>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowThresholdModal(false)} disabled={savingThreshold}>
            Đóng
          </Button>
          <Button
            variant="primary"
            disabled={savingThreshold || !newThresholdInput}
            onClick={async () => {
              const val = Number(newThresholdInput);
              if (!val || val <= 0) {
                setThresholdError('Vui lòng nhập số tiền hợp lệ (> 0).');
                return;
              }
              setSavingThreshold(true);
              setThresholdError('');
              try {
                await act(() => supportApi.updateThreshold(val));
                setThresholdSuccess('Đã cập nhật ngưỡng phê duyệt thành công!');
                setTimeout(() => setShowThresholdModal(false), 1200);
              } catch (e) {
                setThresholdError(e.message || 'Lỗi khi cập nhật ngưỡng.');
              } finally {
                setSavingThreshold(false);
              }
            }}
          >
            {savingThreshold ? 'Đang lưu…' : 'Lưu thay đổi'}
          </Button>
        </Modal.Footer>
      </Modal>

      {/* STAT CARDS */}
      <Row className="g-3 mb-4">
        {[
          ['Chờ sơ duyệt', countByStatus('PENDING'), 'warning'],
          ['Chờ khách gửi hàng', countByStatus('AWAITING_RETURN'), 'info'],
          [`Chờ Quản lý duyệt (>= ${formatMoney(currentThreshold)})`, countByStatus('PENDING_APPROVAL'), 'primary'],
          ['Chờ chuyển tiền', countByStatus('PENDING_TRANSFER'), 'warning'],
          ['Đã hoàn tất', countByStatus('REFUNDED'), 'success'],
        ].map(([label, count, variant]) => (
          <Col xs={6} md={4} xl className="col-custom" key={label}>
            <Card className="border-0 shadow-sm h-100">
              <Card.Body className="p-3">
                <div className="small text-secondary text-truncate">{label}</div>
                <div className={`fs-3 fw-bold text-${variant}`}>{count}</div>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {/* BỘ LỌC VÀ BẢNG DỮ LIỆU */}
      <Card className="border-0 shadow-sm">
        <Card.Body className="p-3 p-md-4">
          <Row className="g-3 mb-4">
            <Col md={6}>
              <Form.Label htmlFor="ticket-search" className="fw-semibold small">
                Tìm kiếm yêu cầu
              </Form.Label>
              <ManagerSearch
                label="Tìm kiếm phiếu"
                value={query}
                onChange={(value) => {
                  setQuery(value);
                  setPage(1);
                }}
                placeholder="Tìm mã phiếu, mã đơn hàng, tên khách hàng, SĐT…"
              />
            </Col>

            <Col md={3}>
              <Form.Label htmlFor="ticket-type-filter" className="fw-semibold small">
                Loại yêu cầu
              </Form.Label>
              <Form.Select
                id="ticket-type-filter"
                value={typeFilter}
                onChange={(e) => {
                  setTypeFilter(e.target.value);
                  setPage(1);
                }}
              >
                <option value="all">Tất cả loại yêu cầu</option>
                <option value="REFUND">{ticketTypes.REFUND}</option>
              </Form.Select>
            </Col>

            <Col md={3}>
              <Form.Label htmlFor="ticket-status-filter" className="fw-semibold small">
                Trạng thái xử lý
              </Form.Label>
              <Form.Select
                id="ticket-status-filter"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(1);
                }}
              >
                <option value="all">Tất cả trạng thái</option>
                {Object.entries(ticketStatuses).map(([value, s]) => (
                  <option key={value} value={value}>
                    {s.label}
                  </option>
                ))}
              </Form.Select>
            </Col>
          </Row>

          {filteredTickets.length === 0 ? (
            <div className="empty-state p-5 text-center">
              <h2 className="h5 fw-bold">Không tìm thấy phiếu hỗ trợ nào</h2>
              <p className="text-secondary mb-0">Thử thay đổi từ khóa tìm kiếm hoặc bộ lọc trạng thái.</p>
            </div>
          ) : (
            <Table responsive hover className="manager-table align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Mã phiếu / Thời gian</th>
                  <th>Khách hàng & Đơn hàng</th>
                  <th>Loại yêu cầu</th>
                  <th>Số tiền hoàn</th>
                  <th>Trạng thái</th>
                  <th className="manager-action-cell text-end">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredTickets
                  .slice(
                    (Math.min(page, Math.max(1, Math.ceil(filteredTickets.length / 10))) - 1) * 10,
                    Math.min(page, Math.max(1, Math.ceil(filteredTickets.length / 10))) * 10,
                  )
                  .map((ticket) => {
                    const statusConfig = ticketStatuses[ticket.status] || {
                      label: ticket.status || 'Chưa xác định',
                      variant: 'secondary',
                    };
                    const order = orders.find((item) => item.id === ticket.order_id);

                    return (
                      <tr key={ticket.id}>
                        <td>
                          <Link
                            to={`/management/support-tickets/${encodeURIComponent(ticket.id)}`}
                            className="fw-bold text-decoration-none"
                          >
                            {ticket.ticket_code}
                          </Link>
                          <small className="d-block text-secondary">
                            {formatDateTime(ticket.created_at)}
                          </small>
                        </td>

                        <td>
                          <div className="fw-medium">{order?.receiver_name || ticket.user_id}</div>
                          <div className="small text-secondary">
                            Đơn: {order?.order_code || 'Không có mã đơn'}
                          </div>
                        </td>

                        <td>
                          <Badge bg="dark">
                            {ticketTypes[ticket.ticket_type] || ticket.ticket_type}
                          </Badge>
                        </td>

                        <td>
                          {ticket.refund_amount != null ? (
                            <strong className="text-success">{formatMoney(ticket.refund_amount)}</strong>
                          ) : (
                            <span className="text-muted">-</span>
                          )}
                        </td>

                        <td>
                          <Badge
                            bg={statusConfig.variant}
                            text={statusConfig.variant === 'warning' ? 'dark' : undefined}
                          >
                            {statusConfig.label}
                          </Badge>
                        </td>

                        <td className="text-end">
                          <Link
                            to={`/management/support-tickets/${encodeURIComponent(ticket.id)}`}
                            className="btn btn-sm btn-outline-primary text-nowrap"
                          >
                            Xem / Xử lý
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </Table>
          )}

          <ManagerPagination total={filteredTickets.length} page={page} onChange={setPage} />
        </Card.Body>
      </Card>
    </section>
  );
}
