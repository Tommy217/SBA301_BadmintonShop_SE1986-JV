import axios from 'axios';
import { AUTH_EXPIRED_EVENT, clearSession, getToken, getSession, saveSession } from '@/shared/auth/session';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15000,
  withCredentials: true,
});

let refreshRequest;
export function refreshSession() {
  if (!refreshRequest) {
    const previous = getSession();
    refreshRequest = axios.post(httpClient.defaults.baseURL + '/auth/refresh', null, {
      withCredentials: true, timeout: 15000, headers: { 'X-Refresh-Request': '1' },
    }).then(({ data }) => {
      if (getSession() && getToken() === getToken(previous)) {
        saveSession({ ...getSession(), ...data });
        window.dispatchEvent(new Event('auth-refreshed'));
      }
      return data;
    }).catch((error) => {
      if (error.response?.status === 401) {
        clearSession();
        window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
      }
      throw error;
    }).finally(() => { refreshRequest = null; });
  }
  return refreshRequest;
}

httpClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

httpClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    if (axios.isCancel(error)) return Promise.reject(error);
    const status = error.response?.status;
    const authAction = /\/auth\/(login|register|logout|refresh|forgot-password|reset-password)/.test(error.config?.url || '');
    if (status === 401 && !authAction && !error.config?._retried && getSession()) {
      try {
        await refreshSession();
      } catch (refreshError) {
        return Promise.reject(Object.assign(new Error('Không thể làm mới phiên đăng nhập. Vui lòng thử lại.'), {
          status: refreshError.response?.status, cause: refreshError,
        }));
      }
      if (getSession()) return httpClient({ ...error.config, _retried: true });
    }
    if (status === 401 && !authAction) {
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
