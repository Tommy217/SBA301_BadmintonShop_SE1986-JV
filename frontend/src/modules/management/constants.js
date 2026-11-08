const f = (key, label, type = 'text', options) => ({ key, label, type, options });
const active = f('is_active', 'Đang hoạt động', 'checkbox');
const ref = (key, label, table) => f(key, label, 'ref', table);
export const catalogs = {
  brands: {
    title: 'Thương hiệu',
    fields: [
      f('name', 'Tên thương hiệu'),
      f('slug', 'Slug'),
      f('logo_url', 'Logo thương hiệu', 'image'),
      active,
    ],
  },
  categories: {
    title: 'Danh mục',
    fields: [
      ref('parent_id', 'Danh mục cha', 'categories'),
      f('name', 'Tên danh mục'),
      f('slug', 'Slug'),
      active,
    ],
  },
  products: {
    title: 'Sản phẩm',
    fields: [
      ref('category_id', 'Danh mục', 'categories'),
      ref('brand_id', 'Thương hiệu', 'brands'),
      f('product_type', 'Loại sản phẩm', 'select', ['racket', 'shoes', 'apparel']),
      f('name', 'Tên sản phẩm'),
      f('slug', 'Slug chia sẻ'),
      f('description', 'Mô tả', 'textarea'),
      f('base_price', 'Giá cơ bản', 'number'),
      active,
    ],
  },
  racket_specs: {
    title: 'Thông số vợt',
    key: 'product_id',
    fields: [
      ref('product_id', 'Sản phẩm vợt', 'products'),
      f('balance_type', 'Cân bằng', 'select', ['head_heavy', 'even', 'head_light']),
      f('flex', 'Độ cứng', 'select', ['stiff', 'medium', 'flexible']),
      f('play_style', 'Lối chơi', 'select', ['attack', 'defense', 'all_round']),
      f('max_tension_lbs', 'Sức căng tối đa (lbs)', 'number'),
      f('material', 'Chất liệu'),
      f('shaft_diameter', 'Đường kính đũa', 'number'),
    ],
  },
  shoe_specs: {
    title: 'Thông số giày',
    key: 'product_id',
    fields: [
      ref('product_id', 'Sản phẩm giày', 'products'),
      f('gender', 'Giới tính', 'select', ['male', 'female', 'unisex']),
      f('sole_type', 'Loại đế'),
      f('technology', 'Công nghệ'),
      f('foot_width', 'Độ rộng', 'select', ['narrow', 'regular', 'wide']),
    ],
  },
  apparel_specs: {
    title: 'Thông số trang phục',
    key: 'product_id',
    fields: [
      ref('product_id', 'Trang phục', 'products'),
      f('gender', 'Giới tính', 'select', ['male', 'female', 'unisex', 'kids']),
      f('apparel_kind', 'Loại trang phục', 'select', [
        'shirt',
        'shorts',
        'skirt',
        'dress',
        'jacket',
        'socks',
        'other',
      ]),
      f('material', 'Chất liệu'),
    ],
  },
  product_variants: {
    title: 'Biến thể & tồn kho',
    fields: [
      ref('product_id', 'Sản phẩm', 'products'),
      f('sku', 'SKU'),
      f('barcode', 'Mã vạch'),
      f('color', 'Màu'),
      f('size', 'Kích cỡ'),
      f('price', 'Giá bán', 'number'),
      f('cost_price', 'Giá vốn', 'number'),
      f('stock_quantity', 'Tồn kho', 'number'),
      f('low_stock_threshold', 'Ngưỡng cảnh báo', 'number'),
      f('weight_grams', 'Trọng lượng (gram)', 'number'),
      active,
    ],
  },
  product_images: {
    title: 'Hình ảnh sản phẩm',
    fields: [
      ref('product_id', 'Sản phẩm', 'products'),
      ref('variant_id', 'Biến thể (tùy chọn)', 'product_variants'),
      f('url', 'Hình ảnh', 'image'),
      f('sort_order', 'Thứ tự', 'number'),
    ],
  },
  product_price_history: {
    title: 'Lịch sử giá',
    readonly: true,
    fields: [
      ref('variant_id', 'Biến thể', 'product_variants'),
      f('old_price', 'Giá cũ', 'number'),
      f('new_price', 'Giá mới', 'number'),
      f('changed_by', 'Người thay đổi'),
      f('changed_at', 'Thời điểm'),
    ],
  },
  vouchers: {
    title: 'Voucher',
    fields: [
      f('code', 'Mã voucher'),
      f('audience', 'Đối tượng sử dụng', 'select', ['public', 'private']),
      f('name', 'Tên voucher'),
      f('description', 'Mô tả', 'textarea'),
      f('discount_type', 'Loại giảm', 'select', ['percent', 'fixed']),
      f('discount_value', 'Mức giảm', 'number'),
      f('min_order', 'Đơn tối thiểu', 'number'),
      f('max_discount', 'Giảm tối đa', 'number'),
      f('usage_limit', 'Giới hạn sử dụng', 'number'),
      f('per_user_limit', 'Giới hạn mỗi khách', 'number'),
      f('applies_to', 'Áp dụng cho', 'select', ['all', 'category', 'brand', 'product']),
      f('starts_at', 'Bắt đầu', 'datetime-local'),
      f('ends_at', 'Kết thúc', 'datetime-local'),
      active,
    ],
  },
  voucher_scopes: {
    title: 'Phạm vi voucher',
    fields: [
      ref('voucher_id', 'Voucher', 'vouchers'),
      f('scope_type', 'Loại phạm vi', 'select', ['category', 'brand', 'product']),
      f('scope_id', 'ID đối tượng áp dụng'),
    ],
  },
  voucher_usages: {
    title: 'Lượt sử dụng voucher',
    readonly: true,
    fields: [
      ref('voucher_id', 'Voucher', 'vouchers'),
      f('user_id', 'Khách hàng'),
      ref('order_id', 'Đơn hàng', 'orders'),
      f('discount_amount', 'Tiền giảm', 'number'),
      f('used_at', 'Thời điểm'),
    ],
  },
};
export const statuses = {
  pending: 'Chờ xác nhận',
  confirmed: 'Đã xác nhận',
  preparing: 'Đang chuẩn bị',
  shipping: 'Đang giao',
  completed: 'Hoàn tất',
  cancelled: 'Đã hủy',
  returned: 'Đã trả hàng',
};
export const payments = {
  unpaid: 'Chưa thanh toán',
  paid: 'Đã thanh toán',
  partially_refunded: 'Hoàn tiền một phần',
  refunded: 'Đã hoàn tiền',
};
