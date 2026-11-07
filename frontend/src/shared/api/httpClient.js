import axios from 'axios';
import { AUTH_EXPIRED_EVENT, clearSession, getToken } from '@/shared/auth/session';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15000,
});

httpClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

httpClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (axios.isCancel(error)) return Promise.reject(error);
    const status = error.response?.status;
    if (status === 401 && error.config?.headers?.Authorization) {
      clearSession();
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    }
    const timeout = ['ECONNABORTED', 'ETIMEDOUT'].includes(error.code);
    const readOnly = ['get', 'head', 'options'].includes(error.config?.method?.toLowerCase());
    const message = timeout
      ? readOnly
        ? 'Máy chủ phản hồi quá lâu. Vui lòng thử lại.'
        : 'Chưa nhận được xác nhận từ máy chủ. Hãy kiểm tra dữ liệu trước khi gửi lại.'
      : error.response
        ? error.response.data?.message ||
          ([502, 503, 504, 530].includes(status)
            ? 'Máy chủ tạm thời chưa sẵn sàng. Vui lòng thử lại sau.'
            : 'Yêu cầu không thành công. Vui lòng thử lại.')
        : 'Không kết nối được máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.';
    return Promise.reject(Object.assign(new Error(message), { status, cause: error }));
  },
);
