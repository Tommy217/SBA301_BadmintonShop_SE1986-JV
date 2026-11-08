import { useState, useMemo, useEffect } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Image, Row, Table } from 'react-bootstrap';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { supportApi } from '@/modules/support/api/support.api';
import { RETURN_WINDOW_DAYS } from '@/modules/support/constants';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import '../support.css';

export default function CreateSupportTicketPage() {
  const { data, busy, act } = useShop();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialOrderId = searchParams.get('orderId') || '';

  const [orderId, setOrderId] = useState(initialOrderId);
  const [selectedItems, setSelectedItems] = useState({}); // { [orderItemId]: { checked: boolean, quantity: number } }
  const [reason, setReason] = useState('');
  const [description, setDescription] = useState('');
  const [qrImage, setQrImage] = useState('');
  const [qrFileName, setQrFileName] = useState('');
  const [evidenceFiles, setEvidenceFiles] = useState([]); // [{ name: string, url: string, type: 'image' | 'video' }]
  const [error, setError] = useState('');

  // Orders eligible for support ticket
  const allOrders = data?.orders || [];
  const existingTickets = data?.support_tickets || [];
  const allOrderItems = data?.order_items || [];

  // Filter orders that are eligible for support ticket
  const eligibleOrders = useMemo(() => {
    return [...allOrders]
      .filter((order) => {
        if (order.payment_status !== 'paid' || ['cancelled', 'returned'].includes(order.status)) {
          return false;
        }
        const orderTickets = existingTickets.filter((t) => String(t.order_id) === String(order.id));
        // Đang có phiếu trong tiến trình xử lý
        const hasActiveTicket = orderTickets.some((t) =>
          ['PENDING', 'AWAITING_RETURN', 'PENDING_APPROVAL', 'PENDING_TRANSFER'].includes(t.status),
        );
        if (hasActiveTicket) return false;

        // Đã hoàn tiền thành công
        const isRefunded = orderTickets.some((t) => t.status === 'REFUNDED');
        if (isRefunded) return false;

        // Bị từ chối kiểm định thực tế tại kho
        const hasInspectionFailed = orderTickets.some(
          (t) => t.status === 'INSPECTION_FAILED' || t.inspection != null,
        );
        if (hasInspectionFailed) return false;

        // Số lần gửi không tính hủy >= 2
        const nonCancelledCount = orderTickets.filter((t) => t.status !== 'CANCELLED').length;
        if (nonCancelledCount >= 2) return false;

        return true;
      })
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  }, [allOrders, existingTickets]);

  const previousRejectedTicket = useMemo(() => {
    if (!orderId) return null;
    return (
      existingTickets
        .filter((t) => String(t.order_id) === String(orderId) && t.status === 'REJECTED')
        .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))[0] || null
    );
  }, [existingTickets, orderId]);

  useEffect(() => {
    if (initialOrderId && allOrderItems.length > 0) {
      setOrderId(initialOrderId);
      const initialItems = {};
      const items = allOrderItems.filter((item) => String(item.order_id) === String(initialOrderId));
      for (const item of items) {
        initialItems[item.id] = {
          checked: false,
          quantity: 1,
        };
      }
      setSelectedItems(initialItems);
    }
  }, [initialOrderId, allOrderItems]);

  const selectedOrder = useMemo(() => {
    return allOrders.find((o) => o.id === orderId);
  }, [allOrders, orderId]);

  // Order items for the selected order
  const currentOrderItems = useMemo(() => {
    if (!orderId) return [];
    return allOrderItems.filter((item) => item.order_id === orderId);
  }, [allOrderItems, orderId]);

  // Handle order selection change
  function handleOrderChange(e) {
    const newOrderId = e.target.value;
    setOrderId(newOrderId);
    // Reset selected items
    const initialItems = {};
    const items = allOrderItems.filter((item) => item.order_id === newOrderId);
    for (const item of items) {
      initialItems[item.id] = {
        checked: false,
        quantity: 1,
      };
    }
    setSelectedItems(initialItems);
  }

  // Handle item checkbox change
  function handleItemCheck(itemId, checked) {
    setSelectedItems((prev) => ({
      ...prev,
      [itemId]: {
        ...(prev[itemId] || { quantity: 1 }),
        checked,
      },
    }));
  }

  // Handle item quantity change
  function handleQuantityChange(itemId, quantity, maxQty) {
    const val = Math.max(1, Math.min(Number(quantity) || 1, maxQty));
    setSelectedItems((prev) => ({
      ...prev,
      [itemId]: {
        ...(prev[itemId] || { checked: true }),
        quantity: val,
      },
    }));
  }

  // Handle QR upload (Data URL)
  function handleQrUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setError('Mã QR ngân hàng phải là tệp hình ảnh (PNG, JPG, JPEG).');
      return;
    }
    const reader = new FileReader();
    reader.onload = (event) => {
      setQrImage(event.target.result);
      setQrFileName(file.name);
    };
    reader.readAsDataURL(file);
  }

  // Handle Evidence upload
  function handleEvidenceUpload(e) {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;

    for (const file of files) {
      const isImage = file.type.startsWith('image/');
      const isVideo = file.type.startsWith('video/');
      if (!isImage && !isVideo) continue;

      const reader = new FileReader();
      reader.onload = (event) => {
        setEvidenceFiles((prev) => [
          ...prev,
          {
            name: file.name,
            url: event.target.result,
            type: isImage ? 'image' : 'video',
          },
        ]);
      };
      reader.readAsDataURL(file);
    }
  }

  function removeEvidence(index) {
    setEvidenceFiles((prev) => prev.filter((_, i) => i !== index));
  }

  // Calculated estimated refund amount
  const estimatedRefundAmount = useMemo(() => {
    let sum = 0;
    for (const [itemId, state] of Object.entries(selectedItems)) {
      if (state.checked) {
        const item = currentOrderItems.find((oi) => oi.id === itemId);
        if (item) {
          sum += (item.unit_price || 0) * (state.quantity || 1);
        }
      }
    }
    return sum;
  }, [selectedItems, currentOrderItems]);

  async function submitSupportTicket(event) {
    event.preventDefault();
    setError('');

    if (!orderId) {
      setError('Vui lòng chọn đơn hàng cần hỗ trợ.');
      return;
    }

    const itemsToSubmit = Object.entries(selectedItems)
      .filter(([, s]) => s.checked)
      .map(([orderItemId, s]) => ({
        order_item_id: orderItemId,
        quantity: s.quantity,
      }));

    if (itemsToSubmit.length === 0) {
      setError('Vui lòng tích chọn ít nhất 1 sản phẩm trong đơn hàng cần trả.');
      return;
    }

    if (!qrImage) {
      setError('Vui lòng tải lên ảnh mã QR tài khoản ngân hàng nhận tiền.');
      return;
    }

    if (evidenceFiles.length === 0) {
      setError('Vui lòng tải lên ít nhất 1 hình ảnh hoặc video bằng chứng lỗi/vấn đề sản phẩm.');
      return;
    }

    try {
      await act(() =>
        supportApi.createTicket({
          order_id: orderId,
          ticket_type: 'REFUND',
          reason: reason.trim(),
          description: description.trim(),
          items: itemsToSubmit,
          refund_qr_url: qrImage,
          evidence_urls: evidenceFiles.map((f) => f.url),
        }),
      );
      navigate('/support-tickets', { state: { created: true } });
    } catch (ticketError) {
      setError(ticketError.message || 'Lỗi khi tạo yêu cầu trả hàng & hoàn tiền.');
    }
  }

  return (
    <section className="support-page support-customer-page support-form-page">
      <div className="eyebrow">HỖ TRỢ KHÁCH HÀNG</div>
      <div className="support-heading d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
        <div>
          <h1 className="h2 fw-bold mb-1">Tạo yêu cầu hỗ trợ</h1>
          <p className="text-secondary mb-0">
            Hỗ trợ xử lý trả hàng và hoàn tiền trực tiếp cho các đơn hàng đã thanh toán trực tuyến.
          </p>
        </div>
        <Button className="app-back-button" as={Link} to="/support-tickets" variant="outline-secondary">
          ← Quay lại danh sách
        </Button>
      </div>

      {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

      {eligibleOrders.length === 0 ? (
        <Alert variant="warning" className="border-warning">
          <h4 className="alert-heading fw-bold fs-6">Không có đơn hàng đủ điều kiện</h4>
          <p className="mb-0">
            Bạn hiện không có đơn hàng đã thanh toán nào hoặc tất cả đơn hàng đã có yêu cầu hỗ trợ đang được xử lý.
          </p>
        </Alert>
      ) : (
        <Form onSubmit={submitSupportTicket}>
          {/* BƯỚC 1: CHỌN ĐƠN HÀNG */}
          <Card className="border-0 mb-4 shadow-sm">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">1. CHỌN ĐƠN HÀNG CẦN HỖ TRỢ</h2>
            </Card.Header>
            <Card.Body className="p-4">
              <Row className="g-3">
                <Col md={6}>
                  <Form.Group controlId="order-select">
                    <Form.Label className="fw-semibold">
                      Đơn hàng đã nhận <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Select value={orderId} onChange={handleOrderChange} required>
                      <option value="">-- Chọn đơn hàng của bạn --</option>
                      {eligibleOrders.map((ord) => {
                        const isRetry = existingTickets.some(
                          (t) => String(t.order_id) === String(ord.id) && t.status === 'REJECTED',
                        );
                        return (
                          <option key={ord.id} value={ord.id}>
                            {ord.order_code || `Đơn hàng #${ord.id}`} - {formatDateTime(ord.created_at)} - {formatMoney(ord.total_amount)}
                            {isRetry ? ' (Gửi lại lần 2)' : ''}
                          </option>
                        );
                      })}
                    </Form.Select>
                    <Form.Text muted>Áp dụng cho các đơn hàng đã thanh toán của bạn.</Form.Text>
                  </Form.Group>
                </Col>

                <Col md={6}>
                  <Form.Group controlId="ticket-type">
                    <Form.Label className="fw-semibold">
                      Hình thức hỗ trợ
                    </Form.Label>
                    <Form.Control
                      readOnly
                      value="Trả hàng & Hoàn tiền (REFUND)"
                      className="bg-light fw-bold text-success"
                    />
                  </Form.Group>
                </Col>
              </Row>

              {selectedOrder && (
                <div className="mt-3 p-3 rounded bg-light border small">
                  <div className="row g-2">
                    <div className="col-sm-4">
                      <span className="text-secondary">Người nhận:</span> <strong>{selectedOrder.receiver_name}</strong>
                    </div>
                    <div className="col-sm-4">
                      <span className="text-secondary">Điện thoại:</span> <strong>{selectedOrder.receiver_phone}</strong>
                    </div>
                    <div className="col-sm-4">
                      <span className="text-secondary">Tổng tiền đơn:</span> <strong className="text-primary">{formatMoney(selectedOrder.total_amount)}</strong>
                    </div>
                    <div className="col-12">
                      <span className="text-secondary">Địa chỉ giao:</span> {selectedOrder.shipping_address}
                    </div>
                  </div>
                </div>
              )}

              {previousRejectedTicket && (
                <Alert variant="warning" className="mt-3 mb-0 small border-warning">
                  <div className="fw-bold mb-1">
                    ⚠️ Đơn hàng này đang gửi lại yêu cầu hỗ trợ (Lần 2 / Tối đa 2 lần)
                  </div>
                  <div>
                    Yêu cầu trước đó ({previousRejectedTicket.ticket_code}) đã bị từ chối với lý do:{' '}
                    <em>"{previousRejectedTicket.reject_reason || 'Chưa đáp ứng điều kiện tiếp nhận'}"</em>.
                  </div>
                  <div className="mt-1 text-muted">
                    Vui lòng tải lên bằng chứng rõ ràng hơn hoặc cập nhật thông tin chính xác để nhân viên hỗ trợ duyệt tiếp nhận.
                  </div>
                </Alert>
              )}
            </Card.Body>
          </Card>

          {/* BƯỚC 2: CHỌN SẢN PHẨM TRẢ & HOÀN TIỀN */}
          {selectedOrder && (
            <Card className="border-0 mb-4 shadow-sm">
              <Card.Header className="bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                <h2 className="h6 fw-bold mb-0 text-primary">2. CHỌN SẢN PHẨM CẦN TRẢ & HOÀN TIỀN</h2>
                <Badge bg="info" text="dark">Tối đa số lượng đã mua</Badge>
              </Card.Header>
              <Card.Body className="p-0">
                <Table responsive hover className="align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th style={{ width: '40px' }} className="text-center">Chọn</th>
                      <th>Sản phẩm đã mua</th>
                      <th style={{ width: '130px' }} className="text-end">Đơn giá</th>
                      <th style={{ width: '120px' }} className="text-center">Số lượng</th>
                      <th style={{ width: '140px' }} className="text-end">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentOrderItems.map((item) => {
                      const itemState = selectedItems[item.id] || { checked: false, quantity: 1 };

                      return (
                        <tr key={item.id} className={itemState.checked ? 'table-active' : ''}>
                          <td className="text-center">
                            <Form.Check
                              type="checkbox"
                              checked={itemState.checked}
                              onChange={(e) => handleItemCheck(item.id, e.target.checked)}
                            />
                          </td>
                          <td>
                            <div className="fw-semibold">{item.product_name}</div>
                            {item.variant_info && (
                              <small className="text-secondary d-block">{item.variant_info}</small>
                            )}
                          </td>
                          <td className="text-end">{formatMoney(item.unit_price)}</td>
                          <td className="text-center">
                            <Form.Control
                              type="number"
                              size="sm"
                              min={1}
                              max={item.quantity}
                              disabled={!itemState.checked}
                              value={itemState.quantity}
                              onChange={(e) => handleQuantityChange(item.id, e.target.value, item.quantity)}
                              className="text-center mx-auto"
                              style={{ width: '70px' }}
                            />
                            <small className="text-muted d-block mt-1">Đã mua: {item.quantity}</small>
                          </td>
                          <td className="text-end fw-semibold">
                            {formatMoney((item.unit_price || 0) * (itemState.checked ? itemState.quantity : 0))}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </Table>

                {estimatedRefundAmount > 0 && (
                  <div className="p-3 bg-light border-top text-end">
                    <span className="text-secondary me-2">Số tiền hoàn hàng dự kiến:</span>
                    <strong className="fs-5 text-primary">{formatMoney(estimatedRefundAmount)}</strong>
                    <div className="small text-muted mt-1">
                      * Phí vận chuyển ban đầu sẽ được hoàn thêm nếu nguyên nhân được xác định là lỗi từ phía cửa hàng (STORE_FAULT).
                    </div>
                  </div>
                )}
              </Card.Body>
            </Card>
          )}

          {/* BƯỚC 3: LÝ DO & MÔ TẢ */}
          <Card className="border-0 mb-4 shadow-sm">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">3. NỘI DUNG YÊU CẦU</h2>
            </Card.Header>
            <Card.Body className="p-4">
              <Form.Group className="mb-3" controlId="ticket-reason">
                <Form.Label className="fw-semibold">
                  Lý do yêu cầu <span className="text-danger">*</span>
                </Form.Label>
                <Form.Control
                  required
                  maxLength={300}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Ví dụ: Sản phẩm bị nứt khung khi mở hộp, giao nhầm mã sản phẩm..."
                />
                <Form.Text muted>Tối đa 300 ký tự.</Form.Text>
              </Form.Group>

              <Form.Group controlId="ticket-description">
                <Form.Label className="fw-semibold">Mô tả chi tiết</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  maxLength={5000}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Mô tả cụ thể tình trạng hàng hóa hoặc ghi chú thêm cho nhân viên tiếp nhận (không bắt buộc)"
                />
                <Form.Text muted>Tối đa 5000 ký tự.</Form.Text>
              </Form.Group>
            </Card.Body>
          </Card>

          {/* BƯỚC 4: UPLOAD MÃ QR NHẬN TIỀN & BẰNG CHỨNG */}
          <Card className="border-0 mb-4 shadow-sm">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">4. TÀI KHOẢN NHẬN TIỀN (QR) & BẰNG CHỨNG</h2>
            </Card.Header>
            <Card.Body className="p-4">
              <Row className="g-4">
                {/* Upload QR */}
                <Col md={6}>
                  <div className="p-3 border rounded h-100 bg-light">
                    <Form.Group controlId="ticket-qr">
                      <Form.Label className="fw-bold d-flex align-items-center justify-content-between">
                        <span>Ảnh mã QR nhận tiền <span className="text-danger">*</span></span>
                        <Badge bg="danger">Bắt buộc</Badge>
                      </Form.Label>
                      <p className="small text-secondary mb-2">
                        Tải lên ảnh mã QR tài khoản ngân hàng của bạn. Nhân viên sẽ quét mã này để chuyển khoản hoàn tiền.
                      </p>
                      <Form.Control
                        type="file"
                        accept="image/png, image/jpeg, image/jpg"
                        onChange={handleQrUpload}
                        required={!qrImage}
                      />
                    </Form.Group>

                    {qrImage && (
                      <div className="mt-3 text-center p-2 bg-white rounded border">
                        <Image
                          src={qrImage}
                          alt="QR Code nhận tiền"
                          thumbnail
                          style={{ maxHeight: '180px', objectFit: 'contain' }}
                        />
                        <div className="small text-muted mt-1">{qrFileName || 'Mã QR đã tải lên'}</div>
                      </div>
                    )}
                  </div>
                </Col>

                {/* Upload Bằng chứng */}
                <Col md={6}>
                  <div className="p-3 border rounded h-100 bg-light">
                    <Form.Group controlId="ticket-evidence">
                      <Form.Label className="fw-bold d-flex align-items-center justify-content-between">
                        <span>Hình ảnh / Video bằng chứng <span className="text-danger">*</span></span>
                        <Badge bg="danger">Tối thiểu 1 file</Badge>
                      </Form.Label>
                      <p className="small text-secondary mb-2">
                        Chụp rõ nhãn mác, góc cạnh bị trầy xước, nứt gãy hoặc chi tiết lỗi sản phẩm để nhân viên đối soát nhanh nhất.
                      </p>
                      <Form.Control
                        type="file"
                        multiple
                        accept="image/*, video/*"
                        onChange={handleEvidenceUpload}
                        required={evidenceFiles.length === 0}
                      />
                    </Form.Group>

                    {evidenceFiles.length > 0 && (
                      <div className="mt-3 d-flex flex-wrap gap-2">
                        {evidenceFiles.map((file, idx) => (
                          <div
                            key={idx}
                            className="position-relative border rounded bg-white p-1 text-center"
                            style={{ width: '90px' }}
                          >
                            {file.type === 'image' ? (
                              <Image
                                src={file.url}
                                alt={file.name}
                                style={{ width: '80px', height: '60px', objectFit: 'cover' }}
                                rounded
                              />
                            ) : (
                              <div
                                className="bg-dark text-white d-flex align-items-center justify-content-center rounded"
                                style={{ width: '80px', height: '60px', fontSize: '10px' }}
                              >
                                VIDEO
                              </div>
                            )}
                            <button
                              type="button"
                              onClick={() => removeEvidence(idx)}
                              className="btn btn-sm btn-danger position-absolute top-0 end-0 p-0 rounded-circle"
                              style={{ width: '20px', height: '20px', lineHeight: '18px', fontSize: '12px' }}
                              title="Xóa tệp"
                            >
                              ×
                            </button>
                            <small className="d-block text-truncate mt-1" style={{ fontSize: '11px' }}>
                              {file.name}
                            </small>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </Col>
              </Row>
            </Card.Body>
          </Card>

          {/* HÀNH ĐỘNG SUBMIT */}
          <div className="d-flex flex-wrap gap-3 mb-5">
            <Button type="submit" size="lg" variant="primary" disabled={busy || !orderId}>
              {busy ? 'Đang gửi yêu cầu...' : 'Gửi yêu cầu trả hàng & hoàn tiền'}
            </Button>
            <Button as={Link} to="/support-tickets" variant="outline-secondary" size="lg" disabled={busy}>
              Hủy bỏ
            </Button>
          </div>
        </Form>
      )}
    </section>
  );
}
