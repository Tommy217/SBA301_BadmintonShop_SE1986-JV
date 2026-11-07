import React, { useEffect, useState } from "react";
import { consoleApi } from "../services/fastShipApi";
import { formatDate, formatMoney } from "../format";
import { Link } from "../router";

// Hop thu yeu cau tao van don tu Badminton Shop (trang thai requested)
export default function RequestsPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");

  function load() {
    setError("");
    consoleApi.list("requested").then(setItems).catch((e) => setError(e.message));
  }

  useEffect(load, []);

  return (
    <main className="main-content">
      <div className="card">
        <div className="console-head">
          <div>
            <h1 className="console-title">Yêu cầu tạo vận đơn</h1>
            <p className="console-desc">Yêu cầu từ Badminton Shop đang chờ FastShip duyệt hoặc từ chối.</p>
          </div>
          <button className="btn-copy" onClick={load}>Tải lại</button>
        </div>

        {error && <div className="console-alert">{error}</div>}
        {!items && !error && <p>Đang tải...</p>}
        {items && items.length === 0 && <p className="console-empty">Không có yêu cầu nào đang chờ duyệt.</p>}

        {items && items.length > 0 && (
          <table className="console-table">
            <thead>
              <tr>
                <th>Đơn của shop</th>
                <th>Người nhận</th>
                <th>Dịch vụ</th>
                <th>Phí dự kiến</th>
                <th>Gửi lúc</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id}>
                  <td>
                    <strong>{s.shopOrderCode || s.shopOrderId}</strong>
                    <div className="console-sub">Vận đơn shop #{s.shopShipmentId}</div>
                  </td>
                  <td>
                    {s.receiverName}
                    <div className="console-sub">{s.receiverPhone} · {s.shippingAddress}</div>
                  </td>
                  <td>{s.serviceName}<div className="console-sub">{s.packageWeightGram} g</div></td>
                  <td>{formatMoney(s.shippingFee)}</td>
                  <td>{formatDate(s.createdAt)}</td>
                  <td><Link to={`/requests/${s.id}`} className="btn-copy">Xem & duyệt</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </main>
  );
}
