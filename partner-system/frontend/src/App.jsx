import React from "react";
import { Link, useLocationPath } from "./router";
import TrackPage from "./pages/TrackPage";
import RequestsPage from "./pages/RequestsPage";
import RequestDetailPage from "./pages/RequestDetailPage";
import ShipmentsPage from "./pages/ShipmentsPage";

// /track?code= (cong khai, shop redirect vao day), /requests, /requests/:id, /shipments (nhan vien FastShip)
export default function App() {
  const { path, search } = useLocationPath();
  const detail = path.match(/^\/requests\/([^/]+)$/);

  let page;
  if (detail) page = <RequestDetailPage id={decodeURIComponent(detail[1])} />;
  else if (path === "/requests") page = <RequestsPage />;
  else if (path === "/shipments") page = <ShipmentsPage />;
  else page = <TrackPage key={search} />;

  const active = (prefix) => (path.startsWith(prefix) ? "nav-link active" : "nav-link");

  return (
    <div>
      <header className="header">
        <div className="header-inner">
          <Link to="/track" className="brand">
            <div className="brand-logo">⚡</div>
            <div>
              <div className="brand-title">FastShip <span>Express</span></div>
              <div className="brand-sub">Đối tác vận chuyển chính thức</div>
            </div>
          </Link>
          <nav className="nav">
            <Link to="/track" className={active("/track")}>Tra cứu</Link>
            <Link to="/requests" className={active("/requests")}>Yêu cầu mới</Link>
            <Link to="/shipments" className={active("/shipments")}>Vận đơn</Link>
            <a href="tel:19006868" className="hotline-badge">📞 <strong>1900 6868</strong></a>
          </nav>
        </div>
      </header>
      {page}
    </div>
  );
}
