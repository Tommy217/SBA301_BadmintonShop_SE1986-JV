import { httpClient } from '@/shared/api/httpClient';

const catalogBaseURL = import.meta.env.VITE_BACKEND_URL || httpClient.defaults.baseURL;

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
    const requests = tables.map((table) => catalogHttp.get(catalogEndpoints[table], config));
    const responses = await Promise.all(requests);

    const data = {};
    tables.forEach((table, index) => {
      data[table] = responses[index] || [];
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
