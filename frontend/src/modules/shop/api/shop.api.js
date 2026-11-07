import { sendShopAction } from '@/app/providers/shop/shop.api';

// Each function persists to SQL Server and returns the scoped store snapshot.
export const cartApi = {
  addItem: (variantId, quantity = 1) => sendShopAction({ type: 'add', variant_id: variantId, quantity }),
  updateItem: (payload) => sendShopAction({ type: 'update', ...payload }),
  removeItem: (itemId) => sendShopAction({ type: 'remove', id: itemId }),
  selectAll: (selected) => sendShopAction({ type: 'select_all', selected }),
};

export const orderApi = {
  checkout: (payload) => sendShopAction({ type: 'checkout', ...payload }),
};
