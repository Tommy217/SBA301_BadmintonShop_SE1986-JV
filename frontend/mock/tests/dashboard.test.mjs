import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { buildInventory } from '../../src/modules/management/utils/inventoryData.js';
import { buildReport } from '../../src/modules/management/utils/reportData.js';
import { seedDashboard } from '../seed/dashboard.mjs';

test('inventory counts all SKU stock, separates low/out, excludes unknown costs', () => {
  const inventory = buildInventory({
    products: [{ id: 'p', product_type: 'racket' }],
    product_variants: [
      {
        id: 'a',
        product_id: 'p',
        is_active: true,
        stock_quantity: 0,
        price: 100,
        cost_price: 50,
        low_stock_threshold: 5,
      },
      {
        id: 'b',
        product_id: 'p',
        is_active: true,
        stock_quantity: 3,
        price: 100,
        cost_price: 50,
        low_stock_threshold: 5,
      },
      {
        id: 'c',
        product_id: 'p',
        is_active: false,
        stock_quantity: 10,
        price: 200,
        cost_price: 0,
        low_stock_threshold: 5,
      },
    ],
    orders: [
      { id: 'o', status: 'pending' },
      { id: 'x', status: 'cancelled' },
    ],
    order_items: [
      { order_id: 'o', quantity: 2 },
      { order_id: 'x', quantity: 7 },
    ],
  });
  assert.equal(inventory.totalQuantity, 13);
  assert.equal(inventory.activeQuantity, 3);
  assert.equal(inventory.low, 1);
  assert.equal(inventory.out, 1);
  assert.equal(inventory.allocated, 2);
  assert.equal(inventory.saleValue, 2300);
  assert.equal(inventory.costValue, 150);
  assert.equal(inventory.missingCost, 1);
  assert.equal(inventory.products[0].quantity, 13);
  assert.equal(inventory.groups[0].quantity, 13);
});
test('dashboard seed is repeatable and preserves existing stock', () => {
  const db = JSON.parse(readFileSync(new URL('../db.json', import.meta.url), 'utf8'));
  const existing = structuredClone(db.product_variants.filter((v) => !v.is_demo));
  seedDashboard(db);
  const once = structuredClone(db);
  seedDashboard(db);
  assert.deepEqual(db, once);
  assert.deepEqual(
    db.product_variants.filter((v) => !v.is_demo),
    existing,
  );
  assert.equal(db.orders.filter((o) => o.id.startsWith('demo-order-')).length, 72);
  const report = buildReport(db);
  assert.ok(report.months.length >= 6);
  assert.ok(report.revenue > 1000000);
  for (const o of db.orders.filter((o) => o.id.startsWith('demo-order-'))) {
    const subtotal = db.order_items
      .filter((i) => i.order_id === o.id)
      .reduce((s, i) => s + i.line_total, 0);
    assert.equal(o.subtotal, subtotal);
    assert.equal(o.total_amount, subtotal - o.discount_amount + o.shipping_fee);
    assert.equal(db.payments.find((p) => p.order_id === o.id).amount, o.total_amount);
  }
});
