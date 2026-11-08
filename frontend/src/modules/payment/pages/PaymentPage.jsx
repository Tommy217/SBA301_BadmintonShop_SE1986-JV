import { Button, Card, Form } from 'react-bootstrap';
import PaymentMethodSelector from '@/modules/payment/components/PaymentMethodSelector';
import Icon from '@/shared/components/Icon';
import { formatMoney } from '@/shared/utils/format';

function PaymentPage({
  paymentMethod,
  onPaymentMethodChange,
  onSubmit,
  busy = false,
  paid = false,
  total = 0,
}) {
  return (
    <Card className="payment-card h-100">
      <Card.Body className="p-4">
        <div className="payment-amount-banner">
          <span>Số tiền cần thanh toán</span>
          <strong>{formatMoney(total)}</strong>
          <small>Đối chiếu số tiền trước khi xác nhận trên ứng dụng ngân hàng.</small>
        </div>
        <div className="mb-4">
          <h2 className="h5 fw-bold mb-2">Phương thức thanh toán</h2>
          <p className="small text-secondary mb-0">
            Thanh toán trực tuyến bằng ứng dụng ngân hàng của bạn.
          </p>
        </div>

        <Form onSubmit={onSubmit}>
          <fieldset disabled={busy || paid}>
            <PaymentMethodSelector paymentMethod={paymentMethod} onChange={onPaymentMethodChange} />
          </fieldset>

          {paymentMethod === 'card' && (
            <p className="payment-note small text-secondary mt-3 mb-0">
              Bạn sẽ được hướng dẫn thanh toán qua cổng thanh toán trực tuyến sau khi xác nhận đơn
              hàng.
            </p>
          )}

          <div className="payment-howto">
            <h3>Sau khi tiếp tục</h3>
            <div>
              <span>1</span>Mở ứng dụng ngân hàng và quét mã VietQR.
            </div>
            <div>
              <span>2</span>Kiểm tra thông tin, xác nhận và chờ cập nhật đơn hàng.
            </div>
          </div>
          <div className="payment-security">
            <Icon name="shield" size={17} />
            Thanh toán qua cổng payOS
          </div>
          <Button
            className="payment-submit w-100 mt-4"
            type="submit"
            size="lg"
            disabled={busy || paid}
          >
            {paid ? 'Đơn hàng đã thanh toán' : busy ? 'Đang xử lý…' : 'Tiếp tục thanh toán'}
            {!paid && !busy && <span aria-hidden="true">→</span>}
          </Button>
        </Form>
      </Card.Body>
    </Card>
  );
}

export default PaymentPage;
