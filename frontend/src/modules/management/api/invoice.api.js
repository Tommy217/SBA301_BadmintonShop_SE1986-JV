import { httpClient } from '@/shared/api/httpClient';

const backend = (
  import.meta.env.VITE_INVOICE_API_URL ||
  import.meta.env.VITE_BACKEND_URL ||
  'http://localhost:8080'
).replace(/\/+$/, '');
const baseURL = backend.endsWith('/api') ? backend : backend + '/api';
const post = (path, payload) => httpClient.post('/invoices' + path, payload, { baseURL });

// The Java DTO uses camelCase; the existing invoice view uses SQL-style names.
const snake = (object) =>
  Object.fromEntries(
    Object.entries(object).map(([key, value]) => [
      key.replace(/[A-Z]/g, (letter) => '_' + letter.toLowerCase()),
      value,
    ]),
  );
export const mapInvoice = (invoice) => ({
  ...snake(invoice),
  items: (invoice.items || []).map(snake),
  deliveries: (invoice.deliveries || []).map(snake),
});

export const invoiceApi = {
  async list(config) {
    const invoices = await httpClient.get('/invoices', { ...config, baseURL });
    return invoices.map(mapInvoice);
  },
  create: (payload) => post('', payload),
  adjust: ({ invoice_id, amount, description, reason }) =>
    post('/' + invoice_id + '/adjustments', { amount, description, reason }),
  deliver: ({ invoice_id, channel, recipient }) =>
    post('/' + invoice_id + '/deliveries', { channel, recipient }),
  replace: ({ invoice_id }) => post('/' + invoice_id + '/replace'),
  cancel: ({ invoice_id }) => post('/' + invoice_id + '/cancel'),
};
