// Only the essential fields are shown in lists. Full records remain in details/edit forms.
export const catalogListFields = {
  brands: ['name', 'is_active'],
  categories: ['name', 'parent_id', 'is_active'],
  products: ['name', 'product_type', 'base_price', 'is_active'],
  product_variants: ['sku', 'size', 'price', 'stock_quantity'],
  product_images: ['variant_id', 'sort_order'],
  racket_specs: ['balance_type', 'flex', 'play_style'],
  shoe_specs: ['gender', 'sole_type', 'foot_width'],
  apparel_specs: ['gender', 'apparel_kind', 'material'],
  product_price_history: ['variant_id', 'old_price', 'new_price', 'changed_at'],
  vouchers: ['code', 'name', 'audience', 'discount_value', 'is_active'],
  voucher_scopes: ['scope_type', 'scope_id'],
  voucher_usages: ['order_id', 'discount_amount', 'used_at'],
};
