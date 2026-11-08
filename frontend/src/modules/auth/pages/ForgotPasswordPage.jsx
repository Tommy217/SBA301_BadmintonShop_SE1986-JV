import BrandLogo from '@/shared/components/BrandLogo';
import HomeButton from '@/shared/components/HomeButton';
import { useState } from 'react';
import { Alert, Container, Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { Link } from 'react-router';
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
  input: {
    height: '46px',
    borderRadius: '10px',
    border: '1px solid #c9e4f5',
    backgroundColor: '#f8fcff',
    padding: '0 16px',
    fontSize: '14px',
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

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [sentSuccess, setSentSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setError('Vui lòng nhập địa chỉ email của bạn.');
      return;
    }

    setBusy(true);
    try {
      await authApi.forgotPassword({ email: trimmedEmail });
      setSentSuccess(true);
    } catch (err) {
      setError(err.message || 'Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại.');
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
                <div style={styles.eyebrow}>KHÔI PHỤC TÀI KHOẢN</div>
                <h2 style={styles.title}>
                  Lấy lại quyền
                  <br />
                  truy cập.
                </h2>
                <p style={styles.description}>
                  Nhập email đăng ký của bạn. Chúng tôi sẽ gửi một liên kết an toàn để bạn thiết lập lại mật khẩu.
                </p>
                <div style={styles.illustration} aria-hidden="true">
                  <div style={styles.court} />
                  <img src="/products/racket.svg" alt="" style={styles.racketImg} />
                  <span style={styles.illustrationText}>
                    SAFETY
                    <br />
                    & TRUST.
                  </span>
                </div>
                <div style={styles.welcomeNote}>
                  <span style={styles.checkBadge} aria-hidden="true">✓</span> Xác thực an toàn qua hộp thư điện tử.
                </div>
              </Col>

              <Col lg={6} style={styles.formCol}>
                <Card.Body style={styles.formPanel}>
                  <div style={styles.eyebrow}>HỖ TRỢ ĐĂNG NHẬP</div>
                  <h1 style={styles.formHeading}>Quên mật khẩu?</h1>
                  <p style={styles.formIntro}>
                    Đừng lo lắng! Hãy nhập địa chỉ email liên kết với tài khoản của bạn.
                  </p>

                  {error && (
                    <Alert variant="danger" role="alert" dismissible onClose={() => setError('')}>
                      {error}
                    </Alert>
                  )}

                  {sentSuccess ? (
                    <div>
                      <Alert variant="success" className="mb-4">
                        <Alert.Heading className="h6 fw-bold">Yêu cầu đã được tiếp nhận!</Alert.Heading>
                        <p className="mb-2 small">
                          Chúng tôi đã gửi một liên kết đặt lại mật khẩu đến hộp thư <strong>{email}</strong> của bạn.
                        </p>
                        <hr />
                        <p className="mb-0 small text-muted">
                          Liên kết có hiệu lực trong <strong>15 phút</strong>.
                        </p>
                      </Alert>

                      <div className="d-grid gap-2">
                        <Button
                          variant="outline-primary"
                          onClick={() => {
                            setSentSuccess(false);
                            setEmail('');
                          }}
                          style={{ borderRadius: '10px', height: '44px' }}
                        >
                          Gửi lại bằng email khác
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <Form onSubmit={handleSubmit} aria-busy={busy}>
                      <Form.Group className="mb-4" controlId="forgotEmail">
                        <Form.Label className="fw-semibold small text-dark mb-2">Địa chỉ Email</Form.Label>
                        <Form.Control
                          type="email"
                          autoComplete="email"
                          required
                          disabled={busy}
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          placeholder="Nhập email của bạn (vd: ban@example.com)"
                          style={styles.input}
                        />
                      </Form.Group>

                      <Button type="submit" className="w-100" style={styles.submitBtn} disabled={busy}>
                        {busy ? (
                          <>
                            <Spinner size="sm" animation="border" aria-hidden="true" className="me-2" />
                            Đang gửi yêu cầu…
                          </>
                        ) : (
                          'Gửi liên kết khôi phục'
                        )}
                      </Button>
                    </Form>
                  )}

                  <div style={styles.backLinkBox}>
                    Nhớ mật khẩu rồi? <Link to="/login" className="fw-semibold text-decoration-none">Quay lại Đăng nhập</Link>
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

