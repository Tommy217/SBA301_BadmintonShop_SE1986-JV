# FastShip Express - Hệ Thống Đối Tác Vận Chuyển Giả Lập (Mock Carrier)

Hệ thống độc lập mô phỏng đơn vị giao vận bên thứ 3 (như GHN / GHTK) cho đồ án **Badminton Shop (SBA301 SE1986-JV)**.

---

## 📌 Phân Bổ Cổng & Công Nghệ

| Thành phần | Cổng (Port) | Công nghệ | Database | Đường dẫn |
| :--- | :---: | :--- | :--- | :--- |
| **FastShip Backend (Partner API)** | **`8081`** | **Java Spring Boot (Java 21)** | **MongoDB local (`fastship_db`)** | `partner-system/backend` |
| **FastShip Frontend (Tra cứu + màn nhân viên)** | **`8082`** | **React.js (Vite)** | -- | `partner-system/frontend` |
| **Badminton Shop Backend** | **`8080`** | **Java Spring Boot (Java 21)** | **MS SQL Server (`badminton_shop`)** | Dự án Shop chính |
| **Badminton Shop Frontend** | **`5173`** | **React.js (Vite)** | -- | `frontend/` |

---

## 🗄️ Hướng Dẫn Cài Đặt Database MongoDB

1. Cài **MongoDB Community Server** (hoặc chạy Docker: `docker run -d -p 27017:27017 --name mongo mongo:7`).
2. Không cần chạy script: database `fastship_db` và collection `partner_shipments` tự tạo khi backend chạy lần đầu (`spring.data.mongodb.uri=mongodb://localhost:27017/fastship_db`).
3. Khi collection còn trống, backend tự nạp 2 vận đơn mẫu `FS-882910`, `FS-449102` (tắt bằng `fastship.seed-demo=false`).

Mỗi document là 1 vận đơn, hành trình nhúng trong mảng `events[]`. Unique index `shopCode + shopShipmentId` (shop gửi lại không tạo trùng) và `trackingCode` (sparse, chỉ có sau khi duyệt).

---

## 🚀 Hướng Dẫn Khởi Chạy

### 1. Khởi chạy Backend Đối Tác (Port 8081)
Mở terminal tại thư mục `partner-system/backend` (hoặc mở trong IntelliJ / VS Code / Eclipse):
```bash
# Chạy qua Maven
mvn spring-boot:run
```
*(Hoặc chạy trực tiếp file `com.fastship.FastShipApplication.java`)*

### 2. Khởi chạy Frontend Tra Cứu Đối Tác (Port 8082)
Mở terminal tại thư mục `partner-system/frontend`:
```bash
npm install
npm run dev
```
Các màn hình:
- 👉 **`http://localhost:8082/track?code=FS-882910`**: tra cứu công khai (shop mở link này cho khách).
- 👉 **`http://localhost:8082/requests`**: hộp thư yêu cầu từ Badminton Shop, duyệt (cấp mã vận đơn) hoặc từ chối.
- 👉 **`http://localhost:8082/shipments`**: danh sách vận đơn, bưu tá cập nhật hành trình (thay sandbox cũ).
- 👉 **`http://localhost:8082/webhooks`**: nhật ký webhook gửi shop (đã gửi / chờ gửi lại / thất bại), nút **Bắn lại**.

---

## 🔌 API

| Method | URL | Dùng cho | Ghi chú |
| :--- | :--- | :--- | :--- |
| POST | `/api/v1/shipment-requests` | Shop | Tạo yêu cầu `requested`, trả `id` (= `partnerRef` bên shop). Gửi lại cùng `shopShipmentId` trả yêu cầu cũ |
| GET | `/api/v1/shipments/{id}` | Shop | Đọc trạng thái + hành trình (shop "Đồng bộ") |
| POST | `/api/v1/shipments/{id}/cancel` | Shop | Huỷ khi chưa lấy hàng, body `{reason}` |
| GET | `/api/v1/console/shipments?status=` | Nhân viên FastShip | Danh sách |
| POST | `/api/v1/console/shipments/{id}/approve` | Nhân viên FastShip | Duyệt, cấp mã `FS-xxxxxx` |
| POST | `/api/v1/console/shipments/{id}/reject` | Nhân viên FastShip | Từ chối, body `{reason}` |
| POST | `/api/v1/console/shipments/{id}/status` | Bưu tá | `{status, note, location}`, chỉ đúng thứ tự các bước |
| GET | `/api/v1/track/{trackingCode}` | Công khai | Tra cứu |
| GET | `/api/v1/console/webhooks?shipmentId=&state=` | Nhân viên FastShip | Nhật ký webhook (100 bản ghi mới nhất), `state` = `PENDING` / `SENT` / `FAILED` |
| GET | `/api/v1/console/webhooks/config` | Nhân viên FastShip | Địa chỉ shop đang nhận webhook (không trả secret) |
| POST | `/api/v1/console/webhooks/{id}/retry` | Nhân viên FastShip | Gửi lại ngay (cả bản ghi đã gửi, shop tự bỏ qua trùng) |

API cho shop bắt buộc header **`X-Api-Key`** (`fastship.api-key`, mặc định `dev-fastship-key`; bên shop đặt cùng giá trị ở `fastship.api-key` của `backend/badmintonshop`).

### Webhook FastShip → shop

Mỗi lần duyệt / từ chối / đổi trạng thái / shop huỷ, FastShip ghi 1 bản ghi vào collection `webhook_outbox` rồi `WebhookDispatcher` (5 giây/lần) gửi `POST` tới `fastship.webhook.url` (mặc định `http://localhost:8080/api/webhooks/carriers/fastship`):

- Header `X-FastShip-Signature: sha256=<hex HMAC-SHA256(fastship.webhook.secret, body)>`, `X-FastShip-Event-Id`.
- Body: `eventId`, `type`, `occurredAt`, `shopShipmentId`, `partnerRef`, `status`, `version`, `data` (cả vận đơn, giống `GET /api/v1/shipments/{id}`).
- Lỗi (shop tắt, HTTP khác 2xx) thì gửi lại sau 1, 2, 4, 8, 16 phút; lần thứ 6 vẫn lỗi → `FAILED`. Shop vẫn tự sửa lệch bằng nút "Đồng bộ" và job 5 phút.
- `fastship.webhook.url` để trống thì tắt webhook. Vận đơn mẫu (FS-882910, FS-449102) không gửi webhook.

Đối soát 2 bên: `node partner-system/scripts/reconcile.mjs` (xem `documents/module/shipping/test-scenarios.md`).

---

## ☁️ Deploy (Vercel + Render + MongoDB Atlas)

| Thành phần | Nơi chạy | Cấu hình |
| :--- | :--- | :--- |
| FastShip FE | Vercel, Root Directory `partner-system/frontend`, framework Vite | `VITE_FASTSHIP_API=https://<service>.onrender.com/api/v1`; `vercel.json` đưa mọi đường dẫn (`/track`, `/requests`, `/shipments`, `/webhooks`) về `index.html` |
| FastShip BE | Render Web Service, Root Directory `partner-system/backend`, Runtime Docker (`Dockerfile`) | Biến môi trường bên dưới, không đặt `PORT` |
| Database | MongoDB Atlas | Network Access `0.0.0.0/0` |

Biến môi trường trên Render (nhập từng biến, không bọc dấu nháy):

| Biến | Giá trị |
| :--- | :--- |
| `MONGODB_URI` | `mongodb+srv://<user>:<password>@<cluster>.mongodb.net/fastship_db?retryWrites=true&w=majority` |
| `MONGODB_DATABASE` | `fastship_db` |
| `FASTSHIP_API_KEY` | chuỗi ngẫu nhiên ≥ 32 ký tự, **đặt trùng** `FASTSHIP_API_KEY` của shop backend |
| `FASTSHIP_PORTAL_URL` | `https://<app>.vercel.app/track` |
| `CORS_ORIGINS` | `https://<app>.vercel.app,http://localhost:8082` |
| `SHOP_WEBHOOK_URL` | `https://<shop-backend>/api/webhooks/carriers/fastship` (shop phải truy cập được từ Internet; shop chạy `localhost` thì để **trống** để tắt webhook, shop vẫn đồng bộ bằng nút / job) |
| `FASTSHIP_WEBHOOK_SECRET` | chuỗi ngẫu nhiên ≥ 32 ký tự, **đặt trùng** `FASTSHIP_WEBHOOK_SECRET` của shop backend |

Bí mật (chuỗi Atlas, API key, webhook secret) **không ghi vào file trong repo**. Chạy máy với Atlas: đặt các biến trên trong Run Configuration của IntelliJ (Environment variables).

Shop backend (deploy riêng) cần: `FASTSHIP_API_URL=https://<service>.onrender.com`, `FASTSHIP_API_KEY` và `FASTSHIP_WEBHOOK_SECRET` trùng bên trên; tuỳ chọn `FASTSHIP_SYNC_INTERVAL_MS` (mặc định 300000 = 5 phút). Job đồng bộ của shop gọi Render đều đặn nên cũng giúp FastShip ít bị "ngủ" khi còn vận đơn đang giao. Shop frontend: `VITE_FASTSHIP_PORTAL_URL=https://<app>.vercel.app/track`.
