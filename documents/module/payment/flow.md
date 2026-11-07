# LUỒNG HOẠT ĐỘNG MODULE THANH TOÁN (PAYMENT FLOW)
> **Dự án:** SBA301_BadmintonShop_SE1986-JV  
> **Cổng thanh toán:** payOS (VietQR Napas 24/7) 
> **Quyết định phạm vi (Scope Decision):** **LOẠI BỎ HOÀN TOÀN COD**. 100% đơn hàng online bắt buộc thanh toán trực tuyến qua payOS VietQR trước khi xuất kho giao hàng.  
> **Kiến trúc:** Java 21, Spring Boot, Spring Data JPA, Spring Security, SQL Server, Event-Driven

---

## 1. TỔNG QUAN KIẾN TRÚC & CÁC PHƯƠNG THỨC THANH TOÁN

Hệ thống hỗ trợ 2 luồng thanh toán chính (loại bỏ hoàn toàn COD):
1. **payOS (VietQR Napas 24/7):** Thanh toán tự động 24/7 qua cổng OpenBanking. Khách quét mã VietQR bằng ứng dụng ngân hàng bất kỳ, Webhook tự động cập nhật đơn hàng sang `paid` và `confirmed` trong vòng vài giây. Đơn hàng chỉ được đóng gói và giao đi khi đã thanh toán thành công.
2. **Refund (Hoàn tiền thủ công qua quét QR):** Áp dụng khi đổi trả hàng (`support_tickets` - UC15), hỗ trợ hoàn tiền qua quét mã QR tài khoản ngân hàng của khách với phân quyền duyệt < 3.000.000 VNĐ (`STAFF`) và $\ge$ 3.000.000 VNĐ (`MANAGER`).

---

## 2. CHI TIẾT CÁC LUỒNG HOẠT ĐỘNG (FLOWS & SEQUENCE DIAGRAMS)

### 2.1. Luồng 1: Thanh toán trực tuyến qua payOS (VietQR Flow)

```mermaid
sequenceDiagram
    autonumber
    actor User as Khách hàng (User)
    participant FE as Frontend (React)
    participant BE as Backend (PaymentController / Service)
    participant PayOS as Cổng payOS (VietQR)
    participant BankApp as App Ngân hàng (Mobile Banking)
    participant DB as SQL Server (badminton_shop)
    participant Event as EventPublisher (PaymentSuccessEvent)

    User->>FE: Chọn phương thức "VietQR (payOS)" & bấm "Đặt hàng"
    FE->>BE: POST /api/v1/payments/create (orderId, amount, description)
    Note over BE: 1. Sinh paymentCode (PAY-yyyyMMdd-XXXXX)<br/>2. Sinh payosOrderCode (số nguyên < 53 bit)<br/>3. Tính chữ ký HMAC-SHA256 request
    BE->>PayOS: POST /v2/payment-requests (orderCode, amount, signature...)
    PayOS-->>BE: Trả về checkoutUrl, qrCode, paymentLinkId
    BE->>DB: Lưu bảng payments (status='pending', expires_at = now + 15m)
    BE->>DB: Ghi log bảng payment_transactions (direction='request')
    BE-->>FE: Trả về JSON (paymentId, checkoutUrl, qrCodeUrl)
    
    alt Cách 1: Quét mã QR trực tiếp trên trang Web
        FE->>User: Hiển thị mã VietQR động trên màn hình
        FE->>BE: Chạy Polling (cứ 3s gọi GET /api/v1/payments/{id})
    else Cách 2: Chuyển hướng sang payOS
        FE->>User: window.location.href = checkoutUrl
    end

    User->>BankApp: Mở App Ngân hàng quét mã VietQR & bấm Xác nhận chuyển khoản
    BankApp->>PayOS: Chuyển tiền Napas 24/7 thành công
    PayOS->>BE: POST /api/v1/payments/payos/webhook (code="00", data, signature)
    
    Note over BE: 1. Ghi log callback vào payment_transactions<br/>2. Xác thực chữ ký HMAC-SHA256 với checksumKey<br/>3. Kiểm tra Idempotency (chống trùng lặp)<br/>4. Cập nhật payments (status='paid', paid_at=now)<br/>5. Cập nhật orders (payment_status='paid', status='confirmed')
    BE->>DB: UPDATE payments & orders
    BE->>Event: publishEvent(PaymentSuccessEvent)
    Note over Event: Bắn tín hiệu sang Order/Kho/Hóa đơn tự động đóng gói
    BE-->>PayOS: HTTP 200 {"success": true, "message": "Processed"}
    
    FE->>BE: GET /api/v1/payments/{id} nhận status='paid'
    FE->>User: Hiển thị màn hình "Thanh toán thành công & Đang chuẩn bị hàng"!
```

---

### 2.2. Luồng 2: Xử lý Hoàn tiền đổi trả (Refund Flow - UC15 & UC13)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    actor Staff as Nhân viên CSKH
    actor Manager as Quản lý (Manager)
    participant FE as Frontend Quản trị
    participant BE as Backend Payment / Support Service
    participant DB as SQL Server

    Customer->>FE: Tạo Ticket hoàn tiền (chọn đơn, lý do, upload ảnh QR ngân hàng)
    Staff->>FE: Nhận hàng hoàn trả & kiểm tra chất lượng (PASSED / FAILED)
    
    alt Số tiền hoàn < 3.000.000 VNĐ
        Staff->>BE: POST /api/staff/support-tickets/{id}/approve-refund
        BE->>DB: UPDATE tickets SET status='PENDING_TRANSFER'
    else Số tiền hoàn >= 3.000.000 VNĐ
        Staff->>BE: Trình phiếu sang Quản lý duyệt (PENDING_APPROVAL)
        Manager->>BE: POST /api/manager/support-tickets/{id}/decision (Approve)
        BE->>DB: UPDATE tickets SET status='PENDING_TRANSFER'
    end

    Staff->>Customer: Mở app ngân hàng quét ảnh QR của khách để chuyển tiền
    Staff->>BE: POST /api/staff/support-tickets/{id}/refund-transfer (transactionReference, note)
    Note over BE: 1. Lưu bảng refunds (status='completed')<br/>2. Cập nhật payments.status='refunded'<br/>3. Cập nhật orders.payment_status='refunded'<br/>4. Cập nhật tickets.status='REFUNDED'
    BE->>DB: INSERT refunds & UPDATE payments, orders, tickets
    BE-->>FE: Trả về kết quả hoàn tiền thành công
    FE->>Staff: Hiển thị thông báo phiếu đã hoàn tất (REFUNDED)
```

---

## 3. CƠ CHẾ BẢO MẬT VÀ XỬ LÝ LỖI TRONG LUỒNG THANH TOÁN

### 3.1. Xác thực chữ ký số Webhook (HMAC-SHA256)
* Khi payOS gửi dữ liệu qua webhook, server tính toán lại mã băm HMAC-SHA256 từ object `data` theo thứ tự chữ cái của các trường (canonical alphabet sorting).
* Nếu chữ ký tính được **khớp** với `signature` từ payOS $\rightarrow$ Chấp nhận giao dịch.
* Nếu chữ ký **sai lệch** (bị sửa đổi payload trên đường truyền hoặc bị kẻ gian bắn webhook giả) $\rightarrow$ Từ chối ngay lập tức, không cập nhật DB.

### 3.2. Cơ chế Idempotency (Chống xử lý trùng lặp giao dịch)
* Trong môi trường mạng, payOS có thể retry gửi Webhook nhiều lần nếu đường truyền chập chờn.
* Khi nhận webhook, `PaymentServiceImpl` kiểm tra trạng thái hiện tại của `Payment`:
  * Nếu `payment.getStatus().equals("paid")`, hệ thống ghi nhận log và **bỏ qua không cập nhật lại**, trả về HTTP 200 `{ "success": true }`.
  * Tránh rủi ro cộng tiền 2 lần, bắn duplicate event hoặc xuất trùng hóa đơn.

### 3.3. Thời hạn phiên thanh toán (Timeout & Expired)
* Mỗi link payOS có trường `expires_at = NOW + 15 phút`.
* Sau 15 phút nếu khách không quét mã chuyển khoản, cron job hoặc scheduler có thể quét các payment `pending` quá hạn $\rightarrow$ chuyển trạng thái sang `expired` và nhả số lượng tồn kho sản phẩm.

---

## 4. BẢNG TRẠNG THÁI THANH TOÁN & ĐỒNG BỘ ĐƠN HÀNG

| Trạng thái `payments.status` | Trạng thái `orders.payment_status` | Trạng thái `orders.status` | Ý nghĩa nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `pending` | `unpaid` | `pending` | Đơn hàng vừa tạo, đang chờ khách quét VietQR trong 15 phút |
| `paid` | `paid` | `confirmed` | Khách đã chuyển khoản thành công qua payOS. Đơn hàng sẵn sàng đóng gói & giao |
| `failed` | `unpaid` | `pending` | Giao dịch qua cổng bị lỗi, khách có thể bấm "Thanh toán lại" |
| `expired` | `unpaid` | `cancelled` | Hết 15 phút khách không trả tiền, đơn bị hủy và nhả kho tự động |
| `cancelled` | `unpaid` | `cancelled` | Khách chủ động hủy yêu cầu thanh toán |
| `refunded` | `refunded` | `returned` / `completed` | Đã xử lý hoàn tiền lại cho khách hàng sau khi duyệt ticket |

---

## 5. CÁC API ENDPOINTS PHỤC VỤ LUỒNG THANH TOÁN

| Method | Endpoint | Mục đích | Quyền truy cập |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/payments/create` | Tạo phiên thanh toán payOS VietQR | Authenticated User |
| `GET` | `/api/v1/payments/{id}` | Tra cứu trạng thái thanh toán theo ID (Polling 3s) | Authenticated User |
| `GET` | `/api/v1/payments/by-code/{code}` | Tra cứu theo mã nội bộ `PAY-...` | Authenticated User |
| `GET` | `/api/v1/payments/by-payos-code/{code}` | Tra cứu theo mã số nguyên payOS | Authenticated User |
| `GET` | `/api/v1/payments/order/{orderId}` | Lịch sử các lần thanh toán của đơn | Authenticated User |
| `POST` | `/api/v1/payments/{id}/cancel` | Hủy link thanh toán | Authenticated User |
| `POST` | `/api/staff/support-tickets/{id}/approve-refund` | Duyệt hoàn tiền khoản < 3.000.000 VNĐ | Staff |
| `POST` | `/api/manager/support-tickets/{id}/decision` | Thẩm định & Duyệt hoàn tiền $\ge$ 3.000.000 VNĐ | Manager |
| `POST` | `/api/staff/support-tickets/{id}/refund-transfer` | Ghi nhận chuyển khoản thủ công sau khi quét QR | Staff |
| `POST` | `/api/v1/payments/payos/webhook` | Cổng payOS bắn Webhook thông báo | Public (Bảo vệ bằng HMAC) |
| `POST` | `/api/v1/payments/payos/confirm-webhook`| Xác thực URL khi cài đặt payOS | Public |

---

## 6. HƯỚNG DẪN TEST MOCK TOÀN BỘ LUỒNG (POSTMAN & CURL)

### Bước 1: Tạo phiên thanh toán (Tạo mã QR / Link)
```bash
curl -X POST http://localhost:8080/api/v1/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 1500000,
    "description": "Vot Yonex Astrox 88D",
    "buyerName": "Nguyen Van A",
    "buyerEmail": "a.nguyen@example.com",
    "buyerPhone": "0912345678"
  }'
```
*Nhận về `payosOrderCode` (ví dụ: `5567891234`) và `checkoutUrl`, `qrCodeUrl`.*

### Bước 2: Giả lập Webhook payOS báo chuyển khoản thành công
```bash
curl -X POST http://localhost:8080/api/v1/payments/payos/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "code": "00",
    "desc": "success",
    "data": {
      "orderCode": 5567891234,
      "amount": 1500000,
      "description": "Vot Yonex Astrox 88D",
      "accountNumber": "1903678910",
      "reference": "FT2610318899",
      "currency": "VND",
      "code": "00"
    },
    "signature": "d682498522646c05dbb320d75a34e0622e03fcba0bf3e0f9b378033ee9bc27a9"
  }'
```

### Bước 3: Kiểm tra trạng thái thanh toán đã sang `paid`
```bash
curl http://localhost:8080/api/v1/payments/1
```
*Trường `status` sẽ trả về `"paid"` và `gatewayTxnRef` là `"FT2610318899"`.*
