import { ManagerPagination, ManagerSearch } from '@/shared/components/ManagerControls';
import { useState } from 'react';
import { Badge, Button, Form, Table } from 'react-bootstrap';
import { Link } from 'react-router';
import { formatMoney, formatNumber } from '@/shared/utils/format';
import { exportCsv, optionLabels } from '@/modules/management/utils/managementUtils';

const PAGE_SIZE = 10;

export default function InventoryOverview({ inventory }) {
  const [search, setSearch] = useState('');
  const [type, setType] = useState('');
  const [stock, setStock] = useState('');
  const [expanded, setExpanded] = useState(null);
  const [page, setPage] = useState(1);
  const rows = inventory.products.filter(
    (p) =>
      (!type || p.product_type === type) &&
      (!stock || (stock === 'out' ? p.out > 0 : stock === 'low' ? p.low > 0 : p.quantity > 0)) &&
      [p.name, p.brand, ...p.variants.map((v) => v.sku)]
        .join(' ')
        .toLocaleLowerCase('vi')
        .includes(search.toLocaleLowerCase('vi')),
  );
  const currentPage = Math.min(page, Math.max(1, Math.ceil(rows.length / PAGE_SIZE)));
  const pageRows = rows.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function changeFilter(setValue, value) {
    setValue(value);
    setPage(1);
    setExpanded(null);
  }

  function changePage(value) {
    setPage(value);
    setExpanded(null);
  }

  function download() {
    exportCsv(
      'bao-cao-ton-kho.csv',
      [
        'Sản phẩm',
        'Loại',
        'SKU',
        'Màu',
        'Cỡ',
        'Tồn kho',
        'Giá vốn',
        'Giá bán',
        'Ngưỡng cảnh báo',
        'Hoạt động',
      ],
      rows.flatMap((p) =>
        p.variants.map((v) => [
          p.name,
          optionLabels[p.product_type],
          v.sku,
          v.color,
          v.size,
          v.stock_quantity,
          v.cost_price,
          v.price,
          v.low_stock_threshold ?? 5,
          v.is_active ? 'Có' : 'Không',
        ]),
      ),
    );
  }
  return (
    <>
      <div className="manager-section-heading">
        <div>
          <div className="eyebrow">KHO HÀNG HIỆN TẠI</div>
          <h2>Tổng quan toàn bộ tồn kho</h2>
          <p>
            Số lượng hiện tại của tất cả SKU, gồm cả biến thể tạm ẩn. Bộ lọc ngày doanh thu không
            ảnh hưởng tồn kho.
          </p>
        </div>
        <Button variant="outline-primary" onClick={download}>
          Xuất tồn kho
        </Button>
      </div>
      <div className="manager-stats manager-stock-stats">
        {[
          ['Tổng sản phẩm', inventory.totalProducts, inventory.totalVariants + ' biến thể / SKU'],
          [
            'Tổng số lượng tồn',
            formatNumber(inventory.totalQuantity),
            formatNumber(inventory.activeQuantity) + ' thuộc SKU đang hoạt động',
          ],
          [
            'Giá trị kho theo giá bán',
            formatMoney(inventory.saleValue),
            'Tồn kho × giá bán hiện tại',
          ],
          [
            'Giá trị kho theo giá vốn',
            formatMoney(inventory.costValue),
            inventory.missingCost
              ? inventory.missingCost + ' SKU chưa có giá vốn, chưa được tính'
              : 'Tồn kho × giá vốn hiện tại',
          ],
        ].map(([label, value, hint]) => (
          <div className="manager-panel" key={label}>
            <span className="text-secondary">{label}</span>
            <strong>{value}</strong>
            <small>{hint}</small>
          </div>
        ))}
      </div>
      <div className="manager-stock-signals">
        <button
          type="button"
          onClick={() => changeFilter(setStock, stock === 'low' ? '' : 'low')}
          className={'manager-signal warning' + (stock === 'low' ? ' selected' : '')}
        >
          <strong>{inventory.low}</strong>
          <span>SKU sắp hết hàng</span>
        </button>
        <button
          type="button"
          onClick={() => changeFilter(setStock, stock === 'out' ? '' : 'out')}
          className={'manager-signal danger' + (stock === 'out' ? ' selected' : '')}
        >
          <strong>{inventory.out}</strong>
          <span>SKU đã hết hàng</span>
        </button>
        <div className="manager-signal">
          <strong>{inventory.allocated}</strong>
          <span>Sản phẩm trong đơn đang xử lý / giao</span>
          <small>Đã trừ khỏi tồn kho khi đặt hàng</small>
        </div>
      </div>
      <div className="manager-panel mb-4">
        <h2>Phân bổ số lượng theo loại sản phẩm</h2>
        <div className="manager-stock-groups">
          {inventory.groups.map((g) => (
            <button
              type="button"
              key={g.type}
              onClick={() => changeFilter(setType, type === g.type ? '' : g.type)}
              className={type === g.type ? 'selected' : ''}
            >
              <div>
                <strong>{optionLabels[g.type]}</strong>
                <span>{formatNumber(g.quantity)} sản phẩm</span>
              </div>
              <div className="manager-stock-track">
                <span
                  style={{
                    width: `${inventory.totalQuantity ? (g.quantity / inventory.totalQuantity) * 100 : 0}%`,
                  }}
                />
              </div>
              <small>
                {g.products} mẫu sản phẩm ·{' '}
                {inventory.totalQuantity
                  ? Math.round((g.quantity / inventory.totalQuantity) * 100)
                  : 0}
                % tổng kho
              </small>
            </button>
          ))}
        </div>
      </div>
      <div className="manager-panel manager-list-panel">
        <div className="manager-title">
          <h2>Chi tiết tồn kho theo sản phẩm</h2>
          <Link to="/management/inventory">Điều chỉnh tồn kho →</Link>
        </div>
        <div className="manager-filters">
          <ManagerSearch
            label="Tìm sản phẩm trong kho"
            placeholder="Tìm tên sản phẩm, thương hiệu, SKU…"
            value={search}
            onChange={(value) => changeFilter(setSearch, value)}
          />
          <Form.Select
            aria-label="Loại sản phẩm trong kho"
            value={type}
            onChange={(e) => changeFilter(setType, e.target.value)}
          >
            <option value="">Tất cả loại sản phẩm</option>
            {inventory.groups.map((g) => (
              <option key={g.type} value={g.type}>
                {optionLabels[g.type]}
              </option>
            ))}
          </Form.Select>
          <Form.Select
            aria-label="Tình trạng tồn kho"
            value={stock}
            onChange={(e) => changeFilter(setStock, e.target.value)}
          >
            <option value="">Tất cả tồn kho</option>
            <option value="in">Còn hàng</option>
            <option value="low">Có SKU sắp hết</option>
            <option value="out">Có SKU hết hàng</option>
          </Form.Select>
        </div>
        <Table responsive hover className="align-middle manager-table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Tổng tồn</th>
              <th>Giá trị theo giá bán</th>
              <th>Cảnh báo</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {pageRows.map((p) => (
              <InventoryRow
                key={p.id}
                product={p}
                expanded={expanded === p.id}
                toggle={() => setExpanded(expanded === p.id ? null : p.id)}
              />
            ))}
            {!rows.length && (
              <tr>
                <td colSpan="5" className="text-center py-4">
                  Không tìm thấy sản phẩm phù hợp.
                </td>
              </tr>
            )}
          </tbody>
        </Table>
        <ManagerPagination
          total={rows.length}
          page={currentPage}
          pageSize={PAGE_SIZE}
          onChange={changePage}
        />
        <small className="text-secondary">
          Kết quả lọc: {rows.length}/{inventory.totalProducts} sản phẩm · Tổng tồn trong kết quả:{' '}
          {formatNumber(rows.reduce((s, p) => s + p.quantity, 0))}
        </small>
      </div>
    </>
  );
}
function InventoryRow({ product: p, expanded, toggle }) {
  return (
    <>
      <tr>
        <td>
          <strong>{p.name}</strong>
          <small className="d-block text-secondary">
            {p.brand} · {optionLabels[p.product_type]} · {p.variants.length} SKU
            {p.is_demo ? ' · Mẫu' : ''}
          </small>
        </td>
        <td>
          <strong>{p.quantity}</strong>
        </td>
        <td>{formatMoney(p.saleValue)}</td>
        <td>
          {p.out > 0 && (
            <Badge bg="danger" className="me-1">
              {p.out} hết
            </Badge>
          )}
          {p.low > 0 && (
            <Badge bg="warning" text="dark">
              {p.low} sắp hết
            </Badge>
          )}
          {!p.out && !p.low && <Badge bg="success">Ổn định</Badge>}
        </td>
        <td className="manager-action-cell">
          <Button variant="outline-primary" size="sm" onClick={toggle} aria-expanded={expanded}>
            {expanded ? 'Thu gọn' : 'Xem SKU'}
          </Button>
          <Link className="d-block small mt-2" to="/management/inventory">
            Điều chỉnh
          </Link>
        </td>
      </tr>
      {expanded && (
        <tr className="manager-variant-row">
          <td colSpan="7">
            <Table responsive size="sm" className="mb-0">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Màu / kích cỡ</th>
                  <th>Tồn kho</th>
                  <th>Ngưỡng cảnh báo</th>
                  <th>Giá vốn</th>
                  <th>Giá bán</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {p.variants.map((v) => (
                  <tr key={v.id}>
                    <td>{v.sku}</td>
                    <td>
                      {v.color} / {v.size}
                    </td>
                    <td>{v.stock_quantity}</td>
                    <td>{v.low_stock_threshold ?? 5}</td>
                    <td>{v.cost_price > 0 ? formatMoney(v.cost_price) : 'Chưa cập nhật'}</td>
                    <td>{formatMoney(v.price)}</td>
                    <td>
                      {!v.is_active
                        ? 'Tạm ẩn'
                        : v.stock_quantity === 0
                          ? 'Hết hàng'
                          : v.stock_quantity <= (v.low_stock_threshold ?? 5)
                            ? 'Sắp hết'
                            : 'Còn hàng'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </td>
        </tr>
      )}
    </>
  );
}
