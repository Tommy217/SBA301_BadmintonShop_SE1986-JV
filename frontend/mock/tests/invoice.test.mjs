import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { manageInvoice, syncCompletedOrderInvoices } from '../actions/invoice.mjs';
import { startServer } from '../server.mjs';

function seed() {
  return {
    orders: [
      {
        id: 'order-1',
        order_code: 'BS-ORDER-1',
        status: 'completed',
        payment_status: 'paid',
        receiver_name: 'Nguyễn Văn A',
        shipping_address: '12 Nguyễn Trãi, TP Hồ Chí Minh',
        subtotal: 200000,
        discount_amount: 10000,
        shipping_fee: 30000,
        total_amount: 220000,
      },
    ],
    order_items: [
      {
        id: 'order-item-1',
        order_id: 'order-1',
        product_name: 'Vợt cầu lông',
        variant_info: 'Màu xanh',
        unit_price: 200000,
        quantity: 1,
        line_total: 200000,
      },
    ],
    invoices: [],
    invoice_items: [],
    invoice_deliveries: [],
  };
}

test('automatically invoices completed paid orders once using order snapshots', () => {
  const db = seed();
  assert.equal(syncCompletedOrderInvoices(db), true);
  assert.equal(db.invoices.length, 1);
  assert.equal(db.invoices[0].invoice_type, 'sale');
  assert.equal(db.invoices[0].status, 'issued');
  assert.equal(db.invoices[0].issued_by, null);
  assert.equal(db.invoices[0].buyer_name, 'Nguyễn Văn A');
  assert.equal(db.invoices[0].total_amount, 220000);
  assert.match(db.invoices[0].invoice_no, /^INV-\d{8}-[A-F0-9]{6}$/);
  assert.equal(db.invoice_items[0].description, 'Vợt cầu lông - Màu xanh');
  assert.equal(db.invoice_items[0].invoice_id, db.invoices[0].id);
  assert.equal(syncCompletedOrderInvoices(db), false);
  assert.equal(db.invoices.length, 1);

  const unpaid = seed();
  unpaid.orders[0].payment_status = 'unpaid';
  assert.equal(syncCompletedOrderInvoices(unpaid), false);
  assert.equal(unpaid.invoices.length, 0);
});

test('adjustment invoice preserves parent link, sign and reason', () => {
  const db = seed();
  syncCompletedOrderInvoices(db);
  const parent = db.invoices[0];
  manageInvoice(db, {
    type: 'invoice_adjust',
    invoice_id: parent.id,
    amount: -25000,
    reason: 'Điều chỉnh giảm theo thỏa thuận',
    description: 'Giảm giá sau bán',
  });
  const adjustment = db.invoices[1];
  assert.equal(adjustment.invoice_type, 'adjustment');
  assert.equal(adjustment.parent_invoice_id, parent.id);
  assert.equal(adjustment.total_amount, -25000);
  assert.equal(adjustment.reason, 'Điều chỉnh giảm theo thỏa thuận');
  assert.equal(db.invoice_items[1].line_total, -25000);
  assert.throws(
    () =>
      manageInvoice(db, {
        type: 'invoice_adjust',
        invoice_id: parent.id,
        amount: 0,
        reason: 'Không hợp lệ',
      }),
    /khác 0/,
  );
});

test('replacement links a new sale invoice and permits replacing it again', () => {
  const db = seed();
  syncCompletedOrderInvoices(db);
  const original = db.invoices[0];
  manageInvoice(db, { type: 'invoice_replace', invoice_id: original.id });
  const replacement = db.invoices[1];
  assert.equal(original.status, 'replaced');
  assert.equal(replacement.invoice_type, 'sale');
  assert.equal(replacement.parent_invoice_id, original.id);
  assert.equal(replacement.total_amount, original.total_amount);
  manageInvoice(db, { type: 'invoice_replace', invoice_id: replacement.id });
  assert.equal(replacement.status, 'replaced');
  assert.equal(db.invoices.length, 3);
});

test('cancellation and invoice delivery respect statuses and delivery schema', () => {
  const db = seed();
  syncCompletedOrderInvoices(db);
  const invoice = db.invoices[0];
  manageInvoice(db, {
    type: 'invoice_delivery',
    invoice_id: invoice.id,
    channel: 'email',
    recipient: 'buyer@example.com',
  });
  manageInvoice(db, { type: 'invoice_delivery', invoice_id: invoice.id, channel: 'download' });
  assert.deepEqual(
    db.invoice_deliveries.map(({ channel, status, sent_at }) => [
      channel,
      status,
      Boolean(sent_at),
    ]),
    [
      ['email', 'queued', false],
      ['download', 'sent', true],
    ],
  );
  assert.throws(
    () =>
      manageInvoice(db, {
        type: 'invoice_delivery',
        invoice_id: invoice.id,
        channel: 'email',
        recipient: 'not-an-email',
      }),
    /Email nhận hóa đơn không hợp lệ/,
  );
  manageInvoice(db, { type: 'invoice_cancel', invoice_id: invoice.id });
  assert.equal(invoice.status, 'cancelled');
  assert.throws(
    () => manageInvoice(db, { type: 'invoice_cancel', invoice_id: invoice.id }),
    /đang phát hành/,
  );
});

test('completed invoices are backfilled and visible to managers and staff', async () => {
  const directory = mkdtempSync(join(tmpdir(), 'invoice-management-'));
  const path = join(directory, 'db.json');
  const db = JSON.parse(readFileSync(new URL('../db.json', import.meta.url), 'utf8'));
  db.orders = seed().orders;
  db.order_items = seed().order_items;
  db.invoices = [];
  db.invoice_items = [];
  db.invoice_deliveries = [];
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
    const manager = await login('manager1@gmail.com');
    const managerRead = await fetch(`${base}/api/shop`, {
      headers: { Authorization: 'Bearer ' + manager.authToken },
    });
    assert.equal(managerRead.status, 200);
    const managerData = await managerRead.json();
    assert.equal(managerData.invoices.length, 1);
    assert.equal(managerData.invoices[0].issued_by, null);
    assert.equal(managerData.invoice_items.length, 1);

    const manualIssue = await fetch(`${base}/api/shop`, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + manager.authToken },
      body: JSON.stringify({ type: 'invoice_issue', order_id: 'order-1' }),
    });
    assert.equal(manualIssue.status, 400);

    const staff = await login('staff1@gmail.com');
    const staffRead = await fetch(`${base}/api/shop`, {
      headers: { Authorization: 'Bearer ' + staff.authToken },
    });
    const staffData = await staffRead.json();
    assert.equal(staffData.invoices.length, 1);
    assert.equal(staffData.invoice_items.length, 1);
    assert.deepEqual(staffData.invoice_deliveries, []);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    rmSync(directory, { recursive: true, force: true });
  }
});
