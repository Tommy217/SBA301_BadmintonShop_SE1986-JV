import { Alert, Button, Container, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import PaymentStatus from '@/modules/payment/components/PaymentStatus';
import { getTransactionStatus } from '@/modules/payment/utils/paymentMapper';
import usePayment from '@/modules/payment/hooks/usePayment';
import '@/modules/payment/PaymentResultPage.css';

export default function PaymentResultPage() {
  const model = usePayment({ enablePolling: true });
  const navigate = useNavigate();
  if (model.loading)
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" />
      </Container>
    );
  if (!model.available)
    return (
      <Container className="py-5">
        <Alert variant="info">{model.error || 'Không tìm thấy đơn hàng trả trước.'}</Alert>
        <Button as={Link} to="/orders">
          Xem đơn hàng
        </Button>
      </Container>
    );
  const statusKey =
    model.order.payment_status === 'paid'
      ? 'success'
      : model.record.status === 'failed'
        ? 'failed'
        : 'pending';
  return (
    <main className="payment-result-page">
      <Container>
        <PaymentStatus
          payment={model.payment}
          status={getTransactionStatus(statusKey)}
          showRetry={statusKey === 'failed'}
          onRetry={() => navigate('/payments?order=' + encodeURIComponent(model.orderId))}
          onHome={() => navigate('/')}
        />
        <div className="text-center mt-3">
          <Button as={Link} to="/orders" variant="outline-primary">
            Xem đơn hàng của tôi
          </Button>
        </div>
      </Container>
    </main>
  );
}
