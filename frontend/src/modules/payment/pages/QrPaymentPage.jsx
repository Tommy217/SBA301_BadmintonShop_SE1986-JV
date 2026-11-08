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
    if (!model.orderId || !model.record?.id)
      throw new Error('Không tìm thấy phiên thanh toán hiện tại.');
    await paymentApi.cancelPaymentById(model.record.id, reason);
    refresh?.().catch(() => {});
    navigate('/payments?order=' + encodeURIComponent(model.orderId), {
      replace: true,
      state: { paymentCancelled: true },
    });
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
        <Button
          className="app-back-button"
          as={Link}
          to={model.orderId ? '/payments?order=' + encodeURIComponent(model.orderId) : '/orders'}
        >
          Quay lại
        </Button>
      </Container>
    );
  if (['cancelled', 'returned'].includes(model.order.status))
    return (
      <Container fluid className="py-3">
        <Alert variant="info">Đơn hàng đã hủy hoặc trả lại, không thể tiếp tục thanh toán.</Alert>
        <Button as={Link} to="/orders" className="app-back-button">
          Quay lại đơn hàng
        </Button>
      </Container>
    );
  if (model.order.payment_status === 'paid')
    return (
      <Container fluid className="py-3">
        <Alert variant="success">Đơn hàng đã thanh toán thành công.</Alert>
        <Button as={Link} to={'/payments/result?order=' + encodeURIComponent(model.orderId)}>
          Xem kết quả thanh toán
        </Button>
      </Container>
    );
  if (['cancelled', 'expired', 'failed'].includes(model.record?.status))
    return (
      <Container fluid className="py-3">
        <Alert variant="info">
          Phiên thanh toán này đã kết thúc. Bạn có thể tạo mã QR mới để thanh toán đơn hàng.
        </Alert>
        <Button as={Link} to={'/payments?order=' + encodeURIComponent(model.orderId)}>
          Thanh toán lại
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
