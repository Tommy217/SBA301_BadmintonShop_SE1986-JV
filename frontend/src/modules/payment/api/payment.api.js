import { httpClient } from '@/shared/api/httpClient';
import { fetchShopData, sendShopAction } from '@/app/providers/shop/shop.api';

export const paymentApi = {
  getShopData: (config) => fetchShopData(config),
  startPayment: (payload) => sendShopAction({ type: 'start_payment', ...payload }),
  syncPaymentByOrder: (orderId) => httpClient.get(`/payments/order/${encodeURIComponent(orderId)}/sync`),
};
