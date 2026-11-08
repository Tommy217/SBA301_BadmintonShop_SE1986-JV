import BrandLogo from '@/shared/components/BrandLogo';
import HomeButton from '@/shared/components/HomeButton';
import { useRef, useState } from 'react';
import { Alert, Container, Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import PublicFooter from '@/shared/components/PublicFooter';
import '@/shared/styles/app.css';
import { authApi } from '../api/auth.api';

const styles = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: '#f8fafc',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '16px',
    padding: '20px 32px',
    backgroundColor: '#ffffff',
    borderBottom: '1px solid #e3edf4',
  },
  main: {
    display: 'flex',
    alignItems: 'center',
    flex: 1,
    padding: '48px 16px',
  },
  card: {
    maxWidth: '1000px',
    margin: 'auto',
    border: '1px solid #dceaf4',
    borderRadius: '22px',
    overflow: 'hidden',
    boxShadow: '0 16px 50px rgba(36, 77, 114, 0.08)',
  },
  row: {
    minHeight: '560px',
  },
  welcomeCol: {
    position: 'relative',
    display: 'flex',
    flexDirection: 'column',
    padding: '48px',
    backgroundColor: '#e7f4fd',
    overflow: 'hidden',
  },
  eyebrow: {
    margin: '0 0 16px',
    fontSize: '11px',
    lineHeight: 1.6,
    letterSpacing: '1.8px',
    fontWeight: 700,
    color: '#65829f',
  },
  title: {
    position: 'relative',
    zIndex: 1,
    fontSize: 'clamp(34px, 3.4vw, 44px)',
    fontWeight: 800,
    lineHeight: 1.25,
    letterSpacing: '-1px',
    color: '#173d5b',
    margin: '0 0 18px',
  },
  description: {
    color: '#4c7189',
    fontSize: '14px',
    lineHeight: 1.85,
    maxWidth: '340px',
    margin: 0,
    position: 'relative',
    zIndex: 1,
  },
  illustration: {
    position: 'relative',
    flex: 1,
    minHeight: '190px',
    marginTop: '12px',
  },
  court: {
    position: 'absolute',
    inset: '26px -90px 22px -100px',
    border: '1px solid #b9dbee',
    transform: 'rotate(-22deg)',
  },
  racketImg: {
    position: 'absolute',
    width: '210px',
    height: '235px',
    left: 0,
    top: '-6px',
    transform: 'rotate(28deg)',
  },
  illustrationText: {
    position: 'absolute',
    right: 0,
    bottom: '32px',
    color: '#528aac',
    fontSize: '23px',
    fontWeight: 800,
    lineHeight: 1.25,
    letterSpacing: '2px',
  },
  welcomeNote: {
    position: 'relative',
    zIndex: 1,
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '12px',
    lineHeight: 1.6,
    color: '#3d7292',
    marginTop: '20px',
  },
  checkBadge: {
    display: 'inline-grid',
    placeItems: 'center',
    flexShrink: 0,
    width: '22px',
    height: '22px',
    borderRadius: '50%',
    backgroundColor: '#d3ebfa',
  },
  formCol: {
    display: 'flex',
    alignItems: 'center',
    backgroundColor: '#ffffff',
  },
  formPanel: {
    width: '100%',
    padding: '48px',
  },
  formHeading: {
    fontSize: '32px',
    lineHeight: 1.35,
    fontWeight: 750,
    letterSpacing: '-0.6px',
    color: '#203e56',
    margin: '0 0 10px',
  },
  formIntro: {
    fontSize: '14px',
    color: '#647f92',
    lineHeight: 1.75,
    margin: '0 0 30px',
  },
  input: {
    height: '46px',
    border: '1px solid #cbdde9',
    borderRadius: '10px',
    backgroundColor: '#fbfdff',
    fontSize: '14px',
    padding: '11px 14px',
  },
  passwordField: {
    position: 'relative',
  },
  passwordInput: {
    height: '46px',
    border: '1px solid #cbdde9',
    borderRadius: '10px',
    backgroundColor: '#fbfdff',
    fontSize: '14px',
    padding: '11px 76px 11px 14px',
  },
  passwordToggle: {
    position: 'absolute',
    top: '5px',
    right: '6px',
    height: '36px',
    minWidth: '54px',
    padding: '6px 10px',
    border: 0,
    borderRadius: '7px',
    backgroundColor: 'transparent',
    color: '#426b87',
    fontSize: '13px',
    fontWeight: 500,
    boxShadow: 'none',
  },
  submitBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '48px',
    borderRadius: '10px',
    fontWeight: 600,
    fontSize: '15px',
    marginTop: '6px',
  },
  registerLinkBox: {
    display: 'flex',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: '5px',
    borderTop: '1px solid #e4edf4',
    paddingTop: '24px',
    marginTop: '28px',
    fontSize: '14px',
    lineHeight: 1.7,
    color: '#627d90',
  },
};

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
    <div style={styles.page}>
      <div className="shop-topbar">Đồng hành cùng đam mê cầu lông · Sẵn sàng cho trận đấu mới</div>
      <header style={styles.header}>
        <BrandLogo />
        <HomeButton />
      </header>
      <main style={styles.main}>
        <Container>
          <Card style={styles.card}>
            <Row className="g-0" style={styles.row}>
              <Col lg={6} style={styles.welcomeCol}>
                <div style={styles.eyebrow}>YOUR JOURNEY STARTS HERE</div>
                <h2 style={styles.title}>
                  Khởi đầu mới.
                  <br />
                  Sẵn sàng ra sân.
                </h2>
                <p style={styles.description}>Tạo tài khoản và tìm người bạn đồng hành cho từng đường cầu của bạn.</p>
                <div style={styles.illustration} aria-hidden="true">
                  <div style={styles.court} />
                  <img src="/products/racket.svg" alt="" style={styles.racketImg} />
                  <span style={styles.illustrationText}>
                    PLAY
                    <br />
                    YOUR BEST.
                  </span>
                </div>
                <div style={styles.welcomeNote}>
                  <span style={styles.checkBadge} aria-hidden="true">✓</span> Trang bị tốt hơn. Tự tin trên sân.
                </div>
              </Col>
              <Col lg={6} style={styles.formCol}>
                <Card.Body style={styles.formPanel}>
                  <div style={styles.eyebrow}>CHÀO MỪNG ĐẾN BADMINTON SHOP</div>
                  <h1 style={styles.formHeading}>Tạo tài khoản</h1>
                  <p style={styles.formIntro}>Điền thông tin bên dưới để bắt đầu.</p>

                  {error && (
                    <Alert variant="danger" dismissible onClose={() => setError('')}>
                      {error}
                    </Alert>
                  )}

                  <Form onSubmit={handleSubmit}>
                    <Form.Group className="mb-3" controlId="registerName">
                      <Form.Label className="fw-semibold small text-dark mb-1">Họ và tên</Form.Label>
                      <Form.Control
                        type="text"
                        autoComplete="name"
                        required
                        maxLength={150}
                        placeholder="Nhập họ và tên của bạn"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        disabled={busy}
                        style={styles.input}
                      />
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="registerEmail">
                      <Form.Label className="fw-semibold small text-dark mb-1">Email</Form.Label>
                      <Form.Control
                        type="email"
                        autoComplete="email"
                        required
                        maxLength={190}
                        placeholder="Nhập địa chỉ email của bạn"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        disabled={busy}
                        style={styles.input}
                      />
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="registerPhone">
                      <Form.Label className="fw-semibold small text-dark mb-1">Số điện thoại (tùy chọn)</Form.Label>
                      <Form.Control
                        type="tel"
                        autoComplete="tel"
                        maxLength={20}
                        placeholder="Nhập số điện thoại"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        disabled={busy}
                        style={styles.input}
                      />
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="registerPassword">
                      <Form.Label className="fw-semibold small text-dark mb-1">Mật khẩu</Form.Label>
                      <div style={styles.passwordField}>
                        <Form.Control
                          type={showPassword ? 'text' : 'password'}
                          autoComplete="new-password"
                          required
                          placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          disabled={busy}
                          style={styles.passwordInput}
                        />
                        <Button
                          type="button"
                          variant="outline-secondary"
                          style={styles.passwordToggle}
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
                      <Form.Label className="fw-semibold small text-dark mb-1">Xác nhận mật khẩu</Form.Label>
                      <Form.Control
                        type={showPassword ? 'text' : 'password'}
                        autoComplete="new-password"
                        required
                        placeholder="Nhập lại mật khẩu"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        disabled={busy}
                        style={styles.input}
                      />
                    </Form.Group>
                    <Button variant="primary" type="submit" className="w-100" style={styles.submitBtn} disabled={busy}>
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
                  <div style={styles.registerLinkBox}>
                    Đã có tài khoản? <Link to="/login" className="fw-semibold text-decoration-none">Đăng nhập</Link>
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
