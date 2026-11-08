import { useEffect, useState } from 'react';
import {
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
import Icon from '@/shared/components/Icon';

const styles = {
  page: {
    width: '100%',
    minWidth: 0,
    margin: 0,
    padding: '0 0 32px',
    color: '#293e53',
  },
  heading: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '24px',
    marginBottom: '26px',
  },
  eyebrow: {
    margin: '0 0 8px',
    color: '#65829f',
    fontSize: '11px',
    fontWeight: 700,
    letterSpacing: '1.8px',
  },
  title: {
    margin: '0 0 9px',
    color: '#24384d',
    fontSize: '30px',
    fontWeight: 750,
    letterSpacing: '-0.8px',
  },
  security: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    color: '#618775',
    backgroundColor: '#edf6f1',
    border: '1px solid #deebe3',
    padding: '10px 14px',
    borderRadius: '20px',
    fontSize: '12px',
    whiteSpace: 'nowrap',
  },
  card: {
    border: '1px solid #e1e8f0',
    borderRadius: '16px',
    boxShadow: '0 4px 20px rgba(41, 75, 112, 0.06)',
    backgroundColor: '#ffffff',
    overflow: 'hidden',
  },
  sidebar: {
    padding: '20px 12px 12px',
  },
  identity: {
    padding: '8px 8px 22px',
    marginBottom: '10px',
    textAlign: 'center',
    borderBottom: '1px solid #edf1f5',
  },
  avatarPreview: {
    position: 'relative',
    display: 'grid',
    placeItems: 'center',
    width: '72px',
    height: '72px',
    margin: '0 auto 14px',
    backgroundColor: '#edf4fc',
    border: '4px solid #f6f9fd',
    outline: '1px solid #e4ecf5',
    borderRadius: '50%',
    fontSize: '28px',
    fontWeight: 700,
    color: '#376da9',
    overflow: 'hidden',
  },
  avatarImg: {
    position: 'absolute',
    inset: 0,
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  navPills: {
    gap: '5px',
  },
  getNavLinkStyle: (isActive) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '13px 12px',
    borderRadius: '9px',
    color: isActive ? '#376da9' : '#708295',
    backgroundColor: isActive ? '#edf4ff' : 'transparent',
    borderColor: isActive ? '#e0ebfa' : 'transparent',
    fontSize: '13px',
    fontWeight: 600,
    border: '1px solid',
    cursor: 'pointer',
    textDecoration: 'none',
  }),
  addressBadgeCount: {
    marginLeft: 'auto',
    padding: '2px 7px',
    borderRadius: '6px',
    fontSize: '11px',
    backgroundColor: '#ffffff',
    color: '#758ba1',
  },
  contentBody: {
    padding: '28px',
  },
  formFooter: {
    display: 'flex',
    justifyContent: 'flex-end',
    paddingTop: '22px',
    marginTop: '26px',
    borderTop: '1px solid #edf1f5',
  },
  addressItemCard: (isDefault) => ({
    padding: '20px',
    border: '1px solid',
    borderColor: isDefault ? '#bed2ea' : '#e4ebf3',
    borderRadius: '12px',
    backgroundColor: isDefault ? '#f7faff' : '#ffffff',
    marginBottom: '14px',
  }),
  defaultBadge: {
    color: '#3b78a7',
    backgroundColor: '#e8f1fb',
    padding: '5px 8px',
    fontSize: '11px',
    fontWeight: 600,
    borderRadius: '5px',
  },
  emptyAddress: {
    display: 'grid',
    justifyItems: 'center',
    padding: '42px 20px',
    border: '1px dashed #dce5ef',
    backgroundColor: '#f9fbfe',
    borderRadius: '12px',
    color: '#8ca2b8',
    textAlign: 'center',
  },
  passwordForm: {
    maxWidth: '560px',
    paddingTop: '4px',
  },
};

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

  useEffect(() => {
    let active = true;
    const loadProfileData = async () => {
      try {
        setLoading(true);
        const [profile, addressList] = await Promise.all([
          profileApi.getProfile(),
          profileApi.getAddresses(),
        ]);

        if (active && profile) {
          setProfileForm({
            fullName: profile.fullName || '',
            email: profile.email || '',
            phone: profile.phone || '',
            gender: profile.gender || 'male',
            birthDate: profile.birthDate || '',
            avatarUrl: profile.avatarUrl || '',
          });
        }
        if (active && addressList) {
          setAddresses(addressList);
        }
      } catch (err) {
        if (active) {
          setFeedback({
            type: 'danger',
            message: err.message || 'Không thể tải thông tin hồ sơ.',
          });
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    loadProfileData();
    return () => {
      active = false;
    };
  }, []);

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFeedback({ type: '', message: '' });

    try {
      const updated = await profileApi.updateProfile({
        fullName: profileForm.fullName.trim(),
        phone: profileForm.phone.trim(),
        gender: profileForm.gender,
        birthDate: profileForm.birthDate || null,
        avatarUrl: profileForm.avatarUrl.trim() || null,
      });

      if (user) {
        login({ ...user, ...updated });
      }

      setFeedback({ type: 'success', message: 'Cập nhật hồ sơ thành công!' });
    } catch (err) {
      setFeedback({ type: 'danger', message: err.message || 'Cập nhật thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setFeedback({ type: 'danger', message: 'Mật khẩu mới và xác nhận mật khẩu không khớp.' });
      return;
    }

    setSaving(true);
    setFeedback({ type: '', message: '' });

    try {
      await profileApi.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });

      setFeedback({ type: 'success', message: 'Đổi mật khẩu thành công!' });
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setFeedback({ type: 'danger', message: err.message || 'Đổi mật khẩu thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  const openAddressModal = (address = null) => {
    setEditingAddress(address);
    if (address) {
      setAddressForm({
        receiverName: address.receiverName || '',
        phone: address.phone || '',
        province: address.province || '',
        district: address.district || '',
        ward: address.ward || '',
        street: address.street || '',
        isDefault: Boolean(address.isDefault),
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
    <section style={styles.page}>
      <header style={styles.heading}>
        <div>
          <p style={styles.eyebrow}>TÀI KHOẢN CỦA BẠN</p>
          <h1 style={styles.title}>Hồ sơ cá nhân</h1>
          <p className="text-secondary small mb-0">Quản lý thông tin, địa chỉ nhận hàng và bảo mật tài khoản.</p>
        </div>
        <span style={styles.security}>
          <Icon name="shield" size={18} /> Thông tin được bảo mật
        </span>
      </header>
      {feedback.message && !showAddressModal && (
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
        <Tab.Container
          activeKey={activeTab}
          onSelect={(k) => {
            setActiveTab(k);
            setFeedback({ type: '', message: '' });
          }}
        >
          <Row className="g-4">
            <Col lg={3}>
              <Card style={{ ...styles.card, ...styles.sidebar }}>
                <div style={styles.identity}>
                  <div style={styles.avatarPreview}>
                    <span>
                      {(profileForm.fullName || profileForm.email || 'K')
                        .trim()
                        .charAt(0)
                        .toUpperCase()}
                    </span>
                    {profileForm.avatarUrl && (
                      <img
                        src={profileForm.avatarUrl}
                        alt="Ảnh đại diện"
                        style={styles.avatarImg}
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                        }}
                      />
                    )}
                  </div>
                  <h2 className="h6 fw-bold mb-1 text-truncate">
                    {profileForm.fullName || 'Khách hàng'}
                  </h2>
                  <small className="text-secondary text-truncate d-block">
                    {profileForm.email}
                  </small>
                </div>
                <Nav variant="pills" className="flex-column" style={styles.navPills}>
                  <Nav.Item>
                    <Nav.Link
                      eventKey="profile"
                      style={styles.getNavLinkStyle(activeTab === 'profile')}
                    >
                      <Icon name="receipt" size={18} />
                      <span>Thông tin tài khoản</span>
                    </Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link
                      eventKey="addresses"
                      style={styles.getNavLinkStyle(activeTab === 'addresses')}
                    >
                      <Icon name="pin" size={18} />
                      <span>Sổ địa chỉ</span>
                      <span style={styles.addressBadgeCount}>{addresses.length}</span>
                    </Nav.Link>
                  </Nav.Item>
                  <Nav.Item>
                    <Nav.Link
                      eventKey="password"
                      style={styles.getNavLinkStyle(activeTab === 'password')}
                    >
                      <Icon name="shield" size={18} />
                      <span>Đổi mật khẩu</span>
                    </Nav.Link>
                  </Nav.Item>
                </Nav>
              </Card>
            </Col>

            <Col lg={9}>
              <Tab.Content>
                {/* TAB 1: THÔNG TIN TÀI KHOẢN */}
                <Tab.Pane eventKey="profile">
                  <Card style={styles.card}>
                    <Card.Body style={styles.contentBody}>
                      <h2 className="h5 fw-bold mb-2">Thông tin tài khoản</h2>
                      <p className="text-secondary small mb-4">
                        Quản lý và cập nhật thông tin cá nhân của bạn.
                      </p>
                      <Form onSubmit={handleProfileSubmit}>
                        <Row className="g-3">
                          <Col md={6}>
                            <Form.Group controlId="profileEmail">
                              <Form.Label className="fw-semibold small text-dark mb-1">Email</Form.Label>
                              <Form.Control
                                type="email"
                                value={profileForm.email}
                                disabled
                                className="bg-light"
                              />
                              <Form.Text className="text-muted small">
                                Email đăng nhập không thể thay đổi.
                              </Form.Text>
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileFullName">
                              <Form.Label className="fw-semibold small text-dark mb-1">Họ và tên *</Form.Label>
                              <Form.Control
                                type="text"
                                required
                                maxLength={150}
                                value={profileForm.fullName}
                                onChange={(e) =>
                                  setProfileForm({ ...profileForm, fullName: e.target.value })
                                }
                                placeholder="Nhập họ và tên"
                              />
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profilePhone">
                              <Form.Label className="fw-semibold small text-dark mb-1">Số điện thoại</Form.Label>
                              <Form.Control
                                type="tel"
                                maxLength={20}
                                value={profileForm.phone}
                                onChange={(e) =>
                                  setProfileForm({ ...profileForm, phone: e.target.value })
                                }
                                placeholder="Ví dụ: 0912345678"
                              />
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileBirthDate">
                              <Form.Label className="fw-semibold small text-dark mb-1">Ngày sinh</Form.Label>
                              <Form.Control
                                type="date"
                                value={profileForm.birthDate}
                                onChange={(e) =>
                                  setProfileForm({ ...profileForm, birthDate: e.target.value })
                                }
                              />
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileGender">
                              <Form.Label className="fw-semibold small text-dark mb-1">Giới tính</Form.Label>
                              <Form.Select
                                value={profileForm.gender}
                                onChange={(e) =>
                                  setProfileForm({ ...profileForm, gender: e.target.value })
                                }
                              >
                                <option value="male">Nam</option>
                                <option value="female">Nữ</option>
                                <option value="other">Khác</option>
                              </Form.Select>
                            </Form.Group>
                          </Col>
                          <Col md={6}>
                            <Form.Group controlId="profileAvatarUrl">
                              <Form.Label className="fw-semibold small text-dark mb-1">
                                Đường dẫn ảnh đại diện (URL)
                              </Form.Label>
                              <Form.Control
                                type="url"
                                maxLength={500}
                                value={profileForm.avatarUrl}
                                onChange={(e) =>
                                  setProfileForm({ ...profileForm, avatarUrl: e.target.value })
                                }
                                placeholder="https://example.com/avatar.jpg"
                              />
                            </Form.Group>
                          </Col>
                        </Row>
                        <div style={styles.formFooter}>
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
                  <Card style={styles.card}>
                    <Card.Body style={styles.contentBody}>
                      <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
                        <div>
                          <h2 className="h5 fw-bold mb-1">Sổ địa chỉ nhận hàng</h2>
                          <p className="text-secondary small mb-0">Quản lý địa chỉ giao nhận của bạn.</p>
                        </div>
                        <Button variant="primary" onClick={() => openAddressModal()}>
                          + Thêm địa chỉ mới
                        </Button>
                      </div>

                      {addresses.length === 0 ? (
                        <div style={styles.emptyAddress}>
                          <Icon name="pin" size={32} />
                          <h3 className="h6 fw-bold mt-3 mb-1">Chưa có địa chỉ nhận hàng</h3>
                          <p className="text-secondary small mb-3">Lưu địa chỉ để sử dụng cho những lần mua sắm tiếp theo.</p>
                          <Button variant="outline-primary" size="sm" onClick={() => openAddressModal()}>
                            Thêm địa chỉ đầu tiên
                          </Button>
                        </div>
                      ) : (
                        addresses.map((addr) => (
                          <div
                            key={addr.id}
                            style={styles.addressItemCard(addr.isDefault)}
                          >
                            <div className="d-flex justify-content-between align-items-start gap-3">
                              <div className="min-w-0">
                                <div className="d-flex align-items-center flex-wrap gap-2 mb-2 small">
                                  <strong className="fs-6">{addr.receiverName}</strong>
                                  <span className="text-secondary">|</span>
                                  <span className="text-secondary">{addr.phone}</span>
                                  {addr.isDefault && (
                                    <Badge style={styles.defaultBadge}>
                                      Mặc định
                                    </Badge>
                                  )}
                                </div>
                                <div className="text-secondary small">{addr.fullAddress}</div>
                              </div>
                              <div className="d-flex gap-2 flex-shrink-0">
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
                  <Card style={styles.card}>
                    <Card.Body style={styles.contentBody}>
                      <h2 className="h5 fw-bold mb-2">Đổi mật khẩu</h2>
                      <p className="text-secondary small mb-4">
                        Để bảo mật tài khoản, vui lòng không chia sẻ mật khẩu cho người khác.
                      </p>
                      <Form onSubmit={handlePasswordSubmit} style={styles.passwordForm}>
                        <Form.Group className="mb-3" controlId="currentPassword">
                          <Form.Label className="fw-semibold small text-dark mb-1">Mật khẩu hiện tại *</Form.Label>
                          <Form.Control
                            type={showPassword ? 'text' : 'password'}
                            required
                            value={passwordForm.currentPassword}
                            onChange={(e) =>
                              setPasswordForm({ ...passwordForm, currentPassword: e.target.value })
                            }
                            placeholder="Nhập mật khẩu hiện tại"
                          />
                        </Form.Group>
                        <Form.Group className="mb-3" controlId="newPassword">
                          <Form.Label className="fw-semibold small text-dark mb-1">Mật khẩu mới *</Form.Label>
                          <Form.Control
                            type={showPassword ? 'text' : 'password'}
                            required
                            minLength={6}
                            maxLength={50}
                            value={passwordForm.newPassword}
                            onChange={(e) =>
                              setPasswordForm({ ...passwordForm, newPassword: e.target.value })
                            }
                            placeholder="Nhập mật khẩu mới (từ 6-50 ký tự)"
                          />
                        </Form.Group>
                        <Form.Group className="mb-3" controlId="confirmPassword">
                          <Form.Label className="fw-semibold small text-dark mb-1">Xác nhận mật khẩu mới *</Form.Label>
                          <Form.Control
                            type={showPassword ? 'text' : 'password'}
                            required
                            value={passwordForm.confirmPassword}
                            onChange={(e) =>
                              setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })
                            }
                            placeholder="Nhập lại mật khẩu mới"
                          />
                        </Form.Group>
                        <Form.Check
                          type="checkbox"
                          id="showPasswordCheck"
                          label="Hiện mật khẩu"
                          checked={showPassword}
                          onChange={(e) => setShowPassword(e.target.checked)}
                          className="mb-4 text-secondary small"
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
      <Modal
        show={showAddressModal}
        onHide={() => setShowAddressModal(false)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">
            {editingAddress ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ nhận hàng'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleAddressSubmit}>
          <Modal.Body>
            {feedback.message && feedback.type === 'danger' && (
              <Alert variant="danger">{feedback.message}</Alert>
            )}
            <Row className="g-3">
              <Col md={6}>
                <Form.Group controlId="modalReceiverName">
                  <Form.Label className="fw-semibold small text-dark mb-1">Tên người nhận *</Form.Label>
                  <Form.Control
                    type="text"
                    required
                    maxLength={150}
                    value={addressForm.receiverName}
                    onChange={(e) =>
                      setAddressForm({ ...addressForm, receiverName: e.target.value })
                    }
                    placeholder="Họ tên người nhận"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group controlId="modalPhone">
                  <Form.Label className="fw-semibold small text-dark mb-1">Số điện thoại *</Form.Label>
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
                  <Form.Label className="fw-semibold small text-dark mb-1">Tỉnh / Thành phố *</Form.Label>
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
                  <Form.Label className="fw-semibold small text-dark mb-1">Quận / Huyện *</Form.Label>
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
                  <Form.Label className="fw-semibold small text-dark mb-1">Phường / Xã</Form.Label>
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
                  <Form.Label className="fw-semibold small text-dark mb-1">
                    Địa chỉ chi tiết (Số nhà, tên đường) *
                  </Form.Label>
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
                  className="small text-secondary"
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
    </section>
  );
}
