import { useState } from 'react';
import { Alert, Button, Form, Modal, Spinner } from 'react-bootstrap';
import { Link } from 'react-router';
import { carrierServices } from '@/modules/delivery/constants';
import { deliveryApi } from '@/modules/delivery/api/delivery.api';
import { useCarriers, useShipments } from '@/modules/delivery/hooks/useDelivery';
import '../delivery.css';

const SHIPPABLE = ['confirmed', 'preparing'];
const INACTIVE = ['cancelled', 'rejected'];

// Nut "Tao van don" + modal chon DVVC / goi dich vu / can nang / ghi chu (trang quan ly don hang).
export default function CreateShipmentAction({ order, onCreated }) {
  const shippable = SHIPPABLE.includes(order.status);
  const { data: page, reload } = useShipments({ orderId: order.id, size: 5 });
  const { data: carrierList } = useCarriers();
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const active = (page?.content || []).find((s) => !INACTIVE.includes(s.status));

  function open() {
    setError('');
    setNotice('');
    setForm({
      carrierCode: carrierList?.[0]?.code || 'fastship',
      serviceType: 'standard',
      weightGrams: 500,
      note: '',
    });
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      const res = await deliveryApi.create({
        orderId: order.id,
        carrierCode: form.carrierCode,
        serviceType: form.serviceType,
        weightGrams: form.weightGrams ? Number(form.weightGrams) : null,
        note: form.note.trim() || null,
      });
      setForm(null);
      setNotice(res.message);
      reload();
      onCreated?.();
    } catch (err) {
      setError(err.message || 'Không tạo được vận đơn.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      {active && (
        <Button as={Link} to={`/management/deliveries/${active.id}`} variant="outline-secondary">
          Xem vận đơn {active.tracking_number || '(chờ đối tác)'}
        </Button>
      )}
      {shippable && page && !active && (
        <Button variant="outline-primary" onClick={open}>
          Tạo vận đơn
        </Button>
      )}
      {notice && (
        <Alert
          variant="success"
          className="w-100 mb-0 py-2"
          dismissible
          onClose={() => setNotice('')}
        >
          {notice}
        </Alert>
      )}

      <Modal show={form !== null} onHide={() => setForm(null)} centered className="delivery-modal">
        {form && (
          <Form onSubmit={submit}>
            <Modal.Header closeButton>
              <Modal.Title className="h5">Tạo vận đơn cho đơn {order.order_code}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              {error && <Alert variant="danger">{error}</Alert>}
              <Form.Group className="mb-3" controlId="shipment-carrier">
                <Form.Label>Đơn vị vận chuyển</Form.Label>
                <Form.Select
                  value={form.carrierCode}
                  onChange={(e) => setForm({ ...form, carrierCode: e.target.value })}
                  required
                >
                  {(carrierList || []).map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.name}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group className="mb-3" controlId="shipment-service">
                <Form.Label>Gói dịch vụ</Form.Label>
                <Form.Select
                  value={form.serviceType}
                  onChange={(e) => setForm({ ...form, serviceType: e.target.value })}
                >
                  {Object.entries(carrierServices).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group className="mb-3" controlId="shipment-weight">
                <Form.Label>Cân nặng (gram)</Form.Label>
                <Form.Control
                  type="number"
                  min={1}
                  max={50000}
                  value={form.weightGrams}
                  onChange={(e) => setForm({ ...form, weightGrams: e.target.value })}
                />
              </Form.Group>
              <Form.Group controlId="shipment-note">
                <Form.Label>Ghi chú cho đơn vị vận chuyển</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={2}
                  maxLength={500}
                  value={form.note}
                  onChange={(e) => setForm({ ...form, note: e.target.value })}
                />
              </Form.Group>
            </Modal.Body>
            <Modal.Footer>
              <Button variant="outline-secondary" onClick={() => setForm(null)}>
                Đóng
              </Button>
              <Button type="submit" disabled={saving || !carrierList?.length}>
                {saving && <Spinner animation="border" size="sm" className="me-2" />}
                Tạo vận đơn
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Modal>
    </>
  );
}
