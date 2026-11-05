import { httpClient } from '@/shared/api/httpClient';
import { catalogApi } from '@/modules/management/api/catalog.api';

// Fetches the store snapshot: Carts and orders from mock server, Catalog (brands, categories, products, vouchers)
// directly from the Java backend at http://localhost:8080.
export async function fetchShopData(config) {
  const [shopSnapshot, catalogData] = await Promise.all([
    httpClient.get('/shop', config).catch(() => ({})),
    catalogApi.list(config),
  ]);
  return { ...shopSnapshot, ...catalogData };
}

// Dispatches shop actions (e.g. cart modifications) and merges latest catalog from Java backend (8080).
export async function sendShopAction(action) {
  const [shopSnapshot, catalogData] = await Promise.all([
    httpClient.post('/shop', action),
    catalogApi.list(),
  ]);
  return { ...shopSnapshot, ...catalogData };
}
