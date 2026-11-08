import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Modal, Spinner } from 'react-bootstrap';
import { Link } from 'react-router';
import { profileApi } from '@/modules/auth/api/auth.api';
import { useAuth } from '@/app/providers/auth/AuthContext';
import Icon from '@/shared/components/Icon';
import './SavedAddressPicker.css';

export default function SavedAddressPicker({ onSelect, form, disabled }) {
  const { user } = useAuth();
  const [show, setShow] = useState(false);
  const [result, setResult] = useState(null);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!show) return;
    let active = true;
    profileApi
      .getAddresses()
      .then((addresses) => {
        if (active) setResult({ addresses, userId: user?.id, attempt });
      })
      .catch((error) => {
        if (active) setResult({ error: error.message, userId: user?.id, attempt });
      });
    return () => {
      active = false;
    };
  }, [show, user?.id, attempt]);
  // Reopening always reads the latest address book, including changes made in Profile.
  function open() {
    setResult(null);
    setShow(true);
  }
  const current = result?.userId === user?.id && result?.attempt === attempt ? result : null;
  const addresses = [...(current?.addresses || [])].sort(
    (a, b) => Number(b.isDefault) - Number(a.isDefault),
  );
  return (
    <>
      <Button
        className="saved-address-open"
        type="button"
        variant="outline-primary"
        onClick={open}
        disabled={disabled}
      >
        <Icon name="pin" size={16} /> Chọn địa chỉ đã lưu
      </Button>
      <Modal
        show={show}
        onHide={() => setShow(false)}
        centered
        scrollable
        dialogClassName="saved-address-modal"
        aria-labelledby="saved-address-title"
      >
        <Modal.Header closeButton>
          <div>
            <Modal.Title id="saved-address-title">Địa chỉ nhận hàng</Modal.Title>
            <p className="small text-secondary mb-0 mt-2">
              Chọn địa chỉ trong sổ địa chỉ của bạn để điền nhanh thông tin giao hàng.
            </p>
          </div>
        </Modal.Header>
        <Modal.Body>
          {!current ? (
            <div className="text-center py-5">
              <Spinner animation="border" />
              <p className="small text-secondary mt-3">Đang tải địa chỉ…</p>
            </div>
          ) : current.error ? (
            <Alert variant="warning">
              {current.error}
              <Button size="sm" className="ms-2" onClick={() => setAttempt((n) => n + 1)}>
                Thử lại
              </Button>
            </Alert>
          ) : !addresses.length ? (
            <div className="saved-address-empty">
              <Icon name="pin" size={36} />
              <h3>Chưa có địa chỉ đã lưu</h3>
              <p>Bạn có thể thêm địa chỉ trong hồ sơ hoặc nhập thông tin giao hàng trực tiếp.</p>
              <Button variant="light" onClick={() => setShow(false)}>
                Nhập địa chỉ mới
              </Button>
            </div>
          ) : (
            <div className="saved-address-list">
              {addresses.map((address) => {
                const fullAddress =
                  address.fullAddress ||
                  [address.street, address.ward, address.district, address.province]
                    .filter(Boolean)
                    .join(', ');
                const selected =
                  form.receiver_name === address.receiverName &&
                  form.receiver_phone === address.phone &&
                  form.shipping_address === fullAddress;
                return (
                  <button
                    type="button"
                    className={`saved-address-choice ${selected ? 'is-selected' : ''}`}
                    key={address.id}
                    disabled={disabled}
                    onClick={() => {
                      onSelect({
                        receiver_name: address.receiverName || '',
                        receiver_phone: address.phone || '',
                        shipping_address: fullAddress,
                      });
                      setShow(false);
                    }}
                  >
                    <span className="saved-address-icon">
                      <Icon name="pin" />
                    </span>
                    <span className="saved-address-copy">
                      <span className="saved-address-contact">
                        <strong>{address.receiverName}</strong>
                        <span>{address.phone}</span>
                        {address.isDefault && (
                          <Badge bg="light" text="primary">
                            Mặc định
                          </Badge>
                        )}
                      </span>
                      <span className="saved-address-detail">{fullAddress}</span>
                      <span className="saved-address-action">
                        {selected ? 'Đang sử dụng' : 'Giao đến địa chỉ này'}{' '}
                        <span aria-hidden="true">→</span>
                      </span>
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button as={Link} to="/profile" variant="link" className="me-auto">
            Quản lý sổ địa chỉ
          </Button>
          <Button variant="light" onClick={() => setShow(false)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
