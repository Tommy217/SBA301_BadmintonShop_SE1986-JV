import { httpClient } from '@/shared/api/httpClient';

export const employeeApi = {
  getEmployees: (params = {}) => httpClient.get('/admin/employees', { params }),
  getEmployeeById: (id) => httpClient.get(`/admin/employees/${id}`),
  createEmployee: (employeeData) => httpClient.post('/admin/employees', employeeData),
  updateEmployee: (id, updateData) => httpClient.put(`/admin/employees/${id}`, updateData),
  updateEmployeeStatus: (id, status) => httpClient.patch(`/admin/employees/${id}/status`, { status }),
};

export const customerApi = {
  getCustomers: (params = {}) => httpClient.get('/admin/customers', { params }),
  getCustomerById: (id) => httpClient.get(`/admin/customers/${id}`),
  updateCustomerStatus: (id, status) => httpClient.patch(`/admin/customers/${id}/status`, { status }),
};
