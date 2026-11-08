import BrandLogo from '@/shared/components/BrandLogo';
import HomeButton from '@/shared/components/HomeButton';
import { useRef, useState } from 'react';
import { Alert, Container, Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { Link, useNavigate, useLocation } from 'react-router';
import { useAuth } from '@/app/providers/auth/AuthContext';
import { getRoleHomePath } from '@/shared/auth/rolePaths';
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
    height: '50px',
    border: '1px solid #cbdde9',
    borderRadius: '10px',
    backgroundColor: '#fbfdff',
    fontSize: '14px',
    padding: '13px 15px',
  },
  passwordField: {
    position: 'relative',
  },
  passwordInput: {
    height: '50px',
    border: '1px solid #cbdde9',
    borderRadius: '10px',
    backgroundColor: '#fbfdff',
    fontSize: '14px',
    padding: '13px 76px 13px 15px',
  },
  passwordToggle: {
    position: 'absolute',
    top: '7px',
    right: '7px',
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
    minHeight: '50px',
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

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const submitting = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    submitting.current = true;
    setBusy(true);
    setError('');
    try {
      const result = await authApi.login({ email, password });
      login(result);
      const from = location.state?.from;
      const homePath = getRoleHomePath(result.role);
      const returnPath =
        homePath === '/' &&
        typeof from?.pathname === 'string' &&
        /^\/(cart|order|orders|payment)(\/|$)/.test(from.pathname)
          ? from.pathname + (from.search || '')
          : homePath;
      navigate(returnPath, { replace: true });
    } catch (err) {
      setError(err.message || 'Đăng nhập không thành công.');
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
                <div style={styles.eyebrow}>FOR YOUR NEXT MATCH</div>
                <h2 style={styles.title}>
                  Nâng tầm từng
                  <br />
                  đường cầu.
                </h2>
                <p style={styles.description}>Đăng nhập để tiếp tục hành trình mua sắm và tìm trang bị phù hợp cho bạn.</p>
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
                  <div style={styles.eyebrow}>CHÀO MỪNG BẠN TRỞ LẠI</div>
                  <h1 style={styles.formHeading}>Đăng nhập</h1>
                  <p style={styles.formIntro}>Thật vui khi gặp lại bạn tại Badminton Shop.</p>
                  {location.state?.message && !error && (
                    <Alert variant="info">{location.state.message}</Alert>
                  )}
                  {error && (
                    <Alert variant="danger" role="alert">
                      {error}
                    </Alert>
                  )}
                  <Form onSubmit={submit} aria-busy={busy}>
                    <Form.Group className="mb-4" controlId="loginEmail">
                      <Form.Label className="fw-semibold small text-dark mb-2">Email</Form.Label>
                      <Form.Control
                        type="email"
                        autoComplete="username"
                        required
                        disabled={busy}
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="Nhập địa chỉ email của bạn"
                        style={styles.input}
                      />
                    </Form.Group>
                    <Form.Group className="mb-4" controlId="loginPassword">
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <Form.Label className="fw-semibold small text-dark mb-0">Mật khẩu</Form.Label>
                        <Link to="/forgot-password" className="small text-decoration-none text-primary">
                          Quên mật khẩu?
                        </Link>
                      </div>
                      <div style={styles.passwordField}>
                        <Form.Control
                          type={showPassword ? 'text' : 'password'}
                          autoComplete="current-password"
                          required
                          disabled={busy}
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="Nhập mật khẩu"
                          style={styles.passwordInput}
                        />
                        <Button
                          type="button"
                          variant="outline-secondary"
                          style={styles.passwordToggle}
                          aria-controls="loginPassword"
                          aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                          aria-pressed={showPassword}
                          onClick={() => setShowPassword((value) => !value)}
                        >
                          {showPassword ? 'Ẩn' : 'Hiện'}
                        </Button>
                      </div>
                    </Form.Group>
                    <Button type="submit" className="w-100" style={styles.submitBtn} disabled={busy}>
                      {busy ? (
                        <>
                          <Spinner
                            size="sm"
                            animation="border"
                            aria-hidden="true"
                            className="me-2"
                          />
                          Đang đăng nhập…
                        </>
                      ) : (
                        'Đăng nhập'
                      )}
                    </Button>
                  </Form>
                  <div style={styles.registerLinkBox}>
                    Chưa có tài khoản? <Link to="/register" className="fw-semibold text-decoration-none">Đăng ký ngay</Link>
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
