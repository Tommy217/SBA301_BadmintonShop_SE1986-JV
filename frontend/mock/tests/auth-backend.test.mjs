import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { createApiHandler } from '../server.mjs';

const fixture = new URL('../db.json', import.meta.url);

async function withServers(run) {
  const calls = [];
  const identity = createServer((req, res) => {
    calls.push({ path: req.url, authorization: req.headers.authorization });
    res.setHeader('Content-Type', 'application/json');
    if (req.headers.authorization !== 'Bearer backend-token') {
      res.writeHead(401);
      res.end('{}');
    } else {
      res.end(JSON.stringify({ id: 4, email: 'customer@gmail.com', roles: ['CUSTOMER'] }));
    }
  });
  identity.listen(0, '127.0.0.1');
  await once(identity, 'listening');
  const backendUrl = 'http://127.0.0.1:' + identity.address().port;
  const shop = createServer(createApiHandler(fixture, backendUrl));
  shop.listen(0, '127.0.0.1');
  await once(shop, 'listening');
  try {
    await run('http://127.0.0.1:' + shop.address().port, calls);
  } finally {
    shop.closeAllConnections();
    identity.closeAllConnections();
    await Promise.all([
      new Promise((resolve) => shop.close(resolve)),
      new Promise((resolve) => identity.close(resolve)),
    ]);
  }
}

test('mock API no longer provides login or user management', async () => {
  await withServers(async (base) => {
    for (const path of ['/api/auth/login', '/api/users']) {
      const response = await fetch(base + path, { method: 'POST', body: '{}' });
      assert.equal(response.status, 404);
    }
  });
});

test('mock API rejects old tokens using Java identity endpoint', async () => {
  await withServers(async (base, calls) => {
    const response = await fetch(base + '/api/shop', {
      headers: { Authorization: 'Bearer mock-old' },
    });
    assert.equal(response.status, 401);
    assert.deepEqual(calls, [{ path: '/api/auth/me', authorization: 'Bearer mock-old' }]);
  });
});

test('customer cannot invoke management actions with a backend token', async () => {
  await withServers(async (base, calls) => {
    for (const type of [
      'manager_update_order',
      'review_support_ticket',
      'manager_decision_support_ticket',
    ]) {
      const response = await fetch(base + '/api/shop', {
        method: 'POST',
        headers: { Authorization: 'Bearer backend-token' },
        body: JSON.stringify({ type }),
      });
      assert.equal(response.status, 403, type);
    }
    assert.equal(calls.length, 3);
  });
});
