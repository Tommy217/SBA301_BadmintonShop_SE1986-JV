import { Alert, Button, Card, Col, Container, Row, Spinner } from 'react-bootstrap';
import { Link } from 'react-router';
import PaymentSummary from '@/modules/payment/components/PaymentSummary';
import usePayment from '@/modules/payment/hooks/usePayment';
import PaymentPage from './PaymentPage';
import '@/modules/payment/PaymentPage.css';
import Icon from '@/shared/components/Icon';

function CheckoutPage() {
  const payment = usePayment();

  if (payment.loading)
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" />
        <p>Đang tải đơn hàng…</p>
      </Container>
    );
  if (!payment.available)
    return (
      <Container className="py-5">
        <Alert variant="info">
          {payment.error || 'Chọn đơn hàng trả trước để tiếp tục thanh toán.'}
        </Alert>
        <Button as={Link} to="/orders">
          Xem đơn hàng
        </Button>
      </Container>
    );
  return (
    <section className="payment-page">
      <Container fluid className="payment-container">
        <Button as={Link} to="/orders" variant="outline-secondary" className="app-back-button mb-3">
          ← Quay lại đơn hàng
        </Button>
        <header className="payment-heading mb-4">
          <div>
            <p className="payment-eyebrow">HOÀN TẤT ĐƠN HÀNG</p>
            <h1 className="h3 fw-bold mb-2">Thanh toán</h1>
            <p className="text-secondary mb-0">
              Chỉ còn một bước để sẵn sàng cho trận đấu tiếp theo.
            </p>
          </div>
          <div className="payment-order-reference">
            <Icon name="receipt" size={18} />
            <div>
              <span>Mã đơn hàng</span>
              <strong>{payment.order.order_code}</strong>
            </div>
          </div>
        </header>

        <ol className="payment-progress" aria-label="Tiến trình thanh toán">
          <li aria-current="step">
            <span>1</span>Phương thức thanh toán
          </li>
          <li>
            <span>2</span>Quét mã & hoàn tất
          </li>
        </ol>

        {payment.error && <Alert variant="danger">{payment.error}</Alert>}
        <Row className="g-4 align-items-start">
          <Col lg={7}>
            <PaymentPage
              paymentMethod={payment.paymentMethod}
              onPaymentMethodChange={payment.setPaymentMethod}
              onSubmit={payment.startPayment}
              busy={payment.busy}
              paid={payment.order.payment_status === 'paid'}
              total={payment.total}
            />
          </Col>

          <Col lg={5}>
            <Card className="delivery-card mb-4">
              <Card.Body className="p-4">
                <div className="d-flex align-items-center justify-content-between gap-3 mb-3">
                  <div>
                    <h2 className="h6 fw-bold mb-0 payment-delivery-title">
                      <Icon name="pin" size={18} />
                      Địa chỉ nhận hàng
                    </h2>
                  </div>
                  <Button
                    className="delivery-edit"
                    variant="link"
                    type="button"
                    as={Link}
                    to="/orders"
                  >
                    Xem đơn hàng
                  </Button>
                </div>
                <p className="delivery-recipient fw-semibold mb-1">
                  {payment.order.receiver_name} <span>•</span> {payment.order.receiver_phone}
                </p>
                <p className="text-secondary small mb-0">{payment.order.shipping_address}</p>
              </Card.Body>
            </Card>

            <PaymentSummary
              isExpanded={payment.isOrderExpanded}
              items={payment.orderItems}
              onToggle={() => payment.setIsOrderExpanded((expanded) => !expanded)}
              shippingFee={payment.shippingFee}
              subtotal={payment.subtotal}
              total={payment.total}
              discount={payment.order.discount_amount}
            />
          </Col>
        </Row>
      </Container>
    </section>
  );
}

export default CheckoutPage;
