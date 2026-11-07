import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { mutate, startServer } from '../server.mjs';
const seed = () => {
  const db = JSON.parse(readFileSync(new URL('../db.json', import.meta.url), 'utf8'));
  db.cart_items = [];
  db.orders = [];
  db.order_items = [];
  db.payments = [];
  db.order_status_history = [];
  db.support_tickets = [];
  db.deliveries = [];
  return db;
};
const delivery = {
  type: 'checkout',
  source: 'cart',
  receiver_name: 'Nguyễn Văn A',
  receiver_phone: '0901234567',
  shipping_address: '12 Nguyễn Trãi, TP Hồ Chí Minh',
};
test('checkout selected items, preserve unselected cart, snapshot prices, deduct stock', () => {
  const db = seed(),
    [v, w] = db.product_variants,
    stock = v.stock_quantity;
  mutate(db, { type: 'add', variant_id: v.id });
  mutate(db, { type: 'add', variant_id: v.id });
  mutate(db, { type: 'add', variant_id: w.id });
  mutate(db, { type: 'update', id: db.cart_items[1].id, is_selected: false });
  mutate(db, delivery);
  assert.equal(db.orders.length, 1);
  assert.equal(db.order_items[0].quantity, 2);
  assert.equal(db.order_items[0].unit_price, v.price);
  assert.equal(v.stock_quantity, stock - 2);
  assert.equal(db.cart_items.length, 1);
  assert.equal(db.cart_items[0].variant_id, w.id);
  assert.equal(db.orders[0].total_amount, v.price * 2);
  assert.equal(db.orders[0].shipping_fee, 0);
  assert.equal(db.payments[0].method, 'cod');
  assert.equal(db.order_status_history[0].to_status, 'pending');
  assert.throws(() => mutate(db, delivery), /Chọn/);
});
test('invalid quantity, invalid phone and stock shortage leave order untouched', () => {
  const db = seed(),
    v = db.product_variants[0];
  mutate(db, { type: 'add', variant_id: v.id });
  assert.throws(() => mutate(db, { type: 'update', id: db.cart_items[0].id, quantity: 0 }));
  assert.throws(() => mutate(db, { ...delivery, receiver_phone: '123' }));
  v.stock_quantity = 0;
  assert.throws(() => mutate(db, delivery));
  assert.equal(db.orders.length, 0);
  assert.equal(db.cart_items.length, 1);
});
test('shipping below threshold and remove action', () => {
  const db = seed(),
    v = db.product_variants.find((v) => v.price < 1000000);
  mutate(db, { type: 'add', variant_id: v.id });
  mutate(db, delivery);
  assert.equal(db.orders[0].shipping_fee, 30000);
  mutate(db, { type: 'add', variant_id: v.id });
  mutate(db, { type: 'remove', id: db.cart_items[0].id });
  assert.equal(db.cart_items.length, 0);
});
test('create support ticket for an existing order', () => {
  const db = seed();
  const order = {
    id: 'order-1',
    order_code: 'BS-ORDER-1',
    user_id: 'customer-1',
  };
  db.orders.push(order);
  mutate(db, {
    type: 'create_support_ticket',
    order_id: order.id,
    ticket_type: 'REFUND',
    reason: 'Sản phẩm bị lỗi',
    description: 'Vợt bị nứt sau khi nhận hàng.',
  });

  assert.equal(db.support_tickets.length, 1);
  assert.equal(db.support_tickets[0].order_id, order.id);
  assert.equal(db.support_tickets[0].user_id, order.user_id);
  assert.equal(db.support_tickets[0].ticket_type, 'REFUND');
  assert.equal(db.support_tickets[0].status, 'PENDING');
  assert.equal(db.support_tickets[0].reason, 'Sản phẩm bị lỗi');
});

test('create support ticket rejects invalid order, type and reason', () => {
  const db = seed();
  db.orders.push({ id: 'order-1', user_id: 'customer-1' });
  const action = {
    type: 'create_support_ticket',
    order_id: 'order-1',
    ticket_type: 'REFUND',
    reason: 'Lý do hợp lệ',
  };

  for (const invalid of [
    { ...action, order_id: 'missing-order' },
    { ...action, ticket_type: 'invalid' },
    { ...action, reason: ' '.repeat(301) },
    { ...action, description: 'x'.repeat(5001) },
  ]) {
    assert.throws(() => mutate(db, invalid));
    assert.equal(db.support_tickets.length, 0);
  }
});

test('update support ticket stores handling decision, response and timestamps', () => {
  const db = seed();
  db.support_tickets.push({
    id: 'ticket-1',
    status: 'open',
    reviewed_at: null,
    resolved_at: null,
    updated_at: '2026-10-16T12:00:00.000Z',
  });

  mutate(db, {
    type: 'update_support_ticket',
    ticket_id: 'ticket-1',
    status: 'resolved',
    resolution_type: 'refund',
    proposed_resolution: 'Đã hoàn tất hoàn tiền.',
    proposed_refund_amount: 125000,
    handled_by: 'staff1@gmail.com',
  });

  const ticket = db.support_tickets[0];
  assert.equal(ticket.status, 'resolved');
  assert.equal(ticket.resolution_type, 'refund');
  assert.equal(ticket.proposed_resolution, 'Đã hoàn tất hoàn tiền.');
  assert.equal(ticket.proposed_refund_amount, 125000);
  assert.equal(ticket.handled_by, 'staff1@gmail.com');
  assert.ok(ticket.reviewed_at);
  assert.ok(ticket.resolved_at);
  assert.equal(ticket.updated_at, ticket.resolved_at);
});

test('update support ticket rejects invalid status, refund and missing final response', () => {
  const db = seed();
  db.support_tickets.push({
    id: 'ticket-1',
    status: 'open',
    reviewed_at: null,
    resolved_at: null,
  });
  const action = {
    type: 'update_support_ticket',
    ticket_id: 'ticket-1',
    status: 'approved',
    resolution_type: 'refund',
    proposed_resolution: 'Đã duyệt yêu cầu hoàn tiền.',
    proposed_refund_amount: 100000,
  };

  for (const invalid of [
    { ...action, status: 'unknown' },
    { ...action, status: 'rejected', proposed_resolution: '' },
    { ...action, proposed_refund_amount: -1 },
    { ...action, proposed_refund_amount: '100000' },
  ]) {
    assert.throws(() => mutate(db, invalid));
    assert.equal(db.support_tickets[0].status, 'open');
    assert.equal(db.support_tickets[0].reviewed_at, null);
  }
});

test('HTTP API excludes existing user passwords, returns empty request lists for guests and rejects invalid actions', async () => {
  const server = startServer(0);
  await new Promise((resolve) => server.once('listening', resolve));
  try {
    const url = 'http://127.0.0.1:' + server.address().port + '/api/shop';
    const res = await fetch(url);
    assert.equal(res.status, 200);
    const payload = await res.json();
    assert.equal('users' in payload, false);
    assert.deepEqual(payload.support_tickets, []);
    assert.deepEqual(payload.ticket_items, []);
    const invalid = await fetch(url, {
      method: 'POST',
      body: JSON.stringify({ type: 'bad' }),
    });
    assert.equal(invalid.status, 401);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test('customer login checks credentials and returns public profile only', async () => {
  const server = startServer(0);
  await new Promise((resolve) => server.once('listening', resolve));
  try {
    const url = 'http://127.0.0.1:' + server.address().port + '/api/auth/login';
    const login = (email, password) =>
      fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
    const response = await login(' CUSTOMER2@gmail.com ', '123');
    assert.equal(response.status, 200);
    const user = await response.json();
    assert.equal(user.email, 'customer2@gmail.com');
    assert.deepEqual(user.roles, ['CUSTOMER']);
    assert.ok(user.authToken.startsWith('mock-'));
    assert.equal('password' in user, false);
    assert.equal((await login('customer2@gmail.com', 'wrong')).status, 401);
    assert.equal((await login('missing@gmail.com', '123')).status, 401);
    assert.equal((await fetch(url)).status, 405);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test('only managers and staff can update support tickets through the HTTP API', async () => {
  const db = seed();
  db.support_tickets.push({
    id: 'ticket-auth',
    status: 'open',
    reviewed_at: null,
    resolved_at: null,
  });
  const tempDir = mkdtempSync(join(tmpdir(), 'badminton-shop-test-'));
  const databasePath = join(tempDir, 'db.json');
  writeFileSync(databasePath, JSON.stringify(db));
  const server = startServer(0, databasePath);
  await new Promise((resolve) => server.once('listening', resolve));
  try {
    const url = 'http://127.0.0.1:' + server.address().port + '/api/shop';
    const login = async (email) => {
      const response = await fetch(
        'http://127.0.0.1:' + server.address().port + '/api/auth/login',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password: '123' }),
        },
      );
      return response.json();
    };
    const customer = await login('customer1@gmail.com');
    const admin = await login('admin1@gmail.com');
    const manager = await login('manager1@gmail.com');
    const staff = await login('staff1@gmail.com');
    const action = {
      type: 'update_support_ticket',
      ticket_id: 'ticket-auth',
      status: 'under_review',
      resolution_type: null,
      proposed_resolution: 'Đang kiểm tra yêu cầu.',
    };
    const denied = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ' + customer.authToken,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(action),
    });
    assert.equal(denied.status, 403);

    const adminDenied = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ' + admin.authToken,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(action),
    });
    assert.equal(adminDenied.status, 403);

    const managerAccepted = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ' + manager.authToken,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(action),
    });
    assert.equal(managerAccepted.status, 200);

    const accepted = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ' + staff.authToken,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(action),
    });
    assert.equal(accepted.status, 200);
    const responseData = await accepted.json();
    assert.equal(responseData.support_tickets[0].status, 'under_review');
  } finally {
    await new Promise((resolve) => server.close(resolve));
    rmSync(tempDir, { recursive: true, force: true });
  }
});

test('adding to cart never creates orders or payments', () => {
  const db = seed();
  const stock = db.product_variants[0].stock_quantity;
  mutate(db, { type: 'add', variant_id: db.product_variants[0].id });
  assert.equal(db.cart_items.length, 1);
  assert.equal(db.orders.length, 0);
  assert.equal(db.order_items.length, 0);
  assert.equal(db.payments.length, 0);
  assert.equal(db.product_variants[0].stock_quantity, stock);
});

test('direct order leaves cart exactly unchanged and uses chosen variant and quantity', () => {
  const db = seed();
  const [v, w] = db.product_variants;
  mutate(db, { type: 'add', variant_id: v.id });
  mutate(db, { type: 'add', variant_id: w.id });
  const before = structuredClone(db.cart_items);
  mutate(db, { ...delivery, source: 'direct', variant_id: v.id, quantity: 2 });
  assert.deepEqual(db.cart_items, before);
  assert.equal(db.orders.length, 1);
  assert.equal(db.order_items.length, 1);
  assert.equal(db.order_items[0].variant_id, v.id);
  assert.equal(db.order_items[0].quantity, 2);
  assert.equal(db.orders[0].total_amount, v.price * 2);
});

test('direct prepaid order works without cart, persists payment choice and authoritative amount', () => {
  const db = seed();
  const v = db.product_variants.find((v) => v.price < 1000000);
  mutate(db, {
    ...delivery,
    source: 'direct',
    variant_id: v.id,
    quantity: 1,
    payment_method: 'prepaid',
  });
  const order = db.orders[0],
    payment = db.payments[0];
  assert.equal(db.cart_items.length, 0);
  assert.equal(order.payment_status, 'unpaid');
  assert.equal(payment.method, 'qr');
  assert.equal(payment.amount, v.price + 30000);
  for (const gateway of ['mbbank', 'momo', 'card']) {
    mutate(db, {
      type: 'start_payment',
      order_id: order.id,
      gateway,
      amount: 1,
    });
    assert.equal(payment.gateway, gateway);
    assert.equal(payment.amount, order.total_amount);
    assert.equal(order.payment_status, 'unpaid');
  }
  assert.equal(db.orders.length, 1);
  assert.throws(() =>
    mutate(db, {
      type: 'start_payment',
      order_id: order.id,
      gateway: 'invalid',
    }),
  );
});

test('checkout rejects missing source, bad direct quantity, stock and payment method before creating order', () => {
  const db = seed(),
    v = db.product_variants[0];
  for (const overrides of [
    { source: undefined },
    { source: 'direct', variant_id: v.id, quantity: 0 },
    { source: 'direct', variant_id: v.id, quantity: v.stock_quantity + 1 },
    { source: 'direct', variant_id: 'missing', quantity: 1 },
    {
      source: 'direct',
      variant_id: v.id,
      quantity: 1,
      payment_method: 'invalid',
    },
  ])
    assert.throws(() => mutate(db, { ...delivery, ...overrides }));
  assert.equal(db.orders.length, 0);
  assert.equal(db.payments.length, 0);
});

test('COD orders cannot enter prepaid payment flow', () => {
  const db = seed(),
    v = db.product_variants[0];
  mutate(db, { ...delivery, source: 'direct', variant_id: v.id, quantity: 1 });
  assert.throws(() =>
    mutate(db, {
      type: 'start_payment',
      order_id: db.orders[0].id,
      gateway: 'mbbank',
    }),
  );
  assert.equal(db.payments[0].method, 'cod');
});

test('checkout uses explicit UI selection even when stored selection is stale', () => {
  const db = seed();
  const [v, w] = db.product_variants;
  mutate(db, { type: 'add', variant_id: v.id });
  mutate(db, { type: 'add', variant_id: w.id });
  const chosen = db.cart_items[0].id;
  db.cart_items[0].is_selected = false;
  db.cart_items[1].is_selected = true;
  mutate(db, {
    ...delivery,
    cart_item_ids: [chosen],
    payment_method: 'prepaid',
    gateway: 'momo',
  });
  assert.equal(db.order_items[0].variant_id, v.id);
  assert.equal(db.cart_items.length, 1);
  assert.equal(db.cart_items[0].variant_id, w.id);
  assert.equal(db.payments[0].gateway, 'momo');
  assert.equal(db.payments[0].method, 'ewallet');
});

test('direct prepaid checkout prepares QR immediately, without touching cart', () => {
  const db = seed(),
    v = db.product_variants[0];
  mutate(db, {
    ...delivery,
    source: 'direct',
    variant_id: v.id,
    quantity: 1,
    payment_method: 'prepaid',
  });
  assert.equal(db.payments[0].gateway, 'mbbank');
  assert.equal(db.payments[0].amount, db.orders[0].total_amount);
  assert.equal(db.cart_items.length, 0);
  assert.equal(db.orders[0].payment_status, 'unpaid');
});

test('guest cannot access private shop data or submit actions; login enables shop access', async () => {
  const server = startServer(0);
  await new Promise((resolve) => server.once('listening', resolve));
  try {
    const root = 'http://127.0.0.1:' + server.address().port;
    const guest = await (await fetch(root + '/api/shop')).json();
    assert.ok(guest.products.length);
    assert.deepEqual(guest.cart_items, []);
    assert.deepEqual(guest.orders, []);
    for (const type of ['add', 'checkout', 'start_payment']) {
      const response = await fetch(root + '/api/shop', {
        method: 'POST',
        body: JSON.stringify({ type }),
      });
      assert.equal(response.status, 401);
    }
    const login = await fetch(root + '/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'customer2@gmail.com', password: '123' }),
    });
    const { authToken } = await login.json();
    const response = await fetch(root + '/api/shop', {
      headers: { Authorization: 'Bearer ' + authToken },
    });
    assert.equal(response.status, 200);
    const authenticated = await response.json();
    assert.ok(authenticated.carts.length);
    const invalid = await fetch(root + '/api/shop', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + authToken },
      body: JSON.stringify({ type: 'bad' }),
    });
    assert.equal(invalid.status, 400);
    const expired = await fetch(root + '/api/shop', {
      headers: { Authorization: 'Bearer invalid' },
    });
    assert.equal(expired.status, 401);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});
