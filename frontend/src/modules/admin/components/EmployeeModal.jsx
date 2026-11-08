import { useState, useEffect } from 'react';
import { Modal, Form, Button, Alert } from 'react-bootstrap';
import { ROLES } from '@/shared/constants/roles';

export default function EmployeeModal({ show, onHide, onSave, user, currentAdminId }) {
  const isEditing = Boolean(user);
  const isSelf = isEditing && currentAdminId != null && String(user.id) === String(currentAdminId);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    role: ROLES.STAFF,
    password: '',
    status: 'active',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (show) {
      if (user) {
        setFormData({
          fullName: user.fullName || '',
          email: user.email || '',
          role: user.role || ROLES.STAFF,
          password: '',
          status: user.status || (user.locked ? 'locked' : 'active'),
        });
      } else {
        setFormData({
          fullName: '',
          email: '',
          role: ROLES.STAFF,
          password: '',
          status: 'active',
        });
      }
      setError('');
      setSubmitting(false);
    }
  }, [show, user]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const fullName = formData.fullName.trim();
    if (!fullName) {
      setError('Vui lòng nhập họ và tên nhân viên.');
      return;
    }

    if (!isEditing) {
      const email = formData.email.trim();
      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setError('Vui lòng nhập địa chỉ email hợp lệ.');
        return;
      }
      if (!formData.password || formData.password.length < 6) {
        setError('Mật khẩu phải có ít nhất 6 ký tự.');
        return;
      }
    } else {
      if (formData.password && formData.password.length < 6) {
        setError('Mật khẩu mới phải có ít nhất 6 ký tự.');
        return;
      }
    }

    try {
      setSubmitting(true);
      if (isEditing) {
        const payload = {
          fullName,
          status: formData.status,
        };
        if (!isSelf) {
          payload.role = formData.role;
        }
        if (formData.password) {
          payload.password = formData.password;
        }
        await onSave(payload);
      } else {
        await onSave({
          fullName,
          email: formData.email.trim(),
          password: formData.password,
          role: formData.role,
        });
      }
      onHide();
    } catch (err) {
      setError(err.message || 'Không thể lưu thông tin nhân viên.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered backdrop="static">
      <Modal.Header closeButton>
        <Modal.Title>{isEditing ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới'}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          {error && (
            <Alert variant="danger" dismissible onClose={() => setError('')} className="py-2">
              {error}
            </Alert>
          )}

          {isSelf && (
            <Alert variant="warning" className="py-2 small">
              Bạn đang chỉnh sửa tài khoản của chính mình. Quyền hạn và trạng thái tài khoản không thể tự thay đổi.
            </Alert>
          )}

          <Form.Group className="mb-3" controlId="employeeFullName">
            <Form.Label>Họ và tên <span className="text-danger">*</span></Form.Label>
            <Form.Control
              type="text"
              name="fullName"
              placeholder="Nguyễn Văn A"
              value={formData.fullName}
              onChange={handleChange}
              required
              autoFocus
            />
          </Form.Group>

          <Form.Group className="mb-3" controlId="employeeEmail">
            <Form.Label>Email <span className="text-danger">*</span></Form.Label>
            <Form.Control
              type="email"
              name="email"
              placeholder="nhanvien@badminton.vn"
              value={formData.email}
              onChange={handleChange}
              disabled={isEditing}
              required
            />
            {isEditing && (
              <Form.Text className="text-muted">Email là định danh tài khoản, không thể thay đổi.</Form.Text>
            )}
          </Form.Group>

          <Form.Group className="mb-3" controlId="employeeRole">
            <Form.Label>Vai trò nội bộ <span className="text-danger">*</span></Form.Label>
            <Form.Select
              name="role"
              value={formData.role}
              onChange={handleChange}
              disabled={isSelf}
            >
              <option value={ROLES.STAFF}>Nhân viên (STAFF)</option>
              <option value={ROLES.MANAGER}>Quản lý (MANAGER)</option>
              <option value={ROLES.ADMIN}>Quản trị viên (ADMIN)</option>
            </Form.Select>
          </Form.Group>

          <Form.Group className="mb-3" controlId="employeePassword">
            <Form.Label>
              {isEditing ? 'Mật khẩu mới (Tùy chọn)' : 'Mật khẩu khởi tạo'} {!isEditing && <span className="text-danger">*</span>}
            </Form.Label>
            <Form.Control
              type="password"
              name="password"
              placeholder={isEditing ? 'Để trống nếu không đổi mật khẩu' : 'Tối thiểu 6 ký tự'}
              value={formData.password}
              onChange={handleChange}
              required={!isEditing}
            />
          </Form.Group>

          {isEditing && (
            <Form.Group className="mb-3" controlId="employeeStatus">
              <Form.Label>Trạng thái tài khoản</Form.Label>
              <Form.Select
                name="status"
                value={formData.status}
                onChange={handleChange}
                disabled={isSelf}
              >
                <option value="active">Hoạt động (Active)</option>
                <option value="locked">Khóa (Locked)</option>
              </Form.Select>
            </Form.Group>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide} disabled={submitting}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" disabled={submitting}>
            {submitting ? 'Đang lưu...' : isEditing ? 'Lưu thay đổi' : 'Tạo tài khoản'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

