import HomeButton from '@/shared/components/HomeButton';
import { Button } from 'react-bootstrap';
import { paymentMethodNames } from '@/modules/payment/constants';
import { formatMoney } from '@/shared/utils/format';

function PaymentStatus({ payment, status, onRetry, onHome, showRetry }) {
  return (
    <section className={`payment-result-card ${status.className}`}>
      <p className="payment-result-eyebrow mb-3">KẾT QUẢ GIAO DỊCH</p>
      <div className="payment-result-icon" aria-hidden="true">
        {status.icon}
      </div>
      <h1 className="h3 fw-bold mb-2">{status.title}</h1>
      <p className="payment-result-description mb-4" role="status">
        {status.description}
      </p>

      <div className="payment-result-details">
        <div>
          <span>Mã đơn hàng</span>
          <strong>{payment?.orderCode || '—'}</strong>
        </div>
        <div>
          <span>Phương thức thanh toán</span>
          <strong>{paymentMethodNames[payment?.paymentMethod] || 'VietQR qua payOS'}</strong>
        </div>
        <div>
          <span>Số tiền giao dịch</span>
          <strong>{formatMoney(payment?.total || 0)}</strong>
        </div>
        <div>
          <span>Trạng thái</span>
          <strong className="payment-result-status">{status.title}</strong>
        </div>
      </div>

      <div className="payment-result-actions">
        {showRetry && (
          <Button variant="success" onClick={onRetry}>
            Thử thanh toán lại
          </Button>
        )}
        <HomeButton onClick={onHome} />
      </div>
    </section>
  );
}

export default PaymentStatus;
