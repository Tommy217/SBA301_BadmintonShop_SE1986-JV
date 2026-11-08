import { useEffect } from 'react';
import { Alert, Button, Container, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import PaymentStatus from '@/modules/payment/components/PaymentStatus';
import { getTransactionStatus } from '@/modules/payment/utils/paymentMapper';
import usePayment from '@/modules/payment/hooks/usePayment';
import { useShop } from '@/app/providers/shop/ShopContext';
import { paymentApi } from '@/modules/payment/api/payment.api';
import '@/modules/payment/PaymentResultPage.css';

export default function PaymentResultPage() {
  const model = usePayment({ enablePolling: true });
  const { refresh } = useShop();
  const navigate = useNavigate();

  useEffect(() => {
    if (model.orderId) {
      paymentApi.syncPaymentByOrder(model.orderId).catch(() => {});
    }
    refresh?.();
  }, [model.orderId, refresh]);
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
      : model.order.status === 'cancelled' || model.record?.status === 'cancelled'
        ? 'cancelled'
        : ['failed', 'expired'].includes(model.record?.status)
          ? 'failed'
          : 'pending';
  return (
    <main className="payment-result-page">
      <Container>
        <PaymentStatus
          payment={model.payment}
          status={getTransactionStatus(statusKey)}
          showRetry={
            ['failed', 'cancelled'].includes(statusKey) &&
            model.order.payment_status === 'unpaid' &&
            !['cancelled', 'returned'].includes(model.order.status)
          }
          onRetry={() => navigate('/payments?order=' + encodeURIComponent(model.orderId))}
          onHome={() => navigate('/')}
        />
        <div className="text-center mt-3 d-flex justify-content-center gap-2">
          {statusKey === 'pending' && (
            <Button
              as={Link}
              to={'/payments/qr?order=' + encodeURIComponent(model.orderId)}
              variant="primary"
            >
              Quay lại quét mã QR
            </Button>
          )}
          <Button as={Link} to="/orders" variant="outline-primary">
            Xem đơn hàng của tôi
          </Button>
        </div>
      </Container>
    </main>
  );
}
