import { httpClient } from '@/shared/api/httpClient';
import { sendShopAction } from '@/app/providers/shop/shop.api';

export const paymentApi = {
  getShopData: (config) => httpClient.get('/shop/mine', config),
  startPayment: (payload) => sendShopAction({ type: 'start_payment', ...payload }),
  cancelPayment: (payload) => sendShopAction({ type: 'cancel_payment', ...payload }),
  cancelPaymentByOrder: (orderId, reason) =>
    httpClient.post(`/payments/order/${encodeURIComponent(orderId)}/cancel`, {
      cancellationReason: reason || 'Khách hàng chủ động hủy thanh toán',
    }),
  cancelPaymentById: (paymentId, reason) =>
    httpClient.post(`/payments/${encodeURIComponent(paymentId)}/cancel`, {
      cancellationReason: reason || 'Khách hàng chủ động hủy thanh toán',
    }),
  syncPaymentByOrder: (orderId) => httpClient.get(`/payments/order/${encodeURIComponent(orderId)}/sync`),
  getPaymentTimeout: () => httpClient.get('/payments/settings/timeout'),
  updatePaymentTimeout: (timeoutMinutes) =>
    sendShopAction({ type: 'update_payment_timeout', timeout_minutes: timeoutMinutes }),
};
