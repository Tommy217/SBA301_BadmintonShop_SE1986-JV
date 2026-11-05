import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Table } from 'react-bootstrap';
import { useSearchParams } from 'react-router';
import { ManagerPagination, ManagerSearch } from '@/shared/components/ManagerControls';

import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { exportCsv, localDate } from '@/modules/management/utils/managementUtils';
import { invoiceApi } from '@/modules/management/api/management.api';

const invoiceStatus = {
  issued: 'Đang phát hành',
  cancelled: 'Đã hủy',
  replaced: 'Đã thay thế',
};
const invoiceType = { sale: 'Hóa đơn bán', adjustment: 'Hóa đơn điều chỉnh' };
const deliveryStatus = {
  queued: 'Đang xếp hàng',
  sent: 'Đã ghi nhận',
  failed: 'Thất bại',
};
const badgeVariant = {
  issued: 'success',
  cancelled: 'secondary',
  replaced: 'warning',
};
function escapeHtml(value) {
  return String(value ?? '').replace(
    /[&<>"']/g,
    (character) =>
      ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
      })[character],
  );
}

export default function InvoiceManagementPage() {
  const [invoices, setInvoices] = useState([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [draft, setDraft] = useState({
    orderId: '',
    buyerName: '',
    buyerEmail: '',
    buyerAddress: '',
    buyerTaxCode: '',
  });
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(1);
  const [showAdjust, setShowAdjust] = useState(false);
  const [showEmail, setShowEmail] = useState(false);
  const [showReplace, setShowReplace] = useState(false);
  const [adjustment, setAdjustment] = useState({ amount: '', description: '', reason: '' });
  const [recipient, setRecipient] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setInvoices(await invoiceApi.list());
      setError('');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    invoiceApi
      .list({ signal: controller.signal })
      .then((result) => {
        if (!controller.signal.aborted) setInvoices(result);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  async function act(request) {
    setBusy(true);
    try {
      const result = await request();
      try {
        await refresh();
      } catch (e) {
        setError('Thao tác đã lưu, nhưng tải lại thất bại: ' + e.message);
      }
      return result;
    } finally {
      setBusy(false);
    }
  }

  async function createInvoice(event) {
    event.preventDefault();
    setError('');
    try {
      const result = await act(() =>
        invoiceApi.create({ ...draft, orderId: Number(draft.orderId) }),
      );
      setShowCreate(false);
      setParams({ invoice: result.id });
      setNotice('Đã phát hành hóa đơn.');
    } catch (e) {
      setError(e.message);
    }
  }

  const invoiceItems = invoices.flatMap((invoice) => invoice.items);
  const deliveries = invoices.flatMap((invoice) => invoice.deliveries);
  const selected = invoices.find((invoice) => String(invoice.id) === params.get('invoice'));
  const rows = useMemo(
    () =>
      [...invoices]
        .sort((a, b) => new Date(b.issued_at) - new Date(a.issued_at))
        .filter(
          (invoice) =>
            (!status || invoice.status === status) &&
            (!type || invoice.invoice_type === type) &&
            (!from || localDate(invoice.issued_at) >= from) &&
            (!to || localDate(invoice.issued_at) <= to) &&
            [invoice.invoice_no, invoice.buyer_name, invoice.buyer_email]
              .join(' ')
              .toLocaleLowerCase('vi')
              .includes(search.toLocaleLowerCase('vi')),
        ),
    [invoices, status, type, from, to, search],
  );
  const currentPage = Math.min(page, Math.max(1, Math.ceil(rows.length / 10)));

  function selectInvoice(invoice) {
    setParams({ invoice: invoice.id });
    setError('');
  }

  async function submitAdjustment(event) {
    event.preventDefault();
    setError('');
    try {
      await act(() =>
        invoiceApi.adjust({
          invoice_id: selected.id,
          amount: Number(adjustment.amount),
          description: adjustment.description,
          reason: adjustment.reason,
        }),
      );
      setShowAdjust(false);
      setAdjustment({ amount: '', description: '', reason: '' });
      setNotice('Đã lập hóa đơn điều chỉnh.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function submitEmail(event) {
    event.preventDefault();
    setError('');
    try {
      await act(() =>
        invoiceApi.deliver({
          invoice_id: selected.id,
          channel: 'email',
          recipient,
        }),
      );
      setShowEmail(false);
      setNotice('Đã tạo yêu cầu gửi hóa đơn qua email; trạng thái hiện tại là đang xếp hàng.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function replaceInvoice() {
    setError('');
    try {
      await act(() => invoiceApi.replace({ invoice_id: selected.id }));
      setShowReplace(false);
      setNotice('Đã thay thế hóa đơn và phát hành hóa đơn mới.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function cancelInvoice() {
    setError('');
    try {
      await act(() => invoiceApi.cancel({ invoice_id: selected.id }));
      setNotice('Đã hủy hóa đơn.');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function printInvoice() {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      setError('Trình duyệt đã chặn cửa sổ in. Hãy cho phép cửa sổ bật lên rồi thử lại.');
      return;
    }
    setError('');
    try {
      await act(() => invoiceApi.deliver({ invoice_id: selected.id, channel: 'download' }));
      const items = invoiceItems.filter((item) => String(item.invoice_id) === String(selected.id));
      const lines = items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.description)}</td><td>${Number(item.quantity)}</td><td>${formatMoney(item.unit_price)}</td><td>${formatMoney(item.line_total)}</td></tr>`,
        )
        .join('');
      printWindow.document.write(
        `<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${escapeHtml(selected.invoice_no)}</title><style>body{font:14px Arial,sans-serif;max-width:800px;margin:40px auto;color:#173d5b}h1{font-size:24px}table{width:100%;border-collapse:collapse;margin:24px 0}th,td{border:1px solid #ccd8e0;padding:10px;text-align:left}th{background:#f1f6fa}.total{text-align:right;font-size:18px;font-weight:bold}</style></head><body><h1>HÓA ĐƠN BÁN HÀNG</h1><p><strong>${escapeHtml(selected.invoice_no)}</strong><br>Ngày phát hành: ${escapeHtml(formatDateTime(selected.issued_at))}</p><p>Người mua: ${escapeHtml(selected.buyer_name)}<br>Email: ${escapeHtml(selected.buyer_email)}<br>Địa chỉ: ${escapeHtml(selected.buyer_address)}<br>Mã số thuế: ${escapeHtml(selected.buyer_tax_code)}</p><p>Mã đơn hàng: ${escapeHtml(selected.order_code)}</p><table><thead><tr><th>Diễn giải</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>${lines}</tbody></table><p>Tạm tính: ${formatMoney(selected.subtotal)}<br>Giảm giá: ${formatMoney(selected.discount_amount)}<br>Phí giao hàng: ${formatMoney(selected.shipping_fee)}</p><p class="total">Tổng thanh toán: ${formatMoney(selected.total_amount)}</p><script>window.onload=()=>window.print()</script></body></html>`,
      );
      printWindow.document.close();
      setNotice(
        'Đã ghi nhận thao tác tải/in hóa đơn. Chọn “Lưu dưới dạng PDF” trong hộp thoại in nếu cần tệp PDF.',
      );
    } catch (requestError) {
      printWindow.close();
      setError(requestError.message);
    }
  }

  function downloadCsv() {
    exportCsv(
      'hoa-don.csv',
      [
        'Số hóa đơn',
        'Mã đơn',
        'Người mua',
        'Email',
        'Loại',
        'Trạng thái',
        'Ngày phát hành',
        'Tổng tiền',
      ],
      rows.map((invoice) => [
        invoice.invoice_no,
        invoice.order_code,
        invoice.buyer_name,
        invoice.buyer_email,
        invoiceType[invoice.invoice_type],
        invoiceStatus[invoice.status],
        invoice.issued_at,
        invoice.total_amount,
      ]),
    );
  }

  return (
    <section className="manager-page manager-invoices">
      <div className="eyebrow">QUẢN LÝ TÀI CHÍNH</div>
      <div className="manager-title">
        <div>
          <h1>Quản lý hóa đơn</h1>
          <p>Phát hành và quản lý hóa đơn cho đơn hàng hoàn tất, đã thanh toán.</p>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          <Button
            disabled={busy || loading}
            onClick={() => {
              setError('');
              setShowCreate(true);
            }}
          >
            Phát hành hóa đơn
          </Button>
          <Button
            variant="outline-primary"
            disabled={busy || loading}
            onClick={() => refresh().catch((requestError) => setError(requestError.message))}
          >
            Làm mới
          </Button>
          <Button variant="outline-primary" onClick={downloadCsv}>
            Xuất CSV
          </Button>
        </div>
      </div>
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice('')}>
          {notice}
        </Alert>
      )}
      {error && !showCreate && !showAdjust && !showEmail && !showReplace && (
        <Alert variant="danger" dismissible onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {loading && <p role="status">Đang tải hóa đơn…</p>}
      <div className="manager-invoice-stats">
        <div>
          <span>Tổng hóa đơn</span>
          <strong>{invoices.length}</strong>
        </div>
        <div>
          <span>Đang phát hành</span>
          <strong>{invoices.filter((invoice) => invoice.status === 'issued').length}</strong>
        </div>
        <div>
          <span>Đã điều chỉnh</span>
          <strong>
            {invoices.filter((invoice) => invoice.invoice_type === 'adjustment').length}
          </strong>
        </div>
        <div>
          <span>Đơn hoàn tất có hóa đơn</span>
          <strong>
            {
              new Set(
                invoices
                  .filter(
                    (invoice) =>
                      invoice.order_status === 'completed' && invoice.invoice_type === 'sale',
                  )
                  .map((invoice) => invoice.order_id),
              ).size
            }
          </strong>
        </div>
      </div>
      <div className="manager-panel manager-list-panel">
        <div className="manager-filters">
          <ManagerSearch
            placeholder="Tìm số hóa đơn, người mua, email…"
            label="Tìm hóa đơn"
            value={search}
            onChange={(value) => {
              setSearch(value);
              setPage(1);
            }}
          />
          <Form.Select
            aria-label="Lọc trạng thái hóa đơn"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(1);
            }}
          >
            <option value="">Tất cả trạng thái</option>
            {Object.entries(invoiceStatus).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Form.Select>
          <Form.Select
            aria-label="Lọc loại hóa đơn"
            value={type}
            onChange={(event) => {
              setType(event.target.value);
              setPage(1);
            }}
          >
            <option value="">Tất cả loại hóa đơn</option>
            {Object.entries(invoiceType).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Form.Select>
        </div>
        <div className="manager-filters">
          <Form.Group>
            <Form.Label>Từ ngày</Form.Label>
            <Form.Control
              type="date"
              value={from}
              max={to || undefined}
              onChange={(event) => {
                setFrom(event.target.value);
                setPage(1);
              }}
            />
          </Form.Group>
          <Form.Group>
            <Form.Label>Đến ngày</Form.Label>
            <Form.Control
              type="date"
              value={to}
              min={from || undefined}
              onChange={(event) => {
                setTo(event.target.value);
                setPage(1);
              }}
            />
          </Form.Group>
        </div>
        <div className="table-responsive">
          <Table hover className="manager-table align-middle">
            <thead>
              <tr>
                <th>Số hóa đơn</th>
                <th>Người mua</th>
                <th>Loại</th>
                <th>Ngày phát hành</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.slice((currentPage - 1) * 10, currentPage * 10).map((invoice) => (
                <tr key={invoice.id}>
                  <td>
                    <strong>{invoice.invoice_no}</strong>
                    <small className="d-block text-secondary">
                      {invoice.order_code || `Đơn #${invoice.order_id}`}
                    </small>
                  </td>
                  <td>
                    {invoice.buyer_name || '—'}
                    <small className="d-block text-secondary">
                      {invoice.buyer_email || 'Không có email'}
                    </small>
                  </td>
                  <td>{invoiceType[invoice.invoice_type] || invoice.invoice_type}</td>
                  <td>{new Date(invoice.issued_at).toLocaleDateString('vi-VN')}</td>
                  <td>
                    <strong>{formatMoney(invoice.total_amount)}</strong>
                  </td>
                  <td>
                    <Badge bg={badgeVariant[invoice.status] || 'secondary'}>
                      {invoiceStatus[invoice.status] || invoice.status}
                    </Badge>
                  </td>
                  <td>
                    <Button
                      variant="outline-primary"
                      size="sm"
                      onClick={() => selectInvoice(invoice)}
                    >
                      Chi tiết
                    </Button>
                  </td>
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan="7" className="text-center py-5">
                    Chưa có hóa đơn phù hợp. Chọn “Phát hành hóa đơn” cho đơn hàng đã hoàn tất và
                    thanh toán.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </div>
        <ManagerPagination total={rows.length} page={currentPage} onChange={setPage} />
      </div>

      <Modal
        scrollable
        className="manager-modal"
        show={!!selected}
        onHide={() => setParams({})}
        size="lg"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>{selected?.invoice_no}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selected && (
            <>
              {error && (
                <Alert variant="danger" dismissible onClose={() => setError('')}>
                  {error}
                </Alert>
              )}
              <div className="d-flex gap-2 mb-3">
                <Badge bg={badgeVariant[selected.status]}>{invoiceStatus[selected.status]}</Badge>
                <Badge bg="info">{invoiceType[selected.invoice_type]}</Badge>
              </div>
              <div className="manager-invoice-detail">
                <div>
                  <span>Đơn hàng</span>
                  <strong>{selected.order_code || `#${selected.order_id}`}</strong>
                </div>
                <div>
                  <span>Ngày phát hành</span>
                  <strong>{formatDateTime(selected.issued_at)}</strong>
                </div>
                <div>
                  <span>Người mua</span>
                  <strong>{selected.buyer_name || '—'}</strong>
                </div>
                <div>
                  <span>Email</span>
                  <strong>{selected.buyer_email || '—'}</strong>
                </div>
                <div>
                  <span>Địa chỉ</span>
                  <strong>{selected.buyer_address || '—'}</strong>
                </div>
                <div>
                  <span>Mã số thuế</span>
                  <strong>{selected.buyer_tax_code || '—'}</strong>
                </div>
                {selected.reason && (
                  <div>
                    <span>Lý do</span>
                    <strong>{selected.reason}</strong>
                  </div>
                )}
              </div>
              <Table responsive className="manager-table mt-3">
                <thead>
                  <tr>
                    <th>Diễn giải</th>
                    <th>Đơn giá</th>
                    <th>Số lượng</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {invoiceItems
                    .filter((item) => String(item.invoice_id) === String(selected.id))
                    .map((item) => (
                      <tr key={item.id}>
                        <td>{item.description}</td>
                        <td>{formatMoney(item.unit_price)}</td>
                        <td>{item.quantity}</td>
                        <td>{formatMoney(item.line_total)}</td>
                      </tr>
                    ))}
                </tbody>
              </Table>
              <div className="manager-invoice-totals">
                <span>
                  Tạm tính <strong>{formatMoney(selected.subtotal)}</strong>
                </span>
                <span>
                  Giảm giá <strong>{formatMoney(selected.discount_amount)}</strong>
                </span>
                <span>
                  Phí giao hàng <strong>{formatMoney(selected.shipping_fee)}</strong>
                </span>
                <span>
                  Tổng cộng <strong>{formatMoney(selected.total_amount)}</strong>
                </span>
              </div>
              <h2 className="h6 mt-4">Lịch sử gửi hóa đơn</h2>
              {deliveries.filter((delivery) => String(delivery.invoice_id) === String(selected.id))
                .length ? (
                <ul className="manager-invoice-deliveries">
                  {deliveries
                    .filter((delivery) => String(delivery.invoice_id) === String(selected.id))
                    .map((delivery) => (
                      <li key={delivery.id}>
                        {delivery.channel === 'email'
                          ? `Email: ${delivery.recipient}`
                          : 'Tải/in PDF'}{' '}
                        · {deliveryStatus[delivery.status] || delivery.status}
                        {delivery.sent_at && ` · ${formatDateTime(delivery.sent_at)}`}
                      </li>
                    ))}
                </ul>
              ) : (
                <p className="text-secondary small">Chưa có lịch sử gửi hoặc tải hóa đơn.</p>
              )}
              {selected.parent_invoice_id && (
                <p className="small text-secondary">
                  Hóa đơn liên quan: #{selected.parent_invoice_id}
                </p>
              )}
              {selected.status === 'issued' && (
                <div className="d-flex flex-wrap gap-2 mt-4">
                  <Button onClick={printInvoice} disabled={busy || loading}>
                    Tải / In PDF
                  </Button>
                  <Button
                    variant="outline-primary"
                    disabled={busy || loading}
                    onClick={() => {
                      setRecipient(selected.buyer_email || '');
                      setError('');
                      setShowEmail(true);
                    }}
                  >
                    Gửi email
                  </Button>
                  {selected.invoice_type === 'sale' && (
                    <>
                      <Button
                        variant="outline-primary"
                        disabled={busy || loading}
                        onClick={() => {
                          setAdjustment({ amount: '', description: '', reason: '' });
                          setError('');
                          setShowAdjust(true);
                        }}
                      >
                        Lập điều chỉnh
                      </Button>
                      <Button
                        variant="outline-primary"
                        disabled={busy || loading}
                        onClick={() => {
                          setError('');
                          setShowReplace(true);
                        }}
                      >
                        Thay thế
                      </Button>
                    </>
                  )}
                  <Button
                    variant="outline-danger"
                    onClick={cancelInvoice}
                    disabled={busy || loading}
                  >
                    Hủy hóa đơn
                  </Button>
                </div>
              )}
            </>
          )}
        </Modal.Body>
      </Modal>

      <Modal show={showCreate} onHide={() => setShowCreate(false)} centered>
        <Form onSubmit={createInvoice}>
          <Modal.Header closeButton>
            <Modal.Title>Phát hành hóa đơn</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <Form.Group className="mb-3">
              <Form.Label>ID đơn hàng đã hoàn tất và thanh toán</Form.Label>
              <Form.Control
                type="number"
                min="1"
                step="1"
                required
                value={draft.orderId}
                onChange={(e) => setDraft({ ...draft, orderId: e.target.value })}
              />
            </Form.Group>
            {[
              ['buyerName', 'Tên người mua (để trống dùng tên người nhận)', 150],
              ['buyerEmail', 'Email người mua', 190],
              ['buyerAddress', 'Địa chỉ (để trống dùng địa chỉ đơn hàng)', 500],
              ['buyerTaxCode', 'Mã số thuế', 30],
            ].map(([key, label, max]) => (
              <Form.Group className="mb-3" key={key}>
                <Form.Label>{label}</Form.Label>
                <Form.Control
                  type={key === 'buyerEmail' ? 'email' : 'text'}
                  maxLength={max}
                  value={draft[key]}
                  onChange={(e) => setDraft({ ...draft, [key]: e.target.value })}
                />
              </Form.Group>
            ))}
          </Modal.Body>
          <Modal.Footer>
            <Button type="submit" disabled={busy || loading}>
              Phát hành
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showAdjust} onHide={() => setShowAdjust(false)} centered>
        <Form onSubmit={submitAdjustment}>
          <Modal.Header closeButton>
            <Modal.Title>Lập hóa đơn điều chỉnh</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <p className="small text-secondary">
              Nhập số dương để tăng, số âm để giảm giá trị hóa đơn. Hóa đơn điều chỉnh sẽ liên kết
              với hóa đơn gốc.
            </p>
            <Form.Group className="mb-3">
              <Form.Label>Số tiền điều chỉnh (VND)</Form.Label>
              <Form.Control
                type="number"
                step="1"
                required
                value={adjustment.amount}
                onChange={(event) => setAdjustment({ ...adjustment, amount: event.target.value })}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Diễn giải</Form.Label>
              <Form.Control
                maxLength={255}
                value={adjustment.description}
                onChange={(event) =>
                  setAdjustment({ ...adjustment, description: event.target.value })
                }
              />
            </Form.Group>
            <Form.Group>
              <Form.Label>Lý do điều chỉnh</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                maxLength={300}
                required
                value={adjustment.reason}
                onChange={(event) => setAdjustment({ ...adjustment, reason: event.target.value })}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={() => setShowAdjust(false)}>
              Đóng
            </Button>
            <Button type="submit" disabled={busy || loading}>
              Lập điều chỉnh
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showEmail} onHide={() => setShowEmail(false)} centered>
        <Form onSubmit={submitEmail}>
          <Modal.Header closeButton>
            <Modal.Title>Gửi hóa đơn qua email</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <p className="small text-secondary">
              Yêu cầu sẽ được ghi vào lịch sử gửi ở trạng thái xếp hàng.
            </p>
            <Form.Group>
              <Form.Label>Email người nhận</Form.Label>
              <Form.Control
                type="email"
                maxLength={190}
                required
                value={recipient}
                onChange={(event) => setRecipient(event.target.value)}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={() => setShowEmail(false)}>
              Đóng
            </Button>
            <Button type="submit" disabled={busy || loading}>
              Tạo yêu cầu gửi
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showReplace} onHide={() => setShowReplace(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Thay thế hóa đơn?</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {error && <Alert variant="danger">{error}</Alert>}Hóa đơn hiện tại sẽ chuyển sang trạng
          thái “Đã thay thế” và một hóa đơn bán mới sẽ được phát hành cho cùng đơn hàng.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setShowReplace(false)}>
            Đóng
          </Button>
          <Button variant="primary" disabled={busy || loading} onClick={replaceInvoice}>
            Xác nhận thay thế
          </Button>
        </Modal.Footer>
      </Modal>
    </section>
  );
}
