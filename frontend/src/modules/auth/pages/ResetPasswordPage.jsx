import BrandLogo from '@/shared/components/BrandLogo';
import HomeButton from '@/shared/components/HomeButton';
import { useState } from 'react';
import { Alert, Container, Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { Link, useNavigate, useSearchParams } from 'react-router';
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
    fontSize: 'clamp(32px, 3.2vw, 42px)',
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
    right: '8px',
    bottom: '18px',
    textAlign: 'right',
    fontSize: '15px',
    fontWeight: 800,
    color: '#26597f',
    letterSpacing: '2px',
    lineHeight: 1.2,
  },
  welcomeNote: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '14px 18px',
    backgroundColor: 'rgba(255, 255, 255, 0.75)',
    border: '1px solid #c9e4f5',
    borderRadius: '12px',
    fontSize: '13px',
    color: '#1c4a6e',
    fontWeight: 500,
  },
  checkBadge: {
    width: '20px',
    height: '20px',
    borderRadius: '50%',
    backgroundColor: '#0284c7',
    color: '#ffffff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '12px',
    fontWeight: 'bold',
  },
  formCol: {
    backgroundColor: '#ffffff',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
  },
  formPanel: {
    padding: '48px',
  },
  formHeading: {
    fontSize: '26px',
    fontWeight: 800,
    color: '#173d5b',
    letterSpacing: '-0.5px',
    marginBottom: '8px',
  },
  formIntro: {
    color: '#65829f',
    fontSize: '14px',
    marginBottom: '32px',
  },
  passwordField: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
  },
  passwordInput: {
    height: '46px',
    borderRadius: '10px',
    border: '1px solid #c9e4f5',
    backgroundColor: '#f8fcff',
    padding: '0 52px 0 16px',
    fontSize: '14px',
  },
  passwordToggle: {
    position: 'absolute',
    right: '8px',
    height: '32px',
    padding: '0 10px',
    fontSize: '12px',
    borderRadius: '6px',
    lineHeight: '30px',
  },
  submitBtn: {
    height: '46px',
    borderRadius: '10px',
    backgroundColor: '#0284c7',
    borderColor: '#0284c7',
    fontSize: '15px',
    fontWeight: 600,
    boxShadow: '0 4px 12px rgba(2, 132, 199, 0.2)',
  },
  backLinkBox: {
    marginTop: '28px',
    paddingTop: '20px',
    borderTop: '1px solid #eef4f9',
    textAlign: 'center',
    fontSize: '14px',
    color: '#65829f',
  },
};

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!token) {
      setError('Mã liên kết không hợp lệ hoặc bị thiếu. Vui lòng kiểm tra lại liên kết trong email hoặc gửi lại yêu cầu mới.');
      return;
    }

    if (!password || password.length < 6) {
      setError('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không trùng khớp với mật khẩu mới.');
      return;
    }

    setBusy(true);
    try {
      await authApi.resetPassword({ token, newPassword: password });
      navigate('/login', {
        state: { message: 'Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới của bạn.' },
      });
    } catch (err) {
      setError(err.message || 'Không thể đặt lại mật khẩu. Liên kết có thể đã hết hạn hoặc đã được sử dụng.');
    } finally {
      setBusy(false);
    }
  };

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
                <div style={styles.eyebrow}>BẢO MẬT TÀI KHOẢN</div>
                <h2 style={styles.title}>
                  Thiết lập
                  <br />
                  mật khẩu mới.
                </h2>
                <p style={styles.description}>
                  Tạo một mật khẩu mạnh mẽ và an toàn để bảo vệ tài khoản và thông tin mua sắm của bạn.
                </p>
                <div style={styles.illustration} aria-hidden="true">
                  <div style={styles.court} />
                  <img src="/products/racket.svg" alt="" style={styles.racketImg} />
                  <span style={styles.illustrationText}>
                    SAFE
                    <br />
                    & SECURE.
                  </span>
                </div>
                <div style={styles.welcomeNote}>
                  <span style={styles.checkBadge} aria-hidden="true">✓</span> Tối thiểu 6 ký tự để đảm bảo độ bảo mật.
                </div>
              </Col>

              <Col lg={6} style={styles.formCol}>
                <Card.Body style={styles.formPanel}>
                  <div style={styles.eyebrow}>ĐẶT LẠI MẬT KHẨU</div>
                  <h1 style={styles.formHeading}>Mật khẩu mới</h1>
                  <p style={styles.formIntro}>
                    Vui lòng nhập mật khẩu mới và ghi nhớ để sử dụng cho các lần đăng nhập tiếp theo.
                  </p>

                  {error && (
                    <Alert variant="danger" role="alert" dismissible onClose={() => setError('')}>
                      {error}
                    </Alert>
                  )}

                  {!token && (
                    <Alert variant="warning" className="mb-4">
                      Không tìm thấy mã token trong liên kết. Vui lòng truy cập từ liên kết được gửi trong email hoặc{' '}
                      <Link to="/forgot-password" className="fw-semibold">
                        yêu cầu liên kết mới tại đây
                      </Link>
                      .
                    </Alert>
                  )}

                  <Form onSubmit={handleSubmit} aria-busy={busy}>
                    <Form.Group className="mb-3" controlId="resetNewPassword">
                      <Form.Label className="fw-semibold small text-dark mb-2">Mật khẩu mới</Form.Label>
                      <div style={styles.passwordField}>
                        <Form.Control
                          type={showPassword ? 'text' : 'password'}
                          autoComplete="new-password"
                          required
                          disabled={busy || !token}
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="Nhập ít nhất 6 ký tự"
                          style={styles.passwordInput}
                        />
                        <Button
                          type="button"
                          variant="outline-secondary"
                          style={styles.passwordToggle}
                          aria-controls="resetNewPassword"
                          aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                          onClick={() => setShowPassword((v) => !v)}
                        >
                          {showPassword ? 'Ẩn' : 'Hiện'}
                        </Button>
                      </div>
                    </Form.Group>

                    <Form.Group className="mb-4" controlId="resetConfirmPassword">
                      <Form.Label className="fw-semibold small text-dark mb-2">Xác nhận mật khẩu mới</Form.Label>
                      <div style={styles.passwordField}>
                        <Form.Control
                          type={showConfirmPassword ? 'text' : 'password'}
                          autoComplete="new-password"
                          required
                          disabled={busy || !token}
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          placeholder="Nhập lại mật khẩu mới"
                          style={styles.passwordInput}
                        />
                        <Button
                          type="button"
                          variant="outline-secondary"
                          style={styles.passwordToggle}
                          aria-controls="resetConfirmPassword"
                          aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                          onClick={() => setShowConfirmPassword((v) => !v)}
                        >
                          {showConfirmPassword ? 'Ẩn' : 'Hiện'}
                        </Button>
                      </div>
                    </Form.Group>

                    <Button type="submit" className="w-100" style={styles.submitBtn} disabled={busy || !token}>
                      {busy ? (
                        <>
                          <Spinner size="sm" animation="border" aria-hidden="true" className="me-2" />
                          Đang lưu mật khẩu…
                        </>
                      ) : (
                        'Xác nhận đổi mật khẩu'
                      )}
                    </Button>
                  </Form>

                  <div style={styles.backLinkBox}>
                    Đã nhớ lại mật khẩu? <Link to="/login" className="fw-semibold text-decoration-none">Đăng nhập</Link>
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

