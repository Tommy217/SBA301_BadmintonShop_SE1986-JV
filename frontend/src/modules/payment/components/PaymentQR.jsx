import { Button, Container } from 'react-bootstrap';
import { getPaymentQrImageUrl } from '@/modules/payment/utils/paymentQr';
import { formatMoney } from '@/shared/utils/format';

function PaymentQR({ payment, onCheckResult, onChooseAnotherMethod }) {
  const qrImageUrl = getPaymentQrImageUrl(payment);
  const isPlaceholder = !qrImageUrl;
  const instructions = isPlaceholder
    ? 'Ảnh minh họa — chưa có mã thanh toán.'
    : payment.paymentMethod === 'momo'
      ? 'Sử dụng ứng dụng MoMo để quét mã QR.'
      : 'Sử dụng ứng dụng MB Bank hoặc ứng dụng ngân hàng hỗ trợ quét QR.';

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
            <Button variant="primary" onClick={onCheckResult} disabled={isPlaceholder}>
              Kiểm tra kết quả giao dịch
            </Button>
            <Button className="app-back-button" variant="outline-secondary" onClick={onChooseAnotherMethod}>
              Quay lại chọn phương thức
            </Button>
          </div>
        </section>
      </Container>
    </main>
  );
}

export default PaymentQR;
