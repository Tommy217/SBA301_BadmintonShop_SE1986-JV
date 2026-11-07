import React, { useEffect, useState } from "react";
import { consoleApi } from "../services/fastShipApi";
import { formatDate, formatMoney, getStatusBadge } from "../format";
import { Link, navigate } from "../router";

// Chi tiet 1 yeu cau: duyet (cap ma van don) hoac tu choi
export default function RequestDetailPage({ id }) {
  const [s, setS] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ shippingFee: "", estimatedDays: "", shipperName: "", shipperPhone: "" });
  const [reason, setReason] = useState("");

  useEffect(() => {
    consoleApi.get(id).then(setS).catch((e) => setError(e.message));
  }, [id]);

  async function run(action) {
    setBusy(true);
    setError("");
    try {
      setS(await action());
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  function approve(e) {
    e.preventDefault();
    const body = {
      shippingFee: form.shippingFee === "" ? null : Number(form.shippingFee),
      estimatedDays: form.estimatedDays === "" ? null : Number(form.estimatedDays),
      shipperName: form.shipperName || null,
      shipperPhone: form.shipperPhone || null,
    };
    run(() => consoleApi.approve(id, body));
  }

  function reject(e) {
    e.preventDefault();
    run(() => consoleApi.reject(id, reason.trim()));
  }

  if (!s) {
    return (
      <main className="main-content">
        <div className="card">{error ? <div className="console-alert">{error}</div> : "Đang tải..."}</div>
      </main>
    );
  }

  const badge = getStatusBadge(s.status);
  const pending = s.status === "requested";

  return (
    <main className="main-content">
      <div className="card">
        <div className="console-head">
          <div>
            <Link to="/requests" className="console-back">← Yêu cầu tạo vận đơn</Link>
            <h1 className="console-title">Đơn {s.shopOrderCode || s.shopOrderId}</h1>
            <p className="console-desc">
              Vận đơn shop #{s.shopShipmentId} · gửi lúc {formatDate(s.createdAt)} ·{" "}
              <Link to={`/webhooks?shipmentId=${encodeURIComponent(s.id)}`}>Webhook đã gửi shop</Link>
            </p>
          </div>
          <span className={`badge-status ${badge.cls}`}>{badge.label}</span>
        </div>

        {error && <div className="console-alert">{error}</div>}

        <div className="details-grid">
          <div>
            <div className="info-group">
              <div className="info-label">Người nhận</div>
              <div className="info-val" style={{ fontWeight: 600 }}>{s.receiverName}</div>
              <div className="info-val">SĐT: {s.receiverPhone}</div>
            </div>
            <div className="info-group">
              <div className="info-label">Địa chỉ giao hàng</div>
              <div className="info-val">{s.shippingAddress}</div>
            </div>
            <div className="info-group">
              <div className="info-label">Kiện hàng</div>
              <div className="info-val">{s.packageDesc} · {s.packageWeightGram} g</div>
              {s.shopNote && <div className="info-val">Ghi chú của shop: {s.shopNote}</div>}
            </div>
            <div className="info-group">
              <div className="info-label">Dịch vụ</div>
              <div className="info-val">{s.serviceName} · phí dự kiến {formatMoney(s.shippingFee)}</div>
            </div>
            {s.trackingCode && (
              <div className="info-group">
                <div className="info-label">Mã vận đơn</div>
                <div className="info-val"><Link to={`/track?code=${s.trackingCode}`}>{s.trackingCode}</Link></div>
              </div>
            )}
            {s.rejectReason && (
              <div className="info-group">
                <div className="info-label">Lý do từ chối</div>
                <div className="info-val">{s.rejectReason}</div>
              </div>
            )}
          </div>

          <div>
            {pending ? (
              <>
                <form onSubmit={approve} className="console-form">
                  <h3>Duyệt & cấp mã vận đơn</h3>
                  <label>Phí vận chuyển (₫) <input type="number" min="0" placeholder={String(s.shippingFee)} value={form.shippingFee} onChange={(e) => setForm({ ...form, shippingFee: e.target.value })} /></label>
                  <label>Số ngày giao dự kiến <input type="number" min="0" max="30" placeholder="theo gói dịch vụ" value={form.estimatedDays} onChange={(e) => setForm({ ...form, estimatedDays: e.target.value })} /></label>
                  <label>Bưu tá <input maxLength={150} placeholder="Nguyễn Văn Tuấn (FastShip)" value={form.shipperName} onChange={(e) => setForm({ ...form, shipperName: e.target.value })} /></label>
                  <label>SĐT bưu tá <input maxLength={20} placeholder="0987.654.321" value={form.shipperPhone} onChange={(e) => setForm({ ...form, shipperPhone: e.target.value })} /></label>
                  <button className="search-btn" disabled={busy}>Duyệt yêu cầu</button>
                </form>
                <form onSubmit={reject} className="console-form">
                  <h3>Từ chối</h3>
                  <label>Lý do <textarea required maxLength={300} value={reason} onChange={(e) => setReason(e.target.value)} /></label>
                  <button className="btn-danger" disabled={busy || !reason.trim()}>Từ chối yêu cầu</button>
                </form>
              </>
            ) : (
              <div className="console-form">
                <h3>Đã xử lý</h3>
                <p>{s.statusText}</p>
                <button className="btn-copy" onClick={() => navigate("/shipments")}>Tới danh sách vận đơn</button>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
