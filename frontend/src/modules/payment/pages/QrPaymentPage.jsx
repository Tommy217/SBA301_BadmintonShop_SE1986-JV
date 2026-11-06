import { Alert, Button, Container, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import PaymentQR from '@/modules/payment/components/PaymentQR';
import usePayment from '@/modules/payment/hooks/usePayment';
import '@/modules/payment/QrPaymentPage.css';

export default function QrPaymentPage() {
  const model = usePayment();
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
        <Alert variant="info">
          {model.error || 'Vui lòng chọn phương thức thanh toán cho đơn hàng.'}
        </Alert>
        <Button
          as={Link}
          to={model.orderId ? '/payments?order=' + encodeURIComponent(model.orderId) : '/orders'}
        >
          Quay lại
        </Button>
      </Container>
    );
  return (
    <PaymentQR
      payment={model.payment}
      onCheckResult={() => navigate('/payments/result?order=' + encodeURIComponent(model.orderId))}
      onChooseAnotherMethod={() => navigate('/payments?order=' + encodeURIComponent(model.orderId))}
    />
  );
}
