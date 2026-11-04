import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mapInventorySnapshot } from '../../src/modules/management/utils/inventoryMapping.js';
import { buildInventory } from '../../src/modules/management/utils/inventoryData.js';

test('Java inventory snapshot supplies stock, receipt, audit and allocation fields to React', () => {
  const snapshot = mapInventorySnapshot({
    products: [{ id: 1, name: 'Racket', productType: 'racket', brand: 'Brand', isActive: true }],
    productVariants: [
      {
        id: 2,
        productId: 1,
        sku: 'SKU',
        stockQuantity: 4,
        lowStockThreshold: 5,
        price: 100,
        costPrice: 60,
        isActive: true,
      },
    ],
    suppliers: [{ id: 3, name: 'Supplier' }],
    goodsReceipts: [
      {
        id: 4,
        supplierId: 3,
        receiptCode: 'GR-4',
        totalCost: 120,
        receivedAt: '2026-11-04T12:00:00',
      },
    ],
    goodsReceiptItems: [{ id: 5, receiptId: 4, variantId: 2, quantity: 2, unitCost: 60 }],
    inventoryMovements: [
      { id: 6, variantId: 2, movementType: 'stock_in', quantity: 2, balanceAfter: 4 },
    ],
    stockAudits: [{ id: 7, auditCode: 'AUD-7', status: 'draft', completedAt: null }],
    stockAuditItems: [
      { id: 8, auditId: 7, variantId: 2, systemQty: 4, countedQty: 3, diffQty: -1 },
    ],
    stockAlerts: [{ id: 9, variantId: 2, stockAtAlert: 4, threshold: 5, resolvedAt: null }],
    allocatedQuantity: 12,
  });
  assert.equal(snapshot.product_variants[0].product_id, snapshot.products[0].id);
  assert.equal(snapshot.goods_receipts[0].supplier_id, snapshot.suppliers[0].id);
  assert.equal(snapshot.goods_receipt_items[0].receipt_id, snapshot.goods_receipts[0].id);
  assert.equal(snapshot.inventory_movements[0].balance_after, 4);
  assert.equal(snapshot.stock_audit_items[0].diff_qty, -1);
  assert.equal(snapshot.stock_alerts[0].resolved_at, null);
  const inventory = buildInventory(snapshot);
  assert.equal(inventory.totalQuantity, 4);
  assert.equal(inventory.costValue, 240);
  assert.equal(inventory.saleValue, 400);
  assert.equal(inventory.low, 1);
  assert.equal(inventory.allocated, 12);
});

test('an empty SQL inventory stays empty and zero SQL allocation overrides mock orders', () => {
  const inventory = buildInventory(
    mapInventorySnapshot({
      products: [],
      productVariants: [],
      allocatedQuantity: 0,
      orders: [{ id: 1, status: 'pending' }],
      orderItems: [{ orderId: 1, quantity: 99 }],
    }),
  );
  assert.equal(inventory.totalQuantity, 0);
  assert.equal(inventory.totalProducts, 0);
  assert.equal(inventory.allocated, 0);
});
