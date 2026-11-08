import test from 'node:test';
import assert from 'node:assert/strict';
import { createCommerceActionSender } from '../src/app/providers/shop/commerceTransport.js';

const cases = [
  ['add', {}, 'post', '/cart/items'],
  ['update', { id: '17' }, 'patch', '/cart/items/17'],
  ['remove', { id: '17' }, 'delete', '/cart/items/17'],
  ['select_all', {}, 'patch', '/cart/selection'],
  ['checkout', {}, 'post', '/orders'],
  ['cancel_order', { order_id: '19' }, 'post', '/orders/19/cancel'],
  ['manager_update_order', { id: '19' }, 'patch', '/orders/19/status'],
  ['start_payment', { order_id: '19' }, 'post', '/orders/19/payment'],
  ['cancel_payment', { order_id: '19' }, 'post', '/orders/19/payment/cancel'],
  ['create_support_ticket', {}, 'post', '/support/actions'],
];
for (const [type, fields, method, path] of cases) {
  test(`${type} calls its domain endpoint and returns the committed snapshot`, async () => {
    const response = { orders: [{ id: '19' }] };
    const http = Object.fromEntries(
      ['post', 'patch', 'delete'].map((verb) => [
        verb,
        (...args) => {
          assert.equal(verb, method);
          assert.equal(args[0], path);
          assert.deepEqual(args.at(-1), { timeout: 30000 });
          if (verb !== 'delete') assert.deepEqual(args[1], { type, ...fields });
          return Promise.resolve(response);
        },
      ]),
    );
    assert.equal(await createCommerceActionSender(http)({ type, ...fields }), response);
  });
}
