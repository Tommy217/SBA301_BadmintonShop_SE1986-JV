import { useState, useMemo } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Image, Modal, Row, Table } from 'react-bootstrap';
import { Link, useNavigate, useParams } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { useShop } from '@/app/providers/shop/ShopContext';
import { supportApi } from '@/modules/support/api/support.api';
import {
  ticketStatuses,
  ticketTypes,
  ticketCauses,
  inspectionResults,
  APPROVAL_THRESHOLD,
} from '@/modules/support/constants';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { ROLES } from '@/shared/constants/roles';

export default function SupportTicketManagementDetailPage() {
  const { ticketId } = useParams();
  const { data, busy, act } = useShop();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [previewImage, setPreviewImage] = useState(null);

  // Form states for workflow
  // 1. Initial Review
  const [reviewCause, setReviewCause] = useState('STORE_FAULT');
  const [rejectReason, setRejectReason] = useState('');

  // 2. Inspection
  const [inspectionResult, setInspectionResult] = useState('PASSED');
  const [inspectionNote, setInspectionNote] = useState('');

  // 4. Refund transfer
  const [bankReference, setBankReference] = useState('');
  const [transferNote, setTransferNote] = useState('');

  // 5. Manager Decision
  const [managerRejectReason, setManagerRejectReason] = useState('');

  const ticket = useMemo(() => {
    return (data?.support_tickets || []).find((item) => String(item.id) === ticketId);
  }, [data?.support_tickets, ticketId]);

  if (!ticket) {
    return (
      <section className="manager-page">
        <h1 className="h2 fw-bold mb-2">Không tìm thấy phiếu hỗ trợ</h1>
        <p className="text-secondary mb-4">Phiếu hỗ trợ không tồn tại hoặc đã bị xóa.</p>
        <Button className="app-back-button" as={Link} to="/management/support-tickets" variant="outline-primary">
          Quay lại danh sách
        </Button>
      </section>
    );
  }

  const role = user?.role?.toUpperCase();
  const isManager = role === ROLES.MANAGER || role === ROLES.ADMIN;

  const currentThreshold = Number(data?.refund_approval_threshold) || APPROVAL_THRESHOLD;

  const order = (data?.orders || []).find((item) => item.id === ticket.order_id);
  const allOrderItems = data?.order_items || [];
  const allVariants = data?.product_variants || [];

  const ticketItems = (data?.ticket_items || [])
    .filter((item) => item.ticket_id === ticket.id)
    .map((item) => {
      const orderItem = allOrderItems.find((oi) => oi.id === item.order_item_id);
      return { ...item, orderItem };
    })
    .filter((item) => item.orderItem);

  const statusConfig = ticketStatuses[ticket.status] || {
    label: ticket.status || 'Chưa xác định',
    variant: 'secondary',
  };

  const statusHistory = (data?.ticket_status_history || [])
    .filter((h) => h.ticket_id === ticket.id)
    .sort((a, b) => new Date(a.created_at) - new Date(b.created_at));

  // Calculated refund amount if not yet set
  const calculatedRefund = useMemo(() => {
    if (ticket.refund_amount != null) return ticket.refund_amount;
    let sum = 0;
    for (const item of ticketItems) {
      sum += (item.orderItem?.unit_price || 0) * (item.quantity || 1);
    }
    if (ticket.cause === 'STORE_FAULT' && order) {
      sum += (order.shipping_fee || 0);
    }
    return sum;
  }, [ticket.refund_amount, ticket.cause, ticketItems, order]);

  // ACTION 1: SƠ DUYỆT - DUYỆT
  async function handleApproveReview() {
    setError('');
    setSuccessMsg('');
    try {
      await act(() =>
        supportApi.staffReviewTicket({
          ticketId: ticket.id,
          action: 'approve',
          cause: reviewCause,
          note: `Nhân viên chấp thuận tiếp nhận. Nguyên nhân: ${reviewCause}`,
        }),
      );
      setSuccessMsg('Đã sơ duyệt thành công. Phiếu chuyển sang Chờ gửi lại hàng.');
    } catch (e) {
      setError(e.message || 'Lỗi khi duyệt phiếu.');
    }
  }

  // ACTION 1: SƠ DUYỆT - TỪ CHỐI
  async function handleRejectReview(e) {
    e.preventDefault();
    if (!rejectReason.trim()) {
      setError('Vui lòng nhập lý do từ chối yêu cầu.');
      return;
    }
    setError('');
    setSuccessMsg('');
    try {
      await act(() =>
        supportApi.staffReviewTicket({
          ticketId: ticket.id,
          action: 'reject',
          rejectReason: rejectReason.trim(),
        }),
      );
      setSuccessMsg('Đã từ chối yêu cầu.');
    } catch (e) {
      setError(e.message || 'Lỗi khi từ chối phiếu.');
    }
  }

  // ACTION 2: THẨM ĐỊNH HÀNG
  async function handleInspectionSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    try {
      await act(() =>
        supportApi.staffInspectTicket({
          ticketId: ticket.id,
          result: inspectionResult,
          note: inspectionNote.trim(),
        }),
      );
      setSuccessMsg(
        inspectionResult === 'PASSED'
          ? 'Đã ghi nhận thẩm định ĐẠT. Hệ thống đã cập nhật bước tiếp theo.'
          : 'Đã ghi nhận thẩm định KHÔNG ĐẠT. Phiếu chuyển sang thất bại.',
      );
    } catch (e) {
      setError(e.message || 'Lỗi khi ghi nhận biên bản kiểm tra.');
    }
  }

  // ACTION 4: DUYỆT HOÀN TIỀN DƯỚI 3 TRIỆU (STAFF)
  async function handleStaffApproveRefund() {
    setError('');
    setSuccessMsg('');
    try {
      await act(() => supportApi.staffApproveRefund({ ticketId: ticket.id }));
      setSuccessMsg('Nhân viên đã duyệt hoàn tiền. Phiếu chuyển sang Chờ chuyển tiền.');
    } catch (e) {
      setError(e.message || 'Lỗi khi duyệt hoàn tiền.');
    }
  }

  // ACTION 5: QUẢN LÝ DUYỆT / TỪ CHỐI (MANAGER DECISION)
  async function handleManagerDecision(decision) {
    if (decision === 'reject' && !managerRejectReason.trim()) {
      setError('Vui lòng nhập lý do từ chối phê duyệt hoàn tiền.');
      return;
    }
    setError('');
    setSuccessMsg('');
    try {
      await act(() =>
        supportApi.managerDecision({
          ticketId: ticket.id,
          decision,
          rejectReason: managerRejectReason.trim(),
        }),
      );
      setSuccessMsg(
        decision === 'approve'
          ? 'Quản lý đã phê duyệt khoản hoàn tiền. Phiếu chuyển sang Chờ chuyển tiền.'
          : 'Quản lý đã từ chối yêu cầu hoàn tiền.',
      );
    } catch (e) {
      setError(e.message || 'Lỗi khi ghi nhận quyết định Quản lý.');
    }
  }

  // ACTION 6: CHUYỂN KHOẢN THỦ CÔNG & LƯU MÃ GIAO DỊCH
  async function handleRecordRefundTransfer(e) {
    e.preventDefault();
    if (!bankReference.trim()) {
      setError('Vui lòng nhập mã giao dịch chuyển khoản ngân hàng.');
      return;
    }
    setError('');
    setSuccessMsg('');
    try {
      await act(() =>
        supportApi.staffRecordRefundTransfer({
          ticketId: ticket.id,
          bankReference: bankReference.trim(),
          note: transferNote.trim(),
        }),
      );
      setSuccessMsg('Đã hoàn tất chuyển khoản hoàn tiền. Phiếu hỗ trợ chuyển sang ĐÃ HOÀN TIỀN.');
    } catch (e) {
      setError(e.message || 'Lỗi khi lưu giao dịch hoàn tiền.');
    }
  }

  return (
    <section className="manager-page manager-support-page">
      <div className="eyebrow">QUẢN LÝ VẬN HÀNH</div>
      <div className="manager-title mb-4">
        <div>
          <h1 className="h2 fw-bold mb-1">Xử lý phiếu {ticket.ticket_code}</h1>
          <p className="text-secondary mb-0">
            Quy trình tiếp nhận sơ duyệt, kiểm định và giải quyết trả hàng & hoàn tiền.
          </p>
        </div>
        <Button className="app-back-button" as={Link} to="/management/support-tickets" variant="outline-secondary">
          ← Quay lại danh sách
        </Button>
      </div>

      {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}
      {successMsg && <Alert variant="success" dismissible onClose={() => setSuccessMsg('')}>{successMsg}</Alert>}

      {/* HEADER TỔNG QUAN */}
      <Card className="border-0 shadow-sm mb-4">
        <Card.Body className="p-4">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <div className="small text-secondary mb-1">Mã phiếu</div>
              <h2 className="h4 fw-bold mb-2">{ticket.ticket_code}</h2>
              <div className="d-flex flex-wrap gap-2 align-items-center">
                <Badge bg="dark" className="fs-6">
                  {ticketTypes[ticket.ticket_type] || ticket.ticket_type}
                </Badge>
                <Badge bg={statusConfig.variant} text={statusConfig.variant === 'warning' ? 'dark' : undefined} className="fs-6">
                  {statusConfig.label}
                </Badge>
                {ticket.cause && (
                  <Badge bg="secondary">
                    {ticketCauses[ticket.cause]?.label || ticket.cause}
                  </Badge>
                )}
              </div>
            </div>

            {calculatedRefund > 0 && (
              <div className="text-md-end p-3 rounded bg-light border">
                <div className="small text-secondary">Số tiền hoàn (dự kiến/đã duyệt)</div>
                <div className="fs-4 fw-bold text-success">{formatMoney(calculatedRefund)}</div>
                <div className="small text-muted">
                  {calculatedRefund >= currentThreshold ? 'Cần Quản lý duyệt' : 'Nhân viên có quyền duyệt'}
                </div>
              </div>
            )}
          </div>

          <hr className="my-3" />

          <Row className="g-3 small">
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Khách hàng:</span>
              <strong className="text-dark">{order?.receiver_name || ticket.user_id}</strong>
            </Col>
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Đơn hàng:</span>
              <strong className="text-dark">{order?.order_code || 'Không có mã'}</strong>
            </Col>
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Ngày tạo phiếu:</span>
              <span className="fw-semibold">{formatDateTime(ticket.created_at)}</span>
            </Col>
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Điện thoại / Địa chỉ:</span>
              <span className="text-truncate d-block">{order?.receiver_phone} · {order?.shipping_address}</span>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Row className="g-4 mb-4">
        {/* CỘT TRÁI: THÔNG TIN YÊU CẦU CỦA KHÁCH */}
        <Col lg={7}>
          {/* SẢN PHẨM YÊU CẦU */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">DANH SÁCH SẢN PHẨM KHÁCH YÊU CẦU</h2>
            </Card.Header>
            <Card.Body className="p-0">
              <Table responsive hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Sản phẩm</th>
                    <th className="text-end" style={{ width: '110px' }}>Đơn giá</th>
                    <th className="text-center" style={{ width: '80px' }}>SL</th>
                    <th className="text-end" style={{ width: '120px' }}>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {ticketItems.map(({ id, quantity, orderItem }) => (
                    <tr key={id}>
                      <td>
                        <div className="fw-semibold">{orderItem?.product_name}</div>
                        {orderItem?.variant_info && (
                          <small className="text-secondary d-block">{orderItem.variant_info}</small>
                        )}
                      </td>
                      <td className="text-end">{formatMoney(orderItem?.unit_price || 0)}</td>
                      <td className="text-center">{quantity}</td>
                      <td className="text-end fw-semibold">
                        {formatMoney((orderItem?.unit_price || 0) * quantity)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>

          {/* LÝ DO & MÔ TẢ */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">NỘI DUNG LÝ DO & BẰNG CHỨNG</h2>
            </Card.Header>
            <Card.Body className="p-4">
              <div className="mb-3">
                <span className="small text-secondary d-block">Lý do yêu cầu:</span>
                <span className="fw-semibold">{ticket.reason}</span>
              </div>

              <div className="mb-4">
                <span className="small text-secondary d-block">Mô tả chi tiết:</span>
                <p className="text-secondary mb-0 text-break">
                  {ticket.description || 'Khách hàng không cung cấp mô tả chi tiết.'}
                </p>
              </div>

              {Array.isArray(ticket.evidence_urls) && ticket.evidence_urls.length > 0 && (
                <div>
                  <span className="small text-secondary d-block mb-2">Hình ảnh / Video bằng chứng:</span>
                  <div className="d-flex flex-wrap gap-2">
                    {ticket.evidence_urls.map((url, idx) => (
                      <div
                        key={idx}
                        className="border rounded p-1 bg-light"
                        style={{ cursor: 'pointer' }}
                        onClick={() => setPreviewImage(url)}
                      >
                        <Image
                          src={url}
                          alt={`Evidence ${idx + 1}`}
                          style={{ width: '100px', height: '75px', objectFit: 'cover' }}
                          rounded
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </Card.Body>
          </Card>

          {/* MÃ QR CỦA KHÁCH */}
          {ticket.refund_qr_url && (
            <Card className="border-0 shadow-sm mb-4">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-primary">MÃ QR NGÂN HÀNG CỦA KHÁCH</h2>
              </Card.Header>
              <Card.Body className="p-4 text-center">
                <div
                  className="p-2 border rounded d-inline-block bg-white shadow-sm"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setPreviewImage(ticket.refund_qr_url)}
                  title="Nhấn để phóng to mã QR"
                >
                  <Image
                    src={ticket.refund_qr_url}
                    alt="Mã QR nhận tiền"
                    style={{ maxHeight: '180px', objectFit: 'contain' }}
                  />
                </div>
                <div className="small text-muted mt-2">
                  * Nhấp vào ảnh để phóng to và quét qua ứng dụng ngân hàng khi thực hiện chuyển tiền.
                </div>
              </Card.Body>
            </Card>
          )}
        </Col>

        {/* CỘT PHẢI: KHỐI THAO TÁC THEO LUỒNG VÒNG ĐỜI (WORKFLOW ACTIONS) */}
        <Col lg={5}>
          {/* BƯỚC 1: SƠ DUYỆT (PENDING / OPEN) */}
          {['PENDING', 'open', 'under_review'].includes(ticket.status) && (
            <Card className="border-0 shadow-sm mb-4 border-top border-4 border-warning">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-warning text-dark">
                  ⚡ BƯỚC 1: SƠ DUYỆT YÊU CẦU (SLA 48 GIỜ)
                </h2>
              </Card.Header>
              <Card.Body className="p-4">
                <p className="small text-secondary mb-3">
                  Kiểm tra đơn hàng, thời hạn 7 ngày, lý do và bằng chứng trước khi tiếp nhận hoặc từ chối.
                </p>

                {/* Phê duyệt sơ bộ */}
                <div className="p-3 rounded bg-light border mb-4">
                  <div className="fw-bold mb-2 text-dark">Chấp thuận tiếp nhận:</div>
                  <Form.Group className="mb-3" controlId="review-cause">
                    <Form.Label className="small fw-semibold">
                      Xác định nguyên nhân <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Select
                      value={reviewCause}
                      onChange={(e) => setReviewCause(e.target.value)}
                    >
                      <option value="STORE_FAULT">
                        {ticketCauses.STORE_FAULT.label} (Shop hoàn tiền ship)
                      </option>
                      <option value="CUSTOMER_PREFERENCE">
                        {ticketCauses.CUSTOMER_PREFERENCE.label} (Khách chịu phí ship)
                      </option>
                    </Form.Select>
                    <Form.Text muted>
                      {ticketCauses[reviewCause]?.desc}
                    </Form.Text>
                  </Form.Group>

                  <Button
                    variant="success"
                    className="w-100"
                    disabled={busy}
                    onClick={handleApproveReview}
                  >
                    ✓ Chấp thuận & Chuyển sang chờ gửi hàng
                  </Button>
                </div>

                {/* Từ chối sơ bộ */}
                <div className="p-3 rounded bg-light border">
                  <div className="fw-bold mb-2 text-danger">Từ chối yêu cầu:</div>
                  <Form onSubmit={handleRejectReview}>
                    <Form.Group className="mb-3" controlId="reject-reason">
                      <Form.Label className="small fw-semibold">
                        Lý do từ chối <span className="text-danger">*</span>
                      </Form.Label>
                      <Form.Control
                        as="textarea"
                        rows={2}
                        required
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                        placeholder="Nêu rõ lý do từ chối (quá hạn 7 ngày, bằng chứng không hợp lệ...)"
                      />
                    </Form.Group>
                    <Button variant="outline-danger" type="submit" className="w-100" disabled={busy}>
                      ✕ Từ chối yêu cầu này
                    </Button>
                  </Form>
                </div>
              </Card.Body>
            </Card>
          )}

          {/* BƯỚC 2: THẨM ĐỊNH HÀNG HÓA KHI ĐANG Ở AWAITING_RETURN */}
          {ticket.status === 'AWAITING_RETURN' && (
            <Card className="border-0 shadow-sm mb-4 border-top border-4 border-info">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-info text-dark">
                  🔍 BƯỚC 2: BIÊN BẢN THẨM ĐỊNH HÀNG HOÀN VỀ
                </h2>
              </Card.Header>
              <Card.Body className="p-4">
                <p className="small text-secondary mb-3">
                  Khi khách gửi kiện hàng về kho, nhân viên mở hộp và ghi nhận biên bản kiểm tra:
                </p>

                <Form onSubmit={handleInspectionSubmit}>
                  <Form.Group className="mb-3" controlId="inspection-result">
                    <Form.Label className="fw-semibold small">
                      Kết quả kiểm tra <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Select
                      value={inspectionResult}
                      onChange={(e) => setInspectionResult(e.target.value)}
                    >
                      <option value="PASSED">PASSED - {inspectionResults.PASSED}</option>
                      <option value="FAILED">FAILED - {inspectionResults.FAILED}</option>
                    </Form.Select>
                  </Form.Group>

                  <Form.Group className="mb-3" controlId="inspection-note">
                    <Form.Label className="fw-semibold small">Ghi chú thẩm định chi tiết</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={3}
                      value={inspectionNote}
                      onChange={(e) => setInspectionNote(e.target.value)}
                      placeholder="Ghi nhận hiện trạng tem mác, bao bì, phụ kiện đính kèm..."
                    />
                  </Form.Group>

                  <Button
                    type="submit"
                    variant={inspectionResult === 'PASSED' ? 'primary' : 'danger'}
                    className="w-100"
                    disabled={busy}
                  >
                    Lưu kết quả thẩm định
                  </Button>
                </Form>
              </Card.Body>
            </Card>
          )}

          {/* BƯỚC 3: QUẢN LÝ PHÊ DUYỆT (PENDING_APPROVAL) */}
          {ticket.status === 'PENDING_APPROVAL' && (
            <Card className="border-0 shadow-sm mb-4 border-top border-4 border-primary">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-primary">
                  🛡️ BƯỚC 3: QUẢN LÝ PHÊ DUYỆT HOÀN TIỀN (&ge; {formatMoney(currentThreshold)})
                </h2>
              </Card.Header>
              <Card.Body className="p-4">
                <div className="alert alert-warning mb-3 small">
                  <strong>Hạn mức duyệt:</strong> Số tiền hoàn là{' '}
                  <strong>{formatMoney(calculatedRefund)}</strong> (&ge; {formatMoney(currentThreshold)}).
                  Chỉ tài khoản quyền <strong>Quản lý (Manager)</strong> mới có quyền phê duyệt chuyển tiền.
                </div>

                {isManager ? (
                  <div>
                    <Button
                      variant="success"
                      className="w-100 mb-3"
                      disabled={busy}
                      onClick={() => handleManagerDecision('approve')}
                    >
                      ✓ Quản lý Phê duyệt hoàn tiền
                    </Button>

                    <div className="p-3 bg-light rounded border">
                      <div className="small fw-semibold text-danger mb-2">Từ chối hoàn tiền:</div>
                      <Form.Control
                        as="textarea"
                        rows={2}
                        value={managerRejectReason}
                        onChange={(e) => setManagerRejectReason(e.target.value)}
                        placeholder="Nhập lý do quản lý từ chối hoàn tiền..."
                        className="mb-2"
                      />
                      <Button
                        variant="outline-danger"
                        size="sm"
                        className="w-100"
                        disabled={busy}
                        onClick={() => handleManagerDecision('reject')}
                      >
                        ✕ Quản lý Từ chối
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 rounded bg-light border text-center text-muted small">
                    🔒 Tài khoản hiện tại không có quyền Manager. Vui lòng chuyển phiếu này cho Quản lý phê duyệt.
                  </div>
                )}
              </Card.Body>
            </Card>
          )}

          {/* BƯỚC 4: CHUYỂN TIỀN HOÀN THỦ CÔNG (PENDING_TRANSFER) */}
          {ticket.status === 'PENDING_TRANSFER' && (
            <Card className="border-0 shadow-sm mb-4 border-top border-4 border-success">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-success">
                  💳 BƯỚC 4: CHUYỂN TIỀN THỦ CÔNG & GHI NHẬN MÃ GIAO DỊCH
                </h2>
              </Card.Header>
              <Card.Body className="p-4">
                <div className="text-center p-3 bg-light rounded border mb-3">
                  <div className="small text-secondary mb-1">Số tiền cần chuyển khoản cho khách</div>
                  <div className="fs-3 fw-bold text-success mb-2">{formatMoney(ticket.refund_amount || calculatedRefund)}</div>
                  <div className="small text-muted">
                    Nhân viên mở App ngân hàng quét mã QR ở cột bên trái và chuyển đúng số tiền trên.
                  </div>
                </div>

                <Form onSubmit={handleRecordRefundTransfer}>
                  <Form.Group className="mb-3" controlId="bank-reference">
                    <Form.Label className="small fw-semibold">
                      Mã giao dịch / Mã tham chiếu ngân hàng <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Control
                      required
                      value={bankReference}
                      onChange={(e) => setBankReference(e.target.value)}
                      placeholder="VD: FT26103198765432..."
                      className="font-monospace"
                    />
                    <Form.Text muted>
                      Nhập chính xác mã giao dịch chuyển tiền thành công từ app ngân hàng (bắt buộc, không trùng lặp).
                    </Form.Text>
                  </Form.Group>

                  <Form.Group className="mb-3" controlId="transfer-note">
                    <Form.Label className="small fw-semibold">Ghi chú chuyển khoản</Form.Label>
                    <Form.Control
                      value={transferNote}
                      onChange={(e) => setTransferNote(e.target.value)}
                      placeholder="Ghi chú thêm nếu có..."
                    />
                  </Form.Group>

                  <Button type="submit" variant="success" className="w-100" disabled={busy}>
                    ✓ Xác nhận đã chuyển khoản thành công
                  </Button>
                </Form>
              </Card.Body>
            </Card>
          )}

          {/* KHI ĐÃ KẾT THÚC HOÀN TẤT */}
          {['REFUNDED', 'COMPLETED', 'REJECTED', 'INSPECTION_FAILED', 'CANCELLED'].includes(ticket.status) && (
            <Card className="border-0 shadow-sm mb-4 bg-light">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-secondary">KẾT QUẢ XỬ LÝ PHIẾU HỖ TRỢ</h2>
              </Card.Header>
              <Card.Body className="p-4">
                {ticket.status === 'REFUNDED' && (
                  <div className="text-success">
                    <div className="fw-bold fs-6 mb-1">✓ ĐÃ HOÀN TIỀN THÀNH CÔNG</div>
                    <div className="small mb-1">Số tiền: <strong>{formatMoney(ticket.refund_amount)}</strong></div>
                    <div className="small">Mã GD: <strong>{ticket.refund_record?.bank_reference || '-'}</strong></div>
                  </div>
                )}
                {ticket.status === 'REJECTED' && (
                  <div className="text-danger">
                    <div className="fw-bold fs-6 mb-1">✕ YÊU CẦU ĐÃ BỊ TỪ CHỐI</div>
                    <div className="small">Lý do: {ticket.reject_reason || 'Không đáp ứng điều kiện'}</div>
                  </div>
                )}
                {ticket.status === 'INSPECTION_FAILED' && (
                  <div className="text-danger">
                    <div className="fw-bold fs-6 mb-1">✕ THẨM ĐỊNH HÀNG KHÔNG ĐẠT</div>
                    <div className="small">Sản phẩm hoàn về không đủ điều kiện hoàn tiền. Đã gửi trả lại khách.</div>
                  </div>
                )}
                {ticket.status === 'CANCELLED' && (
                  <div className="text-secondary">
                    <div className="fw-bold fs-6 mb-1">ĐÃ HỦY YÊU CẦU</div>
                    <div className="small">Yêu cầu đã được hủy theo quy định.</div>
                  </div>
                )}
              </Card.Body>
            </Card>
          )}

          {/* LỊCH SỬ CHUYỂN TRẠNG THÁI (AUDIT LOG & TIMELINE) */}
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">LỊCH SỬ TRẠNG THÁI (AUDIT TIMELINE)</h2>
            </Card.Header>
            <Card.Body className="p-4">
              {statusHistory.length === 0 ? (
                <div className="small text-muted">Chưa có lịch sử trạng thái.</div>
              ) : (
                <div className="timeline">
                  {statusHistory.map((h, i) => (
                    <div key={h.id || i} className="mb-3 ps-3 border-start border-2 border-primary position-relative">
                      <div className="small fw-bold text-dark">{h.to_status}</div>
                      <div className="small text-secondary">{h.note}</div>
                      <div className="small text-muted" style={{ fontSize: '11px' }}>
                        {formatDateTime(h.created_at)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* MODAL PHÓNG TO HÌNH ẢNH */}
      <Modal show={Boolean(previewImage)} onHide={() => setPreviewImage(null)} size="lg" centered>
        <Modal.Body className="p-0 text-center bg-dark">
          <Image src={previewImage} alt="Xem trước" fluid style={{ maxHeight: '80vh' }} />
        </Modal.Body>
      </Modal>
    </section>
  );
}
