import { useState, useMemo } from 'react';
import { Alert, Badge, Button, Card, Col, Image, Modal, Row, Table } from 'react-bootstrap';
import { Link, useParams } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { supportApi } from '@/modules/support/api/support.api';
import { ticketStatuses, ticketTypes, ticketCauses } from '@/modules/support/constants';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import '../support.css';

export default function SupportTicketDetailPage() {
  const { ticketId } = useParams();
  const { data, busy, act } = useShop();
  const [error, setError] = useState('');
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);

  const ticket = useMemo(() => {
    return (data?.support_tickets || []).find((item) => String(item.id) === ticketId);
  }, [data?.support_tickets, ticketId]);

  if (!ticket) {
    return (
      <section className="support-page support-customer-page support-empty">
        <div className="eyebrow">HỖ TRỢ KHÁCH HÀNG</div>
        <h1 className="h2 fw-bold mb-2">Không tìm thấy phiếu hỗ trợ</h1>
        <p className="text-secondary mb-4">
          Phiếu hỗ trợ không tồn tại hoặc không thuộc tài khoản của bạn.
        </p>
        <Button className="app-back-button" as={Link} to="/support-tickets" variant="outline-primary">
          Quay lại danh sách phiếu hỗ trợ
        </Button>
      </section>
    );
  }

  const statusConfig = ticketStatuses[ticket.status] || {
    label: ticket.status || 'Chưa xác định',
    variant: 'secondary',
  };

  const order = (data?.orders || []).find((item) => item.id === ticket.order_id);
  const allOrderItems = data?.order_items || [];
  const allVariants = data?.product_variants || [];

  const ticketItems = (data?.ticket_items || [])
    .filter((item) => item.ticket_id === ticket.id)
    .map((item) => {
      const orderItem = allOrderItems.find((oi) => oi.id === item.order_item_id);
      return {
        ...item,
        orderItem,
      };
    })
    .filter((item) => item.orderItem);

  const statusHistory = (data?.ticket_status_history || [])
    .filter((h) => h.ticket_id === ticket.id)
    .sort((a, b) => new Date(a.created_at) - new Date(b.created_at));

  const allOrderTickets = useMemo(() => {
    return (data?.support_tickets || []).filter((t) => String(t.order_id) === String(ticket?.order_id));
  }, [data?.support_tickets, ticket?.order_id]);

  const canRetryTicket = useMemo(() => {
    if (!ticket || ticket.status !== 'REJECTED') return false;
    // Bị từ chối kiểm định thực tế tại kho -> không cho tạo lại
    if (ticket.inspection != null) return false;
    // Đã có phiếu khác đang xử lý (active)
    const hasActive = allOrderTickets.some((t) =>
      ['PENDING', 'AWAITING_RETURN', 'PENDING_APPROVAL', 'PENDING_TRANSFER'].includes(t.status),
    );
    if (hasActive) return false;
    // Giới hạn số lần không tính hủy < 2
    const nonCancelled = allOrderTickets.filter((t) => t.status !== 'CANCELLED');
    return nonCancelled.length < 2;
  }, [ticket, allOrderTickets]);

  // Cancel action
  async function handleCancelTicket() {
    setError('');
    try {
      await act(() => supportApi.cancelTicket(ticket.id));
      setShowCancelModal(false);
    } catch (e) {
      setError(e.message || 'Không thể hủy phiếu hỗ trợ.');
    }
  }

  const canCancel = ['PENDING', 'open'].includes(ticket.status);

  return (
    <section className="support-page support-customer-page support-detail-page">
      <div className="eyebrow">HỖ TRỢ KHÁCH HÀNG</div>
      <div className="support-heading d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
        <div>
          <h1 className="h2 fw-bold mb-1">Chi tiết phiếu {ticket.ticket_code}</h1>
          <p className="text-secondary mb-0">Theo dõi thông tin và tiến trình xử lý từ cửa hàng.</p>
        </div>
        <div className="support-heading-actions d-flex flex-wrap gap-2">
          {canCancel && (
            <Button variant="outline-danger" onClick={() => setShowCancelModal(true)} disabled={busy}>
              Hủy yêu cầu
            </Button>
          )}
          <Button className="app-back-button" as={Link} to="/support-tickets" variant="outline-secondary">
            ← Quay lại danh sách
          </Button>
        </div>
      </div>

      {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

      {/* BANNER HƯỚNG DẪN GỬI HÀNG KHI ĐANG Ở AWAITING_RETURN */}
      {ticket.status === 'AWAITING_RETURN' && (
        <Alert variant="info" className="border-info shadow-sm mb-4">
          <h4 className="alert-heading fw-bold fs-6 mb-2">📦 HƯỚNG DẪN GỬI HÀNG TRẢ VỀ CỬA HÀNG</h4>
          <p className="mb-2">
            Yêu cầu của bạn đã được nhân viên tiếp nhận. Vui lòng đóng gói sản phẩm cẩn thận (bao gồm tem mác, hộp và quà tặng nếu có) và gửi về địa chỉ:
          </p>
          <div className="p-3 bg-white rounded border border-info-subtle mb-2">
            <div><strong>Người nhận:</strong> Bộ phận Tiếp Nhận Hàng Hoàn - Badminton Shop</div>
            <div><strong>Địa chỉ:</strong> Số 123 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, TP. Hà Nội</div>
            <div><strong>Hotline hỗ trợ:</strong> 1900 6868 (8:00 - 21:00)</div>
            <div><strong>Mã tham chiếu ghi trên kiện hàng:</strong> <span className="badge bg-dark">{ticket.ticket_code}</span></div>
          </div>
          <div className="small text-muted">
            * Sau khi nhận được kiện hàng, nhân viên sẽ tiến hành kiểm định chất lượng trong vòng 24 giờ.
          </div>
        </Alert>
      )}

      {/* THẺ TỔNG QUAN TRẠNG THÁI */}
      <Card className="border-0 shadow-sm mb-4">
        <Card.Body className="p-4">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <div className="small text-secondary mb-1">Mã phiếu hỗ trợ</div>
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

            {ticket.refund_amount != null && (
              <div className="text-md-end p-3 rounded bg-light border">
                <div className="small text-secondary">Số tiền hoàn đã xác nhận</div>
                <div className="fs-4 fw-bold text-success">{formatMoney(ticket.refund_amount)}</div>
              </div>
            )}
          </div>

          <hr className="my-3" />

          <Row className="g-3 small">
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Ngày tạo phiếu:</span>
              <span className="fw-semibold">{formatDateTime(ticket.created_at)}</span>
            </Col>
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Cập nhật lần cuối:</span>
              <span className="fw-semibold">{formatDateTime(ticket.updated_at)}</span>
            </Col>
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Mã đơn hàng:</span>
              <span className="fw-semibold">{order?.order_code || 'Không có'}</span>
            </Col>
            <Col sm={6} md={3}>
              <span className="text-secondary d-block">Tổng tiền đơn hàng:</span>
              <span className="fw-semibold">{order ? formatMoney(order.total_amount) : '-'}</span>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Row className="g-4 mb-4">
        {/* CỘT TRÁI: SẢN PHẨM & NỘI DUNG */}
        <Col lg={7}>
          {/* SẢN PHẨM LIÊN QUAN */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">SẢN PHẨM YÊU CẦU HOÀN TIỀN</h2>
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
              <h2 className="h6 fw-bold mb-0 text-primary">NỘI DUNG YÊU CẦU TỪ KHÁCH HÀNG</h2>
            </Card.Header>
            <Card.Body className="p-4">
              <div className="small text-secondary mb-1">Lý do:</div>
              <p className="fw-semibold mb-3">{ticket.reason}</p>

              <div className="small text-secondary mb-1">Mô tả chi tiết:</div>
              <p className="text-secondary mb-0 text-break">
                {ticket.description || 'Không có mô tả chi tiết.'}
              </p>
            </Card.Body>
          </Card>

          {/* BẰNG CHỨNG HÌNH ẢNH / VIDEO */}
          {Array.isArray(ticket.evidence_urls) && ticket.evidence_urls.length > 0 && (
            <Card className="border-0 shadow-sm mb-4">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-primary">HÌNH ẢNH / VIDEO BẰNG CHỨNG</h2>
              </Card.Header>
              <Card.Body className="p-4">
                <div className="d-flex flex-wrap gap-3">
                  {ticket.evidence_urls.map((url, idx) => (
                    <div
                      key={idx}
                      className="border rounded p-1 cursor-pointer bg-light"
                      style={{ cursor: 'pointer' }}
                      onClick={() => setPreviewImage(url)}
                    >
                      <Image
                        src={url}
                        alt={`Evidence ${idx + 1}`}
                        style={{ width: '120px', height: '90px', objectFit: 'cover' }}
                        rounded
                      />
                    </div>
                  ))}
                </div>
              </Card.Body>
            </Card>
          )}
        </Col>

        {/* CỘT PHẢI: QR CODE, KẾT QUẢ XỬ LÝ & TIMELINE */}
        <Col lg={5}>
          {/* MÃ QR NHẬN TIỀN */}
          {ticket.refund_qr_url && (
            <Card className="border-0 shadow-sm mb-4 text-center">
              <Card.Header className="bg-white py-3 border-bottom text-start">
                <h2 className="h6 fw-bold mb-0 text-primary">MÃ QR TÀI KHOẢN NHẬN TIỀN</h2>
              </Card.Header>
              <Card.Body className="p-4">
                <div
                  className="p-2 border rounded d-inline-block bg-white shadow-sm"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setPreviewImage(ticket.refund_qr_url)}
                  title="Nhấn để phóng to"
                >
                  <Image
                    src={ticket.refund_qr_url}
                    alt="Mã QR nhận tiền"
                    style={{ maxHeight: '160px', objectFit: 'contain' }}
                  />
                </div>
                <div className="small text-secondary mt-2">
                  Mã QR do bạn cung cấp để nhận tiền hoàn từ cửa hàng.
                </div>
              </Card.Body>
            </Card>
          )}

          {/* KẾT QUẢ THẨM ĐỊNH HÀNG HÓA */}
          {ticket.inspection && (
            <Card className="border-0 shadow-sm mb-4">
              <Card.Header className="bg-white py-3 border-bottom">
                <h2 className="h6 fw-bold mb-0 text-primary">BIÊN BẢN THẨM ĐỊNH HÀNG HÓA</h2>
              </Card.Header>
              <Card.Body className="p-4">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="small text-secondary">Kết quả:</span>
                  <Badge bg={ticket.inspection.result === 'PASSED' ? 'success' : 'danger'}>
                    {ticket.inspection.result === 'PASSED' ? 'ĐẠT YÊU CẦU' : 'KHÔNG ĐẠT'}
                  </Badge>
                </div>
                <div className="small text-secondary mb-1">Ghi chú kiểm tra:</div>
                <p className="mb-2 p-2 rounded bg-light border small">
                  {ticket.inspection.note || 'Không có ghi chú thêm.'}
                </p>
                <div className="small text-muted">
                  Thời gian: {formatDateTime(ticket.inspection.inspected_at)}
                </div>
              </Card.Body>
            </Card>
          )}

          {/* KẾT QUẢ HOÀN TIỀN ĐÃ CHUYỂN KHOẢN */}
          {ticket.refund_record && (
            <Card className="border-0 shadow-sm mb-4 bg-success-subtle border-success">
              <Card.Body className="p-4 text-success">
                <h2 className="h6 fw-bold mb-2">ĐÃ HOÀN TIỀN THÀNH CÔNG</h2>
                <div className="small mb-1">Số tiền đã chuyển: <strong>{formatMoney(ticket.refund_record.amount)}</strong></div>
                <div className="small mb-1">Mã tham chiếu ngân hàng: <strong className="font-monospace">{ticket.refund_record.bank_reference}</strong></div>
                <div className="small">Thời gian chuyển: {formatDateTime(ticket.refund_record.transferred_at)}</div>
              </Card.Body>
            </Card>
          )}

          {/* LÝ DO TỪ CHỐI (NẾU CÓ) */}
          {ticket.status === 'REJECTED' && (
            <Card className="border-0 shadow-sm mb-4 bg-danger-subtle border-danger">
              <Card.Body className="p-4 text-danger">
                <h2 className="h6 fw-bold mb-2">YÊU CẦU ĐÃ BỊ TỪ CHỐI</h2>
                <p className="mb-0 small">{ticket.reject_reason || 'Yêu cầu không đáp ứng điều kiện tiếp nhận.'}</p>
                {canRetryTicket && (
                  <div className="mt-3 pt-3 border-top border-danger-subtle">
                    <p className="small text-dark mb-2">
                      💡 <strong>Bạn có thể gửi lại yêu cầu:</strong> Nếu nhân viên yêu cầu bổ sung bằng chứng rõ nét hơn hoặc cập nhật thông tin mã QR, bạn có thể tạo lại phiếu khiếu nại mới cho đơn hàng này (Tối đa 2 lần).
                    </p>
                    <Button
                      as={Link}
                      to={`/support-tickets/create?orderId=${encodeURIComponent(ticket.order_id)}`}
                      variant="danger"
                      size="sm"
                      className="fw-semibold"
                    >
                      Tạo lại yêu cầu hỗ trợ mới
                    </Button>
                  </div>
                )}
              </Card.Body>
            </Card>
          )}

          {/* TIẾN TRÌNH LỊCH SỬ XỬ LÝ (TIMELINE) */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">TIẾN TRÌNH XỬ LÝ</h2>
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

      {/* MODAL XÁC NHẬN HỦY */}
      <Modal show={showCancelModal} onHide={() => setShowCancelModal(false)} centered className="support-modal support-customer-modal">
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">Xác nhận hủy yêu cầu hỗ trợ</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Bạn có chắc chắn muốn hủy phiếu hỗ trợ <strong>{ticket.ticket_code}</strong>?
          Hành động này không thể hoàn tác sau khi xác nhận.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowCancelModal(false)}>
            Đóng
          </Button>
          <Button variant="danger" onClick={handleCancelTicket} disabled={busy}>
            {busy ? 'Đang hủy...' : 'Xác nhận hủy'}
          </Button>
        </Modal.Footer>
      </Modal>

      {/* MODAL PHÓNG TO HÌNH ẢNH */}
      <Modal show={Boolean(previewImage)} onHide={() => setPreviewImage(null)} size="lg" centered>
        <Modal.Body className="p-0 text-center bg-dark">
          <Image src={previewImage} alt="Xem trước" fluid style={{ maxHeight: '80vh' }} />
        </Modal.Body>
      </Modal>
    </section>
  );
}
