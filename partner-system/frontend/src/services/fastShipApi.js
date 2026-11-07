const API_BASE = import.meta.env.VITE_FASTSHIP_API || "http://localhost:8081/api/v1";

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
  });
  let json = null;
  try {
    json = await res.json();
  } catch {
    // bo qua body rong
  }
  if (!res.ok) {
    throw new Error(json?.message || `FastShip API lỗi (HTTP ${res.status}).`);
  }
  return json?.data;
}

const post = (path, body) => request(path, { method: "POST", body: JSON.stringify(body ?? {}) });

// Tra cuu cong khai
export const trackShipment = (code) => request(`/track/${encodeURIComponent(code)}`);

// Man hinh nhan vien FastShip
export const consoleApi = {
  list: (status) => request(`/console/shipments${status ? `?status=${encodeURIComponent(status)}` : ""}`),
  get: (id) => request(`/console/shipments/${encodeURIComponent(id)}`),
  approve: (id, body) => post(`/console/shipments/${encodeURIComponent(id)}/approve`, body),
  reject: (id, reason) => post(`/console/shipments/${encodeURIComponent(id)}/reject`, { reason }),
  updateStatus: (id, status, note, location) =>
    post(`/console/shipments/${encodeURIComponent(id)}/status`, { status, note, location }),
};
