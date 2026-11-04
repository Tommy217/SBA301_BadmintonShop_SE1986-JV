export function buildInventory(data) {
  const variants = data.product_variants || [];
  const rows = (data.products || []).map((product) => {
    const items = variants.filter((v) => String(v.product_id) === String(product.id));
    return {
      ...product,
      variants: items,
      quantity: items.reduce((s, v) => s + Number(v.stock_quantity || 0), 0),
      saleValue: items.reduce(
        (s, v) => s + Number(v.stock_quantity || 0) * Number(v.price || 0),
        0,
      ),
      costValue: items.reduce(
        (s, v) => s + Number(v.stock_quantity || 0) * Number(v.cost_price || 0),
        0,
      ),
      low: items.filter(
        (v) =>
          v.is_active && v.stock_quantity > 0 && v.stock_quantity <= (v.low_stock_threshold ?? 5),
      ).length,
      out: items.filter((v) => v.is_active && v.stock_quantity === 0).length,
    };
  });
  const allocatedOrders = new Set(
    (data.orders || [])
      .filter((o) => ['pending', 'confirmed', 'preparing', 'shipping'].includes(o.status))
      .map((o) => o.id),
  );
  return {
    products: rows,
    totalProducts: rows.length,
    totalVariants: variants.length,
    totalQuantity: variants.reduce((s, v) => s + Number(v.stock_quantity || 0), 0),
    activeQuantity: variants
      .filter((v) => v.is_active)
      .reduce((s, v) => s + Number(v.stock_quantity || 0), 0),
    saleValue: variants.reduce(
      (s, v) => s + Number(v.stock_quantity || 0) * Number(v.price || 0),
      0,
    ),
    costValue: variants.reduce(
      (s, v) => s + Number(v.stock_quantity || 0) * Number(v.cost_price || 0),
      0,
    ),
    missingCost: variants.filter((v) => v.stock_quantity > 0 && !(v.cost_price > 0)).length,
    low: variants.filter(
      (v) =>
        v.is_active && v.stock_quantity > 0 && v.stock_quantity <= (v.low_stock_threshold ?? 5),
    ).length,
    out: variants.filter((v) => v.is_active && v.stock_quantity === 0).length,
    allocated:
      data.allocated_quantity ??
      (data.order_items || [])
        .filter((i) => allocatedOrders.has(i.order_id))
        .reduce((s, i) => s + Number(i.quantity), 0),
    groups: ['racket', 'shoes', 'apparel'].map((type) => ({
      type,
      products: rows.filter((p) => p.product_type === type).length,
      quantity: rows.filter((p) => p.product_type === type).reduce((s, p) => s + p.quantity, 0),
    })),
  };
}
