import { useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap';
import { httpClient } from '@/shared/api/httpClient';

export default function VoucherRecipients({ voucherId }) {
  const [state, setState] = useState({ loading: true, ids: [], accounts: [], error: '' });
  const [search, setSearch] = useState('');
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const [attempt, setAttempt] = useState(0);
  const base = `/vouchers/${voucherId}/recipients`;
  useEffect(() => {
    let active = true;
    Promise.all([httpClient.get(base), httpClient.get(base + '/accounts')])
      .then(([ids, accounts]) => {
        if (active) setState({ ids, accounts, loading: false, error: '' });
      })
      .catch((error) => {
        if (active) setState((s) => ({ ...s, loading: false, error: error.message }));
      });
    return () => {
      active = false;
    };
  }, [base, attempt]);
  async function save() {
    setSaving(true);
    setNotice('');
    try {
      await httpClient.put(base, state.ids);
      setState((s) => ({ ...s, error: '' }));
      setNotice('Đã cập nhật tài khoản được cấp mã.');
    } catch (error) {
      setState((s) => ({ ...s, error: error.message }));
    } finally {
      setSaving(false);
    }
  }
  return (
    <Card className="mb-4">
      <Card.Body>
        <h2 className="h6">Tài khoản được cấp voucher riêng</h2>
        <p className="small text-secondary">
          Chỉ các tài khoản được chọn mới có thể xem và sử dụng mã này.
        </p>
        {state.error && (
          <Alert variant="danger">
            {state.error}{' '}
            <Button size="sm" onClick={() => setAttempt((n) => n + 1)}>
              Tải lại
            </Button>
          </Alert>
        )}
        {notice && <Alert variant="success">{notice}</Alert>}
        {state.loading ? (
          <Spinner size="sm" animation="border" />
        ) : (
          <>
            <Form.Control
              aria-label="Tìm tài khoản nhận voucher"
              placeholder="Tìm tên, email hoặc ID tài khoản"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="mb-3"
            />
            <div style={{ maxHeight: 240, overflowY: 'auto' }}>
              {state.accounts
                .filter((a) =>
                  `${a.id} ${a.full_name} ${a.email}`
                    .toLocaleLowerCase('vi')
                    .includes(search.toLocaleLowerCase('vi')),
                )
                .map((a) => (
                  <Form.Check
                    key={a.id}
                    id={`voucher-recipient-${a.id}`}
                    label={`${a.full_name} · ${a.email} (#${a.id})`}
                    className="mb-2"
                    checked={state.ids.includes(a.id)}
                    disabled={saving}
                    onChange={(e) => {
                      setNotice('');
                      setState((s) => ({
                        ...s,
                        ids: e.target.checked
                          ? [...s.ids, a.id]
                          : s.ids.filter((id) => id !== a.id),
                      }));
                    }}
                  />
                ))}
            </div>
            <div className="d-flex align-items-center justify-content-between mt-3">
              <span className="small text-secondary">Đã chọn {state.ids.length} tài khoản</span>
              <Button onClick={save} disabled={saving || !!state.error}>
                {saving ? 'Đang lưu…' : 'Lưu danh sách cấp mã'}
              </Button>
            </div>
          </>
        )}
      </Card.Body>
    </Card>
  );
}
