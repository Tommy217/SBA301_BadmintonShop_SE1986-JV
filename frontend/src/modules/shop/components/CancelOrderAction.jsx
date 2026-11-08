import { useRef, useState } from 'react';
import { Alert, Button, Form, Modal } from 'react-bootstrap';
import { useShop } from '@/app/providers/shop/ShopContext';
import { orderApi } from '../api/shop.api';

const reasons = [
  'Tôi muốn thay đổi sản phẩm hoặc số lượng',
  'Tôi muốn thay đổi địa chỉ nhận hàng',
  'Tôi muốn thay đổi phương thức thanh toán',
  'Tôi không còn nhu cầu mua hàng',
];

export default function CancelOrderAction({ order, onCancelled }) {
  const { act, busy } = useShop();
  const [show, setShow] = useState(false);
  const [reason, setReason] = useState('');
  const [otherReason, setOtherReason] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const lock = useRef(false);
  const locked = busy || submitting;
  if (order.can_cancel !== true && order.can_cancel !== 1) return null;

  async function cancel(event) {
    event.preventDefault();
    const text = reason === 'other' ? otherReason.trim() : reason;
    if (!text) {
      setError('Vui lòng chọn hoặc nhập lý do hủy đơn.');
      return;
    }
    if (locked || lock.current) return;
    lock.current = true;
    setSubmitting(true);
    setError('');
    try {
      await act(() => orderApi.cancel(order.id, text));
      setShow(false);
      onCancelled?.();
    } catch (failure) {
      setError(
        failure.message || 'Không thể hủy đơn. Vui lòng tải lại để kiểm tra trạng thái mới nhất.',
      );
    } finally {
      lock.current = false;
      setSubmitting(false);
    }
  }

  return (
    <>
      <Button
        variant="outline-danger"
        disabled={locked}
        onClick={() => {
          setReason('');
          setOtherReason('');
          setError('');
          setShow(true);
        }}
      >
        Hủy đơn hàng
      </Button>
      <Modal
        show={show}
        onHide={() => !locked && setShow(false)}
        centered
        className="delivery-modal"
        aria-labelledby="cancel-order-title"
      >
        <Form onSubmit={cancel}>
          <Modal.Header closeButton={!locked}>
            <Modal.Title id="cancel-order-title">Hủy đơn hàng</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <p>
              Bạn muốn hủy đơn <strong>{order.order_code}</strong>?
            </p>
            {order.payment_status === 'paid' && (
              <Alert variant="info">
                Đơn đã thanh toán. Sau khi hủy, cửa hàng sẽ xử lý hoàn tiền; tiền chưa được hoàn
                ngay khi bạn xác nhận.
              </Alert>
            )}
            {error && (
              <Alert variant="danger" role="alert">
                {error}
              </Alert>
            )}
            <Form.Group controlId="cancel-order-reason">
              <Form.Label>
                Lý do hủy <span className="text-danger">*</span>
              </Form.Label>
              <Form.Select
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                disabled={locked}
                required
              >
                <option value="">Chọn lý do hủy đơn</option>
                {reasons.map((text) => (
                  <option key={text} value={text}>
                    {text}
                  </option>
                ))}
                <option value="other">Lý do khác</option>
              </Form.Select>
            </Form.Group>
            {reason === 'other' && (
              <Form.Group className="mt-3" controlId="cancel-order-other">
                <Form.Label>Lý do chi tiết</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  maxLength={300}
                  value={otherReason}
                  onChange={(event) => setOtherReason(event.target.value)}
                  disabled={locked}
                  required
                />
                <Form.Text>Tối đa 300 ký tự.</Form.Text>
              </Form.Group>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" disabled={locked} onClick={() => setShow(false)}>
              Giữ lại đơn hàng
            </Button>
            <Button
              type="submit"
              variant="danger"
              disabled={locked || !reason || (reason === 'other' && !otherReason.trim())}
            >
              {submitting ? 'Đang hủy…' : 'Xác nhận hủy đơn'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </>
  );
}
