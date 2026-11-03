import { sendShopAction } from '@/app/providers/shop/shop.api';

// Each function returns the new store snapshot from the mock API.
export const managerOrderApi = {
  updateOrder: (payload) => sendShopAction({ type: 'manager_update_order', ...payload }),
};

export const inventoryApi = {
  receiveStock: (payload) => sendShopAction({ type: 'inventory_receive', ...payload }),
  adjustStock: (payload) => sendShopAction({ type: 'inventory_adjust', ...payload }),
  startAudit: (payload) => sendShopAction({ type: 'inventory_audit_start', ...payload }),
  updateAudit: (payload) => sendShopAction({ type: 'inventory_audit_update', ...payload }),
  completeAudit: (payload) => sendShopAction({ type: 'inventory_audit_complete', ...payload }),
  cancelAudit: (payload) => sendShopAction({ type: 'inventory_audit_cancel', ...payload }),
};

export const invoiceApi = {
  adjust: (payload) => sendShopAction({ type: 'invoice_adjust', ...payload }),
  deliver: (payload) => sendShopAction({ type: 'invoice_delivery', ...payload }),
  replace: (payload) => sendShopAction({ type: 'invoice_replace', ...payload }),
  cancel: (payload) => sendShopAction({ type: 'invoice_cancel', ...payload }),
};
