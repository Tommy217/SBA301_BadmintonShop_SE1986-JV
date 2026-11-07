import { httpClient } from '@/shared/api/httpClient';

export const authApi = {
  me: () => httpClient.get('/auth/me'),
  logout: () => httpClient.post('/auth/logout'),
  login: (credentials) => httpClient.post('/auth/login', credentials),
  register: (data) => httpClient.post('/auth/register', data),
};

export const profileApi = {
  getProfile: () => httpClient.get('/v1/profile'),
  updateProfile: (data) => httpClient.put('/v1/profile', data),
  changePassword: (data) => httpClient.post('/v1/profile/change-password', data),
  getAddresses: () => httpClient.get('/v1/profile/addresses'),
  addAddress: (data) => httpClient.post('/v1/profile/addresses', data),
  updateAddress: (id, data) => httpClient.put(`/v1/profile/addresses/${id}`, data),
  deleteAddress: (id) => httpClient.delete(`/v1/profile/addresses/${id}`),
};
