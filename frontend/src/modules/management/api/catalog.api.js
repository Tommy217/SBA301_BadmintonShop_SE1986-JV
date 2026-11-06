import { getRoles } from '@/shared/auth/session';
import { httpClient } from '@/shared/api/httpClient';

const backendUrl = import.meta.env.VITE_BACKEND_URL?.replace(/\/+$/, '');
const catalogBaseURL =
  import.meta.env.VITE_CATALOG_API_URL ||
  (backendUrl ? backendUrl.replace(/\/api$/, '') + '/api' : httpClient.defaults.baseURL);

export function resolveImageUrl(url) {
  if (!url?.trim()) return '';
  const value = url.trim();
  if (/^data:image\/(png|jpeg|gif|webp);base64,/i.test(value)) return value;
  if (/^https?:\/\//i.test(value) || /^\/products\/[^/]+\.svg$/.test(value)) return value;
  const base = new URL(catalogBaseURL, window.location.origin);
  return new URL(value, base.origin + '/').href;
}

const catalogHttp = {
  get: (url, config) => httpClient.get(url, { ...config, baseURL: catalogBaseURL }),
  post: (url, data, config) => httpClient.post(url, data, { ...config, baseURL: catalogBaseURL }),
  put: (url, data, config) => httpClient.put(url, data, { ...config, baseURL: catalogBaseURL }),
  delete: (url, config) => httpClient.delete(url, { ...config, baseURL: catalogBaseURL }),
};

// Danh sách đường dẫn API tương ứng với từng bảng Catalog
export const catalogEndpoints = {
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
};

// API Catalog dùng chung cho giao diện quản lý và hiển thị
export const catalogApi = {
  // 1. Lấy toàn bộ dữ liệu Catalog từ Backend Java
  async list(config) {
    const tables = Object.keys(catalogEndpoints);
    const management = getRoles().some((role) => ['ADMIN', 'MANAGER', 'STAFF'].includes(role));
    const requests = tables.map((table) =>
      table === 'voucher_usages' && !management
        ? Promise.resolve([])
        : catalogHttp.get(catalogEndpoints[table], config),
    );
    const responses = await Promise.all(requests);

    const data = {};
    tables.forEach((table, index) => {
      data[table] = responses[index] || [];
      if (table === 'brands')
        data[table] = data[table].map((row) => ({
          ...row,
          logo_url: resolveImageUrl(row.logo_url),
        }));
      if (table === 'product_images')
        data[table] = data[table].map((row) => ({ ...row, url: resolveImageUrl(row.url) }));
    });
    return data;
  },

  // 2. Thêm mới hoặc cập nhật một bản ghi
  save({ table, id, record }) {
    const endpoint = catalogEndpoints[table];
    if (!endpoint) throw new Error(`Không tìm thấy bảng: ${table}`);

    // Nếu có id thì gọi PUT để cập nhật, không có id thì gọi POST để thêm mới
    if (id) {
      return catalogHttp.put(`${endpoint}/${id}`, record);
    }
    return catalogHttp.post(endpoint, record);
  },

  // 3. Xóa một bản ghi theo id
  remove({ table, id }) {
    const endpoint = catalogEndpoints[table];
    if (!endpoint) throw new Error(`Không tìm thấy bảng: ${table}`);

    return catalogHttp.delete(`${endpoint}/${id}`);
  },
};

// API chi tiết theo từng thực thể (viết rõ ràng, dễ gọi riêng lẻ)
export const brandApi = {
  getAll: () => catalogHttp.get('/brands'),
  getById: (id) => catalogHttp.get(`/brands/${id}`),
  create: (data) => catalogHttp.post('/brands', data),
  update: (id, data) => catalogHttp.put(`/brands/${id}`, data),
  delete: (id) => catalogHttp.delete(`/brands/${id}`),
};

export const categoryApi = {
  getAll: () => catalogHttp.get('/categories'),
  getById: (id) => catalogHttp.get(`/categories/${id}`),
  create: (data) => catalogHttp.post('/categories', data),
  update: (id, data) => catalogHttp.put(`/categories/${id}`, data),
  delete: (id) => catalogHttp.delete(`/categories/${id}`),
};

export const productApi = {
  getPage: (params, config) => catalogHttp.get('/products/page', { ...config, params }),
  getAll: () => catalogHttp.get('/products'),
  getById: (id) => catalogHttp.get(`/products/${id}`),
  create: (data) => catalogHttp.post('/products', data),
  update: (id, data) => catalogHttp.put(`/products/${id}`, data),
  delete: (id) => catalogHttp.delete(`/products/${id}`),
};

export const voucherApi = {
  getAll: () => catalogHttp.get('/vouchers'),
  getById: (id) => catalogHttp.get(`/vouchers/${id}`),
  create: (data) => catalogHttp.post('/vouchers', data),
  update: (id, data) => catalogHttp.put(`/vouchers/${id}`, data),
  delete: (id) => catalogHttp.delete(`/vouchers/${id}`),
};
