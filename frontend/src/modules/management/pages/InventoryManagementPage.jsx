import { useMemo, useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Nav, Table } from 'react-bootstrap';
import useInventory from '@/modules/management/hooks/useInventory';
import { formatDateTime, formatMoney, formatNumber } from '@/shared/utils/format';
import { buildInventory } from '@/modules/management/utils/inventoryData';
import InventoryOverview from '@/modules/management/components/InventoryOverview';
import { ManagerPagination, ManagerSearch } from '@/shared/components/ManagerControls';
import { inventoryApi } from '@/modules/management/api/management.api';

const EMPTY_INVENTORY = { products: [], product_variants: [] };
const STOCK_PAGE_SIZE = 10;

const movementLabels = {
  stock_in: 'Nhập kho',
  stock_out: 'Xuất kho',
  return_in: 'Hàng trả về',
  audit_adjust: 'Điều chỉnh kiểm kê',
  manual_adjust: 'Điều chỉnh thủ công',
};
const auditLabels = { draft: 'Đang kiểm kê', completed: 'Đã hoàn tất', cancelled: 'Đã hủy' };

function productName(data, variantId) {
  const variant = data.product_variants.find((item) => String(item.id) === String(variantId));
  return (
    data.products.find((item) => String(item.id) === String(variant?.product_id))?.name ||
    'Sản phẩm không tồn tại'
  );
}

export default function InventoryManagementPage() {
  const { data: inventoryData, act, busy, refresh, error: loadError } = useInventory();
  const data = inventoryData || EMPTY_INVENTORY;
  const [tab, setTab] = useState('stock');
  const [search, setSearch] = useState('');
  const [stockPage, setStockPage] = useState(1);
  const [showReceipt, setShowReceipt] = useState(false);
  const [showAdjust, setShowAdjust] = useState(false);
  const [adjustVariant, setAdjustVariant] = useState('');
  const [newQuantity, setNewQuantity] = useState('');
  const [adjustNote, setAdjustNote] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const [receiptItems, setReceiptItems] = useState([
    { variant_id: '', quantity: '1', unit_cost: '0' },
  ]);
  const [receiptNote, setReceiptNote] = useState('');
  const [activeAuditId, setActiveAuditId] = useState('');
  const [auditCounts, setAuditCounts] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const inventory = buildInventory(data);
  const variants = useMemo(() => data.product_variants || [], [data.product_variants]);
  const receipts = data.goods_receipts || [];
  const receiptLines = data.goods_receipt_items || [];
  const movements = data.inventory_movements || [];
  const audits = data.stock_audits || [];
  const auditItems = data.stock_audit_items || [];
  const alerts = data.stock_alerts || [];
  const activeAudit = audits.find((audit) => String(audit.id) === activeAuditId);
  const filteredVariants = useMemo(
    () =>
      variants.filter((variant) => {
        const name = productName(data, variant.id);
        return [name, variant.sku, variant.color, variant.size]
          .join(' ')
          .toLocaleLowerCase('vi')
          .includes(search.toLocaleLowerCase('vi'));
      }),
    [variants, data, search],
  );
  const currentStockPage = Math.min(
    stockPage,
    Math.max(1, Math.ceil(filteredVariants.length / STOCK_PAGE_SIZE)),
  );
  const pageVariants = filteredVariants.slice(
    (currentStockPage - 1) * STOCK_PAGE_SIZE,
    currentStockPage * STOCK_PAGE_SIZE,
  );
  const auditRows = activeAudit
    ? auditItems.filter((item) => String(item.audit_id) === String(activeAudit.id))
    : [];

  function fail(requestError) {
    setError(requestError.message);
  }

  async function submitReceipt(event) {
    event.preventDefault();
    setError('');
    try {
      await act(() =>
        inventoryApi.receiveStock({
          supplier_id: supplierId || null,
          items: receiptItems,
          note: receiptNote,
        }),
      );
      setShowReceipt(false);
      setReceiptItems([{ variant_id: '', quantity: '1', unit_cost: '0' }]);
      setReceiptNote('');
      setNotice('Đã tạo phiếu nhập kho và cập nhật tồn kho.');
    } catch (requestError) {
      fail(requestError);
    }
  }

  async function submitAdjustment(event) {
    event.preventDefault();
    setError('');
    try {
      await act(() =>
        inventoryApi.adjustStock({
          variant_id: adjustVariant,
          counted_qty: Number(newQuantity),
          note: adjustNote,
        }),
      );
      setShowAdjust(false);
      setNotice('Đã điều chỉnh tồn kho và ghi sổ biến động.');
    } catch (requestError) {
      fail(requestError);
    }
  }

  async function startAudit() {
    setError('');
    try {
      const result = await act(() => inventoryApi.startAudit({ note: '' }));
      const audit = result;
      if (audit) {
        setActiveAuditId(String(audit.id));
        setAuditCounts({});
        setTab('audits');
      }
      setNotice('Đã tạo phiếu kiểm kê nháp với số tồn hệ thống tại thời điểm bắt đầu.');
    } catch (requestError) {
      fail(requestError);
    }
  }

  async function saveAudit(complete = false) {
    setError('');
    try {
      await act(() =>
        inventoryApi.updateAudit({
          audit_id: activeAudit.id,
          items: auditRows.map((item) => ({
            variant_id: item.variant_id,
            counted_qty: auditCounts[item.variant_id] ?? item.counted_qty,
            note: item.note || '',
          })),
        }),
      );
      if (complete) {
        await act(() => inventoryApi.completeAudit({ audit_id: activeAudit.id }));
        setNotice('Đã hoàn tất kiểm kê; chênh lệch tồn kho được cập nhật vào sổ kho.');
        setActiveAuditId('');
      } else {
        setNotice('Đã lưu số lượng kiểm kê.');
      }
    } catch (requestError) {
      fail(requestError);
    }
  }

  async function cancelAudit(auditId) {
    setError('');
    try {
      await act(() => inventoryApi.cancelAudit({ audit_id: auditId }));
      setActiveAuditId('');
      setNotice('Đã hủy phiếu kiểm kê nháp.');
    } catch (requestError) {
      fail(requestError);
    }
  }

  function openAudit(audit) {
    setActiveAuditId(String(audit.id));
    setAuditCounts({});
    setError('');
  }

  if (!inventoryData) {
    return (
      <section className="manager-page">
        <h1>Quản lý kho</h1>
        {loadError ? (
          <Alert variant="danger">
            {loadError} <Button onClick={() => refresh().catch(fail)}>Thử lại</Button>
          </Alert>
        ) : (
          <p>Đang tải dữ liệu kho từ backend Java…</p>
        )}
      </section>
    );
  }

  const openAlerts = alerts.filter((alert) => alert.status === 'open');
  return (
    <section className="manager-page">
      <div className="eyebrow">QUẢN LÝ KHO HÀNG</div>
      <div className="manager-title">
        <div>
          <h1>Quản lý inventory</h1>
          <p>Nhập kho, theo dõi biến động, kiểm kê và xử lý cảnh báo theo SKU.</p>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          <Button variant="outline-primary" disabled={busy} onClick={() => refresh().catch(fail)}>
            Làm mới
          </Button>
          <Button
            disabled={busy}
            onClick={() => {
              setSupplierId(String(data.suppliers?.[0]?.id || ''));
              setShowReceipt(true);
            }}
          >
            + Nhập kho
          </Button>
          <Button variant="outline-primary" disabled={busy} onClick={startAudit}>
            + Tạo phiếu kiểm kê
          </Button>
        </div>
      </div>
      {loadError && <Alert variant="danger">{loadError}</Alert>}
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice('')}>
          {notice}
        </Alert>
      )}
      {error && !showReceipt && !showAdjust && (
        <Alert variant="danger" dismissible onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      <div className="manager-stock-signals">
        <div className="manager-signal">
          <strong>{formatNumber(inventory.totalQuantity)}</strong>
          <span>Tổng số lượng tồn</span>
        </div>
        <div className="manager-signal warning">
          <strong>{openAlerts.length}</strong>
          <span>Cảnh báo đang mở</span>
        </div>
        <div className="manager-signal">
          <strong>{receipts.length}</strong>
          <span>Phiếu nhập kho</span>
        </div>
      </div>
      <Nav variant="tabs" className="manager-entity-tabs" aria-label="Chức năng quản lý kho">
        {[
          ['stock', 'Tồn kho'],
          ['receipts', 'Phiếu nhập'],
          ['audits', 'Kiểm kê'],
          ['movements', 'Sổ kho'],
          ['alerts', 'Cảnh báo'],
        ].map(([key, label]) => (
          <Nav.Item key={key}>
            <Nav.Link as="button" type="button" active={tab === key} onClick={() => setTab(key)}>
              {label}
              {key === 'alerts' && openAlerts.length > 0 && (
                <Badge bg="warning" text="dark" className="ms-2">
                  {openAlerts.length}
                </Badge>
              )}
            </Nav.Link>
          </Nav.Item>
        ))}
      </Nav>

      {tab === 'stock' && (
        <>
          <InventoryOverview inventory={inventory} />
          <div className="manager-panel manager-list-panel mt-4">
            <div className="manager-title">
              <div>
                <h2>Điều chỉnh tồn kho theo SKU</h2>
                <p>Điều chỉnh thủ công sẽ ghi lại lý do và tồn sau điều chỉnh vào sổ kho.</p>
              </div>
            </div>
            <div className="manager-filters">
              <ManagerSearch
                label="Tìm SKU"
                placeholder="Tìm SKU, sản phẩm, màu, kích cỡ…"
                value={search}
                onChange={(value) => {
                  setSearch(value);
                  setStockPage(1);
                }}
              />
            </div>
            <div className="table-responsive">
              <Table hover className="manager-table align-middle">
                <thead>
                  <tr>
                    <th>Sản phẩm / SKU</th>
                    <th>Biến thể</th>
                    <th>Tồn hiện tại</th>
                    <th>Ngưỡng</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {pageVariants.map((variant) => (
                    <tr key={variant.id}>
                      <td>
                        <strong>{productName(data, variant.id)}</strong>
                        <small className="d-block text-secondary">{variant.sku}</small>
                      </td>
                      <td>{[variant.color, variant.size].filter(Boolean).join(' / ') || '—'}</td>
                      <td>{variant.stock_quantity}</td>
                      <td>{variant.low_stock_threshold ?? 5}</td>
                      <td>
                        <Button
                          size="sm"
                          variant="outline-primary"
                          disabled={busy}
                          onClick={() => {
                            setAdjustVariant(String(variant.id));
                            setNewQuantity(String(variant.stock_quantity));
                            setAdjustNote('');
                            setShowAdjust(true);
                          }}
                        >
                          Điều chỉnh
                        </Button>
                      </td>
                    </tr>
                  ))}
                  {!filteredVariants.length && (
                    <tr>
                      <td colSpan="5" className="text-center py-4">
                        Không tìm thấy SKU.
                      </td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div>
            <ManagerPagination
              total={filteredVariants.length}
              page={currentStockPage}
              pageSize={STOCK_PAGE_SIZE}
              onChange={setStockPage}
            />
          </div>
        </>
      )}

      {tab === 'receipts' && (
        <div className="manager-panel manager-list-panel">
          <h2>Phiếu nhập kho</h2>
          <div className="table-responsive">
            <Table hover className="manager-table align-middle">
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Nhà cung cấp</th>
                  <th>Ngày nhập</th>
                  <th>Số SKU</th>
                  <th>Tổng giá vốn</th>
                  <th>Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                {[...receipts]
                  .sort((a, b) => new Date(b.received_at) - new Date(a.received_at))
                  .map((receipt) => {
                    const supplier = data.suppliers?.find(
                      (item) => item.id === receipt.supplier_id,
                    );
                    const lines = receiptLines.filter(
                      (item) => String(item.receipt_id) === String(receipt.id),
                    );
                    return (
                      <tr key={receipt.id}>
                        <td>
                          <strong>{receipt.receipt_code}</strong>
                        </td>
                        <td>
                          {supplier?.name || 'Không chỉ định'}
                          <small className="d-block text-secondary">{supplier?.phone || ''}</small>
                        </td>
                        <td>{formatDateTime(receipt.received_at)}</td>
                        <td>{lines.length}</td>
                        <td>{formatMoney(receipt.total_cost)}</td>
                        <td>{receipt.note || '—'}</td>
                      </tr>
                    );
                  })}
                {!receipts.length && (
                  <tr>
                    <td colSpan="6" className="text-center py-5">
                      Chưa có phiếu nhập kho.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
        </div>
      )}

      {tab === 'audits' && (
        <div className="manager-panel manager-list-panel">
          <div className="manager-title">
            <div>
              <h2>Phiếu kiểm kê</h2>
              <p>
                Phiếu nháp chụp tồn hệ thống tại thời điểm bắt đầu; kiểm tra chênh lệch trước khi
                hoàn tất.
              </p>
            </div>
          </div>
          <div className="table-responsive">
            <Table hover className="manager-table align-middle">
              <thead>
                <tr>
                  <th>Mã kiểm kê</th>
                  <th>Ngày bắt đầu</th>
                  <th>Trạng thái</th>
                  <th>Ghi chú</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {[...audits]
                  .sort((a, b) => new Date(b.started_at) - new Date(a.started_at))
                  .map((audit) => (
                    <tr key={audit.id}>
                      <td>
                        <strong>{audit.audit_code}</strong>
                      </td>
                      <td>{formatDateTime(audit.started_at)}</td>
                      <td>
                        <Badge
                          bg={
                            audit.status === 'draft'
                              ? 'warning'
                              : audit.status === 'completed'
                                ? 'success'
                                : 'secondary'
                          }
                          text={audit.status === 'draft' ? 'dark' : undefined}
                        >
                          {auditLabels[audit.status]}
                        </Badge>
                      </td>
                      <td>{audit.note || '—'}</td>
                      <td>
                        {audit.status === 'draft' && (
                          <>
                            <Button
                              size="sm"
                              variant="outline-primary"
                              onClick={() => openAudit(audit)}
                            >
                              Tiếp tục
                            </Button>
                            <Button
                              size="sm"
                              variant="outline-danger"
                              disabled={busy}
                              className="ms-2"
                              onClick={() => cancelAudit(audit.id)}
                            >
                              Hủy
                            </Button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                {!audits.length && (
                  <tr>
                    <td colSpan="5" className="text-center py-5">
                      Chưa có phiếu kiểm kê.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
          {activeAudit && (
            <div className="manager-audit-editor mt-4">
              <div className="manager-title">
                <div>
                  <h2>Kiểm kê {activeAudit.audit_code}</h2>
                  <p>Nhập số lượng thực đếm. Chênh lệch sẽ cập nhật tồn khi hoàn tất phiếu.</p>
                </div>
                <Button variant="outline-secondary" onClick={() => setActiveAuditId('')}>
                  Đóng phiếu
                </Button>
              </div>
              <div className="table-responsive">
                <Table className="manager-table align-middle">
                  <thead>
                    <tr>
                      <th>Sản phẩm / SKU</th>
                      <th>Tồn hệ thống</th>
                      <th>Thực đếm</th>
                      <th>Chênh lệch</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditRows.map((item) => {
                      const counted = Number(auditCounts[item.variant_id] ?? item.counted_qty);
                      return (
                        <tr key={item.id}>
                          <td>
                            <strong>{productName(data, item.variant_id)}</strong>
                            <small className="d-block text-secondary">
                              {
                                variants.find(
                                  (variant) => String(variant.id) === String(item.variant_id),
                                )?.sku
                              }
                            </small>
                          </td>
                          <td>{item.system_qty}</td>
                          <td>
                            <Form.Control
                              aria-label={`Số lượng kiểm kê ${item.variant_id}`}
                              type="number"
                              min="0"
                              step="1"
                              value={auditCounts[item.variant_id] ?? item.counted_qty}
                              onChange={(event) =>
                                setAuditCounts({
                                  ...auditCounts,
                                  [item.variant_id]: event.target.value,
                                })
                              }
                            />
                          </td>
                          <td>
                            <Badge
                              bg={counted === Number(item.system_qty) ? 'secondary' : 'warning'}
                              text={counted === Number(item.system_qty) ? undefined : 'dark'}
                            >
                              {counted - Number(item.system_qty) > 0 ? '+' : ''}
                              {counted - Number(item.system_qty)}
                            </Badge>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </Table>
              </div>
              <div className="d-flex gap-2 flex-wrap">
                <Button variant="outline-primary" disabled={busy} onClick={() => saveAudit(false)}>
                  Lưu số đếm
                </Button>
                <Button disabled={busy} onClick={() => saveAudit(true)}>
                  Hoàn tất kiểm kê & cập nhật kho
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {tab === 'movements' && (
        <div className="manager-panel manager-list-panel">
          <h2>Sổ biến động kho</h2>
          <div className="table-responsive">
            <Table hover className="manager-table align-middle">
              <thead>
                <tr>
                  <th>Thời điểm</th>
                  <th>SKU / Sản phẩm</th>
                  <th>Loại</th>
                  <th>Thay đổi</th>
                  <th>Tồn sau giao dịch</th>
                  <th>Tham chiếu / Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                {[...movements]
                  .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
                  .map((movement) => (
                    <tr key={movement.id}>
                      <td>{formatDateTime(movement.created_at)}</td>
                      <td>
                        {productName(data, movement.variant_id)}
                        <small className="d-block text-secondary">
                          {
                            variants.find(
                              (variant) => String(variant.id) === String(movement.variant_id),
                            )?.sku
                          }
                        </small>
                      </td>
                      <td>{movementLabels[movement.movement_type] || movement.movement_type}</td>
                      <td className={movement.quantity < 0 ? 'text-danger' : 'text-success'}>
                        {movement.quantity > 0 ? '+' : ''}
                        {movement.quantity}
                      </td>
                      <td>{movement.balance_after}</td>
                      <td>
                        {movement.ref_type || '—'}
                        {movement.ref_id ? ` #${movement.ref_id}` : ''}
                        <small className="d-block text-secondary">{movement.note || ''}</small>
                      </td>
                    </tr>
                  ))}
                {!movements.length && (
                  <tr>
                    <td colSpan="6" className="text-center py-5">
                      Chưa có biến động kho.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
        </div>
      )}

      {tab === 'alerts' && (
        <div className="manager-panel manager-list-panel">
          <h2>Cảnh báo tồn kho</h2>
          <div className="table-responsive">
            <Table hover className="manager-table align-middle">
              <thead>
                <tr>
                  <th>Sản phẩm / SKU</th>
                  <th>Tồn lúc cảnh báo</th>
                  <th>Ngưỡng</th>
                  <th>Trạng thái</th>
                  <th>Thời điểm</th>
                  <th>Đã xử lý</th>
                </tr>
              </thead>
              <tbody>
                {[...alerts]
                  .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
                  .map((alert) => (
                    <tr key={alert.id}>
                      <td>
                        <strong>{productName(data, alert.variant_id)}</strong>
                        <small className="d-block text-secondary">
                          {
                            variants.find(
                              (variant) => String(variant.id) === String(alert.variant_id),
                            )?.sku
                          }
                        </small>
                      </td>
                      <td>{alert.stock_at_alert}</td>
                      <td>{alert.threshold}</td>
                      <td>
                        <Badge
                          bg={alert.status === 'open' ? 'warning' : 'success'}
                          text={alert.status === 'open' ? 'dark' : undefined}
                        >
                          {alert.status === 'open' ? 'Đang mở' : 'Đã xử lý'}
                        </Badge>
                      </td>
                      <td>{formatDateTime(alert.created_at)}</td>
                      <td>{alert.resolved_at ? formatDateTime(alert.resolved_at) : '—'}</td>
                    </tr>
                  ))}
                {!alerts.length && (
                  <tr>
                    <td colSpan="6" className="text-center py-5">
                      Chưa có cảnh báo tồn kho.
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
        </div>
      )}

      <Modal
        show={showReceipt}
        onHide={() => setShowReceipt(false)}
        size="lg"
        centered
        scrollable
        className="manager-modal"
      >
        <Form onSubmit={submitReceipt}>
          <Modal.Header closeButton>
            <Modal.Title>Phiếu nhập kho</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <Form.Group className="mb-3">
              <Form.Label>Nhà cung cấp</Form.Label>
              <Form.Select
                value={supplierId}
                onChange={(event) => setSupplierId(event.target.value)}
              >
                <option value="">Không chỉ định nhà cung cấp</option>
                {(data.suppliers || []).map((supplier) => (
                  <option key={supplier.id} value={supplier.id}>
                    {supplier.name} · {supplier.phone}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <div className="manager-receipt-lines">
              {receiptItems.map((item, index) => (
                <div className="manager-receipt-line" key={index}>
                  <Form.Group>
                    <Form.Label>SKU</Form.Label>
                    <Form.Select
                      required
                      value={item.variant_id}
                      onChange={(event) =>
                        setReceiptItems(
                          receiptItems.map((line, i) =>
                            i === index ? { ...line, variant_id: event.target.value } : line,
                          ),
                        )
                      }
                    >
                      <option value="">Chọn SKU</option>
                      {variants.map((variant) => (
                        <option key={variant.id} value={variant.id}>
                          {variant.sku} · {productName(data, variant.id)}
                        </option>
                      ))}
                    </Form.Select>
                  </Form.Group>
                  <Form.Group>
                    <Form.Label>Số lượng</Form.Label>
                    <Form.Control
                      type="number"
                      min="1"
                      step="1"
                      required
                      value={item.quantity}
                      onChange={(event) =>
                        setReceiptItems(
                          receiptItems.map((line, i) =>
                            i === index ? { ...line, quantity: event.target.value } : line,
                          ),
                        )
                      }
                    />
                  </Form.Group>
                  <Form.Group>
                    <Form.Label>Đơn giá nhập</Form.Label>
                    <Form.Control
                      type="number"
                      min="0"
                      step="1"
                      required
                      value={item.unit_cost}
                      onChange={(event) =>
                        setReceiptItems(
                          receiptItems.map((line, i) =>
                            i === index ? { ...line, unit_cost: event.target.value } : line,
                          ),
                        )
                      }
                    />
                  </Form.Group>
                  <Button
                    type="button"
                    variant="outline-danger"
                    disabled={receiptItems.length === 1}
                    aria-label="Xóa dòng nhập"
                    onClick={() => setReceiptItems(receiptItems.filter((_, i) => i !== index))}
                  >
                    ×
                  </Button>
                </div>
              ))}
            </div>
            <Button
              type="button"
              variant="outline-primary"
              className="my-3"
              onClick={() =>
                setReceiptItems([
                  ...receiptItems,
                  { variant_id: '', quantity: '1', unit_cost: '0' },
                ])
              }
            >
              + Thêm SKU
            </Button>
            <Form.Group>
              <Form.Label>Ghi chú</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                maxLength={300}
                value={receiptNote}
                onChange={(event) => setReceiptNote(event.target.value)}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={() => setShowReceipt(false)}>
              Đóng
            </Button>
            <Button type="submit" disabled={busy}>
              Lưu phiếu nhập
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal
        show={showAdjust}
        onHide={() => setShowAdjust(false)}
        centered
        className="manager-modal"
      >
        <Form onSubmit={submitAdjustment}>
          <Modal.Header closeButton>
            <Modal.Title>Điều chỉnh tồn kho</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <p className="small text-secondary">
              SKU: {variants.find((variant) => String(variant.id) === adjustVariant)?.sku} · Tồn
              hiện tại:{' '}
              {variants.find((variant) => String(variant.id) === adjustVariant)?.stock_quantity}
            </p>
            <Form.Group className="mb-3">
              <Form.Label>Tồn kho thực tế</Form.Label>
              <Form.Control
                type="number"
                min="0"
                step="1"
                required
                value={newQuantity}
                onChange={(event) => setNewQuantity(event.target.value)}
              />
            </Form.Group>
            <Form.Group>
              <Form.Label>Lý do điều chỉnh</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                maxLength={300}
                required
                value={adjustNote}
                onChange={(event) => setAdjustNote(event.target.value)}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={() => setShowAdjust(false)}>
              Đóng
            </Button>
            <Button type="submit" disabled={busy}>
              Lưu điều chỉnh
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </section>
  );
}
