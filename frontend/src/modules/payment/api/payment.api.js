import { httpClient } from '@/shared/api/httpClient';
import { sendShopAction } from '@/app/providers/shop/shop.api';

export const paymentApi = {
  getShopData: (config) => httpClient.get('/shop/mine', config),
  startPayment: (payload) => sendShopAction({ type: 'start_payment', ...payload }),
  syncPaymentByOrder: (orderId) => httpClient.get(`/payments/order/${encodeURIComponent(orderId)}/sync`),
};
