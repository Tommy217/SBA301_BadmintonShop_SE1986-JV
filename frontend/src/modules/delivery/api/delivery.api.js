import { httpClient } from '@/shared/api/httpClient';

// REST cua module delivery (Spring Boot). httpClient tra ve ApiResponse { code, message, data }.
const data = (response) => response.data;

export const deliveryApi = {
  // manager / staff
  list: (params, config) => httpClient.get('/manager/shipments', { ...config, params }).then(data),
  get: (id, config) => httpClient.get(`/manager/shipments/${id}`, config).then(data),
  create: (payload) => httpClient.post('/manager/shipments', payload),
  resend: (id) => httpClient.post(`/manager/shipments/${id}/resend`).then(data),
  cancel: (id, reason) => httpClient.post(`/manager/shipments/${id}/cancel`, { reason }).then(data),
  sync: (id) => httpClient.post(`/manager/shipments/${id}/sync`).then(data),
  carriers: (config) => httpClient.get('/carriers', config).then(data),

  // khach hang
  orderShipment: (orderId, config) =>
    httpClient.get(`/orders/${orderId}/shipment`, config).then(data),
  syncOrderShipment: (orderId) => httpClient.post(`/orders/${orderId}/shipment/sync`).then(data),
  myShipments: (config) => httpClient.get('/my/shipments', config).then(data),
};
