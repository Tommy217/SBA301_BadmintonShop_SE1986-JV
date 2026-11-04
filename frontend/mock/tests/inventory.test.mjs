import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { updateManagedOrder } from '../actions/management.mjs';
import { manageInventory, prepareInventoryData } from '../actions/inventory.mjs';
import { mutate, startServer } from '../server.mjs';

const suppliers = [{ id: 1, name: 'Test supplier' }];

function seed() {
  const db = JSON.parse(readFileSync(new URL('../db.json', import.meta.url), 'utf8'));
  db.products = [{ id: 901, name: 'Test racket', product_type: 'racket', is_active: true }];
  db.product_variants = [
    {
      id: 902,
      product_id: 901,
      sku: 'TEST-902',
      is_active: true,
      stock_quantity: 20,
      low_stock_threshold: 5,
      price: 100000,
      cost_price: 50000,
      color: 'Black',
      size: '4U',
    },
  ];
  db.product_variants.push({ ...db.product_variants[0], id: 903, sku: 'TEST-903' });
  db.suppliers = suppliers.map((supplier) => ({ ...supplier }));
  db.invoices = [];
  db.invoice_items = [];
  db.invoice_deliveries = [];
  db.goods_receipts = [];
  db.goods_receipt_items = [];
  db.inventory_movements = [];
  db.stock_audits = [];
  db.stock_audit_items = [];
  db.stock_alerts = [];
  return db;
}

test('preserves supplied fixtures and synchronizes one stock alert per low-stock period', () => {
  const db = seed();
  const variant = db.product_variants[0];
  for (const item of db.product_variants) {
    item.stock_quantity = Number(item.low_stock_threshold ?? 5) + 1;
  }
  variant.stock_quantity = variant.low_stock_threshold;
  prepareInventoryData(db);
  assert.deepEqual(db.suppliers, suppliers);
  const openAlert = db.stock_alerts.filter(
    (alert) => alert.status === 'open' && alert.variant_id === variant.id,
  );
  assert.equal(openAlert.length, 1);
  prepareInventoryData(db);
  assert.equal(db.stock_alerts.length, openAlert.length);
  variant.stock_quantity = variant.low_stock_threshold + 1;
  prepareInventoryData(db);
  assert.equal(db.stock_alerts[0].status, 'resolved');
  assert.ok(db.stock_alerts[0].resolved_at);
  variant.stock_quantity = variant.low_stock_threshold;
  prepareInventoryData(db);
  assert.equal(
    db.stock_alerts.filter((alert) => alert.status === 'open' && alert.variant_id === variant.id)
      .length,
    1,
  );
  assert.equal(db.stock_alerts.length, 2);
});

test('goods receipt maps supplier, receipt lines and stock-in ledger to schema fields', () => {
  const db = seed();
  const variant = db.product_variants[0];
  const previous = variant.stock_quantity;
  manageInventory(
    db,
    {
      type: 'inventory_receive',
      supplier_id: String(suppliers[0].id),
      note: 'Nhập hàng đầu tháng',
      items: [{ variant_id: variant.id, quantity: '4', unit_cost: '750000' }],
    },
    7,
  );
  const receipt = db.goods_receipts[0];
  const line = db.goods_receipt_items[0];
  const movement = db.inventory_movements[0];
  assert.equal(receipt.supplier_id, suppliers[0].id);
  assert.equal(receipt.received_by, 7);
  assert.equal(receipt.total_cost, 3000000);
  assert.equal(receipt.note, 'Nhập hàng đầu tháng');
  assert.equal(line.receipt_id, receipt.id);
  assert.equal(line.variant_id, variant.id);
  assert.equal(line.quantity, 4);
  assert.equal(line.unit_cost, 750000);
  assert.equal(variant.stock_quantity, previous + 4);
  assert.equal(movement.movement_type, 'stock_in');
  assert.equal(movement.quantity, 4);
  assert.equal(movement.balance_after, previous + 4);
  assert.equal(movement.ref_type, 'goods_receipt');
  assert.equal(movement.ref_id, receipt.id);

  const untouched = variant.stock_quantity;
  assert.throws(
    () =>
      manageInventory(db, {
        type: 'inventory_receive',
        supplier_id: 999,
        items: [{ variant_id: variant.id, quantity: 1, unit_cost: 100 }],
      }),
    /Nhà cung cấp không hợp lệ/,
  );
  assert.equal(variant.stock_quantity, untouched);
});

test('stock audit snapshots, saves counts and records differences only when completed', () => {
  const db = seed();
  prepareInventoryData(db);
  const variant = db.product_variants[0];
  const initialQuantity = variant.stock_quantity;
  manageInventory(db, { type: 'inventory_audit_start', note: 'Kiểm kê tháng' }, 5);
  const audit = db.stock_audits[0];
  const item = db.stock_audit_items.find((entry) => entry.variant_id === variant.id);
  assert.equal(audit.status, 'draft');
  assert.equal(audit.conducted_by, 5);
  assert.equal(item.system_qty, initialQuantity);
  manageInventory(db, {
    type: 'inventory_audit_update',
    audit_id: audit.id,
    items: [{ variant_id: variant.id, counted_qty: initialQuantity + 2, note: 'Tìm thấy thêm' }],
  });
  assert.equal(item.diff_qty, 2);
  assert.equal(variant.stock_quantity, initialQuantity);
  manageInventory(db, { type: 'inventory_audit_complete', audit_id: audit.id }, 5);
  assert.equal(audit.status, 'completed');
  assert.ok(audit.completed_at);
  assert.equal(variant.stock_quantity, initialQuantity + 2);
  const adjustment = db.inventory_movements.find((movement) => movement.ref_type === 'stock_audit');
  assert.equal(adjustment.movement_type, 'audit_adjust');
  assert.equal(adjustment.quantity, 2);
  assert.equal(adjustment.balance_after, initialQuantity + 2);

  manageInventory(db, { type: 'inventory_audit_start' });
  const staleAudit = db.stock_audits.at(-1);
  const secondVariant = db.product_variants[1];
  secondVariant.stock_quantity++;
  assert.throws(
    () => manageInventory(db, { type: 'inventory_audit_complete', audit_id: staleAudit.id }),
    /đã thay đổi/,
  );
  assert.equal(staleAudit.status, 'draft');
});

test('manual adjustment requires a reason and records signed change with resulting balance', () => {
  const db = seed();
  const variant = db.product_variants[0];
  const previous = variant.stock_quantity;
  manageInventory(
    db,
    {
      type: 'inventory_adjust',
      variant_id: variant.id,
      counted_qty: previous - 1,
      note: 'Điều chỉnh sau đối soát',
    },
    8,
  );
  const movement = db.inventory_movements[0];
  assert.equal(variant.stock_quantity, previous - 1);
  assert.equal(movement.movement_type, 'manual_adjust');
  assert.equal(movement.quantity, -1);
  assert.equal(movement.balance_after, previous - 1);
  assert.equal(movement.ref_type, 'manual');
  assert.equal(movement.created_by, 8);
  assert.throws(
    () =>
      manageInventory(db, {
        type: 'inventory_adjust',
        variant_id: variant.id,
        counted_qty: -1,
        note: 'Giá trị không hợp lệ',
      }),
    /không âm/,
  );
});

test('checkout and cancellation append stock-out and return entries', () => {
  const db = seed();
  db.inventory_movements = [];
  const variant = db.product_variants.find((entry) => entry.stock_quantity > 0);
  const initial = variant.stock_quantity;
  mutate(db, {
    type: 'checkout',
    source: 'direct',
    variant_id: variant.id,
    quantity: 1,
    receiver_name: 'Test',
    receiver_phone: '0901234567',
    shipping_address: '12 Nguyễn Trãi, TP Hồ Chí Minh',
  });
  const order = db.orders.at(-1);
  assert.equal(db.inventory_movements[0].movement_type, 'stock_out');
  assert.equal(db.inventory_movements[0].quantity, -1);
  assert.equal(variant.stock_quantity, initial - 1);
  updateManagedOrder(
    db,
    {
      id: order.id,
      expected_status: 'pending',
      expected_payment_status: 'unpaid',
      status: 'cancelled',
      note: 'Khách yêu cầu hủy',
    },
    null,
  );
  assert.equal(db.inventory_movements[1].movement_type, 'return_in');
  assert.equal(db.inventory_movements[1].quantity, 1);
  assert.equal(db.inventory_movements[1].balance_after, initial);
  assert.equal(variant.stock_quantity, initial);
});

test('mock rejects inventory mutations after migration to Java', async () => {
  const directory = mkdtempSync(join(tmpdir(), 'inventory-management-'));
  const path = join(directory, 'db.json');
  const db = seed();
  writeFileSync(path, JSON.stringify(db));
  const server = startServer(0, path);
  await new Promise((resolve) => server.once('listening', resolve));
  try {
    const base = `http://127.0.0.1:${server.address().port}`;
    const login = async (email) => {
      const response = await fetch(`${base}/api/auth/login`, {
        method: 'POST',
        body: JSON.stringify({ email, password: '123' }),
      });
      return response.json();
    };
    const staff = await login('staff1@gmail.com');
    const initial = await fetch(`${base}/api/shop`, {
      headers: { Authorization: 'Bearer ' + staff.authToken },
    });
    const initialData = await initial.json();
    assert.equal(initialData.suppliers.length, suppliers.length);
    assert.ok(Array.isArray(initialData.goods_receipts));
    assert.ok(Array.isArray(initialData.invoice_items));
    const variant = db.product_variants[0];
    const response = await fetch(`${base}/api/shop`, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + staff.authToken },
      body: JSON.stringify({
        type: 'inventory_receive',
        supplier_id: suppliers[0].id,
        items: [{ variant_id: variant.id, quantity: 2, unit_cost: 1000 }],
      }),
    });
    assert.equal(response.status, 400);
    assert.match((await response.json()).message, /8080/);
    assert.equal(JSON.parse(readFileSync(path, 'utf8')).goods_receipts.length, 0);

    const customer = await login('customer1@gmail.com');
    const denied = await fetch(`${base}/api/shop`, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + customer.authToken },
      body: JSON.stringify({ type: 'inventory_audit_start' }),
    });
    assert.equal(denied.status, 403);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    rmSync(directory, { recursive: true, force: true });
  }
});
