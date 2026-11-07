import { useEffect, useState } from 'react';
import {
  Container,
  Row,
  Col,
  Card,
  Nav,
  Tab,
  Form,
  Button,
  Alert,
  Badge,
  Modal,
  Spinner,
} from 'react-bootstrap';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { profileApi } from '@/modules/auth/api/auth.api';

export default function ProfilePage() {
  const { user, login } = useAuth();
  const [activeTab, setActiveTab] = useState('profile');

  // Loading & Alerts
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', message: '' });

  // Profile form state
  const [profileForm, setProfileForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    gender: 'male',
    birthDate: '',
    avatarUrl: '',
  });

  // Password form state
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);

  // Addresses state
  const [addresses, setAddresses] = useState([]);
  const [showAddressModal, setShowAddressModal] = useState(false);
  const [editingAddress, setEditingAddress] = useState(null);
  const [addressForm, setAddressForm] = useState({
    receiverName: '',
    phone: '',
    province: '',
    district: '',
    ward: '',
    street: '',
    isDefault: false,
  });

  // Initial data loading
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [profileData, addressList] = await Promise.all([
        profileApi.getProfile(),
        profileApi.getAddresses(),
      ]);

      if (profileData) {
        setProfileForm({
          fullName: profileData.fullName || '',
          email: profileData.email || '',
          phone: profileData.phone || '',
          gender: profileData.gender || 'male',
          birthDate: profileData.birthDate || '',
          avatarUrl: profileData.avatarUrl || '',
        });
      }
      setAddresses(addressList || []);
    } catch (err) {
      setFeedback({
        type: 'danger',
        message: err.message || 'Không thể tải dữ liệu hồ sơ. Vui lòng thử lại.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFeedback({ type: '', message: '' });

    try {
      const updated = await profileApi.updateProfile({
        fullName: profileForm.fullName.trim(),
        phone: profileForm.phone ? profileForm.phone.trim() : null,
        gender: profileForm.gender,
        birthDate: profileForm.birthDate || null,
        avatarUrl: profileForm.avatarUrl ? profileForm.avatarUrl.trim() : null,
      });

      setFeedback({ type: 'success', message: 'Cập nhật thông tin cá nhân thành công!' });
      // Đồng bộ thông tin vào AuthContext
      if (user && login) {
        login({ ...user, fullName: updated.fullName, email: updated.email });
      }
    } catch (err) {
      setFeedback({ type: 'danger', message: err.message || 'Cập nhật thất bại. Vui lòng kiểm tra lại.' });
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setFeedback({ type: 'danger', message: 'Xác nhận mật khẩu mới không khớp.' });
      return;
    }

    setSaving(true);
    setFeedback({ type: '', message: '' });

    try {
      const res = await profileApi.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword,
      });

      setFeedback({ type: 'success', message: res?.message || 'Đổi mật khẩu thành công!' });
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setFeedback({ type: 'danger', message: err.message || 'Đổi mật khẩu thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  const openAddressModal = (addr = null) => {
    setEditingAddress(addr);
    if (addr) {
      setAddressForm({
        receiverName: addr.receiverName || '',
        phone: addr.phone || '',
        province: addr.province || '',
        district: addr.district || '',
        ward: addr.ward || '',
        street: addr.street || '',
        isDefault: addr.isDefault || false,
      });
    } else {
      setAddressForm({
        receiverName: profileForm.fullName || '',
        phone: profileForm.phone || '',
        province: '',
        district: '',
        ward: '',
        street: '',
        isDefault: addresses.length === 0,
      });
    }
    setShowAddressModal(true);
  };

  const handleAddressSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFeedback({ type: '', message: '' });

    try {
      if (editingAddress) {
        await profileApi.updateAddress(editingAddress.id, addressForm);
        setFeedback({ type: 'success', message: 'Cập nhật địa chỉ thành công!' });
      } else {
        await profileApi.addAddress(addressForm);
        setFeedback({ type: 'success', message: 'Thêm địa chỉ mới thành công!' });
      }
      setShowAddressModal(false);
      const updatedList = await profileApi.getAddresses();
      setAddresses(updatedList || []);
    } catch (err) {
      setFeedback({ type: 'danger', message: err.message || 'Lưu địa chỉ thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAddress = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) return;
    setSaving(true);
    setFeedback({ type: '', message: '' });

    try {
      await profileApi.deleteAddress(id);
      setFeedback({ type: 'success', message: 'Xóa địa chỉ thành công!' });
      const updatedList = await profileApi.getAddresses();
      setAddresses(updatedList || []);
    } catch (err) {
      setFeedback({ type: 'danger', message: err.message || 'Xóa địa chỉ thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Container className="profile-container py-4">
      <style>{`
        .profile-container {
          max-width: 1040px;
          margin: 0 auto;
          padding-bottom: 48px;
        }
        .profile-card {
          border-radius: 16px;
          border: 1px solid #e3edf4;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
          background: #fff;
          overflow: hidden;
        }
        .profile-nav-pills {
          background: #f4f8fb;
          border-radius: 12px;
          padding: 6px;
          gap: 6px;
        }
        .profile-nav-pills .nav-link {
          color: #506677;
          font-weight: 500;
          border-radius: 8px;
          padding: 10px 18px;
          transition: all 0.2s ease;
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .profile-nav-pills .nav-link.active {
          background: #0d6efd;
          color: #fff;
          box-shadow: 0 2px 8px rgba(13, 110, 253, 0.25);
        }
        .profile-avatar-preview {
          width: 96px;
          height: 96px;
          border-radius: 50%;
          object-fit: cover;
          border: 3px solid #e3edf4;
          background: #eef5fa;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 36px;
          font-weight: 600;
          color: #0d6efd;
        }
        .address-item-card {
          border: 1px solid #e3edf4;
          border-radius: 12px;
          padding: 18px 20px;
          margin-bottom: 16px;
          transition: all 0.2s ease;
          background: #fff;
        }
        .address-item-card.is-default {
          border-color: #0d6efd;
          background: #fbfdff;
        }
        .address-item-card:hover {
          box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
        }
        .badge-default-addr {
          background: #e7f1ff;
          color: #0d6efd;
          font-weight: 600;
          padding: 4px 10px;
          border-radius: 6px;
          font-size: 12px;
        }
      `}</style>
      <div className="eyebrow">TÀI KHOẢN KHÁCH HÀNG</div>
      <h1 className="h2 fw-bold mb-4">Hồ sơ cá nhân</h1>

      {feedback.message && (
        <Alert
          variant={feedback.type}
          dismissible
          onClose={() => setFeedback({ type: '', message: '' })}
          className="mb-4"
        >
          {feedback.message}
        </Alert>
      )}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
          <p className="mt-3 text-secondary">Đang nạp thông tin cá nhân...</p>
        </div>
      ) : (
        <Tab.Container activeKey={activeTab} onSelect={(k) => { setActiveTab(k); setFeedback({ type: '', message: '' }); }}>
          <Row>
            <Col lg={3} className="mb-4">
              <Card className="profile-card p-3">
                <div className="text-center py-3 mb-2">
                  <div className="profile-avatar-preview mx-auto mb-3">
                    {profileForm.avatarUrl ? (
                      <img
                        src={profileForm.avatarUrl}
                        alt="Avatar"
                        className="w-100 h-100 rounded-circle"
                        onError={(e) => { e.target.style.display = 'none'; }}
                      />
                    ) : (
                      (profileForm.fullName || profileForm.email).trim().charAt(0).toUpperCase()
                    )}
                  </div>
                  <h2 className="h6 fw-bold mb-1 text-truncate">{profileForm.fullName || 'Khách hàng'}</h2>
                  <small className="text-secondary text-truncate d-block">{profileForm.email}</small>
                </div>
                <Nav variant="pills" className="flex-column profile-nav-pills">
                  <Nav.Item>
                    <Nav.Link eventKey="profile">Thông tin tài khoản</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="addresses">Sổ địa chỉ nhận hàng</Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link eventKey="password">Đổi mật khẩu</Nav.Link>
                  </Nav.Item>
                </Nav>
              </Card>
            </Col>

            <Col lg={9}>
              <Tab.Content>
                {/* TAB 1: THÔNG TIN TÀI KHOẢN */}
                <Tab.Pane eventKey="profile">
                  <Card className="profile-card p-4">
                    <Card.Body>
                      <h2 className="h5 fw-bold mb-3">Thông tin tài khoản</h2>
                      <p className="text-secondary mb-4">Quản lý và cập nhật thông tin cá nhân của bạn.</p>
                      <Form onSubmit={handleProfileSubmit}>
                        <Row className="g-3">
                          <Col md={6}>
                            <Form.Group controlId="profileEmail">
                              <Form.Label className="fw-semibold">Email</Form.Label>
                              <Form.Control
                                type="email"
                                value={profileForm.email}
                                disabled
                                className="bg-light"
                              />
                              <Form.Text className="text-muted">Email đăng nhập không thể thay đổi.</Form.Text>
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileFullName">
                              <Form.Label className="fw-semibold">Họ và tên *</Form.Label>
                              <Form.Control
                                type="text"
                                required
                                maxLength={150}
                                value={profileForm.fullName}
                                onChange={(e) => setProfileForm({ ...profileForm, fullName: e.target.value })}
                                placeholder="Nhập họ và tên"
                              />
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profilePhone">
                              <Form.Label className="fw-semibold">Số điện thoại</Form.Label>
                              <Form.Control
                                type="tel"
                                maxLength={20}
                                value={profileForm.phone}
                                onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                                placeholder="Ví dụ: 0912345678"
                              />
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileBirthDate">
                              <Form.Label className="fw-semibold">Ngày sinh</Form.Label>
                              <Form.Control
                                type="date"
                                value={profileForm.birthDate}
                                onChange={(e) => setProfileForm({ ...profileForm, birthDate: e.target.value })}
                              />
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileGender">
                              <Form.Label className="fw-semibold">Giới tính</Form.Label>
                              <Form.Select
                                value={profileForm.gender}
                                onChange={(e) => setProfileForm({ ...profileForm, gender: e.target.value })}
                              >
                                <option value="male">Nam</option>
                                <option value="female">Nữ</option>
                                <option value="other">Khác</option>
                              </Form.Select>
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileAvatarUrl">
                              <Form.Label className="fw-semibold">Đường dẫn ảnh đại diện (URL)</Form.Label>
                              <Form.Control
                                type="url"
                                maxLength={500}
                                value={profileForm.avatarUrl}
                                onChange={(e) => setProfileForm({ ...profileForm, avatarUrl: e.target.value })}
                                placeholder="https://example.com/avatar.jpg"
                              />
                            </Form.Group>
                          </Col>
                        </Row>
                        <div className="mt-4 pt-2">
                          <Button type="submit" variant="primary" disabled={saving}>
                            {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                          </Button>
                        </div>
                      </Form>
                    </Card.Body>
                  </Card>
                </Tab.Pane>

                {/* TAB 2: SỔ ĐỊA CHỈ */}
                <Tab.Pane eventKey="addresses">
                  <Card className="profile-card p-4">
                    <Card.Body>
                      <div className="d-flex justify-content-between align-items-center mb-4">
                        <div>
                          <h2 className="h5 fw-bold mb-1">Sổ địa chỉ nhận hàng</h2>
                          <p className="text-secondary mb-0">Quản lý địa chỉ giao nhận của bạn.</p>
                        </div>
                        <Button variant="primary" onClick={() => openAddressModal()}>
                          + Thêm địa chỉ mới
                        </Button>
                      </div>

                      {addresses.length === 0 ? (
                        <div className="text-center py-5 text-secondary">
                          <p>Bạn chưa lưu địa chỉ nhận hàng nào.</p>
                          <Button variant="outline-primary" onClick={() => openAddressModal()}>
                            Thêm địa chỉ đầu tiên
                          </Button>
                        </div>
                      ) : (
                        addresses.map((addr) => (
                          <div
                            key={addr.id}
                            className={`address-item-card ${addr.isDefault ? 'is-default' : ''}`}
                          >
                            <div className="d-flex justify-content-between align-items-start gap-2">
                              <div>
                                <div className="d-flex align-items-center gap-2 mb-1">
                                  <strong className="fs-6">{addr.receiverName}</strong>
                                  <span className="text-secondary">|</span>
                                  <span className="text-secondary">{addr.phone}</span>
                                  {addr.isDefault && (
                                    <Badge bg="light" className="badge-default-addr">
                                      Mặc định
                                    </Badge>
                                  )}
                                </div>
                                <div className="text-secondary">{addr.fullAddress}</div>
                              </div>
                              <div className="d-flex gap-2">
                                <Button
                                  variant="outline-secondary"
                                  size="sm"
                                  onClick={() => openAddressModal(addr)}
                                >
                                  Sửa
                                </Button>
                                <Button
                                  variant="outline-danger"
                                  size="sm"
                                  onClick={() => handleDeleteAddress(addr.id)}
                                >
                                  Xóa
                                </Button>
                              </div>
                            </div>
                          </div>
                        ))
                      )}
                    </Card.Body>
                  </Card>
                </Tab.Pane>

                {/* TAB 3: ĐỔI MẬT KHẨU */}
                <Tab.Pane eventKey="password">
                  <Card className="profile-card p-4">
                    <Card.Body>
                      <h2 className="h5 fw-bold mb-3">Đổi mật khẩu</h2>
                      <p className="text-secondary mb-4">
                        Để bảo mật tài khoản, vui lòng không chia sẻ mật khẩu cho người khác.
                      </p>
                      <Form onSubmit={handlePasswordSubmit} style={{ maxWidth: '480px' }}>
                        <Form.Group className="mb-3" controlId="currentPassword">
                          <Form.Label className="fw-semibold">Mật khẩu hiện tại *</Form.Label>
                          <Form.Control
                            type={showPassword ? 'text' : 'password'}
                            required
                            value={passwordForm.currentPassword}
                            onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                            placeholder="Nhập mật khẩu hiện tại"
                          />
                        </Form.Group>
                        <Form.Group className="mb-3" controlId="newPassword">
                          <Form.Label className="fw-semibold">Mật khẩu mới *</Form.Label>
                          <Form.Control
                            type={showPassword ? 'text' : 'password'}
                            required
                            minLength={6}
                            maxLength={50}
                            value={passwordForm.newPassword}
                            onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                            placeholder="Nhập mật khẩu mới (từ 6-50 ký tự)"
                          />
                        </Form.Group>
                        <Form.Group className="mb-3" controlId="confirmPassword">
                          <Form.Label className="fw-semibold">Xác nhận mật khẩu mới *</Form.Label>
                          <Form.Control
                            type={showPassword ? 'text' : 'password'}
                            required
                            value={passwordForm.confirmPassword}
                            onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                            placeholder="Nhập lại mật khẩu mới"
                          />
                        </Form.Group>
                        <Form.Check
                          type="checkbox"
                          id="showPasswordCheck"
                          label="Hiện mật khẩu"
                          checked={showPassword}
                          onChange={(e) => setShowPassword(e.target.checked)}
                          className="mb-4 text-secondary"
                        />
                        <Button type="submit" variant="primary" disabled={saving}>
                          {saving ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
                        </Button>
                      </Form>
                    </Card.Body>
                  </Card>
                </Tab.Pane>
              </Tab.Content>
            </Col>
          </Row>
        </Tab.Container>
      )}

      {/* MODAL THÊM / SỬA ĐỊA CHỈ */}
      <Modal show={showAddressModal} onHide={() => setShowAddressModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">
            {editingAddress ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ nhận hàng'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleAddressSubmit}>
          <Modal.Body>
            <Row className="g-3">
              <Col md={6}>
                <Form.Group controlId="modalReceiverName">
                  <Form.Label className="fw-semibold">Tên người nhận *</Form.Label>
                  <Form.Control
                    type="text"
                    required
                    maxLength={150}
                    value={addressForm.receiverName}
                    onChange={(e) => setAddressForm({ ...addressForm, receiverName: e.target.value })}
                    placeholder="Họ tên người nhận"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group controlId="modalPhone">
                  <Form.Label className="fw-semibold">Số điện thoại *</Form.Label>
                  <Form.Control
                    type="tel"
                    required
                    maxLength={20}
                    value={addressForm.phone}
                    onChange={(e) => setAddressForm({ ...addressForm, phone: e.target.value })}
                    placeholder="Số điện thoại liên hệ"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group controlId="modalProvince">
                  <Form.Label className="fw-semibold">Tỉnh / Thành phố *</Form.Label>
                  <Form.Control
                    type="text"
                    required
                    maxLength={100}
                    value={addressForm.province}
                    onChange={(e) => setAddressForm({ ...addressForm, province: e.target.value })}
                    placeholder="Ví dụ: TP. Hồ Chí Minh"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group controlId="modalDistrict">
                  <Form.Label className="fw-semibold">Quận / Huyện *</Form.Label>
                  <Form.Control
                    type="text"
                    required
                    maxLength={100}
                    value={addressForm.district}
                    onChange={(e) => setAddressForm({ ...addressForm, district: e.target.value })}
                    placeholder="Ví dụ: Quận 1"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group controlId="modalWard">
                  <Form.Label className="fw-semibold">Phường / Xã</Form.Label>
                  <Form.Control
                    type="text"
                    maxLength={100}
                    value={addressForm.ward}
                    onChange={(e) => setAddressForm({ ...addressForm, ward: e.target.value })}
                    placeholder="Ví dụ: Phường Bến Nghé"
                  />
                </Form.Group>
              </Col>
              <Col md={12}>
                <Form.Group controlId="modalStreet">
                  <Form.Label className="fw-semibold">Địa chỉ chi tiết (Số nhà, tên đường) *</Form.Label>
                  <Form.Control
                    type="text"
                    required
                    maxLength={255}
                    value={addressForm.street}
                    onChange={(e) => setAddressForm({ ...addressForm, street: e.target.value })}
                    placeholder="Ví dụ: 123 Lê Lợi"
                  />
                </Form.Group>
              </Col>
              <Col md={12}>
                <Form.Check
                  type="checkbox"
                  id="modalIsDefault"
                  label="Đặt làm địa chỉ nhận hàng mặc định"
                  checked={addressForm.isDefault}
                  onChange={(e) => setAddressForm({ ...addressForm, isDefault: e.target.checked })}
                />
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowAddressModal(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={saving}>
              {saving ? 'Đang lưu...' : 'Lưu địa chỉ'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </Container>
  );
}

