import { useState, useMemo } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Image, Row, Table } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { supportApi } from '@/modules/support/api/support.api';
import { ticketTypes, RETURN_WINDOW_DAYS } from '@/modules/support/constants';
import { formatDateTime, formatMoney } from '@/shared/utils/format';

export default function CreateSupportTicketPage() {
  const { data, busy, act } = useShop();
  const navigate = useNavigate();

  const [orderId, setOrderId] = useState('');
  const [ticketType, setTicketType] = useState('REFUND');
  const [selectedItems, setSelectedItems] = useState({}); // { [orderItemId]: { checked: boolean, quantity: number, replacementVariantId: string } }
  const [reason, setReason] = useState('');
  const [description, setDescription] = useState('');
  const [qrImage, setQrImage] = useState('');
  const [qrFileName, setQrFileName] = useState('');
  const [evidenceFiles, setEvidenceFiles] = useState([]); // [{ name: string, url: string, type: 'image' | 'video' }]
  const [error, setError] = useState('');

  // Orders eligible for support ticket
  const allOrders = useShop().data?.orders || [];
  const existingTickets = useShop().data?.support_tickets || [];
  const allOrderItems = useShop().data?.order_items || [];
  const allVariants = useShop().data?.product_variants || [];

  // Filter orders that don't already have an active ticket
  const eligibleOrders = useMemo(() => {
    return [...allOrders]
      .filter((order) => {
        const hasTicket = existingTickets.some(
          (t) => t.order_id === order.id && !['CANCELLED'].includes(t.status),
        );
        return !hasTicket;
      })
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  }, [allOrders, existingTickets]);

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
        replacementVariantId: '',
      };
    }
    setSelectedItems(initialItems);
  }

  // Handle item checkbox change
  function handleItemCheck(itemId, checked) {
    setSelectedItems((prev) => ({
      ...prev,
      [itemId]: {
        ...(prev[itemId] || { quantity: 1, replacementVariantId: '' }),
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
        ...(prev[itemId] || { checked: true, replacementVariantId: '' }),
        quantity: val,
      },
    }));
  }

  // Handle replacement variant selection
  function handleReplacementVariantChange(itemId, variantId) {
    setSelectedItems((prev) => ({
      ...prev,
      [itemId]: {
        ...(prev[itemId] || { checked: true, quantity: 1 }),
        replacementVariantId: variantId,
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

  // Find same-price replacement variants for an item
  function getEligibleReplacementVariants(orderItem) {
    return allVariants.filter(
      (v) =>
        v.product_id === orderItem.product_id &&
        v.id !== orderItem.variant_id &&
        Number(v.price) === Number(orderItem.unit_price),
    );
  }

  // Calculated estimated refund amount
  const estimatedRefundAmount = useMemo(() => {
    if (ticketType !== 'REFUND') return 0;
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
  }, [ticketType, selectedItems, currentOrderItems]);

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
        replacement_variant_id: ticketType === 'EXCHANGE' ? s.replacementVariantId : null,
      }));

    if (itemsToSubmit.length === 0) {
      setError('Vui lòng tích chọn ít nhất 1 sản phẩm trong đơn hàng cần đổi/trả.');
      return;
    }

    if (ticketType === 'EXCHANGE') {
      const missingVariant = itemsToSubmit.some((item) => !item.replacement_variant_id);
      if (missingVariant) {
        setError('Vui lòng chọn sản phẩm / biến thể thay thế cho tất cả các món cần đổi.');
        return;
      }
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
          ticket_type: ticketType,
          reason: reason.trim(),
          description: description.trim(),
          items: itemsToSubmit,
          refund_qr_url: qrImage,
          evidence_urls: evidenceFiles.map((f) => f.url),
        }),
      );
      navigate('/support-tickets', { state: { created: true } });
    } catch (ticketError) {
      setError(ticketError.message || 'Không thể gửi phiếu hỗ trợ. Vui lòng thử lại.');
    }
  }

  return (
    <>
      <div className="eyebrow">HỖ TRỢ KHÁCH HÀNG (UC15)</div>
      <h1 className="h2 fw-bold mb-2">Tạo yêu cầu Đổi trả & Hoàn tiền</h1>
      <p className="text-secondary mb-4">
        Gửi yêu cầu đổi sản phẩm hoặc trả hàng hoàn tiền theo quy định trong vòng {RETURN_WINDOW_DAYS} ngày kể từ ngày nhận hàng.
      </p>

      {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

      {eligibleOrders.length === 0 ? (
        <Card className="border-0 p-4 text-center">
          <Card.Body>
            <h2 className="h5 fw-bold mb-2">Không có đơn hàng đủ điều kiện tạo yêu cầu</h2>
            <p className="text-secondary mb-3">
              Mỗi đơn hàng chỉ được tạo tối đa 1 phiếu hỗ trợ và phải trong thời hạn cho phép.
            </p>
            <Button as={Link} to="/orders" variant="primary">
              Xem lịch sử đơn hàng
            </Button>
          </Card.Body>
        </Card>
      ) : (
        <Form onSubmit={submitSupportTicket}>
          {/* BƯỚC 1: CHỌN ĐƠN HÀNG & LOẠI YÊU CẦU */}
          <Card className="border-0 mb-4 shadow-sm">
            <Card.Header className="bg-white py-3 border-bottom">
              <h2 className="h6 fw-bold mb-0 text-primary">1. THÔNG TIN ĐƠN HÀNG & HÌNH THỨC HỖ TRỢ</h2>
            </Card.Header>
            <Card.Body className="p-4">
              <Row className="g-3">
                <Col md={6}>
                  <Form.Group controlId="ticket-order">
                    <Form.Label className="fw-semibold">
                      Chọn đơn hàng <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Select required value={orderId} onChange={handleOrderChange}>
                      <option value="">-- Chọn đơn hàng của bạn --</option>
                      {eligibleOrders.map((order) => (
                        <option key={order.id} value={order.id}>
                          {order.order_code} · {formatMoney(order.total_amount)} ({formatDateTime(order.created_at)})
                        </option>
                      ))}
                    </Form.Select>
                  </Form.Group>
                </Col>

                <Col md={6}>
                  <Form.Group controlId="ticket-type">
                    <Form.Label className="fw-semibold">
                      Loại yêu cầu <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Select
                      value={ticketType}
                      onChange={(e) => setTicketType(e.target.value)}
                    >
                      <option value="REFUND">{ticketTypes.REFUND} (Hoàn tiền vào tài khoản)</option>
                      <option value="EXCHANGE">{ticketTypes.EXCHANGE} (Đổi size / màu cùng giá)</option>
                    </Form.Select>
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
            </Card.Body>
          </Card>

          {/* BƯỚC 2: CHỌN SẢN PHẨM & BIẾN THỂ ĐỔI */}
          {selectedOrder && (
            <Card className="border-0 mb-4 shadow-sm">
              <Card.Header className="bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                <h2 className="h6 fw-bold mb-0 text-primary">2. CHỌN SẢN PHẨM CẦN {ticketType === 'REFUND' ? 'HOÀN TIỀN' : 'ĐỔI'}</h2>
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
                      {ticketType === 'EXCHANGE' && <th>Sản phẩm thay thế (Cùng giá)</th>}
                      <th style={{ width: '140px' }} className="text-end">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentOrderItems.map((item) => {
                      const itemState = selectedItems[item.id] || { checked: false, quantity: 1, replacementVariantId: '' };
                      const replacementVariants = ticketType === 'EXCHANGE' ? getEligibleReplacementVariants(item) : [];

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

                          {ticketType === 'EXCHANGE' && (
                            <td>
                              <Form.Select
                                size="sm"
                                disabled={!itemState.checked}
                                value={itemState.replacementVariantId}
                                onChange={(e) => handleReplacementVariantChange(item.id, e.target.value)}
                              >
                                <option value="">-- Chọn biến thể đổi (size/màu) --</option>
                                {replacementVariants.map((v) => (
                                  <option key={v.id} value={v.id}>
                                    {v.sku || 'Variant'} · {v.color || ''} {v.size || ''} ({formatMoney(v.price)})
                                  </option>
                                ))}
                              </Form.Select>
                              {replacementVariants.length === 0 && (
                                <small className="text-danger d-block mt-1">
                                  Không có biến thể khác cùng giá với sản phẩm này.
                                </small>
                              )}
                            </td>
                          )}

                          <td className="text-end fw-semibold">
                            {formatMoney((item.unit_price || 0) * (itemState.checked ? itemState.quantity : 0))}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </Table>

                {ticketType === 'REFUND' && estimatedRefundAmount > 0 && (
                  <div className="p-3 bg-light border-top text-end">
                    <span className="text-secondary me-2">Số tiền hoàn hàng dự kiến:</span>
                    <strong className="fs-5 text-primary">{formatMoney(estimatedRefundAmount)}</strong>
                    <div className="small text-muted mt-1">
                      * Phí vận chuyển ban đầu sẽ được hoàn thêm nếu lỗi được xác định từ phía cửa hàng (STORE_FAULT).
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
                  placeholder="Ví dụ: Sản phẩm bị nứt khung khi mở hộp, giao nhầm size 42 thành 43..."
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
                        Tải lên ảnh mã QR tài khoản ngân hàng của bạn. Nhân viên sẽ quét mã này để chuyển khoản hoàn tiền (áp dụng cho cả Đổi hàng nếu hết sản phẩm thay thế).
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
              {busy ? 'Đang gửi yêu cầu...' : 'Gửi yêu cầu hỗ trợ'}
            </Button>
            <Button as={Link} to="/support-tickets" variant="outline-secondary" size="lg" disabled={busy}>
              Hủy bỏ
            </Button>
          </div>
        </Form>
      )}
    </>
  );
}
