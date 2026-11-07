import { useAsync } from '@/shared/hooks/useAsync';
import { deliveryApi } from '@/modules/delivery/api/delivery.api';
import { toDelivery, toDeliveries } from '@/modules/delivery/api/delivery.mapper';

// Trang quan ly loc / dem tren client nen lay 1 trang toi da (100 van don moi nhat).
export function useShipments(params = {}) {
  const key = JSON.stringify(params);
  return useAsync(
    () =>
      deliveryApi.list({ page: 0, size: 100, ...params }).then((page) => ({
        ...page,
        content: toDeliveries(page?.content),
      })),
    [key],
  );
}

export function useShipment(id) {
  return useAsync(() => deliveryApi.get(id).then(toDelivery), [id]);
}

export function useCarriers() {
  return useAsync(() => deliveryApi.carriers(), []);
}

// Khach hang: van don moi nhat cua 1 don (null neu chua co)
export function useOrderShipment(orderId, enabled = true) {
  return useAsync(
    () => (enabled ? deliveryApi.orderShipment(orderId).then(toDelivery) : Promise.resolve(null)),
    [orderId, enabled],
  );
}

// Khach hang: van don cua tat ca don cua minh
export function useMyShipments(enabled = true) {
  return useAsync(
    () => (enabled ? deliveryApi.myShipments().then(toDeliveries) : Promise.resolve([])),
    [enabled],
  );
}
