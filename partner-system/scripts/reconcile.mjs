#!/usr/bin/env node
// So khop van don giua Badminton Shop (SQL Server) va FastShip (MongoDB) qua API, khong can driver DB.
// Khop theo shopShipmentId (= shipments.id ben shop). Thoat ma 1 neu co lech.
//
// Chay (Node 18+), dung tai khoan STAFF / MANAGER / ADMIN cua shop:
//   SHOP_EMAIL=manager@... SHOP_PASSWORD=... node partner-system/scripts/reconcile.mjs
// Bien tuy chon: SHOP_API (mac dinh http://localhost:8080), FASTSHIP_API (mac dinh http://localhost:8081/api/v1)

const SHOP_API = (process.env.SHOP_API || "http://localhost:8080").replace(/\/+$/, "");
const FASTSHIP_API = (process.env.FASTSHIP_API || "http://localhost:8081/api/v1").replace(/\/+$/, "");
const { SHOP_EMAIL, SHOP_PASSWORD } = process.env;

async function json(url, options = {}) {
  const res = await fetch(url, { ...options, headers: { "Content-Type": "application/json", ...(options.headers || {}) } });
  const body = await res.json().catch(() => null);
  if (!res.ok) throw new Error(`${options.method || "GET"} ${url} -> HTTP ${res.status} ${body?.message || ""}`.trim());
  return body;
}

async function shopToken() {
  if (!SHOP_EMAIL || !SHOP_PASSWORD) {
    throw new Error("Thiếu SHOP_EMAIL / SHOP_PASSWORD (tài khoản STAFF, MANAGER hoặc ADMIN của shop).");
  }
  const body = await json(`${SHOP_API}/api/auth/login`, {
    method: "POST",
    body: JSON.stringify({ email: SHOP_EMAIL, password: SHOP_PASSWORD }),
  });
  const token = body?.accessToken || body?.authToken || body?.data?.accessToken;
  if (!token) throw new Error("Đăng nhập shop không trả token.");
  return token;
}

async function shopShipments(token) {
  const all = [];
  for (let page = 0; ; page++) {
    const body = await json(`${SHOP_API}/api/manager/shipments?page=${page}&size=100`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const data = body?.data || {};
    all.push(...(data.content || []));
    if (page + 1 >= (data.totalPages || 0)) return all;
  }
}

async function partnerShipments() {
  const body = await json(`${FASTSHIP_API}/console/shipments`);
  // bo van don mau (shopShipmentId <= 0)
  return (body?.data || []).filter((s) => Number(s.shopShipmentId) > 0);
}

function countBy(list, key) {
  return list.reduce((acc, item) => ({ ...acc, [item[key]]: (acc[item[key]] || 0) + 1 }), {});
}

async function main() {
  const [shop, partner] = await Promise.all([shopToken().then(shopShipments), partnerShipments()]);
  const partnerById = new Map(partner.map((p) => [Number(p.shopShipmentId), p]));
  const shopIds = new Set(shop.map((s) => Number(s.id)));
  const problems = [];
  let matched = 0;
  let unsent = 0;

  for (const s of shop) {
    const p = partnerById.get(Number(s.id));
    if (!p) {
      if (s.status === "requested" && !s.partnerRef) unsent++; // chua gui duoc, shop bam "Gui lai"
      else if (s.partnerRef) problems.push({ shopShipmentId: s.id, issue: "FastShip không có vận đơn", shop: s.status, partner: "-" });
      continue;
    }
    if (s.partnerRef && s.partnerRef !== p.id) {
      problems.push({ shopShipmentId: s.id, issue: `partnerRef khác (${s.partnerRef} / ${p.id})`, shop: s.status, partner: p.status });
    } else if (s.status !== p.status) {
      problems.push({ shopShipmentId: s.id, issue: "Lệch trạng thái", shop: s.status, partner: p.status });
    } else {
      matched++;
    }
  }
  for (const p of partner) {
    if (!shopIds.has(Number(p.shopShipmentId))) {
      problems.push({ shopShipmentId: p.shopShipmentId, issue: "Shop không có vận đơn", shop: "-", partner: p.status });
    }
  }

  console.log(`Shop:     ${shop.length} vận đơn`, countBy(shop, "status"));
  console.log(`FastShip: ${partner.length} vận đơn`, countBy(partner, "status"));
  console.log(`Khớp: ${matched} · Chưa gửi sang FastShip: ${unsent} · Lệch: ${problems.length}`);
  if (problems.length) {
    console.table(problems);
    console.log('Gợi ý: bấm "Đồng bộ" ở trang vận đơn của shop, hoặc "Bắn lại" ở Nhật ký webhook của FastShip, rồi chạy lại script.');
    process.exitCode = 1;
  } else {
    console.log("OK: hai hệ thống khớp nhau.");
  }
}

main().catch((e) => {
  console.error("Lỗi:", e.message);
  process.exitCode = 2;
});
