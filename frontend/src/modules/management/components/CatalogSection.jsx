import ImagePicker from './ImagePicker';
import { isDemoProduct } from '@/modules/shop/utils/catalogProducts';
import { productImage, productImageFallback } from '@/modules/shop/utils/productImages';
import { ManagerSearch, ManagerPagination } from '@/shared/components/ManagerControls';
import { catalogListFields } from '@/modules/management/utils/catalogListFields';
import { useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Table } from 'react-bootstrap';
import { Link } from 'react-router';
import { useCatalog } from './CatalogContext';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { catalogs } from '@/modules/management/constants';
import {
  createCatalogDefaults,
  dateTimeInput,
  exportCsv,
  optionLabels,
  requiredFields,
  slugify,
} from '@/modules/management/utils/managementUtils';
import { catalogApi } from '../api/catalog.api';

const pageSize = 10;
export default function CatalogSection({
  catalog,
  scope = {},
  embedded = false,
  allowCreate = true,
  onManage,
  onSaved,
}) {
  const { data, act, busy } = useCatalog();
  const [imageReading, setImageReading] = useState(false);
  const [search, setSearch] = useState('');
  const [active, setActive] = useState('');
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState(null);
  const [detail, setDetail] = useState(null);
  const [remove, setRemove] = useState(null);
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState('');
  const config = catalogs[catalog];
  if (!config)
    return (
      <Alert variant="warning">
        Không tìm thấy catalog. <Link to="/management">Về dashboard</Link>
      </Alert>
    );
  const matchesScope = (row) =>
    Object.entries(scope).every(([field, value]) =>
      Array.isArray(value)
        ? value.map(String).includes(String(row[field]))
        : String(row[field]) === String(value),
    );
  const rows = (data[catalog] || []).filter(
    (row) => matchesScope(row) && (catalog !== 'products' || !isDemoProduct(row)),
  );
  const key = config.key || 'id';
  const required = requiredFields[catalog] || [];
  const hasActive = config.fields.some((f) => f.key === 'is_active');
  function label(table, id) {
    const row = (data[table] || []).find((r) => String(r.id) === String(id));
    return row?.name || row?.sku || row?.code || row?.order_code || id || '—';
  }
  function display(field, row) {
    const value = row[field.key];
    if (value == null || value === '') return '—';
    if (field.type === 'image') return 'Đã có ảnh';
    if (field.type === 'checkbox') return value ? 'Hoạt động' : 'Tạm ẩn';
    if (field.type === 'ref') return label(field.options, value);
    if (field.key === 'scope_id')
      return label(
        { category: 'categories', brand: 'brands', product: 'products' }[row.scope_type],
        value,
      );
    if (
      /price|min_order|max_discount|discount_amount/.test(field.key) ||
      (field.key === 'discount_value' && row.discount_type === 'fixed')
    )
      return formatMoney(value);
    if (field.key === 'discount_value' && row.discount_type === 'percent') return value + '%';
    if (field.key.endsWith('_at')) return formatDateTime(value);
    return optionLabels[value] || String(value);
  }
  const filtered = rows.filter(
    (row) =>
      (active === '' || !!row.is_active === (active === 'true')) &&
      config.fields
        .map((f) => display(f, row))
        .join(' ')
        .toLocaleLowerCase('vi')
        .includes(search.toLocaleLowerCase('vi')),
  );
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const current = Math.min(page, totalPages);
  const visible = filtered.slice((current - 1) * pageSize, current * pageSize);
  async function perform(action, success) {
    setError('');
    try {
      const result = await act(() =>
        action.type === 'catalog_save'
          ? catalogApi.save(action.payload)
          : catalogApi.remove(action.payload),
      );
      setNotice(success);
      if (onSaved && action.type === 'catalog_save') onSaved(result, action.payload.id);
      return true;
    } catch (e) {
      setError(e.message);
      return false;
    }
  }
  async function save(e) {
    e.preventDefault();
    if (imageReading || busy) return;
    if (
      config.fields.some(
        (f) => f.type === 'image' && required.includes(f.key) && !draft.record[f.key],
      )
    ) {
      setError('Vui lòng chọn hình ảnh.');
      return;
    }
    const record = { ...draft.record };
    for (const f of config.fields)
      if (f.type === 'datetime-local' && record[f.key])
        record[f.key] = new Date(record[f.key]).toISOString();
    if (
      await perform(
        {
          type: 'catalog_save',
          payload: { table: catalog, record: { ...record, ...scope }, id: draft.id },
        },
        'Đã lưu ' + config.title.toLowerCase() + '.',
      )
    ) {
      setDraft(null);
    }
  }
  function open(row) {
    setError('');
    const defaults = createCatalogDefaults();
    const record = row
      ? { ...row }
      : Object.fromEntries(
          config.fields.map((f) => [
            f.key,
            defaults[f.key] ??
              (f.type === 'checkbox' ? true : f.type === 'select' ? f.options[0] : ''),
          ]),
        );
    for (const f of config.fields)
      if (f.type === 'datetime-local' && record[f.key])
        record[f.key] = dateTimeInput(record[f.key]);
    setDraft({ id: row?.[key], record: { ...record, ...scope } });
  }
  function change(field, value) {
    setDraft((previous) => {
      const record = { ...previous.record, [field]: value };
      if (
        field === 'name' &&
        !previous.id &&
        (!record.slug || record.slug === slugify(previous.record.name || ''))
      )
        record.slug = slugify(value);
      if (field === 'product_id' && catalog === 'product_images') record.variant_id = '';
      if (field === 'scope_type') record.scope_id = '';
      if (field === 'voucher_id') {
        record.scope_type =
          data.vouchers.find((v) => String(v.id) === String(value))?.applies_to || 'category';
        record.scope_id = '';
      }
      return { ...previous, record };
    });
  }
  function options(field) {
    let table = field.options;
    if (field.key === 'scope_id')
      table = { category: 'categories', brand: 'brands', product: 'products' }[
        draft?.record.scope_type
      ];
    if (field.type === 'select')
      return field.options.map((value) => ({ value, label: optionLabels[value] || value }));
    return (data[table] || [])
      .filter((row) => {
        if (catalog === 'categories') return String(row.id) !== String(draft?.id);
        if (catalog === 'voucher_scopes' && field.key === 'voucher_id')
          return row.applies_to !== 'all';
        if (field.key === 'variant_id' && catalog === 'product_images')
          return String(row.product_id) === String(draft?.record.product_id);
        if (field.key === 'product_id' && config.key)
          return (
            row.product_type ===
            { racket_specs: 'racket', shoe_specs: 'shoes', apparel_specs: 'apparel' }[catalog]
          );
        return true;
      })
      .map((row) => ({ value: row.id, label: label(table, row.id) }));
  }
  const fields = (catalogListFields[catalog] || [])
    .map((key) => config.fields.find((f) => f.key === key))
    .filter((f) => f && (!Object.hasOwn(scope, f.key) || Array.isArray(scope[f.key])));
  const singleRecord = embedded && Object.hasOwn(scope, 'id') && rows.length === 1;
  const recordActions = (row) =>
    !config.readonly && (
      <div className="manager-record-actions">
        <Button
          onClick={() => {
            setDetail(null);
            open(row);
          }}
        >
          Chỉnh sửa
        </Button>
        {hasActive && (
          <Button
            variant="outline-secondary"
            disabled={busy || imageReading}
            onClick={() => {
              setDetail(null);
              perform(
                {
                  type: 'catalog_save',
                  payload: {
                    table: catalog,
                    id: row[key],
                    record: { ...row, is_active: !row.is_active },
                  },
                },
                row.is_active ? 'Đã tạm ẩn bản ghi.' : 'Đã kích hoạt bản ghi.',
              );
            }}
          >
            {row.is_active ? 'Tạm ẩn' : 'Kích hoạt'}
          </Button>
        )}
        <Button
          variant="outline-danger"
          onClick={() => {
            setDetail(null);
            setError('');
            setRemove(row);
          }}
        >
          Xóa
        </Button>
      </div>
    );
  const recordDetails = (row) => (
    <dl className="manager-detail">
      {config.fields.map((f) => (
        <div key={f.key} className={f.type === 'textarea' ? 'manager-detail-wide' : ''}>
          <dt>{f.label}</dt>
          <dd>
            {f.type === 'checkbox' ? (
              <Badge bg={row[f.key] ? 'success' : 'secondary'}>{display(f, row)}</Badge>
            ) : (
              display(f, row)
            )}
          </dd>
        </div>
      ))}
    </dl>
  );
  return (
    <section className={embedded ? 'manager-page manager-embedded' : 'manager-page'}>
      {!embedded && <div className="eyebrow">QUẢN LÝ CỬA HÀNG</div>}
      <div className="manager-title">
        <div>
          {embedded ? <h2>{config.title}</h2> : <h1>{config.title}</h1>}
          <p>{rows.length} bản ghi · Tra cứu, cập nhật và quản lý dữ liệu cửa hàng.</p>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          <Button
            variant="outline-primary"
            onClick={() =>
              exportCsv(
                catalog + '.csv',
                config.fields.map((f) => f.label),
                filtered.map((r) => config.fields.map((f) => display(f, r))),
              )
            }
          >
            Xuất CSV
          </Button>
          {!config.readonly && allowCreate && <Button onClick={() => open()}>+ Thêm mới</Button>}
        </div>
      </div>
      {notice && (
        <Alert variant="success" dismissible onClose={() => setNotice(null)}>
          {notice}
        </Alert>
      )}
      {error && !draft && !remove && <Alert variant="danger">{error}</Alert>}
      {config.readonly && (
        <Alert variant="info">
          {catalog === 'product_price_history'
            ? 'Lịch sử tự động ghi lại khi đổi giá bán của biến thể trong sản phẩm này.'
            : 'Lượt sử dụng tự động được ghi nhận khi khách đặt hàng với voucher.'}
        </Alert>
      )}
      {singleRecord ? (
        <div className="manager-panel manager-info-card">
          {recordDetails(rows[0])}
          {recordActions(rows[0])}
        </div>
      ) : (
        <div className="manager-panel manager-list-panel">
          <div className="manager-filters">
            <ManagerSearch
              label="Tìm kiếm catalog"
              placeholder="Tìm theo tên, mã hoặc thông tin liên quan…"
              value={search}
              onChange={(value) => {
                setSearch(value);
                setPage(1);
              }}
            />
            {hasActive && (
              <Form.Select
                aria-label="Lọc hoạt động"
                value={active}
                onChange={(e) => {
                  setActive(e.target.value);
                  setPage(1);
                }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="true">Đang hoạt động</option>
                <option value="false">Tạm ẩn</option>
              </Form.Select>
            )}
          </div>
          <div className="table-responsive">
            <Table hover className="align-middle manager-table">
              <thead>
                <tr>
                  {['brands', 'products', 'product_images'].includes(catalog) && <th>Hình ảnh</th>}
                  {fields.map((f) => (
                    <th key={f.key}>{f.label}</th>
                  ))}
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((row) => (
                  <tr key={row[key]}>
                    {catalog === 'brands' && (
                      <td>
                        {row.logo_url && (
                          <img className="manager-image-thumb" src={row.logo_url} alt={row.name} />
                        )}
                      </td>
                    )}
                    {catalog === 'products' && (
                      <td>
                        <img
                          className="manager-image-thumb"
                          src={productImage(data, row)}
                          onError={(event) => productImageFallback(event, row)}
                          alt={row.name}
                        />
                      </td>
                    )}
                    {catalog === 'product_images' && (
                      <td>
                        <img className="manager-image-thumb" src={row.url} alt="Ảnh sản phẩm" />
                        <small className="d-block text-secondary">Thứ tự: {row.sort_order}</small>
                      </td>
                    )}
                    {fields.map((f) => (
                      <td key={f.key} title={display(f, row)}>
                        {f.type === 'checkbox' ? (
                          <Badge bg={row[f.key] ? 'success' : 'secondary'}>{display(f, row)}</Badge>
                        ) : (
                          display(f, row)
                        )}
                      </td>
                    ))}
                    <td className="manager-action-cell">
                      <Button
                        size="sm"
                        variant="outline-primary"
                        onClick={() => (onManage ? onManage(row) : setDetail(row))}
                      >
                        {onManage ? 'Quản lý chi tiết' : 'Xem chi tiết'}{' '}
                        <span aria-hidden="true">→</span>
                      </Button>
                    </td>
                  </tr>
                ))}
                {!filtered.length && (
                  <tr>
                    <td
                      colSpan={
                        fields.length +
                        1 +
                        (['brands', 'products', 'product_images'].includes(catalog) ? 1 : 0)
                      }
                      className="text-center py-5"
                    >
                      <strong className="d-block mb-2">
                        {rows.length ? 'Không tìm thấy kết quả.' : 'Chưa có bản ghi.'}
                      </strong>
                      <span className="text-secondary">
                        {config.readonly
                          ? 'Dữ liệu sẽ xuất hiện khi phát sinh giao dịch.'
                          : rows.length
                            ? 'Thử từ khóa hoặc bộ lọc khác.'
                            : 'Nhấn “Thêm mới” để tạo bản ghi đầu tiên.'}
                      </span>
                    </td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
          <ManagerPagination
            total={filtered.length}
            page={current}
            pageSize={pageSize}
            onChange={setPage}
          />
        </div>
      )}
      <Modal
        scrollable
        className="manager-modal"
        show={!!draft}
        onHide={() => !busy && !imageReading && setDraft(null)}
        size="lg"
        centered
      >
        <Form className="manager-modal-form" onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title>
              {draft?.id ? 'Chỉnh sửa' : 'Thêm mới'} · {config.title}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <p className="small text-secondary">Các trường có dấu * là bắt buộc.</p>
            {catalog === 'product_variants' && !draft?.id && (
              <Alert variant="info">
                Tồn kho ban đầu sẽ được ghi nhận vào sổ kho. Tồn của SKU đã tạo chỉ điều chỉnh tại
                Quản lý kho.
              </Alert>
            )}
            <div className="manager-form">
              {config.fields
                .filter((f) => !Object.hasOwn(scope, f.key))
                .map((f) => {
                  const dropdown = ['ref', 'select'].includes(f.type) || f.key === 'scope_id';
                  const stockLocked =
                    catalog === 'product_variants' && f.key === 'stock_quantity' && !!draft?.id;
                  return (
                    <Form.Group controlId={'catalog-' + f.key} key={f.key}>
                      <Form.Label>
                        {f.label}
                        {required.includes(f.key) && ' *'}
                      </Form.Label>
                      {f.type === 'image' ? (
                        <ImagePicker
                          value={draft?.record[f.key]}
                          onChange={(value) => change(f.key, value)}
                          disabled={busy || imageReading}
                          required={required.includes(f.key)}
                          onBusyChange={setImageReading}
                        />
                      ) : f.type === 'checkbox' ? (
                        <Form.Check
                          label="Đang hoạt động"
                          checked={!!draft?.record[f.key]}
                          onChange={(e) => change(f.key, e.target.checked)}
                        />
                      ) : dropdown ? (
                        <Form.Select
                          required={required.includes(f.key)}
                          value={draft?.record[f.key] ?? ''}
                          disabled={f.key === key && !!draft?.id}
                          onChange={(e) => change(f.key, e.target.value)}
                        >
                          <option value="">
                            {required.includes(f.key) ? 'Chọn…' : 'Không chọn'}
                          </option>
                          {options(f).map((o) => (
                            <option key={o.value} value={o.value}>
                              {o.label}
                            </option>
                          ))}
                        </Form.Select>
                      ) : (
                        <Form.Control
                          required={required.includes(f.key)}
                          as={f.type === 'textarea' ? 'textarea' : 'input'}
                          type={
                            f.type === 'textarea' ? undefined : f.type === 'url' ? 'text' : f.type
                          }
                          min={f.type === 'number' ? 0 : undefined}
                          step={/quantity|threshold|limit|sort_order/.test(f.key) ? 1 : 'any'}
                          disabled={stockLocked}
                          value={draft?.record[f.key] ?? ''}
                          onChange={(e) =>
                            change(
                              f.key,
                              f.type === 'number' && e.target.value !== ''
                                ? Number(e.target.value)
                                : e.target.value,
                            )
                          }
                        />
                      )}
                      {stockLocked && (
                        <Form.Text className="text-muted">
                          Dùng Quản lý kho để điều chỉnh và ghi sổ biến động.
                        </Form.Text>
                      )}
                    </Form.Group>
                  );
                })}
            </div>
            {catalog === 'products' && (
              <p className="small text-secondary mt-3">
                Sau khi tạo, chọn “Quản lý chi tiết” để thêm hình ảnh, thông số, biến thể, giá bán
                và tồn kho ngay trong sản phẩm.
              </p>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" disabled={busy || imageReading} onClick={() => setDraft(null)}>
              Đóng
            </Button>
            <Button type="submit" disabled={busy || imageReading}>
              {busy ? 'Đang lưu…' : 'Lưu thay đổi'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
      <Modal
        scrollable
        className="manager-modal"
        show={!!detail}
        onHide={() => setDetail(null)}
        centered
        size="lg"
      >
        <Modal.Header closeButton>
          <Modal.Title>Chi tiết · {config.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detail && (
            <>
              {recordDetails(detail)}
              {(detail.url || detail.logo_url) && (
                <img
                  className="manager-preview"
                  src={detail.url || detail.logo_url}
                  alt="Hình ảnh sản phẩm"
                />
              )}
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          {detail && recordActions(detail)}
          <Button variant="light" onClick={() => setDetail(null)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
      <Modal
        scrollable
        className="manager-modal"
        show={!!remove}
        onHide={() => !busy && setRemove(null)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>Xóa bản ghi</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {error && <Alert variant="danger">{error}</Alert>}Xóa “
          {remove?.name || remove?.sku || remove?.code || remove?.[key]}”? Bản ghi có giao dịch hoặc
          dữ liệu đang sử dụng sẽ được bảo vệ.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="light" onClick={() => setRemove(null)}>
            Đóng
          </Button>
          <Button
            variant="danger"
            disabled={busy || imageReading}
            onClick={async () => {
              if (
                await perform(
                  { type: 'catalog_delete', payload: { table: catalog, id: remove[key] } },
                  'Đã xóa bản ghi.',
                )
              )
                setRemove(null);
            }}
          >
            Xác nhận xóa
          </Button>
        </Modal.Footer>
      </Modal>
    </section>
  );
}
