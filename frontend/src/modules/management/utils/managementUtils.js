export const requiredFields = {
  brands: ['name', 'slug'],
  categories: ['name', 'slug'],
  products: ['category_id', 'brand_id', 'product_type', 'name', 'slug', 'base_price'],
  racket_specs: ['product_id', 'balance_type', 'flex', 'play_style'],
  shoe_specs: ['product_id', 'gender', 'foot_width'],
  apparel_specs: ['product_id', 'gender', 'apparel_kind'],
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
    'discount_type',
    'discount_value',
    'min_order',
    'per_user_limit',
    'applies_to',
    'starts_at',
    'ends_at',
  ],
  voucher_scopes: ['voucher_id', 'scope_type', 'scope_id'],
};
export const optionLabels = {
  racket: 'Vợt cầu lông',
  shoes: 'Giày cầu lông',
  apparel: 'Trang phục',
  head_heavy: 'Nặng đầu',
  even: 'Cân bằng',
  head_light: 'Nhẹ đầu',
  stiff: 'Cứng',
  medium: 'Trung bình',
  flexible: 'Dẻo',
  attack: 'Tấn công',
  defense: 'Phòng thủ',
  all_round: 'Toàn diện',
  male: 'Nam',
  female: 'Nữ',
  unisex: 'Nam và nữ',
  kids: 'Trẻ em',
  narrow: 'Hẹp',
  regular: 'Tiêu chuẩn',
  wide: 'Rộng',
  shirt: 'Áo',
  shorts: 'Quần short',
  skirt: 'Váy',
  dress: 'Đầm',
  jacket: 'Áo khoác',
  socks: 'Vớ',
  other: 'Khác',
  percent: 'Phần trăm (%)',
  fixed: 'Số tiền (VND)',
  all: 'Toàn bộ sản phẩm',
  category: 'Danh mục',
  brand: 'Thương hiệu',
  product: 'Sản phẩm',
};
export const orderTransitions = {
  pending: ['confirmed', 'cancelled'],
  confirmed: ['preparing', 'cancelled'],
  preparing: ['cancelled'],
  shipping: [],
  completed: ['returned'],
  cancelled: [],
  returned: [],
};
export function slugify(value) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/gi, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}
export function localDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' });
}
export function dateTimeInput(value) {
  if (!value) return '';
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}
export function exportCsv(filename, headers, rows) {
  const quote = (value) =>
    '"' +
    String(value ?? '')
      .replace(/^[=+@-]/, "'$&")
      .replace(/"/g, '""') +
    '"';
  const csv = '\ufeff' + [headers, ...rows].map((row) => row.map(quote).join(',')).join('\r\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export function createCatalogDefaults() {
  const now = new Date();
  return {
    cost_price: 0,
    stock_quantity: 0,
    weight_grams: 0,
    low_stock_threshold: 5,
    sort_order: 0,
    min_order: 0,
    per_user_limit: 1,
    starts_at: dateTimeInput(now),
    ends_at: dateTimeInput(new Date(now.getTime() + 30 * 86400000)),
  };
}
