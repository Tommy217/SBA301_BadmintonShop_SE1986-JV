import { httpClient } from '@/shared/api/httpClient';

export const authApi = {
  me: () => httpClient.get('/auth/me'),
  logout: () => httpClient.post('/auth/logout'),
  login: (credentials) => httpClient.post('/auth/login', credentials),
  register: (data) => httpClient.post('/auth/register', data),
};
