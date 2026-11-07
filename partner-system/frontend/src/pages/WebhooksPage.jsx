import React, { useEffect, useState } from "react";
import { consoleApi } from "../services/fastShipApi";
import { formatDate, getStatusBadge } from "../format";
import { Link } from "../router";

const STATES = [
  ["", "Tất cả"],
  ["PENDING", "Chờ gửi / gửi lại"],
  ["SENT", "Đã gửi"],
  ["FAILED", "Thất bại"],
];

const STATE_BADGE = {
  PENDING: { label: "Chờ gửi", cls: "badge-warning" },
  SENT: { label: "Đã gửi", cls: "badge-success" },
  FAILED: { label: "Thất bại", cls: "badge-danger" },
};

// Nhat ky webhook FastShip -> shop: moi lan duyet / tu choi / doi trang thai la 1 dong; loi thi tu gui lai, het luot thi bam "Ban lai"
export default function WebhooksPage({ shipmentId }) {
  const [state, setState] = useState("");
  const [items, setItems] = useState(null);
  const [config, setConfig] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyId, setBusyId] = useState(null);

  function load() {
    consoleApi
      .webhooks(shipmentId, state)
      .then((list) => {
        setItems(list);
        setError("");
      })
      .catch((e) => setError(e.message));
  }

  useEffect(() => {
    consoleApi.webhookConfig().then(setConfig).catch(() => setConfig(null));
  }, []);

  useEffect(() => {
    setItems(null);
    load();
    // tu lam moi de thay webhook chuyen tu "Chờ gửi" sang "Đã gửi"
    const timer = window.setInterval(load, 5000);
    return () => window.clearInterval(timer);
  }, [shipmentId, state]); // eslint-disable-line react-hooks/exhaustive-deps

  async function retry(w) {
    setBusyId(w.id);
    setError("");
    try {
      const updated = await consoleApi.retryWebhook(w.id);
      setItems((list) => list.map((x) => (x.id === updated.id ? updated : x)));
      setNotice(
        updated.state === "SENT"
          ? `[${new Date().toLocaleTimeString("vi-VN")}] Đã gửi lại sự kiện ${updated.eventId} cho shop.`
          : `[${new Date().toLocaleTimeString("vi-VN")}] Gửi lại chưa được: ${updated.lastError || "lỗi không rõ"}`,
      );
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
            {shipmentId && <Link to={`/requests/${shipmentId}`} className="console-back">← Về vận đơn</Link>}
            <h1 className="console-title">Nhật ký webhook</h1>
            <p className="console-desc">
              FastShip báo cho shop mỗi khi vận đơn đổi trạng thái. Gửi lỗi sẽ tự gửi lại sau 1, 2, 4, 8, 16 phút (tối đa 6 lần).
            </p>
            {config && (
              <p className="console-sub">
                {config.enabled ? <>Địa chỉ shop: <code>{config.url}</code></> : "Webhook đang tắt (chưa đặt SHOP_WEBHOOK_URL)."}
              </p>
            )}
          </div>
          <select className="console-select" value={state} onChange={(e) => setState(e.target.value)}>
            {STATES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </div>

        {error && <div className="console-alert">{error}</div>}
        {notice && <div className="sandbox-log">{notice}</div>}
        {!items && !error && <p>Đang tải...</p>}
        {items && items.length === 0 && <p className="console-empty">Chưa có webhook nào.</p>}

        {items && items.length > 0 && (
          <table className="console-table">
            <thead>
              <tr>
                <th>Thời điểm</th>
                <th>Vận đơn</th>
                <th>Sự kiện</th>
                <th>Gửi shop</th>
                <th>Lỗi gần nhất</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((w) => {
                const status = getStatusBadge(w.status);
                const sent = STATE_BADGE[w.state] || { label: w.state, cls: "badge-info" };
                return (
                  <tr key={w.id}>
                    <td>{formatDate(w.createdAt)}<div className="console-sub">{w.eventId}</div></td>
                    <td>
                      <Link to={`/requests/${w.shipmentId}`}><strong>{w.trackingCode || "Chưa cấp mã"}</strong></Link>
                      <div className="console-sub">Vận đơn shop #{w.shopShipmentId}</div>
                    </td>
                    <td><span className={`badge-status ${status.cls}`}>{status.label}</span></td>
                    <td>
                      <span className={`badge-status ${sent.cls}`}>{sent.label}</span>
                      <div className="console-sub">
                        {w.attempts} lần{w.lastStatusCode ? ` · HTTP ${w.lastStatusCode}` : ""}
                        {w.state === "PENDING" && w.nextRetryAt && w.attempts > 0 && <> · gửi lại {formatDate(w.nextRetryAt)}</>}
                        {w.state === "SENT" && w.sentAt && <> · {formatDate(w.sentAt)}</>}
                      </div>
                    </td>
                    <td className="console-sub">{w.lastError || "-"}</td>
                    <td>
                      {/* ca webhook da gui cung ban lai duoc: shop phai bo qua (khong ghi trung) */}
                      <button className="btn-copy" disabled={busyId === w.id} onClick={() => retry(w)}>
                        {busyId === w.id ? "Đang gửi..." : "Bắn lại"}
                      </button>
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
