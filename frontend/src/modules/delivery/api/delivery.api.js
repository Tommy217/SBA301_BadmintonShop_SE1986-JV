import { sendShopAction } from '@/app/providers/shop/shop.api';

// Ask Java to read the current status from the carrier backend, without advancing it.
export const deliveryApi = {
  syncCarrierStatus: (payload) => sendShopAction({ type: 'sync_carrier_status', ...payload }),
};
