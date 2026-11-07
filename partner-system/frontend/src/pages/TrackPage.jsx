import React, { useState, useEffect } from "react";
import { trackShipment } from "../services/fastShipApi";
import { formatDate, formatMoney, getStatusBadge } from "../format";

export default function TrackPage() {
  const [code, setCode] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [shipment, setShipment] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initialCode = params.get("code") || params.get("tracking_number") || "FS-882910";
    setCode(initialCode);
    setSearchInput(initialCode);
    loadData(initialCode);
  }, []);

  async function loadData(targetCode) {
    if (!targetCode) return;
    setLoading(true);
    setError("");
    try {
      const data = await trackShipment(targetCode);
      setShipment(data);
    } catch (err) {
      setError(err.message || "Không tìm thấy thông tin vận đơn.");
      setShipment(null);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(e) {
    e.preventDefault();
    const trimmed = searchInput.trim();
    if (trimmed) {
      setCode(trimmed);
      const newUrl = window.location.pathname + "?code=" + encodeURIComponent(trimmed);
      window.history.pushState({}, "", newUrl);
      loadData(trimmed);
    }
  }

  // Calculate step: 1 (pickup), 2 (in_transit), 3 (out), 4 (delivered)
  let step = 1;
  if (shipment) {
    if (shipment.status === "in_transit" || shipment.status === "picked_up") step = 2;
    else if (shipment.status === "out_for_delivery") step = 3;
    else if (shipment.status === "delivered") step = 4;
    else if (shipment.status === "delivery_failed") step = 3;
  }

  const badge = shipment ? getStatusBadge(shipment.status) : null;

  return (
    <div>
      {/* Hero Banner */}
      <section className="hero-banner">
        <h1 className="hero-title">Tra Cứu Hành Trình Bưu Phẩm</h1>
        <p className="hero-desc">Theo dõi vị trí và tiến độ giao nhận bưu kiện theo thời gian thực trên toàn quốc.</p>
        <form className="search-box" onSubmit={handleSearch}>
          <input
            type="text"
            className="search-input"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Nhập mã vận đơn (VD: FS-882910, FS-449102)..."
          />
          <button type="submit" className="search-btn">Tra cứu 🔍</button>
        </form>
      </section>

      {/* Main Content */}
      <main className="main-content">
        {loading && (
          <div className="card" style={{ textAlign: "center", padding: "40px" }}>
            <p>Đang kết nối tới FastShip API (Port 8081)...</p>
          </div>
        )}

        {error && !loading && (
          <div className="card" style={{ textAlign: "center", padding: "40px" }}>
            <div style={{ fontSize: "2.5rem", color: "var(--danger)" }}>⚠️</div>
            <h2 style={{ margin: "10px 0" }}>Không tìm thấy vận đơn</h2>
            <p style={{ color: "var(--gray-500)" }}>{error}</p>
          </div>
        )}

        {shipment && !loading && (
          <>
            <div className="card">
              <div className="overview-top">
                <div className="tracking-code-wrap">
                  <div>
                    <div className="info-label">MÃ VẬN ĐƠN BƯU PHẨM</div>
                    <div className="tracking-code">{shipment.trackingCode}</div>
                  </div>
                  <button className="btn-copy" onClick={() => navigator.clipboard.writeText(shipment.trackingCode)}>
                    Sao chép
                  </button>
                </div>
                <span className={`badge-status ${badge.cls}`}>{badge.label}</span>
              </div>

              {/* Stepper */}
              <div className="stepper">
                <div className={`step-item ${step >= 1 ? (step === 1 ? 'active' : 'completed') : ''}`}>
                  <div className="step-icon">{step > 1 ? '✓' : '1'}</div>
                  <div className="step-label">Tiếp nhận đơn</div>
                  <div className="step-desc">Shop tạo đơn</div>
                </div>
                <div className={`step-item ${step >= 2 ? (step === 2 ? 'active' : 'completed') : ''}`}>
                  <div className="step-icon">{step > 2 ? '✓' : '2'}</div>
                  <div className="step-label">Bưu cục lấy hàng</div>
                  <div className="step-desc">Đang luân chuyển</div>
                </div>
                <div className={`step-item ${step >= 3 ? (step === 3 ? 'active' : 'completed') : ''}`}>
                  <div className="step-icon">{step > 3 ? '✓' : '3'}</div>
                  <div className="step-label">Đang phát hàng</div>
                  <div className="step-desc">Bưu tá đi giao</div>
                </div>
                <div className={`step-item ${step === 4 ? 'completed active' : ''}`}>
                  <div className="step-icon">{step === 4 ? '✓' : '4'}</div>
                  <div className="step-label">Giao thành công</div>
                  <div className="step-desc">Ký nhận kiện hàng</div>
                </div>
              </div>

              {/* Details & Timeline Grid */}
              <div className="details-grid">
                <div>
                  <h3 style={{ fontSize: "1.05rem", fontWeight: 700, marginBottom: "16px" }}>
                    Lịch sử hành trình bưu cục
                  </h3>
                  <div className="timeline">
                    {(shipment.events || []).map((evt) => (
                      <div className="timeline-item" key={evt.id || evt.occurredAt}>
                        <div className="timeline-dot"></div>
                        <div className="timeline-time">{formatDate(evt.occurredAt)}</div>
                        <div className="timeline-title">{evt.title}</div>
                        <div className="timeline-location">📍 {evt.location}</div>
                        {evt.description && <div className="timeline-desc">{evt.description}</div>}
                      </div>
                    ))}
                  </div>
                </div>

                <div>
                  <h3 style={{ fontSize: "1.05rem", fontWeight: 700, marginBottom: "16px" }}>
                    Thông tin kiện hàng & người nhận
                  </h3>
                  <div className="info-group">
                    <div className="info-label">Người nhận</div>
                    <div className="info-val" style={{ fontWeight: 600 }}>{shipment.receiverName}</div>
                    <div className="info-val" style={{ color: "var(--gray-500)", fontSize: "0.85rem" }}>
                      SĐT: {shipment.receiverPhone}
                    </div>
                  </div>
                  <div className="info-group">
                    <div className="info-label">Địa chỉ giao hàng</div>
                    <div className="info-val">{shipment.shippingAddress}</div>
                  </div>
                  <div className="info-group">
                    <div className="info-label">Sản phẩm & Khối lượng</div>
                    <div className="info-val">{shipment.packageDesc || "Sản phẩm cầu lông"}</div>
                    <div className="info-val" style={{ color: "var(--gray-500)", fontSize: "0.85rem" }}>
                      Khối lượng: {shipment.packageWeightGram || 500}g
                    </div>
                  </div>
                  <div className="info-group">
                    <div className="info-label">Dịch vụ & Phí vận chuyển</div>
                    <div className="info-val" style={{ color: "var(--primary)", fontWeight: 700 }}>
                      {shipment.serviceName}
                    </div>
                    <div className="info-val" style={{ fontWeight: 700 }}>
                      Phí: {formatMoney(shipment.shippingFee)} · Không thu hộ
                    </div>
                  </div>

                  <div className="shipper-card">
                    <div className="shipper-avatar">🛵</div>
                    <div>
                      <div style={{ fontSize: "0.75rem", color: "var(--gray-500)", textTransform: "uppercase", fontWeight: 600 }}>
                        Bưu tá phụ trách
                      </div>
                      <div style={{ fontWeight: 700 }}>{shipment.shipperName || "Nguyễn Văn Tuấn (FastShip)"}</div>
                      <a href={`tel:${shipment.shipperPhone}`} style={{ color: "var(--primary)", fontSize: "0.85rem", textDecoration: "none", fontWeight: 600 }}>
                        📞 {shipment.shipperPhone}
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </>
        )}
      </main>
    </div>
  );
}
