export function createCommerceActionSender(httpClient) {
  return function dispatchAction(action) {
    const config = { timeout: 30000 };
    const orderId = encodeURIComponent(action.order_id ?? action.id);
    switch (action.type) {
      case 'add':
        return httpClient.post('/cart/items', action, config);
      case 'update':
        return httpClient.patch(`/cart/items/${encodeURIComponent(action.id)}`, action, config);
      case 'remove':
        return httpClient.delete(`/cart/items/${encodeURIComponent(action.id)}`, config);
      case 'select_all':
        return httpClient.patch('/cart/selection', action, config);
      case 'checkout':
        return httpClient.post('/orders', action, config);
      case 'cancel_order':
        return httpClient.post(`/orders/${orderId}/cancel`, action, config);
      case 'manager_update_order':
        return httpClient.patch(`/orders/${orderId}/status`, action, config);
      case 'start_payment':
        return httpClient.post(`/orders/${orderId}/payment`, action, config);
      case 'cancel_payment':
        return httpClient.post(`/orders/${orderId}/payment/cancel`, action, config);
      default:
        return httpClient.post('/support/actions', action, config);
    }
  };
}
