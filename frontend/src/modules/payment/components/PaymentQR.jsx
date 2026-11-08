import { useState } from 'react';
import { Button, Container, Form, Modal } from 'react-bootstrap';
import { getPaymentQrImageUrl } from '@/modules/payment/utils/paymentQr';
import { formatMoney } from '@/shared/utils/format';

function PaymentQR({ payment, onChooseAnotherMethod, onCancelPayment }) {
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('Khách hàng đổi ý / Hủy thanh toán');
  const [cancelling, setCancelling] = useState(false);

  const qrImageUrl = getPaymentQrImageUrl(payment);
  const isPlaceholder = !qrImageUrl;
  const instructions = isPlaceholder
    ? 'Ảnh minh họa — chưa có mã thanh toán.'
    : payment.paymentMethod === 'momo'
      ? 'Sử dụng ứng dụng MoMo để quét mã QR.'
      : 'Sử dụng ứng dụng MB Bank hoặc ứng dụng ngân hàng hỗ trợ quét QR.';

  const handleConfirmCancel = async () => {
    if (!onCancelPayment) return;
    setCancelling(true);
    try {
      await onCancelPayment(cancelReason);
      setShowCancelModal(false);
    } finally {
      setCancelling(false);
    }
  };

  return (
    <main className="qr-payment-page">
      <Container>
        <section className="qr-payment-card">
          <p className="qr-payment-eyebrow mb-3">THANH TOÁN</p>
          <p className="qr-payment-step mb-2">BƯỚC 2 / 2</p>
          <h1 className="h3 fw-bold mb-2">
            {isPlaceholder ? 'Ảnh thanh toán tạm' : 'Quét mã QR để thanh toán'}
          </h1>
          <p className="qr-payment-instructions mb-4">{instructions}</p>

          <div className="qr-payment-code">
            <img
              src={qrImageUrl || '/images/payment-placeholder-cat.png'}
              alt={
                isPlaceholder
                  ? 'Ảnh mèo minh họa tạm thời'
                  : `Mã QR thanh toán đơn hàng ${payment.orderCode}`
              }
              width="200"
              height="200"
            />
            <span>{isPlaceholder ? 'Ảnh mặc định' : 'VietQR qua payOS'}</span>
          </div>

          <div className="qr-payment-amount">
            <span>Số tiền thanh toán</span>
            <strong>{formatMoney(payment.total)}</strong>
          </div>

          <div className="qr-payment-details">
            <div>
              <span>Người nhận</span>
              <strong>Cửa hàng</strong>
            </div>
            <div>
              <span>Nội dung chuyển khoản</span>
              <strong>{payment.orderCode}</strong>
            </div>
            <div>
              <span>Trạng thái</span>
              <strong className="qr-payment-pending">
                {isPlaceholder ? 'Chưa tạo phiên thanh toán' : 'Đang chờ thanh toán'}
              </strong>
            </div>
          </div>

          {!isPlaceholder && (
            <div className="qr-payment-waiting" role="status">
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
              <span>Hệ thống đang chờ xác nhận giao dịch (tự động cập nhật mỗi 3s)...</span>
            </div>
          )}

          <p className="qr-payment-disclaimer mb-3">
            {isPlaceholder
              ? 'Ảnh này không dùng để chuyển tiền. Đơn hàng vẫn chưa được thanh toán.'
              : 'Chỉ ghi nhận thanh toán sau khi hệ thống nhận xác nhận từ cổng thanh toán.'}
          </p>
          <div className="d-flex flex-wrap justify-content-center gap-2">
            {payment.checkoutUrl && (
              <Button as="a" href={payment.checkoutUrl} variant="outline-primary">
                Mở trang thanh toán payOS
              </Button>
            )}
            <Button className="app-back-button" variant="outline-secondary" onClick={onChooseAnotherMethod}>
              Quay lại chọn phương thức
            </Button>
            {onCancelPayment && (
              <Button variant="outline-danger" onClick={() => setShowCancelModal(true)}>
                Hủy thanh toán
              </Button>
            )}
          </div>
        </section>
      </Container>

      {/* MODAL XÁC NHẬN HỦY THANH TOÁN */}
      <Modal show={showCancelModal} onHide={() => !cancelling && setShowCancelModal(false)} centered>
        <Modal.Header closeButton={!cancelling}>
          <Modal.Title className="h5 fw-bold text-danger">Xác nhận hủy thanh toán</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-3">
            Bạn có chắc chắn muốn hủy phiên thanh toán cho đơn hàng <strong>#{payment.orderCode}</strong>?
          </p>
          <div className="alert alert-warning small py-2">
            Mã QR thanh toán payOS sẽ bị đóng ngay lập tức và đơn hàng sẽ chuyển sang trạng thái đã hủy.
          </div>
          <Form.Group controlId="cancel-reason" className="mt-3">
            <Form.Label className="small fw-semibold">Lý do hủy (tùy chọn):</Form.Label>
            <Form.Select
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              disabled={cancelling}
            >
              <option value="Khách hàng đổi ý / Hủy thanh toán">Tôi đổi ý, không muốn mua nữa</option>
              <option value="Muốn chọn sản phẩm khác">Muốn đổi sang mẫu hoặc số lượng khác</option>
              <option value="Muốn thay đổi địa chỉ nhận hàng">Cần thay đổi thông tin nhận hàng</option>
              <option value="Gặp sự cố khi quét mã QR">Gặp sự cố khi quét mã thanh toán</option>
              <option value="Lý do khác">Lý do khác</option>
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowCancelModal(false)} disabled={cancelling}>
            Đóng
          </Button>
          <Button variant="danger" onClick={handleConfirmCancel} disabled={cancelling}>
            {cancelling ? 'Đang hủy...' : 'Xác nhận hủy thanh toán'}
          </Button>
        </Modal.Footer>
      </Modal>
    </main>
  );
}

export default PaymentQR;
