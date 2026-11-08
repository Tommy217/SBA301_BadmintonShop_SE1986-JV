import { Alert, Button, Container, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import PaymentQR from '@/modules/payment/components/PaymentQR';
import usePayment from '@/modules/payment/hooks/usePayment';
import { useShop } from '@/app/providers/shop/ShopContext';
import { paymentApi } from '../api/payment.api';
import '@/modules/payment/QrPaymentPage.css';

export default function QrPaymentPage() {
  const model = usePayment({ enablePolling: true });
  const { refresh } = useShop();
  const navigate = useNavigate();

  const handleCancelPayment = async (reason) => {
    if (!model.orderId) return;
    try {
      await paymentApi.cancelPayment({
        order_id: model.orderId,
        reason: reason || 'Khách hàng chủ động hủy thanh toán',
      });
      refresh?.();
      navigate('/payments/result?order=' + encodeURIComponent(model.orderId));
    } catch {
      try {
        await paymentApi.cancelPaymentByOrder(model.orderId, reason);
        refresh?.();
        navigate('/payments/result?order=' + encodeURIComponent(model.orderId));
      } catch (e) {
        alert(e.message || 'Không thể hủy thanh toán.');
      }
    }
  };
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
        <Button className="app-back-button"
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
      onChooseAnotherMethod={() => navigate('/payments?order=' + encodeURIComponent(model.orderId))}
      onCancelPayment={handleCancelPayment}
    />
  );
}
