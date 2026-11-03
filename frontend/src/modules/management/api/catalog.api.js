import axios from 'axios';
import { AUTH_EXPIRED_EVENT, clearSession, getToken } from '@/shared/auth/session';

// Direct connection to Spring Boot Java backend on port 8080
const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
export const CATALOG_API_BASE_URL = `${BACKEND_URL}/api`;

export const catalogClient = axios.create({
  baseURL: CATALOG_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

catalogClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

catalogClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (axios.isCancel(error)) return Promise.reject(error);
    const status = error.response?.status;
    if (status === 401 && error.config?.headers?.Authorization) {
      clearSession();
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    }
    const message = error.response
      ? error.response.data?.message || 'Yêu cầu không thành công. Vui lòng thử lại.'
      : 'Không kết nối được server Java (8080). Hãy khởi động backend Spring Boot.';
    return Promise.reject(Object.assign(new Error(message), { status, cause: error }));
  },
);

// Each collection belongs to one of the four Java resource controllers.
export const catalogEndpoints = Object.freeze({
  brands: '/brands',
  categories: '/categories',
  products: '/products',
  product_variants: '/products/variants',
  product_images: '/products/images',
  racket_specs: '/products/racket-specs',
  shoe_specs: '/products/shoe-specs',
  apparel_specs: '/products/apparel-specs',
  product_price_history: '/products/price-history',
  vouchers: '/vouchers',
  voucher_scopes: '/vouchers/scopes',
  voucher_usages: '/vouchers/usages',
});

function endpoint(table, writable = false) {
  if (!Object.hasOwn(catalogEndpoints, table)) throw new Error('Catalog không hợp lệ.');
  if (writable && ['product_price_history', 'voucher_usages'].includes(table))
    throw new Error('Dữ liệu lịch sử chỉ cho phép đọc.');
  return catalogEndpoints[table];
}

// Assemble the same UI shape directly from the Spring Boot backend on port 8080.
export const catalogApi = {
  list: async (config) => Object.fromEntries(await Promise.all(
    Object.entries(catalogEndpoints).map(async ([table, url]) => [table, await catalogClient.get(url, config)]),
  )),
  save: ({ table, id, record }) =>
    id == null
      ? catalogClient.post(endpoint(table, true), record)
      : catalogClient.put(`${endpoint(table, true)}/${encodeURIComponent(id)}`, record),
  remove: ({ table, id }) => catalogClient.delete(`${endpoint(table, true)}/${encodeURIComponent(id)}`),
};
