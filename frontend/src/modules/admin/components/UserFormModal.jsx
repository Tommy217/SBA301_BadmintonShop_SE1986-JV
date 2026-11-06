import { useState } from 'react';
import { Modal, Form, Button, Alert, Spinner } from 'react-bootstrap';

export default function UserFormModal({ show, onHide, user, onSave }) {
  const isEditing = Boolean(user);
  const isCustomer = isEditing && user.roles.includes('CUSTOMER');
  const isCurrentAdmin = isEditing && user.roles.includes('ADMIN');

  // The parent remounts this modal (key) each time it opens, so the initial state is enough.
  const [formData, setFormData] = useState(() =>
    user
      ? {
          fullName: user.fullName,
          email: user.email,
          roles: user.roles,
          password: '',
          isActive: user.isActive,
        }
      : { fullName: '', email: '', roles: ['STAFF'], password: '', isActive: true },
  );

  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    if (name === 'roles') {
      setFormData((prev) => ({ ...prev, roles: [value] }));
    } else if (type === 'checkbox') {
      setFormData((prev) => ({ ...prev, [name]: checked }));
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Nếu là Customer, Admin chỉ cập nhật trạng thái isActive
    if (isCustomer) {
      setSubmitting(true);
      try {
        await onSave({ isActive: formData.isActive });
        onHide();
      } catch (err) {
        setError(err.message || 'Lỗi khi cập nhật trạng thái tài khoản.');
      } finally {
        setSubmitting(false);
      }
      return;
    }

    // Validation cho Staff / Manager / Admin
    if (!formData.fullName.trim()) {
      setError('Vui lòng nhập họ và tên.');
      return;
    }
    if (!formData.email.trim()) {
      setError('Vui lòng nhập địa chỉ email.');
      return;
    }
    if (!isEditing && !formData.password.trim()) {
      setError('Vui lòng nhập mật khẩu.');
      return;
    }

    setSubmitting(true);
    try {
      await onSave({ ...formData });
      onHide();
    } catch (err) {
      setError(err.message || 'Đã có lỗi xảy ra khi lưu người dùng.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered backdrop="static">
      <Modal.Header
        closeButton
        style={{ borderBottom: '1px solid #e2e8f0', backgroundColor: '#f8fafc' }}
      >
        <Modal.Title style={{ fontSize: '18px', fontWeight: 600, color: '#0f172a' }}>
          {isEditing
            ? isCustomer
              ? 'Chi tiết tài khoản khách hàng'
              : 'Chỉnh sửa tài khoản nhân sự'
            : 'Thêm mới tài khoản nhân sự'}
        </Modal.Title>
      </Modal.Header>

      <Form onSubmit={handleSubmit}>
        <Modal.Body style={{ padding: '24px' }}>
          {error && (
            <Alert variant="danger" style={{ fontSize: '13px', padding: '10px 14px' }}>
              {error}
            </Alert>
          )}

          <Form.Group className="mb-3">
            <Form.Label style={{ fontSize: '13px', fontWeight: 600, color: '#334155' }}>
              Họ và tên {!isCustomer && <span style={{ color: '#ef4444' }}>*</span>}
            </Form.Label>
            <Form.Control
              type="text"
              name="fullName"
              placeholder="VD: Nguyễn Văn A"
              value={formData.fullName}
              onChange={handleChange}
              disabled={submitting || isCustomer}
              required={!isCustomer}
            />
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label style={{ fontSize: '13px', fontWeight: 600, color: '#334155' }}>
              Địa chỉ Email {!isCustomer && <span style={{ color: '#ef4444' }}>*</span>}
            </Form.Label>
            <Form.Control
              type="email"
              name="email"
              placeholder="VD: user@badminton.vn"
              value={formData.email}
              onChange={handleChange}
              disabled={submitting || isCustomer || isCurrentAdmin}
              required={!isCustomer}
            />
          </Form.Group>

          {!isCustomer && (
            <Form.Group className="mb-3">
              <Form.Label style={{ fontSize: '13px', fontWeight: 600, color: '#334155' }}>
                Mật khẩu {!isEditing && <span style={{ color: '#ef4444' }}>*</span>}
              </Form.Label>
              <Form.Control
                type="password"
                name="password"
                placeholder={isEditing ? 'Để trống để giữ mật khẩu hiện tại' : 'Nhập mật khẩu'}
                value={formData.password}
                onChange={handleChange}
                disabled={submitting}
                required={!isEditing}
              />
            </Form.Group>
          )}

          <Form.Group className="mb-3">
            <Form.Label style={{ fontSize: '13px', fontWeight: 600, color: '#334155' }}>
              Vai trò
            </Form.Label>
            <Form.Select
              name="roles"
              value={formData.roles[0] || 'STAFF'}
              onChange={handleChange}
              disabled={submitting || isCustomer || isCurrentAdmin}
            >
              {isCustomer && <option value="CUSTOMER">CUSTOMER</option>}
              {isCurrentAdmin && <option value="ADMIN">ADMIN</option>}
              {!isCustomer && !isCurrentAdmin && (
                <>
                  <option value="STAFF">STAFF</option>
                  <option value="MANAGER">MANAGER</option>
                </>
              )}
            </Form.Select>
          </Form.Group>

          <Form.Group className="mb-2">
            <Form.Check
              type="switch"
              id="user-active-switch"
              name="isActive"
              label={
                <span
                  style={{
                    fontSize: '14px',
                    fontWeight: 500,
                    color: formData.isActive ? '#16a34a' : '#dc2626',
                  }}
                >
                  {formData.isActive ? 'Đang hoạt động (Active)' : 'Đã vô hiệu hóa (Inactive)'}
                </span>
              }
              checked={formData.isActive}
              onChange={handleChange}
              disabled={submitting || isCurrentAdmin}
            />
            {isCurrentAdmin && (
              <Form.Text className="text-muted" style={{ fontSize: '12px' }}>
                Không thể tự vô hiệu hóa tài khoản Admin đang đăng nhập.
              </Form.Text>
            )}
          </Form.Group>
        </Modal.Body>

        <Modal.Footer style={{ borderTop: '1px solid #e2e8f0', padding: '16px 24px' }}>
          <Button variant="secondary" onClick={onHide} disabled={submitting}>
            Hủy bỏ
          </Button>
          <Button
            variant="primary"
            type="submit"
            disabled={submitting}
            style={{ backgroundColor: '#2563eb', borderColor: '#2563eb' }}
          >
            {submitting ? (
              <>
                <Spinner animation="border" size="sm" className="me-2" />
                Đang lưu...
              </>
            ) : isEditing ? (
              'Lưu thay đổi'
            ) : (
              'Tạo người dùng'
            )}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
