import { httpClient } from '@/shared/api/httpClient';

export const authApi = {
  me: () => httpClient.get('/auth/me'),
  logout: () => httpClient.post('/auth/logout'),
  login: (credentials) => httpClient.post('/auth/login', credentials),
  register: (data) => httpClient.post('/auth/register', data),
  forgotPassword: (data) => httpClient.post('/auth/forgot-password', data),
  resetPassword: (data) => httpClient.post('/auth/reset-password', data),
};

export const profileApi = {
  getProfile: () => httpClient.get('/profile'),
  updateProfile: (data) => httpClient.put('/profile', data),
  changePassword: (data) => httpClient.post('/profile/change-password', data),
  getAddresses: () => httpClient.get('/profile/addresses'),
  addAddress: (data) => httpClient.post('/profile/addresses', data),
  updateAddress: (id, data) => httpClient.put(`/profile/addresses/${id}`, data),
  deleteAddress: (id) => httpClient.delete(`/profile/addresses/${id}`),
};
