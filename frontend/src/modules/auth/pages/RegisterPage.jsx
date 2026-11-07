import BrandLogo from '@/shared/components/BrandLogo';
import HomeButton from '@/shared/components/HomeButton';
import { useRef, useState } from 'react';
import { Alert, Container, Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import PublicFooter from '@/shared/components/PublicFooter';
import '@/shared/styles/app.css';
import '@/modules/auth/auth.css';
import { authApi } from '../api/auth.api';

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const submitting = useRef(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting.current) return;

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    if (password.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự.');
      return;
    }

    submitting.current = true;
    setBusy(true);
    setError('');

    try {
      const result = await authApi.register({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        phone: phone.trim() || undefined,
      });

      login(result);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || 'Đăng ký không thành công. Vui lòng thử lại.');
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <div className="shop-app login-page register-page">
      <div className="shop-topbar">Đồng hành cùng đam mê cầu lông · Sẵn sàng cho trận đấu mới</div>
      <header className="login-header">
        <BrandLogo />
        <HomeButton />
      </header>
      <main className="login-main">
        <Container>
          <Card className="login-card">
            <Row className="g-0">
              <Col lg={6} className="login-welcome">
                <div className="eyebrow">YOUR JOURNEY STARTS HERE</div>
                <h2>
                  Khởi đầu mới.
                  <br />
                  Sẵn sàng ra sân.
                </h2>
                <p>Tạo tài khoản và tìm người bạn đồng hành cho từng đường cầu của bạn.</p>
                <div className="login-illustration" aria-hidden="true">
                  <div className="login-court" />
                  <img src="/products/racket.svg" alt="" />
                  <span>
                    PLAY
                    <br />
                    YOUR BEST.
                  </span>
                </div>
                <div className="login-welcome-note">
                  <span aria-hidden="true">✓</span> Trang bị tốt hơn. Tự tin trên sân.
                </div>
              </Col>
              <Col lg={6} className="login-form-column">
                <Card.Body className="login-form-panel">
                  <div className="eyebrow">CHÀO MỪNG ĐẾN BADMINTON SHOP</div>
                  <h1>Tạo tài khoản</h1>
                  <p className="login-intro">Điền thông tin bên dưới để bắt đầu.</p>

                  {error && (
                    <Alert variant="danger" dismissible onClose={() => setError('')}>
                      {error}
                    </Alert>
                  )}

                  <Form onSubmit={handleSubmit}>
                    <Form.Group className="mb-3" controlId="registerName">
                      <Form.Label>Họ và tên</Form.Label>
                      <Form.Control
                        type="text"
                        autoComplete="name"
                        required
                        maxLength={150}
                        placeholder="Nhập họ và tên của bạn"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        disabled={busy}
                      />
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="registerEmail">
                      <Form.Label>Email</Form.Label>
                      <Form.Control
                        type="email"
                        autoComplete="email"
                        required
                        maxLength={190}
                        placeholder="Nhập địa chỉ email của bạn"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        disabled={busy}
                      />
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="registerPhone">
                      <Form.Label>Số điện thoại (tùy chọn)</Form.Label>
                      <Form.Control
                        type="tel"
                        autoComplete="tel"
                        maxLength={20}
                        placeholder="Nhập số điện thoại"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        disabled={busy}
                      />
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="registerPassword">
                      <Form.Label>Mật khẩu</Form.Label>
                      <div className="login-password-field">
                        <Form.Control
                          type={showPassword ? 'text' : 'password'}
                          autoComplete="new-password"
                          required
                          placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          disabled={busy}
                        />
                        <Button
                          type="button"
                          variant="outline-secondary"
                          className="login-password-toggle"
                          aria-controls="registerPassword registerConfirmPassword"
                          aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                          aria-pressed={showPassword}
                          onClick={() => setShowPassword((value) => !value)}
                          disabled={busy}
                        >
                          {showPassword ? 'Ẩn' : 'Hiện'}
                        </Button>
                      </div>
                    </Form.Group>
                    <Form.Group className="mb-4" controlId="registerConfirmPassword">
                      <Form.Label>Xác nhận mật khẩu</Form.Label>
                      <Form.Control
                        type={showPassword ? 'text' : 'password'}
                        autoComplete="new-password"
                        required
                        placeholder="Nhập lại mật khẩu"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        disabled={busy}
                      />
                    </Form.Group>
                    <Button variant="primary" type="submit" className="w-100 login-submit" disabled={busy}>
                      {busy ? (
                        <>
                          <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" className="me-2" />
                          Đang tạo tài khoản...
                        </>
                      ) : (
                        'Đăng ký'
                      )}
                    </Button>
                  </Form>
                  <div className="login-register">
                    Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
                  </div>
                </Card.Body>
              </Col>
            </Row>
          </Card>
        </Container>
      </main>
      <PublicFooter />
    </div>
  );
}
