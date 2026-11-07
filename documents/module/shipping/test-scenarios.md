# KỊCH BẢN KIỂM THỬ MODULE SHIPPING & DELIVERY (FastShip)

Kịch bản kiểm thử tay cho luồng shop ↔ FastShip (giai đoạn 5). Luồng và API xem [`flow.md`](flow.md), API phía đối tác xem [`partner-system/README.md`](../../../partner-system/README.md).

## 1. Chuẩn bị

| Thành phần | Lệnh / địa chỉ | Ghi chú |
|---|---|---|
| SQL Server | chạy lại `badmintonshop.sql` | Script xoá và tạo lại DB (có cột mới `shipments.partner_version`) |
| MongoDB | `mongodb://localhost:27017` hoặc Atlas (`MONGODB_URI`) | Collection `partner_shipments`, `webhook_outbox` tự tạo |
| Partner BE | `cd partner-system/backend` → `mvn spring-boot:run` | Cổng 8081, webhook gửi về `http://localhost:8080/api/webhooks/carriers/fastship` |
| Partner FE | `cd partner-system/frontend` → `npm run dev` | Cổng 8082: `/requests`, `/shipments`, `/webhooks`, `/track` |
| Shop BE | `cd backend/badmintonshop` → `.\mvnw spring-boot:run` | Cổng 8080 |
| Shop FE | `cd frontend` → `npm run dev` | Cổng 5173 |

Hai bên phải dùng **cùng** `FASTSHIP_API_KEY` và **cùng** `FASTSHIP_WEBHOOK_SECRET` (chạy máy để trống cả hai thì dùng giá trị mặc định `dev-fastship-key`, `dev-webhook-secret`).

Muốn thử job đồng bộ định kỳ nhanh hơn 5 phút: đặt `FASTSHIP_SYNC_INTERVAL_MS=60000` cho shop BE.

## 2. Kịch bản

Ký hiệu: **Shop** = trang Quản lý giao hàng của shop (Staff / Manager), **FS** = màn nhân viên FastShip (cổng 8082).

| # | Kịch bản | Thao tác | Kết quả phải đạt |
|---|---|---|---|
| K1 | Tạo → duyệt → giao thành công (webhook) | Shop: tạo vận đơn cho đơn `confirmed`. FS `/requests`: Duyệt. FS `/shipments`: Lấy hàng → Phát hàng → Giao thành công. **Không bấm "Đồng bộ"** ở shop | Sau mỗi bước, tối đa khoảng 5–10 giây, shop (tải lại trang) thấy đúng trạng thái, mã `FS-xxxxxx`, phí ship. Hành trình có các mốc `source = webhook`. Đơn hàng: `shipping` → `completed`, đơn đã thanh toán có hoá đơn. FS `/webhooks`: các dòng **Đã gửi**, HTTP 200 |
| K2 | Tạo → từ chối | Shop tạo vận đơn. FS: Từ chối, nhập lý do | Shop: vận đơn `rejected`, mô tả "FastShip từ chối: <lý do>". Tạo được vận đơn mới cho đơn này |
| K3 | Huỷ trước khi lấy hàng | Shop: huỷ vận đơn ở `requested` hoặc `pending_pickup` | Hai bên đều `cancelled`. Đơn đang `shipping` về `preparing`. Huỷ khi `in_transit` → 409, không đổi gì |
| K4 | Shop tắt khi đối tác đổi trạng thái (outbox retry) | Tắt shop BE. FS: cập nhật 1 bước | FS `/webhooks`: dòng **Chờ gửi**, số lần tăng, "gửi lại <giờ>" (1, 2, 4, 8, 16 phút), lỗi `ConnectException`. Bật lại shop BE: tới lượt gửi lại thì tự **Đã gửi**, hoặc bấm **Bắn lại** để gửi ngay. Shop có trạng thái mới |
| K5 | Mất webhook (job pull sync tự sửa) | Dừng partner BE, đặt `SHOP_WEBHOOK_URL=` (để trống = tắt webhook), chạy lại. FS: cập nhật 1 bước. Chờ job (5 phút, hoặc 1 phút nếu đặt `FASTSHIP_SYNC_INTERVAL_MS`) | Shop tự có trạng thái mới, mốc hành trình `source = sync`. Log shop: `dong bo dinh ky: n/n van don` |
| K6 | Gửi cùng webhook 2 lần | FS `/webhooks`: bấm **Bắn lại** ở một dòng **Đã gửi** | Dòng vẫn **Đã gửi** (HTTP 200). Shop trả `DUPLICATE`, không thêm dòng `shipment_events`, trạng thái không đổi |
| K7 | Webhook sai chữ ký | Chạy lệnh ở mục 3 | `401`, `"Chữ ký webhook không hợp lệ."`; DB shop không đổi |
| K8 | Lệch secret | Đặt `FASTSHIP_WEBHOOK_SECRET` khác nhau ở hai bên, FS cập nhật 1 bước | FS `/webhooks`: lỗi HTTP 401, tự gửi lại theo lịch. Shop vẫn sửa được nhờ nút "Đồng bộ" / job |
| K9 | Đối tác tắt lúc tạo vận đơn | Tắt partner BE, shop tạo vận đơn | Vận đơn giữ `requested`, có nút "Gửi lại". Bật partner, bấm "Gửi lại" 2 lần → FastShip chỉ có 1 yêu cầu |
| K10 | Chuyển hoàn | FS: Lấy hàng → Giao thất bại → Chuyển hoàn | Đơn `returned`, tồn kho cộng lại, có `inventory_movements` loại `return_in` |
| K11 | Khách xem đơn | Đăng nhập khách → Đơn của tôi | Thấy trạng thái mới (trang tự làm mới mỗi 10 giây), link tra cứu FastShip mở đúng `/track?code=` |
| K12 | Đối soát | Chạy script mục 4 sau các kịch bản trên | `OK: hai hệ thống khớp nhau.` |

## 3. Lệnh kiểm tra nhanh

Webhook sai chữ ký (PowerShell, dùng `curl.exe`):

```powershell
curl.exe -i -X POST http://localhost:8080/api/webhooks/carriers/fastship `
  -H "Content-Type: application/json" -H "X-FastShip-Signature: sha256=00" `
  -d "{\"shopShipmentId\":1,\"data\":{\"id\":\"x\"}}"
```

SQL kiểm tra (shop):

```sql
SELECT id, order_id, partner_ref, partner_version, tracking_code, status, last_synced_at FROM shipments ORDER BY id DESC;
-- moi moc hanh trinh chi 1 dong (external_event_id unique), source = webhook / sync / manual / carrier
SELECT shipment_id, status, source, external_event_id, occurred_at FROM shipment_events ORDER BY id DESC;
SELECT order_id, from_status, to_status, note, created_at FROM order_status_history ORDER BY id DESC;
```

Mongo kiểm tra (FastShip, `mongosh`):

```js
use fastship_db
db.partner_shipments.find({}, { shopShipmentId: 1, trackingCode: 1, status: 1, version: 1 })
db.webhook_outbox.find({}, { eventId: 1, status: 1, state: 1, attempts: 1, lastError: 1 }).sort({ createdAt: -1 })
```

## 4. Script đối soát

So khớp vận đơn hai bên theo `shopShipmentId` (= `shipments.id`), qua API nên không cần driver DB. Cần Node 18+ và một tài khoản Staff / Manager / Admin của shop.

```powershell
$env:SHOP_EMAIL="manager@..."; $env:SHOP_PASSWORD="..."
node partner-system/scripts/reconcile.mjs
# tuy chon: $env:SHOP_API="https://<shop-be>"; $env:FASTSHIP_API="https://<service>.onrender.com/api/v1"
```

Kết quả: số vận đơn theo trạng thái ở từng bên, số khớp, số vận đơn chưa gửi sang FastShip, bảng các dòng lệch (lệch trạng thái, thiếu bên này / bên kia). Có lệch thì thoát mã 1: bấm "Đồng bộ" ở shop hoặc "Bắn lại" ở FastShip rồi chạy lại.

## 5. Test tự động

```powershell
cd backend\badmintonshop; .\mvnw clean test
cd partner-system\backend; mvn clean test
```

| Test | Nội dung chính |
|---|---|
| `ShipmentServiceTest` (shop) | Tạo / gửi lại / huỷ / đồng bộ; webhook: áp bản mới (`source = webhook`), bỏ mốc trùng, bỏ version cũ, không đổi vận đơn đã kết thúc, nhận `partnerRef` khi lần tạo bị timeout, sai `partnerRef` → 409 |
| `FastShipWebhookServiceTest` (shop) | Vector HMAC-SHA256 chuẩn; sai / thiếu chữ ký, sửa body sau khi ký → 401; JSON hỏng → 400; controller trả 200 + kết quả |
| `ShipmentSyncJobTest` (shop) | Chỉ quét vận đơn chưa kết thúc; lỗi 1 vận đơn không dừng cả lượt; đối tác lỗi 3 lần liên tiếp thì dừng |
| `AuthSecurityTest` (shop) | Webhook vào được khi không đăng nhập |
| `WebhookOutboxServiceTest` (FastShip) | Payload có `eventId`, `version`, ngày ISO; bỏ vận đơn mẫu; gửi thành công → SENT; lùi 1, 2, 4, 8, 16 phút rồi FAILED ở lần 6; "Bắn lại" gửi ngay |
| `PartnerShipmentServiceTest` (FastShip) | Duyệt / từ chối / cập nhật đúng thứ tự; mỗi lần đổi trạng thái ghi 1 webhook, tạo yêu cầu thì không |
