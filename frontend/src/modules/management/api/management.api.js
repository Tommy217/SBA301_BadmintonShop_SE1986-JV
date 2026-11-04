import { sendShopAction } from '@/app/providers/shop/shop.api';

// Each function returns the new store snapshot from the mock API.
export const managerOrderApi = {
  updateOrder: (payload) => sendShopAction({ type: 'manager_update_order', ...payload }),
};

export { inventoryApi } from './inventory.api';

export const invoiceApi = {
  adjust: (payload) => sendShopAction({ type: 'invoice_adjust', ...payload }),
  deliver: (payload) => sendShopAction({ type: 'invoice_delivery', ...payload }),
  replace: (payload) => sendShopAction({ type: 'invoice_replace', ...payload }),
  cancel: (payload) => sendShopAction({ type: 'invoice_cancel', ...payload }),
};
