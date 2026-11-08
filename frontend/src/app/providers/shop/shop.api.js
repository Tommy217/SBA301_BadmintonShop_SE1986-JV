import { httpClient } from '@/shared/api/httpClient';
import { catalogApi } from '@/modules/management/api/catalog.api';
import { mergeShopSnapshot } from './shopSnapshot';
import { commitShopAction } from './shopActions';
import { createCommerceActionSender } from './commerceTransport';

const dispatchAction = createCommerceActionSender(httpClient);

// Both the scoped store snapshot and catalog are served by Spring Boot + SQL Server.
export async function fetchShopData(config) {
  const [orderSnapshot, cartSnapshot, catalogData] = await Promise.all([
    httpClient.get('/orders', config),
    httpClient.get('/cart', config),
    catalogApi.list(config, 'shop'),
  ]);
  return {
    ...mergeShopSnapshot({ ...orderSnapshot, ...cartSnapshot }, catalogData),
    catalogRefreshFailed: false,
  };
}

// Read the catalog after committing the action so stock reflects the saved order.
export async function sendShopAction(action) {
  return commitShopAction(
    () => dispatchAction(action),
    () => catalogApi.list(undefined, 'shop'),
  );
}

// Refresh scoped order/payment data without downloading the catalog again.
export const fetchOrderData = (config) => httpClient.get('/orders', config);
