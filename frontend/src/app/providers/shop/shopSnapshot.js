// Catalog can load independently when the shop snapshot API is unavailable.
// Keep missing sales data distinct from a successfully loaded, empty order list.
export function mergeShopSnapshot(snapshot, catalog) {
  const data = { ...snapshot, ...catalog };
  const ordersAvailable = Array.isArray(snapshot?.orders) && Array.isArray(snapshot?.order_items);
  for (const table of [
    'carts',
    'cart_items',
    'orders',
    'order_items',
    'payments',
    'order_status_history',
    'support_tickets',
    'ticket_items',
    'deliveries',
  ]) {
    if (!Array.isArray(data[table])) data[table] = [];
  }
  return { ...data, ordersAvailable };
}
