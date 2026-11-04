import { httpClient } from '@/shared/api/httpClient';
import { mapInventorySnapshot } from '@/modules/management/utils/inventoryMapping';

const backend = (
  import.meta.env.VITE_INVENTORY_API_URL ||
  import.meta.env.VITE_BACKEND_URL ||
  'http://localhost:8080'
).replace(/\/+$/, '');
const baseURL = backend.endsWith('/api') ? backend : backend + '/api';
const post = (path, payload) => httpClient.post('/inventory' + path, payload, { baseURL });

export const inventoryApi = {
  async list(config) {
    const response = await httpClient.get('/inventory', { ...config, baseURL });
    return mapInventorySnapshot(response);
  },
  receiveStock: ({ supplier_id, items, note }) =>
    post('/receipts', {
      supplierId: supplier_id ? Number(supplier_id) : null,
      note,
      items: items.map((item) => ({
        variantId: Number(item.variant_id),
        quantity: Number(item.quantity),
        unitCost: Number(item.unit_cost),
      })),
    }),
  adjustStock: ({ variant_id, counted_qty, note }) =>
    post('/adjustments', { variantId: Number(variant_id), countedQty: Number(counted_qty), note }),
  startAudit: (payload) => post('/audits', payload),
  updateAudit: ({ audit_id, items }) =>
    httpClient.put(
      '/inventory/audits/' + audit_id,
      {
        items: items.map((item) => ({
          variantId: Number(item.variant_id),
          countedQty: Number(item.counted_qty),
          note: item.note,
        })),
      },
      { baseURL },
    ),
  completeAudit: ({ audit_id }) => post('/audits/' + audit_id + '/complete'),
  cancelAudit: ({ audit_id }) => post('/audits/' + audit_id + '/cancel'),
};
