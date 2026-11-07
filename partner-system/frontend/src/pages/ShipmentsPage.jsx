import React, { useEffect, useState } from "react";
import { consoleApi } from "../services/fastShipApi";
import { COURIER_NEXT, formatDate, getStatusBadge } from "../format";
import { Link } from "../router";

const FILTERS = [
  ["", "Tất cả"],
  ["requested", "Chờ duyệt"],
  ["pending_pickup", "Chờ lấy hàng"],
  ["in_transit", "Đang trung chuyển"],
  ["out_for_delivery", "Đang phát"],
  ["delivery_failed", "Giao thất bại"],
  ["delivered", "Giao thành công"],
  ["returned", "Chuyển hoàn"],
  ["cancelled", "Đã huỷ"],
  ["rejected", "Từ chối"],
];

// Danh sach van don + buu ta cap nhat hanh trinh (thay sandbox cu)
export default function ShipmentsPage() {
  const [status, setStatus] = useState("");
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [log, setLog] = useState("");

  function load(filter = status) {
    setError("");
    consoleApi.list(filter).then(setItems).catch((e) => setError(e.message));
  }

  useEffect(() => load(status), [status]); // eslint-disable-line react-hooks/exhaustive-deps

  async function advance(s, next, label) {
    const note = next === "delivery_failed" ? window.prompt("Lý do giao thất bại?", "Không liên lạc được người nhận") : null;
    if (next === "delivery_failed" && note === null) return;
    setBusyId(s.id);
    setError("");
    try {
      const updated = await consoleApi.updateStatus(s.id, next, note || null, null);
      setItems((list) => list.map((x) => (x.id === updated.id ? updated : x)));
      setLog(`[${new Date().toLocaleTimeString("vi-VN")}] ${updated.trackingCode}: ${label}. Shop bấm "Đồng bộ" để nhận trạng thái mới.`);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <main className="main-content">
      <div className="card">
        <div className="console-head">
          <div>
            <h1 className="console-title">Vận đơn FastShip</h1>
            <p className="console-desc">Bưu tá cập nhật hành trình theo đúng thứ tự các bước giao hàng.</p>
          </div>
          <select className="console-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            {FILTERS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </div>

        {error && <div className="console-alert">{error}</div>}
        {log && <div className="sandbox-log">{log}</div>}
        {!items && !error && <p>Đang tải...</p>}
        {items && items.length === 0 && <p className="console-empty">Không có vận đơn.</p>}

        {items && items.length > 0 && (
          <table className="console-table">
            <thead>
              <tr>
                <th>Mã vận đơn</th>
                <th>Người nhận</th>
                <th>Trạng thái</th>
                <th>Cập nhật</th>
                <th>Thao tác bưu tá</th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => {
                const badge = getStatusBadge(s.status);
                return (
                  <tr key={s.id}>
                    <td>
                      {s.trackingCode ? (
                        <Link to={`/track?code=${s.trackingCode}`}><strong>{s.trackingCode}</strong></Link>
                      ) : (
                        <Link to={`/requests/${s.id}`}>Chưa cấp mã</Link>
                      )}
                      <div className="console-sub">Đơn {s.shopOrderCode || s.shopOrderId || "-"}</div>
                    </td>
                    <td>{s.receiverName}<div className="console-sub">{s.shippingAddress}</div></td>
                    <td><span className={`badge-status ${badge.cls}`}>{badge.label}</span></td>
                    <td>{formatDate(s.updatedAt)}</td>
                    <td>
                      <div className="console-actions">
                        {s.status === "requested" && <Link to={`/requests/${s.id}`} className="btn-copy">Duyệt / từ chối</Link>}
                        {(COURIER_NEXT[s.status] || []).map(([next, label]) => (
                          <button key={next} className="btn-copy" disabled={busyId === s.id} onClick={() => advance(s, next, label)}>
                            {label}
                          </button>
                        ))}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </main>
  );
}
