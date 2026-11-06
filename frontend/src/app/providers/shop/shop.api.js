import { httpClient } from '@/shared/api/httpClient';
import { catalogApi } from '@/modules/management/api/catalog.api';
import { mergeShopSnapshot } from './shopSnapshot';
import { commitShopAction } from './shopActions';

// Both the scoped store snapshot and catalog are served by Spring Boot + SQL Server.
export async function fetchShopData(config) {
  const [shopSnapshot, catalogData] = await Promise.all([
    httpClient.get('/shop', config),
    catalogApi.list(config),
  ]);
  return { ...mergeShopSnapshot(shopSnapshot, catalogData), catalogRefreshFailed: false };
}

// Read the catalog after committing the action so stock reflects the saved order.
export async function sendShopAction(action) {
  return commitShopAction(
    () => httpClient.post('/shop', action),
    () => catalogApi.list(),
  );
}
