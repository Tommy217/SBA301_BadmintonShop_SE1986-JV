import { randomUUID } from 'node:crypto';
import { catalogs } from '../../src/modules/management/constants.js';
import { recordInitialStock } from './inventory.mjs';
const check = (ok, message) => {
  if (!ok) throw new Error(message);
};
export function catalogView(db) {
  return Object.fromEntries(Object.keys(catalogs).map((k) => [k, db[k] || []]));
}
export function saveCatalog(db, action, actor) {
  const config = catalogs[action.table];
  check(config && !config.readonly, 'Catalog không cho phép chỉnh sửa.');
  const table = action.table,
    key = config.key || 'id';
  const rows = (db[table] ||= []);
  const previous =
    action.id == null ? null : rows.find((r) => String(r[key]) === String(action.id));
  check(action.id == null || previous, 'Bản ghi không còn tồn tại.');
  const row = { ...previous };
  for (const field of config.fields) {
    let value = action.record?.[field.key];
    if (field.type === 'checkbox') value = !!value;
    else if (value === '' || value == null) value = null;
    else if (field.type === 'number') {
      value = Number(value);
      check(Number.isFinite(value) && value >= 0, field.label + ': giá trị phải là số không âm.');
    } else {
      value = String(value).trim();
      const maxLength = field.type === 'image' ? 6990544 : 500;
      check(
        value.length <= maxLength,
        field.label + (field.type === 'image' ? ': ảnh tối đa 5 MB.' : ': tối đa 500 ký tự.'),
      );
    }
    if (field.type === 'select')
      check(field.options.includes(value), field.label + ': chọn giá trị hợp lệ.');
    if (field.type === 'ref' && value != null)
      check(
        (db[field.options] || []).some((r) => String(r.id) === value),
        field.label + ': dữ liệu tham chiếu không tồn tại.',
      );
    if (field.type === 'url' && value)
      check(
        /^(https?:\/\/|\/(?!\/))/i.test(value),
        field.label + ': cần URL http, https hoặc đường dẫn ảnh trong cửa hàng.',
      );
    row[field.key] = value;
  }
  const required = {
    brands: ['name', 'slug'],
    categories: ['name', 'slug'],
    products: ['name', 'slug', 'category_id', 'brand_id', 'base_price'],
    racket_specs: ['product_id'],
    shoe_specs: ['product_id'],
    apparel_specs: ['product_id'],
    product_variants: [
      'product_id',
      'sku',
      'price',
      'cost_price',
      'stock_quantity',
      'low_stock_threshold',
    ],
    product_images: ['product_id', 'url', 'sort_order'],
    vouchers: [
      'code',
      'name',
      'discount_value',
      'min_order',
      'per_user_limit',
      'starts_at',
      'ends_at',
    ],
    voucher_scopes: ['voucher_id', 'scope_id'],
  };
  for (const field of required[table] || [])
    check(
      row[field] != null && row[field] !== '',
      `Vui lòng nhập ${config.fields.find((f) => f.key === field)?.label || field}.`,
    );
  for (const field of ['slug', 'sku', 'code', ...(table === 'brands' ? ['name'] : [])])
    if (row[field] != null)
      check(
        !rows.some(
          (r) =>
            r !== previous && String(r[field]).toLowerCase() === String(row[field]).toLowerCase(),
        ),
        `${field} đã tồn tại.`,
      );
  if (table === 'categories') {
    const visited = new Set([String(previous?.id)]);
    let parent = row.parent_id;
    while (parent != null) {
      check(!visited.has(String(parent)), 'Danh mục cha tạo thành vòng lặp.');
      visited.add(String(parent));
      parent = rows.find((r) => String(r.id) === String(parent))?.parent_id;
    }
  }
  if (config.key && previous)
    check(
      String(row.product_id) === String(previous.product_id),
      'Không thể đổi sản phẩm của bộ thông số đã tạo.',
    );
  if (config.key) {
    check(
      !rows.some((r) => r !== previous && String(r.product_id) === String(row.product_id)),
      'Sản phẩm đã có thông số.',
    );
    const type = { racket_specs: 'racket', shoe_specs: 'shoes', apparel_specs: 'apparel' }[table];
    check(
      db.products.find((p) => String(p.id) === row.product_id)?.product_type === type,
      'Loại sản phẩm không phù hợp với thông số.',
    );
  }
  if (table === 'products' && previous && previous.product_type !== row.product_type)
    check(
      !['racket_specs', 'shoe_specs', 'apparel_specs'].some((t) =>
        (db[t] || []).some((r) => String(r.product_id) === String(previous.id)),
      ),
      'Cần xử lý thông số hiện có trước khi đổi loại sản phẩm.',
    );
  if (table === 'product_variants') {
    for (const f of ['stock_quantity', 'low_stock_threshold'])
      check(Number.isInteger(row[f]), 'Số lượng và ngưỡng tồn kho phải là số nguyên.');
    if (previous)
      check(
        Number(row.stock_quantity) === Number(previous.stock_quantity),
        'Tồn kho chỉ được thay đổi tại trang Quản lý kho để ghi nhận sổ biến động.',
      );
    check(
      !rows.some(
        (r) =>
          r !== previous &&
          String(r.product_id) === row.product_id &&
          (r.color || null) === row.color &&
          (r.size || null) === row.size,
      ),
      'Biến thể màu/kích cỡ đã tồn tại.',
    );
  }
  if (table === 'product_images' && row.variant_id)
    check(
      String(db.product_variants.find((v) => String(v.id) === row.variant_id)?.product_id) ===
        row.product_id,
      'Biến thể không thuộc sản phẩm đã chọn.',
    );
  if (table === 'vouchers') {
    check(
      Number.isFinite(Date.parse(row.starts_at)) &&
        Number.isFinite(Date.parse(row.ends_at)) &&
        new Date(row.ends_at) > new Date(row.starts_at),
      'Ngày kết thúc phải sau ngày bắt đầu.',
    );
    check(
      row.discount_value > 0 && (row.discount_type !== 'percent' || row.discount_value <= 100),
      'Mức giảm không hợp lệ.',
    );
    check(
      Number.isInteger(row.per_user_limit) && row.per_user_limit >= 1,
      'Giới hạn mỗi khách phải là số nguyên dương.',
    );
    if (row.usage_limit != null)
      check(
        Number.isInteger(row.usage_limit) && row.usage_limit >= (previous?.used_count || 0),
        'Giới hạn sử dụng không hợp lệ.',
      );
    row.used_count = previous?.used_count || 0;
    check(
      !(db.voucher_scopes || []).some(
        (s) => String(s.voucher_id) === String(previous?.id) && s.scope_type !== row.applies_to,
      ),
      'Phạm vi hiện có không khớp cách áp dụng voucher.',
    );
  }
  if (table === 'voucher_scopes') {
    const voucher = db.vouchers.find((v) => String(v.id) === row.voucher_id);
    check(voucher?.applies_to === row.scope_type, 'Loại phạm vi phải khớp voucher.');
    const target = { brand: 'brands', category: 'categories', product: 'products' }[row.scope_type];
    check(
      (db[target] || []).some((r) => String(r.id) === row.scope_id),
      'Đối tượng áp dụng không tồn tại.',
    );
    check(
      !rows.some(
        (r) =>
          r !== previous &&
          String(r.voucher_id) === row.voucher_id &&
          r.scope_type === row.scope_type &&
          String(r.scope_id) === row.scope_id,
      ),
      'Phạm vi voucher đã tồn tại.',
    );
  }
  const now = new Date().toISOString();
  if (!config.key) row.id = previous?.id || randomUUID();
  if (table === 'products') {
    row.created_at = previous?.created_at || now;
    row.updated_at = now;
    row.created_by = previous?.created_by || actor;
    row.brand = db.brands.find((b) => String(b.id) === row.brand_id)?.name;
  }
  if (table === 'brands')
    for (const p of db.products)
      if (String(p.brand_id) === String(previous?.id)) p.brand = row.name;
  if (table === 'vouchers') row.created_by = previous?.created_by || actor;
  if (table === 'product_variants' && previous && previous.price !== row.price)
    (db.product_price_history ||= []).push({
      id: randomUUID(),
      variant_id: previous.id,
      old_price: previous.price,
      new_price: row.price,
      changed_by: actor,
      changed_at: now,
    });
  if (previous) Object.assign(previous, row);
  else rows.push(row);
  if (table === 'product_variants' && !previous) recordInitialStock(db, row, actor);
}
