import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { mutate } from '../server.mjs';

const seed = () => {
  const db = JSON.parse(readFileSync(new URL('../db.json', import.meta.url), 'utf8'));
  db.support_tickets = [];
  db.ticket_items = [];
  db.ticket_status_history = [];
  db.ticket_inspections = [];
  db.refund_records = [];
  return db;
};

test('UC15: Customer creates REFUND ticket with items and QR code', () => {
  const db = seed();
  const order = db.orders[0];
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);

  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Sản phẩm lỗi nứt khung',
    description: 'Chi tiết vết nứt sau khi mở hộp',
    refund_qr_url: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    evidence_urls: ['data:image/png;base64,fake-evidence'],
    items: [{ order_item_id: orderItem.id, quantity: 1 }],
  });

  assert.equal(db.support_tickets.length, 1);
  const ticket = db.support_tickets[0];
  assert.equal(ticket.ticket_type, 'REFUND');
  assert.equal(ticket.status, 'PENDING');
  assert.ok(ticket.refund_qr_url.length > 0);
  assert.equal(db.ticket_items.length, 1);
  assert.equal(db.ticket_status_history.length, 1);
  assert.equal(db.ticket_status_history[0].to_status, 'PENDING');
});

test('UC15: Staff reviews ticket with cause STORE_FAULT', () => {
  const db = seed();
  const order = db.orders[0];
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);

  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Sản phẩm lỗi',
    refund_qr_url: 'data:image/png;base64,qr',
    items: [{ order_item_id: orderItem.id, quantity: 1 }],
  });

  const ticket = db.support_tickets[0];

  // Staff approves review with cause STORE_FAULT
  mutate(db, {
    type: 'review_support_ticket',
    ticket_id: ticket.id,
    action: 'approve',
    cause: 'STORE_FAULT',
    actor_id: 'staff-1',
  });

  assert.equal(ticket.status, 'AWAITING_RETURN');
  assert.equal(ticket.cause, 'STORE_FAULT');
  assert.equal(db.ticket_status_history.length, 2);
  assert.equal(db.ticket_status_history[1].to_status, 'AWAITING_RETURN');
});

test('UC15: Staff inspects returned product with result PASSED for amount < 3M', () => {
  const db = seed();
  const order = db.orders[0];
  order.shipping_fee = 30000;
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);
  orderItem.unit_price = 500000;

  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Sản phẩm lỗi',
    refund_qr_url: 'data:image/png;base64,qr',
    items: [{ order_item_id: orderItem.id, quantity: 1 }],
  });

  const ticket = db.support_tickets[0];

  mutate(db, {
    type: 'review_support_ticket',
    ticket_id: ticket.id,
    action: 'approve',
    cause: 'STORE_FAULT',
  });

  // Inspection PASSED
  mutate(db, {
    type: 'inspect_support_ticket',
    ticket_id: ticket.id,
    result: 'PASSED',
    note: 'Hàng nguyên vẹn tem mác',
  });

  assert.equal(ticket.refund_amount, 530000); // 500k + 30k ship fee because STORE_FAULT
  assert.equal(ticket.status, 'PENDING_TRANSFER'); // < 3M -> PENDING_TRANSFER directly
});

test('UC15: Staff records manual refund transfer with bank reference', () => {
  const db = seed();
  const order = db.orders[0];
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);

  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Sản phẩm lỗi',
    refund_qr_url: 'data:image/png;base64,qr',
    items: [{ order_item_id: orderItem.id, quantity: 1 }],
  });

  const ticket = db.support_tickets[0];

  mutate(db, {
    type: 'review_support_ticket',
    ticket_id: ticket.id,
    action: 'approve',
    cause: 'STORE_FAULT',
  });

  mutate(db, {
    type: 'inspect_support_ticket',
    ticket_id: ticket.id,
    result: 'PASSED',
    note: 'Đạt yêu cầu',
  });

  // Record transfer
  mutate(db, {
    type: 'refund_transfer_support_ticket',
    ticket_id: ticket.id,
    bank_reference: 'FT26103199998888',
    note: 'Đã chuyển khoản qua Vietcombank',
  });

  assert.equal(ticket.status, 'REFUNDED');
  assert.equal(db.refund_records.length, 1);
  assert.equal(db.refund_records[0].bank_reference, 'FT26103199998888');
});

test('UC15: Customer cancels ticket only when in PENDING', () => {
  const db = seed();
  const order = db.orders[0];
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);

  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Đổi ý',
    refund_qr_url: 'data:image/png;base64,qr',
    items: [{ order_item_id: orderItem.id, quantity: 1 }],
  });

  const ticket = db.support_tickets[0];
  assert.equal(ticket.status, 'PENDING');

  mutate(db, {
    type: 'cancel_support_ticket',
    ticket_id: ticket.id,
  });

  assert.equal(ticket.status, 'CANCELLED');

  // Attempting to cancel again should throw
  assert.throws(() => {
    mutate(db, {
      type: 'cancel_support_ticket',
      ticket_id: ticket.id,
    });
  });
});

test('UC15: Manager approves refund for amount >= 3M', () => {
  const db = seed();
  const order = db.orders[0];
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);
  orderItem.unit_price = 3500000;

  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Vợt cao cấp bị gãy',
    refund_qr_url: 'data:image/png;base64,qr',
    items: [{ order_item_id: orderItem.id, quantity: 1 }],
  });

  const ticket = db.support_tickets[0];

  mutate(db, {
    type: 'review_support_ticket',
    ticket_id: ticket.id,
    action: 'approve',
    cause: 'STORE_FAULT',
  });

  mutate(db, {
    type: 'inspect_support_ticket',
    ticket_id: ticket.id,
    result: 'PASSED',
    note: 'Đạt kiểm định',
  });

  // Because amount >= 3M -> PENDING_APPROVAL
  assert.equal(ticket.status, 'PENDING_APPROVAL');

  // Staff cannot approve directly (must be Manager)
  assert.throws(() => {
    mutate(db, {
      type: 'approve_refund_support_ticket',
      ticket_id: ticket.id,
    });
  });

  // Manager approves
  mutate(db, {
    type: 'manager_decision_support_ticket',
    ticket_id: ticket.id,
    decision: 'approve',
    manager_id: 'manager-1',
  });

  assert.equal(ticket.status, 'PENDING_TRANSFER');
  assert.equal(ticket.approved_by, 'manager-1');
});

test('UC15: Only REFUND ticket type is supported', () => {
  const db = seed();
  const order = db.orders[0];
  const orderItem = db.order_items.find((oi) => oi.order_id === order.id);

  assert.throws(() => {
    mutate(db, {
      type: 'create_support_ticket',
      order_id: order.id,
      ticket_type: 'EXCHANGE',
      reason: 'Đổi vợt',
      refund_qr_url: 'data:image/png;base64,qr',
      items: [{ order_item_id: orderItem.id, quantity: 1 }],
    });
  });
});
