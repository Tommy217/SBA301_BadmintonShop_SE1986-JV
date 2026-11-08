import { ManagerSearch, ManagerPagination } from '@/shared/components/ManagerControls';
import { useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Table } from 'react-bootstrap';
import { Link, useSearchParams } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { statuses, payments } from '@/modules/management/constants';
import { exportCsv, localDate, orderTransitions } from '@/modules/management/utils/managementUtils';
import { managerOrderApi } from '@/modules/management/api/management.api';
import CreateShipmentAction from '@/modules/delivery/components/CreateShipmentAction';

export default function ManagerOrdersPage() {
  const { data, act, busy, refresh } = useShop();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [payment, setPayment] = useState('');
  const [channel, setChannel] = useState('');
  const [from, setFrom] = useState(''),
    [to, setTo] = useState('');
  const [page, setPage] = useState(1);
  const [edit, setEdit] = useState(null);
  const [notice, setNotice] = useState(''),
    [error, setError] = useState('');
  const status = params.get('status') || '';
  const selected = data.orders.find((o) => String(o.id) === params.get('order'));
  const rows = [...data.orders]
    .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
    .filter(
      (o) =>
        (!status || o.status === status) &&
        (!payment || (payment === 'pending_refund' ? o.status === 'cancelled' && o.payment_status === 'paid' : o.payment_status === payment)) &&
        (!channel || o.channel === channel) &&
        (!from || localDate(o.created_at) >= from) &&
        (!to || localDate(o.created_at) <= to) &&
        [o.order_code, o.receiver_name, o.receiver_phone]
          .join(' ')
          .toLocaleLowerCase('vi')
          .includes(search.toLocaleLowerCase('vi')),
    );
  const pages = Math.max(1, Math.ceil(rows.length / 10)),
    current = Math.min(page, pages);
  function parameter(key, value) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next);
  }
  function download() {
    exportCsv(
      'don-hang.csv',
      ['Mã đơn', 'Khách hàng', 'Điện thoại', 'Ngày đặt', 'Trạng thái', 'Thanh toán', 'Tổng tiền'],
      rows.map((o) => [
        o.order_code,
        o.receiver_name,
        o.receiver_phone,
        o.created_at,
        statuses[o.status],
        payments[o.payment_status],
        o.total_amount,
      ]),
    );
  }
  async function update(e) {
    e.preventDefault();
    setError('');
    try {
      await act(() =>
        managerOrderApi.updateOrder({
          id: selected.id,
          expected_status: edit.originalStatus,
          expected_payment_status: edit.originalPayment,
          status: edit.status,
          payment_status: edit.payment,
          note: edit.note,
        }),
      );
      setEdit(null);
      setNotice('Đã cập nhật đơn hàng, tồn kho và lịch sử xử lý.');
    } catch (e) {
      setError(e.message);
    }
  }
  function startEdit() {
    setError('');
    setEdit({
      status: selected.status,
      payment: selected.payment_status,
      note: '',
      originalStatus: selected.status,
      originalPayment: selected.payment_status,
    });
  }
  if (!data.ordersAvailable) {
    return (
      <section className="manager-page">
        <h1>Quản lý đơn hàng</h1>
        <Alert variant="warning">Chưa tải được dữ liệu đơn hàng. Vui lòng thử lại sau.</Alert>
        <Button disabled={busy} onClick={() => refresh().catch(() => {})}>
          Thử lại
        </Button>
      </section>
    );
  }
  return (
    <section className="manager-page">
      <div className="eyebrow">KHÁCH HÀNG & ĐƠN HÀNG</div>
      <div className="manager-title">
        <div>
          <h1>Quản lý đơn hàng</h1>
          <p>Xác nhận đơn, theo dõi thanh toán và lịch sử xử lý của khách.</p>
        </div>
        <div className="d-flex gap-2">
          <Button
            variant="outline-primary"
            disabled={busy}
            onClick={() => refresh().catch(() => {})}
          >
            Làm mới
          </Button>
          <Button onClick={download}>Xuất CSV</Button>
        </div>
      </div>
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice('')}>
          {notice}
        </Alert>
      )}
      <div className="manager-panel manager-list-panel">
        <div className="manager-filters">
          <ManagerSearch
            placeholder="Tìm mã đơn, khách hàng, điện thoại…"
            label="Tìm đơn hàng"
            value={search}
            onChange={(value) => {
              setSearch(value);
              setPage(1);
            }}
          />
          <Form.Select
            aria-label="Trạng thái đơn hàng"
            value={status}
            onChange={(e) => {
              parameter('status', e.target.value);
              setPage(1);
            }}
          >
            <option value="">Tất cả trạng thái</option>
            {Object.entries(statuses).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </Form.Select>
          <Form.Select
            aria-label="Thanh toán"
            value={payment}
            onChange={(e) => {
              setPayment(e.target.value);
              setPage(1);
            }}
          >
            <option value="">Tất cả thanh toán</option>
            <option value="pending_refund">Đơn đã hủy · Chờ hoàn tiền</option>
            {Object.entries(payments).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
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
              onChange={(e) => {
                setFrom(e.target.value);
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
              onChange={(e) => {
                setTo(e.target.value);
                setPage(1);
              }}
            />
          </Form.Group>
          <Form.Group>
            <Form.Label>Kênh bán</Form.Label>
            <Form.Select
              value={channel}
              onChange={(e) => {
                setChannel(e.target.value);
                setPage(1);
              }}
            >
              <option value="">Tất cả kênh</option>
              <option value="online">Trực tuyến</option>
              <option value="in_store">Tại cửa hàng</option>
            </Form.Select>
          </Form.Group>
        </div>
        <div className="table-responsive">
          <Table hover className="manager-table align-middle">
            <thead>
              <tr>
                <th>Đơn hàng</th>
                <th>Khách hàng</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
                <th className="manager-action-cell">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.slice((current - 1) * 10, current * 10).map((o) => (
                <tr key={o.id}>
                  <td>
                    <strong>{o.order_code}</strong>
                    <small className="d-block text-secondary">
                      {new Date(o.created_at).toLocaleDateString('vi-VN')} ·{' '}
                      {o.channel === 'in_store' ? 'Tại cửa hàng' : 'Trực tuyến'}
                    </small>
                  </td>
                  <td>
                    {o.receiver_name || 'Khách tại quầy'}
                    <small className="d-block text-secondary">{o.receiver_phone}</small>
                  </td>
                  <td>
                    <strong>{formatMoney(o.total_amount)}</strong>
                    <small className="d-block text-secondary">
                      {o.status === 'cancelled' && o.payment_status === 'paid' ? 'Chờ hoàn tiền' : payments[o.payment_status] || o.payment_status}
                    </small>
                  </td>
                  <td>
                    <Badge
                      bg={
                        o.status === 'completed'
                          ? 'success'
                          : o.status === 'cancelled'
                            ? 'secondary'
                            : 'info'
                      }
                    >
                      {statuses[o.status] || o.status}
                    </Badge>
                  </td>
                  <td className="manager-action-cell">
                    <Button
                      variant="outline-primary"
                      size="sm"
                      onClick={() => {
                        setEdit(null);
                        setError('');
                        parameter('order', o.id);
                      }}
                    >
                      Xem & xử lý
                    </Button>
                  </td>
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan="5" className="text-center py-5">
                    Không tìm thấy đơn hàng. Thử thay đổi bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </div>
        <ManagerPagination total={rows.length} page={current} onChange={setPage} />
      </div>
      <Modal
        scrollable
        className="manager-modal manager-order-modal"
        show={!!selected}
        onHide={() => {
          parameter('order', '');
          setEdit(null);
        }}
        size="lg"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>{selected?.order_code}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selected && (
            <>
              <div className="d-flex gap-2 mb-4">
                <Badge bg="info">{statuses[selected.status]}</Badge>
                <Badge bg={selected.payment_status === 'paid' ? 'success' : 'secondary'}>
                  {payments[selected.payment_status]}
                </Badge>
              </div>
              <div className="manager-detail-block">
                <h2 className="h6">Thông tin nhận hàng</h2>
                <p>
                  {selected.receiver_name} · {selected.receiver_phone}
                  <br />
                  {selected.shipping_address}
                </p>
                <p>
                  Ghi chú: {selected.note || 'Không có'}
                  <br />
                  Mã voucher: {selected.voucher_code || 'Không sử dụng'}
                </p>
              </div>
              <Table responsive className="manager-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Đơn giá</th>
                    <th>SL</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {data.order_items
                    .filter((i) => i.order_id === selected.id)
                    .map((i) => (
                      <tr key={i.id}>
                        <td>
                          {i.product_name}
                          <small className="d-block">{i.variant_info}</small>
                        </td>
                        <td>{formatMoney(i.unit_price)}</td>
                        <td>{i.quantity}</td>
                        <td>{formatMoney(i.line_total)}</td>
                      </tr>
                    ))}
                </tbody>
              </Table>
              <p>
                Tạm tính: {formatMoney(selected.subtotal)} · Giảm giá:{' '}
                {formatMoney(selected.discount_amount || 0)} · Phí giao:{' '}
                {formatMoney(selected.shipping_fee || 0)}
              </p>
              <strong>Tổng cộng: {formatMoney(selected.total_amount)}</strong>
              <div className="manager-order-actions mt-4">
                <div className="d-flex gap-2 flex-wrap">
                  <Button disabled={busy} onClick={startEdit}>
                    Cập nhật đơn hàng
                  </Button>
                  <Button as={Link} to="/management/deliveries" variant="outline-primary">
                    Quản lý giao hàng
                  </Button>
                  <CreateShipmentAction
                    key={selected.id}
                    order={selected}
                    onCreated={() => refresh().catch(() => {})}
                  />
                </div>
                {edit && (
                  <Form onSubmit={update} className="mt-3">
                    {error && <Alert variant="danger">{error}</Alert>}
                    <div className="manager-form">
                      <Form.Group controlId="order-status">
                        <Form.Label>Trạng thái đơn</Form.Label>
                        <Form.Select
                          value={edit.status}
                          onChange={(e) => setEdit({ ...edit, status: e.target.value })}
                        >
                          {[selected.status, ...(orderTransitions[selected.status] || [])].map(
                            (k) => (
                              <option key={k} value={k}>
                                {statuses[k]}
                              </option>
                            ),
                          )}
                        </Form.Select>
                      </Form.Group>
                      <Form.Group controlId="order-payment">
                        <Form.Label>Thanh toán</Form.Label>
                        <Form.Select
                          value={edit.payment}
                          onChange={(e) => setEdit({ ...edit, payment: e.target.value })}
                        >
                          <option value={selected.payment_status}>
                            {payments[selected.payment_status]}
                          </option>
                          {selected.payment_status === 'unpaid' &&
                            !['cancelled', 'returned'].includes(edit.status) && (
                              <option value="paid">Ghi nhận đã thu tiền</option>
                            )}
                          {['paid', 'partially_refunded'].includes(selected.payment_status) &&
                            ['cancelled', 'returned'].includes(edit.status) && (
                              <option value="refunded">Ghi nhận đã hoàn tiền</option>
                            )}
                        </Form.Select>
                      </Form.Group>
                    </div>
                    <Form.Group controlId="order-reason" className="mt-3">
                      <Form.Label>Lý do / ghi chú xử lý</Form.Label>
                      <Form.Control
                        as="textarea"
                        maxLength={300}
                        required={['cancelled', 'returned'].includes(edit.status)}
                        value={edit.note}
                        onChange={(e) => setEdit({ ...edit, note: e.target.value })}
                      />
                    </Form.Group>
                    <p className="small text-secondary mt-2">
                      Hủy/trả hàng sẽ hoàn lại tồn kho. “Đã thu tiền/hoàn tiền” chỉ ghi nhận khoản
                      đã xử lý thực tế. Tạo vận đơn và giao hàng vẫn thực hiện ở trang quản lý giao
                      hàng.
                    </p>
                    <div className="d-flex gap-2">
                      <Button
                        type="submit"
                        disabled={
                          busy ||
                          (edit.status === selected.status &&
                            edit.payment === selected.payment_status)
                        }
                      >
                        Lưu cập nhật
                      </Button>
                      <Button variant="light" onClick={() => setEdit(null)}>
                        Đóng
                      </Button>
                    </div>
                  </Form>
                )}
              </div>
              <h2 className="h6 mt-4">Lịch sử trạng thái</h2>
              <ol className="manager-timeline">
                {data.order_status_history
                  .filter((h) => h.order_id === selected.id)
                  .sort((a, b) => new Date(a.created_at) - new Date(b.created_at))
                  .map((h) => (
                    <li key={h.id}>
                      <strong>{statuses[h.to_status] || h.to_status}</strong>
                      <small className="d-block text-secondary">
                        {formatDateTime(h.created_at)} · {h.changed_by || 'Hệ thống'}
                      </small>
                      <p>{h.note}</p>
                    </li>
                  ))}
              </ol>
              {!data.order_status_history.some((h) => h.order_id === selected.id) && (
                <p className="text-secondary">Chưa có lịch sử cập nhật.</p>
              )}
            </>
          )}
        </Modal.Body>
      </Modal>
    </section>
  );
}
