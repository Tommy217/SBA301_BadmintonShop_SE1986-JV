import { sendShopAction } from '@/app/providers/shop/shop.api';

// Each function returns the latest SQL-backed snapshot.
export const managerOrderApi = {
  updateOrder: (payload) => sendShopAction({ type: 'manager_update_order', ...payload }),
};

export { inventoryApi } from './inventory.api';

export { invoiceApi } from './invoice.api';
