import InventoryOverview from '@/modules/management/components/InventoryOverview';
import useInventory from '@/modules/management/hooks/useInventory';
import { buildInventory } from '@/modules/management/utils/inventoryData';
import { useState } from 'react';
import { Alert, Badge, Button, Form, Table } from 'react-bootstrap';
import { Link } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatMoney, formatNumber } from '@/shared/utils/format';
import { statuses } from '@/modules/management/constants';
import { exportCsv, localDate } from '@/modules/management/utils/managementUtils';
import { buildReport } from '@/modules/management/utils/reportData';

export default function DashboardPage() {
  const { data, refresh, busy } = useShop();
  const [from, setFrom] = useState(''),
    [to, setTo] = useState('');
  const report = buildReport(data, from, to);
  const { data: stockData, error: stockError, refresh: refreshInventory } = useInventory();
  const inventory = stockData ? buildInventory(stockData) : null;
  const max = Math.max(...report.months.map((m) => m.revenue), 1);
  const low = (stockData?.product_variants || []).filter(
    (v) => v.is_active && v.stock_quantity <= (v.low_stock_threshold ?? 5),
  );
  function period(value) {
    const now = new Date(),
      today = localDate(now);
    setTo(value === 'all' ? '' : today);
    setFrom(
      value === 'month'
        ? today.slice(0, 7) + '-01'
        : value === 'year'
          ? today.slice(0, 4) + '-01-01'
          : value === 'week'
            ? localDate(new Date(now.getTime() - 6 * 86400000))
            : '',
    );
  }
  function download() {
    exportCsv(
      'bao-cao-doanh-thu.csv',
      ['Tháng', 'Tổng đơn', 'Doanh thu hoàn tất'],
      report.months.map((m) => [m.month, m.orders, m.revenue]),
    );
  }
  return (
    <section className="manager-page">
      <div className="eyebrow">TỔNG QUAN KINH DOANH</div>
      {data.orders.some((o) => o.is_demo) && (
        <div className="manager-demo-note">
          Dữ liệu minh họa dùng cho báo cáo doanh thu; tồn kho được tải từ backend Java. Các giao
          dịch mẫu có mã DEMO và được lưu trong db.json.
        </div>
      )}
      <div className="manager-title">
        <div>
          <h1>Dashboard tổng quan</h1>
          <p>Doanh thu, đơn hàng và tồn kho từ dữ liệu cửa hàng.</p>
        </div>
        <div className="d-flex gap-2">
          <Button
            variant="outline-primary"
            disabled={busy}
            onClick={() => Promise.all([refresh(), refreshInventory()]).catch(() => {})}
          >
            Làm mới
          </Button>
          <Button onClick={download}>Xuất báo cáo</Button>
        </div>
      </div>
      {stockError && <Alert variant="danger">{stockError}</Alert>}
      <div className="manager-overview-banner">
        <a href="#dashboard-inventory">
          <span>TỔNG TỒN KHO</span>
          <strong>
            {inventory ? formatNumber(inventory.totalQuantity) : '—'} <small>sản phẩm</small>
          </strong>
          <p>
            {inventory?.totalProducts ?? '—'} mẫu sản phẩm · {inventory?.totalVariants ?? '—'} SKU →
          </p>
        </a>
        <a href="#dashboard-revenue">
          <span>DOANH THU ĐÃ GHI NHẬN</span>
          <strong>{formatMoney(report.revenue)}</strong>
          <p>
            {report.earned.length} đơn hoàn tất, đã thanh toán ·{' '}
            {from || to ? 'Trong kỳ đã chọn' : 'Toàn bộ thời gian'} →
          </p>
        </a>
      </div>
      <div id="dashboard-inventory">
        {inventory ? (
          <InventoryOverview inventory={inventory} />
        ) : (
          <p>Đang tải tồn kho từ backend Java…</p>
        )}
      </div>
      <div id="dashboard-revenue" className="manager-section-heading mt-5">
        <div>
          <div className="eyebrow">DOANH THU & ĐƠN HÀNG</div>
          <h2>Báo cáo hoạt động kinh doanh</h2>
        </div>
      </div>
      <div className="manager-panel mb-4">
        <div className="d-flex gap-2 flex-wrap mb-3">
          {[
            ['all', 'Tất cả'],
            ['week', '7 ngày qua'],
            ['month', 'Tháng này'],
            ['year', 'Năm nay'],
          ].map(([value, label]) => (
            <Button key={value} variant="light" size="sm" onClick={() => period(value)}>
              {label}
            </Button>
          ))}
        </div>
        <div className="manager-filters">
          <Form.Group>
            <Form.Label>Từ ngày</Form.Label>
            <Form.Control
              type="date"
              aria-label="Từ ngày"
              value={from}
              max={to || undefined}
              onChange={(e) => setFrom(e.target.value)}
            />
          </Form.Group>
          <Form.Group>
            <Form.Label>Đến ngày</Form.Label>
            <Form.Control
              type="date"
              aria-label="Đến ngày"
              min={from || undefined}
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </Form.Group>
        </div>
        <small className="text-secondary">
          Theo ngày đặt (giờ Việt Nam). Doanh thu chỉ tính đơn hoàn tất, đã thanh toán; gồm phí giao
          hàng và đã trừ giảm giá.
        </small>
      </div>
      <div className="manager-stats">
        {[
          ['Doanh thu ghi nhận', formatMoney(report.revenue)],
          ['Tổng đơn hàng', report.orders.length],
          ['Giá trị đơn trung bình', formatMoney(report.average)],
          [
            'Chờ xử lý',
            report.orders.filter((o) => ['pending', 'confirmed', 'preparing'].includes(o.status))
              .length,
          ],
        ].map(([name, value]) => (
          <div className="manager-panel" key={name}>
            <span className="text-secondary">{name}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
      <div className="manager-grid">
        <div className="manager-panel">
          <h2>Doanh thu theo tháng</h2>
          <p className="text-secondary small">
            {from || 'Từ đầu'} → {to || 'Hiện tại'} · {report.earned.length} đơn được ghi nhận
          </p>
          {report.months.length ? (
            <>
              <div className="manager-chart" role="img" aria-label="Biểu đồ doanh thu theo tháng">
                {report.months.map((m) => (
                  <div key={m.month}>
                    <span className="manager-bar-value">{formatMoney(m.revenue)}</span>
                    <div
                      className="manager-bar"
                      style={{ height: `${Math.max((m.revenue / max) * 150, 3)}px` }}
                      title={formatMoney(m.revenue)}
                    />
                    <small>
                      {m.month.slice(5)}/{m.month.slice(0, 4)}
                    </small>
                  </div>
                ))}
              </div>
              <Table responsive size="sm" className="mt-4">
                <thead>
                  <tr>
                    <th>Tháng</th>
                    <th>Số đơn</th>
                    <th>Doanh thu</th>
                  </tr>
                </thead>
                <tbody>
                  {report.months.map((m) => (
                    <tr key={m.month}>
                      <td>{m.month}</td>
                      <td>{m.orders}</td>
                      <td>{formatMoney(m.revenue)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </>
          ) : (
            <div className="manager-empty">
              Không có đơn hàng trong khoảng ngày này. Chọn “Tất cả” để xem toàn bộ dữ liệu.
            </div>
          )}
        </div>
        <div className="manager-panel">
          <h2>Tình trạng đơn hàng</h2>
          {Object.entries(statuses).map(([key, label]) => (
            <Link
              to={'/management/orders?status=' + key}
              className="manager-summary text-decoration-none"
              key={key}
            >
              <span>{label}</span>
              <strong>{report.orders.filter((o) => o.status === key).length} →</strong>
            </Link>
          ))}
          <hr />
          <div className="manager-summary">
            <span>Chưa thanh toán</span>
            <strong>{formatMoney(report.awaitingPayment)}</strong>
          </div>
        </div>
      </div>
      <div className="manager-grid mt-4">
        <div className="manager-panel">
          <div className="manager-title">
            <h2>Sản phẩm bán chạy</h2>
            <Link to="/management/catalog/products">Quản lý sản phẩm →</Link>
          </div>
          <Table responsive>
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th>Đã bán</th>
                <th>Tiền hàng</th>
              </tr>
            </thead>
            <tbody>
              {report.topProducts.slice(0, 5).map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.quantity}</td>
                  <td>{formatMoney(p.revenue)}</td>
                </tr>
              ))}
            </tbody>
          </Table>
          {!report.topProducts.length && (
            <p className="text-secondary">Chưa có sản phẩm trong đơn hoàn tất của kỳ này.</p>
          )}
        </div>
        <div className="manager-panel">
          <h2>Cơ cấu doanh thu</h2>
          {[
            ['Tiền hàng', report.subtotal],
            ['Giảm giá', -report.discounts],
            ['Phí giao hàng', report.shipping],
            ['Tổng ghi nhận', report.revenue],
          ].map(([label, value]) => (
            <div className="manager-summary" key={label}>
              <span>{label}</span>
              <strong>{formatMoney(value)}</strong>
            </div>
          ))}
        </div>
      </div>
      <div className="manager-panel mt-4">
        <div className="manager-title">
          <h2>Đơn hàng gần đây</h2>
          <Link to="/management/orders">Xử lý đơn hàng →</Link>
        </div>
        <Table responsive className="align-middle">
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>Ngày đặt</th>
              <th>Trạng thái</th>
              <th>Tổng tiền</th>
            </tr>
          </thead>
          <tbody>
            {[...report.orders]
              .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
              .slice(0, 5)
              .map((o) => (
                <tr key={o.id}>
                  <td>
                    <Link to={'/management/orders?order=' + o.id}>{o.order_code}</Link>
                  </td>
                  <td>{o.receiver_name}</td>
                  <td>{new Date(o.created_at).toLocaleDateString('vi-VN')}</td>
                  <td>
                    <Badge bg={o.status === 'completed' ? 'success' : 'info'}>
                      {statuses[o.status]}
                    </Badge>
                  </td>
                  <td>{formatMoney(o.total_amount)}</td>
                </tr>
              ))}
          </tbody>
        </Table>
        {!report.orders.length && <p className="text-secondary">Không có đơn trong kỳ đã chọn.</p>}
      </div>
      <div className="manager-panel mt-4">
        <div className="manager-title">
          <h2>
            Cảnh báo tồn kho thấp{' '}
            <Badge bg="warning" text="dark">
              {low.length}
            </Badge>
          </h2>
          <Link to="/management/inventory">Bổ sung tồn kho →</Link>
        </div>
        <Table responsive>
          <thead>
            <tr>
              <th>SKU</th>
              <th>Sản phẩm</th>
              <th>Tồn kho</th>
              <th>Ngưỡng cảnh báo</th>
            </tr>
          </thead>
          <tbody>
            {low.slice(0, 8).map((v) => (
              <tr key={v.id}>
                <td>{v.sku}</td>
                <td>
                  {stockData.products.find((p) => String(p.id) === String(v.product_id))?.name}
                </td>
                <td>
                  <Badge
                    bg={v.stock_quantity === 0 ? 'danger' : 'warning'}
                    text={v.stock_quantity === 0 ? 'white' : 'dark'}
                  >
                    {v.stock_quantity}
                  </Badge>
                </td>
                <td>{v.low_stock_threshold ?? 5}</td>
              </tr>
            ))}
          </tbody>
        </Table>
        {stockData && !low.length && (
          <p className="text-secondary">Không có biến thể cần bổ sung tồn kho.</p>
        )}
      </div>
    </section>
  );
}
