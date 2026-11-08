import { httpClient } from '@/shared/api/httpClient';
import { sendShopAction } from '@/app/providers/shop/shop.api';

export const paymentApi = {
  getShopData: (config) => httpClient.get('/shop/mine', config),
  startPayment: (payload) => sendShopAction({ type: 'start_payment', ...payload }),
  syncPaymentByOrder: (orderId) => httpClient.get(`/payments/order/${encodeURIComponent(orderId)}/sync`),
  getPaymentTimeout: () => httpClient.get('/payments/settings/timeout'),
  updatePaymentTimeout: (timeoutMinutes) =>
    sendShopAction({ type: 'update_payment_timeout', timeout_minutes: timeoutMinutes }),
};
