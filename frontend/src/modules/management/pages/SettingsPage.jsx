import { useState, useEffect } from 'react';
import { Alert, Badge, Button, Card, Col, Form, Row } from 'react-bootstrap';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { useShop } from '@/app/providers/shop/ShopContext';
import { supportApi } from '@/modules/support/api/support.api';
import { paymentApi } from '@/modules/payment/api/payment.api';
import { formatMoney } from '@/shared/utils/format';
import { ROLES } from '@/shared/constants/roles';

export default function SettingsPage() {
  const { data, busy, act } = useShop();
  const { user } = useAuth();

  const role = user?.role?.toUpperCase();
  const isManager = role === ROLES.MANAGER || role === ROLES.ADMIN;

  const currentThreshold = Number(data?.refund_approval_threshold) || 3000000;
  const currentTimeout = Number(data?.payment_qr_timeout_minutes) || 15;

  const [thresholdInput, setThresholdInput] = useState(String(currentThreshold));
  const [timeoutInput, setTimeoutInput] = useState(String(currentTimeout));

  const [thresholdMsg, setThresholdMsg] = useState({ error: '', success: '' });
  const [timeoutMsg, setTimeoutMsg] = useState({ error: '', success: '' });
  const [savingThreshold, setSavingThreshold] = useState(false);
  const [savingTimeout, setSavingTimeout] = useState(false);

  useEffect(() => {
    if (data?.refund_approval_threshold != null) {
      setThresholdInput(String(data.refund_approval_threshold));
    }
  }, [data?.refund_approval_threshold]);

  useEffect(() => {
    if (data?.payment_qr_timeout_minutes != null) {
      setTimeoutInput(String(data.payment_qr_timeout_minutes));
    }
  }, [data?.payment_qr_timeout_minutes]);

  if (!isManager) {
    return (
      <section className="manager-page">
        <div className="eyebrow">QUẢN LÝ HỆ THỐNG</div>
        <h1 className="h3 fw-bold mb-3">Cài đặt hệ thống</h1>
        <Alert variant="danger">
          🔒 Bạn không có quyền truy cập trang Cài đặt hệ thống. Chức năng này chỉ dành cho Quản lý (Manager) và Quản trị viên (Admin).
        </Alert>
      </section>
    );
  }

  async function handleSaveThreshold(e) {
    e?.preventDefault();
    const val = Number(thresholdInput);
    if (!val || val <= 0) {
      setThresholdMsg({ error: 'Ngưỡng phê duyệt phải lớn hơn 0 VNĐ.', success: '' });
      return;
    }
    setSavingThreshold(true);
    setThresholdMsg({ error: '', success: '' });
    try {
      await act(() => supportApi.updateThreshold(val));
      setThresholdMsg({ error: '', success: `Đã cập nhật ngưỡng duyệt hoàn tiền thành ${formatMoney(val)}!` });
    } catch (err) {
      setThresholdMsg({ error: err.message || 'Lỗi khi cập nhật ngưỡng duyệt.', success: '' });
    } finally {
      setSavingThreshold(false);
    }
  }

  async function handleSaveTimeout(e) {
    e?.preventDefault();
    const val = Number(timeoutInput);
    if (!val || val < 1 || val > 1440) {
      setTimeoutMsg({ error: 'Thời gian hủy QR phải từ 1 đến 1440 phút (24 giờ).', success: '' });
      return;
    }
    setSavingTimeout(true);
    setTimeoutMsg({ error: '', success: '' });
    try {
      await act(() => paymentApi.updatePaymentTimeout(val));
      setTimeoutMsg({ error: '', success: `Đã cập nhật thời gian hủy mã QR thanh toán thành ${val} phút!` });
    } catch (err) {
      setTimeoutMsg({ error: err.message || 'Lỗi khi cập nhật thời gian hủy mã QR.', success: '' });
    } finally {
      setSavingTimeout(false);
    }
  }

  return (
    <section className="manager-page manager-settings-page">
      <div className="eyebrow">QUẢN LÝ VẬN HÀNH</div>
      <div className="manager-title mb-4">
        <div>
          <h1 className="h2 fw-bold mb-1">Cài đặt hệ thống & Hạn mức vận hành</h1>
          <p className="text-secondary mb-0">
            Cấu hình thời gian hủy thanh toán VietQR và phân quyền hạn mức phê duyệt đổi trả / hoàn tiền.
          </p>
        </div>
      </div>

      <Row className="g-4">
        {/* CARD 1: CÀI ĐẶT THỜI GIAN HỦY QR PAYMENT */}
        <Col lg={6}>
          <Card className="border-0 shadow-sm h-100 border-top border-4 border-warning">
            <Card.Header className="bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
              <h2 className="h5 fw-bold mb-0 text-dark">
                ⏱️ Thời gian hủy mã QR thanh toán (payOS)
              </h2>
              <Badge bg="warning" text="dark" className="fs-6">
                Hiện tại: {currentTimeout} phút
              </Badge>
            </Card.Header>
            <Card.Body className="p-4 d-flex flex-column justify-content-between">
              <div>
                <p className="text-secondary small mb-3">
                  Khi khách hàng nhấn <strong>"Thanh toán ngay"</strong> và chuyển sang màn hình quét VietQR, hệ thống sẽ giữ mã QR và đơn hàng trong khoảng thời gian này.
                </p>

                <div className="p-3 bg-light rounded border mb-3 small">
                  <div className="fw-semibold text-dark mb-1">Cơ chế hoạt động của hệ thống:</div>
                  <ul className="mb-0 ps-3 text-secondary">
                    <li>Sau <strong>{currentTimeout} phút</strong> nếu khách không quét VietQR chuyển tiền, hệ thống sẽ tự động đánh dấu phiên là <code>expired</code>.</li>
                    <li>Đơn hàng tương ứng tự động chuyển sang trạng thái <strong>Hủy (<code>cancelled</code>)</strong>.</li>
                    <li>Số lượng tồn kho sản phẩm được hoàn trả ngay lập tức để khách khác có thể đặt mua.</li>
                  </ul>
                </div>

                {timeoutMsg.error && <Alert variant="danger" className="py-2 small">{timeoutMsg.error}</Alert>}
                {timeoutMsg.success && <Alert variant="success" className="py-2 small">{timeoutMsg.success}</Alert>}

                <Form onSubmit={handleSaveTimeout}>
                  <Form.Group className="mb-3" controlId="timeout-minutes-input">
                    <Form.Label className="fw-semibold small">
                      Thời gian hiệu lực mã QR (Phút) <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Control
                      type="number"
                      min="1"
                      max="1440"
                      value={timeoutInput}
                      onChange={(e) => setTimeoutInput(e.target.value)}
                      disabled={savingTimeout || busy}
                    />
                    <Form.Text className="text-muted d-block mt-1">
                      Thời gian tối thiểu 1 phút, tối đa 1440 phút (24 giờ). Mặc định khuyến nghị: 15 phút.
                    </Form.Text>
                  </Form.Group>

                  {/* Preset quick buttons */}
                  <div className="mb-3">
                    <span className="small text-secondary d-block mb-1">Chọn nhanh:</span>
                    <div className="d-flex flex-wrap gap-2">
                      {[5, 10, 15, 30, 60].map((mins) => (
                        <Button
                          key={mins}
                          type="button"
                          variant={String(mins) === timeoutInput ? 'warning' : 'outline-secondary'}
                          size="sm"
                          onClick={() => setTimeoutInput(String(mins))}
                          disabled={savingTimeout || busy}
                        >
                          {mins} phút {mins === 15 ? '(Khuyến nghị)' : ''}
                        </Button>
                      ))}
                    </div>
                  </div>

                  <Button
                    type="submit"
                    variant="warning"
                    className="w-100 fw-semibold"
                    disabled={savingTimeout || busy || String(currentTimeout) === timeoutInput}
                  >
                    {savingTimeout ? 'Đang lưu…' : '💾 Lưu thời gian hủy QR'}
                  </Button>
                </Form>
              </div>
            </Card.Body>
          </Card>
        </Col>

        {/* CARD 2: CÀI ĐẶT NGƯỠNG DUYỆT HOÀN TIỀN CỦA QUẢN LÝ */}
        <Col lg={6}>
          <Card className="border-0 shadow-sm h-100 border-top border-4 border-primary">
            <Card.Header className="bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
              <h2 className="h5 fw-bold mb-0 text-primary">
                🛡️ Ngưỡng Quản lý phê duyệt hoàn tiền
              </h2>
              <Badge bg="primary" className="fs-6">
                Hiện tại: {formatMoney(currentThreshold)}
              </Badge>
            </Card.Header>
            <Card.Body className="p-4 d-flex flex-column justify-content-between">
              <div>
                <p className="text-secondary small mb-3">
                  Thiết lập hạn mức phân quyền giải quyết yêu cầu Trả hàng & Hoàn tiền giữa <strong>Nhân viên (Staff)</strong> và <strong>Quản lý (Manager)</strong>.
                </p>

                <div className="p-3 bg-light rounded border mb-3 small">
                  <div className="fw-semibold text-dark mb-1">Quy định phân quyền theo hạn mức:</div>
                  <ul className="mb-0 ps-3 text-secondary">
                    <li>
                      <strong>Số tiền hoàn &lt; {formatMoney(currentThreshold)}:</strong> Nhân viên sau khi nhận hàng hoàn từ khách và thẩm định sản phẩm đạt chuẩn có thẩm quyền <strong>phê duyệt hoàn tiền trực tiếp</strong> và chuyển sang chờ quét QR chuyển tiền.
                    </li>
                    <li>
                      <strong>Số tiền hoàn &ge; {formatMoney(currentThreshold)}:</strong> Sau khi Nhân viên kiểm tra sản phẩm, phiếu sẽ được chuyển sang <strong>Chờ Quản lý duyệt (<code>PENDING_APPROVAL</code>)</strong>. Bắt buộc tài khoản Quản lý mới có quyền phê duyệt.
                    </li>
                  </ul>
                </div>

                {thresholdMsg.error && <Alert variant="danger" className="py-2 small">{thresholdMsg.error}</Alert>}
                {thresholdMsg.success && <Alert variant="success" className="py-2 small">{thresholdMsg.success}</Alert>}

                <Form onSubmit={handleSaveThreshold}>
                  <Form.Group className="mb-3" controlId="threshold-amount-input">
                    <Form.Label className="fw-semibold small">
                      Ngưỡng Quản lý phê duyệt (VNĐ) <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Control
                      type="number"
                      step="100000"
                      min="100000"
                      value={thresholdInput}
                      onChange={(e) => setThresholdInput(e.target.value)}
                      disabled={savingThreshold || busy}
                    />
                    <Form.Text className="text-muted d-block mt-1">
                      Xem trước: <strong>{formatMoney(Number(thresholdInput) || 0)}</strong>
                    </Form.Text>
                  </Form.Group>

                  {/* Preset quick buttons */}
                  <div className="mb-3">
                    <span className="small text-secondary d-block mb-1">Chọn nhanh:</span>
                    <div className="d-flex flex-wrap gap-2">
                      {[1000000, 2000000, 3000000, 5000000, 10000000].map((amt) => (
                        <Button
                          key={amt}
                          type="button"
                          variant={String(amt) === thresholdInput ? 'primary' : 'outline-secondary'}
                          size="sm"
                          onClick={() => setThresholdInput(String(amt))}
                          disabled={savingThreshold || busy}
                        >
                          {formatMoney(amt)}
                        </Button>
                      ))}
                    </div>
                  </div>

                  <Button
                    type="submit"
                    variant="primary"
                    className="w-100 fw-semibold"
                    disabled={savingThreshold || busy || String(currentThreshold) === thresholdInput}
                  >
                    {savingThreshold ? 'Đang lưu…' : '💾 Lưu ngưỡng phê duyệt'}
                  </Button>
                </Form>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </section>
  );
}

