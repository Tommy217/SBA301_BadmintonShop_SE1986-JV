import { sendShopAction } from '@/app/providers/shop/shop.api';

// Each function returns the latest SQL-backed snapshot.
export const managerOrderApi = {
  updateOrder: (payload) => sendShopAction({ type: 'manager_update_order', ...payload }),
  createShipment: (orderId) => sendShopAction({ type: 'create_shipment', order_id: orderId }),
};

export { inventoryApi } from './inventory.api';

export { invoiceApi } from './invoice.api';
