# LUỒNG HOẠT ĐỘNG MODULE THANH TOÁN (PAYMENT FLOW)
> **Dự án:** Badminton Shop Management System (SBA301_BadmintonShop_SE1986-JV)  
> **Cổng thanh toán:** payOS (VietQR Napas 24/7)  
> **Công nghệ:** Java 21, Spring Boot 3, Spring Data JPA, Spring Security, SQL Server, React (Vite)  
> **Chuẩn kiến trúc:** RESTful Zero `/v1/` (`/api/payments`)

---

## 1. NGUYÊN TẮC CỐT LÕI & QUYẾT ĐỊNH NGHIỆP VỤ

1. **Loại bỏ hoàn toàn COD (100% Trả trước trực tuyến):**  
   * Tất cả các đơn hàng online bắt buộc thanh toán trực tuyến qua cổng **payOS (VietQR Napas 24/7)** trước khi xác nhận đơn và xuất kho giao hàng.
   * Cửa hàng áp dụng chính sách **0đ phí vận chuyển (Freeship toàn quốc)** cho mọi đơn hàng.
2. **Cơ chế đối soát kép (Dual Verification):**  
   * **Kênh 1 - Webhook Push (Thụ động):** Ngay khi khách chuyển tiền, payOS gửi HTTP POST request về Backend. Backend kiểm tra chữ ký số **HMAC-SHA256 Canonical (A-Z)** và cập nhật đơn hàng thành `paid` tức thì (< 1s).
   * **Kênh 2 - Active Polling Sync (Chủ động - Fail-safe):** Khi khách đang mở màn hình quét mã VietQR, Frontend tự động kích hoạt polling định kỳ 3 giây/lần gọi về Backend để Backend tra cứu API payOS `GET /v2/payment-requests/{orderCode}` (hoặc khi khách bấm nút *"Kiểm tra kết quả giao dịch"*).  
   * **Giá trị thực tiễn:** Giúp hệ thống hoạt động tự động hoàn hảo ngay trên Localhost mà không cần cài đặt ngrok, đồng thời là cơ chế dự phòng cứu cánh trên Production phòng khi Webhook bị nghẽn mạng.
3. **Cơ chế Idempotency (Chống xử lý lặp giao dịch):**  
   * Hàm `markPaymentAsPaid()` kiểm tra nếu giao dịch đã ở trạng thái `paid` thì ghi log và bỏ qua cập nhật trùng lặp, đảm bảo không có xung đột giữa 2 kênh Webhook và Polling.
4. **Thời hạn thanh toán 15 phút (Timeout Scheduler):**  
   * Mỗi link thanh toán VietQR có thời gian hiệu lực là 15 phút.
   * `PaymentTimeoutScheduler` chạy định kỳ mỗi 60 giây quét các đơn quá hạn: chuyển `payments.status = 'expired'`, tự động hủy đơn (`orders.status = 'cancelled'`) và nhả tồn kho sản phẩm về lại cửa hàng.
5. **Bảo mật thanh toán & Cô lập dữ liệu (Security & Data Isolation):**  
   * Khách hàng chỉ có thể tra cứu và thao tác trên phiên thanh toán của chính mình qua bộ lọc [`PaymentAccess.java`](file:///backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/payment/controller/PaymentAccess.java) (`if (!userId.equals(order.getUserId())) throw Forbidden`).
   * Không thể can thiệp hoặc làm giả số tiền từ URL hay body request: Số tiền thanh toán được lấy 100% từ `orders.total_amount` trong Database và được bảo vệ bằng chữ ký HMAC-SHA256 khi gửi sang payOS.

---

## 2. SƠ ĐỒ LUỒNG HOẠT ĐỘNG (FLOWCHART & SEQUENCE DIAGRAM)

### 2.1. Sơ đồ tổng quan quy trình thanh toán (Flowchart)

```mermaid
flowchart TD
    Start([Khách bấm Thanh toán đơn hàng]) --> CreateReq[Backend tạo phiên thanh toán & Ký số HMAC-SHA256]
    CreateReq --> CallPayOS[Gọi payOS API POST /v2/payment-requests]
    CallPayOS --> GetQR[payOS trả về checkoutUrl & mã VietQR]
    GetQR --> SaveDB[Lưu payments status='pending', expires_at = now + 15m]
    SaveDB --> ShowScreen[Frontend hiển thị mã VietQR động trên web]
    
    ShowScreen --> UserPay[Khách mở App ngân hàng quét VietQR & chuyển tiền]
    
    UserPay -.->|Kênh 1: Webhook Push| WebhookRecv[payOS bắn POST /api/payments/payos/webhook]
    WebhookRecv --> CheckHMAC{Xác thực chữ ký HMAC-SHA256}
    CheckHMAC -->|Hợp lệ| CheckIdem{Kiểm tra Idempotency}
    CheckIdem -->|Chưa xử lý| MarkPaid[UPDATE payments='paid' & orders='paid', status='confirmed']
    
    UserPay -.->|Kênh 2: Active Polling Sync| PollingFE[Frontend Polling 3s/lần hoặc bấm Kiểm tra]
    PollingFE --> CallSync[Backend gửi GET /v2/payment-requests/orderCode sang payOS]
    CallSync --> CheckStatus{payOS trả về status?}
    CheckStatus -->|PAID| MarkPaid
    CheckStatus -->|PENDING| WaitNext[Tiếp tục chờ thanh toán]
    
    ShowScreen -.->|Quá 15 phút không thanh toán| Scheduler[PaymentTimeoutScheduler quét định kỳ 60s]
    Scheduler --> CancelOrder[UPDATE payments='expired', orders='cancelled' & Nhả tồn kho]
    
    MarkPaid --> RedirectSuccess[Frontend phát hiện đơn đã paid -> Chuyển sang /payments/result]
    RedirectSuccess --> Warehouse[Kho tiến hành đóng gói & giao hàng]
```

---

### 2.2. Sơ đồ tuần tự chi tiết (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    participant FE as Frontend (React Vite)
    participant BE as Backend (PaymentController & Service)
    participant PayOS as Cổng thanh toán payOS (Napas 24/7)
    participant BankApp as App Ngân hàng (Mobile Banking)
    participant DB as SQL Server (badminton_shop)
    participant Warehouse as Bộ phận Kho / Vận chuyển

    Customer->>FE: Bấm "Thanh toán ngay" tại giỏ hàng / đơn hàng
    FE->>BE: POST /api/shop (action: 'start_payment') hoặc POST /api/payments/create
    Note over BE: 1. Lấy total_amount từ orders<br/>2. Sinh mã payosOrderCode ngẫu nhiên<br/>3. Ký số HMAC-SHA256 request
    BE->>PayOS: POST /v2/payment-requests (orderCode, amount, returnUrl, cancelUrl)
    PayOS-->>BE: Trả về checkoutUrl, qrCodeUrl, paymentLinkId
    BE->>DB: INSERT/UPDATE payments (status='pending', expires_at = now + 15m)
    BE->>DB: INSERT payment_transactions (direction='request', http_status=200)
    BE-->>FE: Trả về dữ liệu thanh toán (qrCodeUrl, checkoutUrl)

    FE->>Customer: Điều hướng tới /payments/qr, hiển thị mã VietQR động
    FE->>BE: Tự động kích hoạt Polling ngầm 3 giây/lần (GET /api/shop)

    Customer->>BankApp: Mở App Ngân hàng quét VietQR & chuyển tiền
    BankApp->>PayOS: Giao dịch chuyển tiền Napas 24/7 thành công

    par Kênh 1: Webhook Push (Khi có Public Domain / ngrok)
        PayOS->>BE: POST /api/payments/payos/webhook (code="00", data, signature)
        Note over BE: 1. Xác thực chữ ký HMAC-SHA256<br/>2. Kiểm tra Idempotency<br/>3. Cập nhật paid
        BE->>DB: UPDATE payments SET status='paid' & orders SET payment_status='paid', status='confirmed'
        BE->>DB: INSERT payment_transactions (direction='callback', http_status=200)
        BE-->>PayOS: HTTP 200 {"code": "00", "message": "Processed"}
    and Kênh 2: Active Polling Sync (Chủ động đối soát - Fail-safe)
        FE->>BE: Polling gọi GET /api/shop (hoặc bấm nút "Kiểm tra kết quả")
        BE->>PayOS: GET /v2/payment-requests/{orderCode} (Header: x-client-id, x-api-key)
        PayOS-->>BE: Trả về trạng thái thực tế: status="PAID", transactions
        BE->>DB: UPDATE payments (paid) & orders (paid, confirmed)
    end

    FE->>BE: Nhịp Polling kế tiếp phát hiện order.payment_status='paid'
    FE->>Customer: Tự động chuyển hướng sang /payments/result (Báo thanh toán thành công!)
    DB-->>Warehouse: Đơn hàng ở trạng thái 'confirmed', sẵn sàng đóng gói & xuất kho
```

---

## 3. BẢNG TRẠNG THÁI THANH TOÁN & ĐỒNG BỘ ĐƠN HÀNG

| `payments.status` | `orders.payment_status` | `orders.status` | Ý nghĩa nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `pending` | `unpaid` | `pending` | Đơn hàng vừa tạo, đang chờ khách quét VietQR trong hạn 15 phút |
| `paid` | `paid` | `confirmed` | Đã nhận thanh toán thành công. Đơn hàng chuyển kho đóng gói |
| `expired` | `unpaid` | `cancelled` | Hết 15 phút khách không chuyển tiền. Tự hủy đơn và nhả lại tồn kho |
| `cancelled` | `unpaid` | `cancelled` | Khách chủ động hủy yêu cầu thanh toán |
| `failed` | `unpaid` | `pending` | Giao dịch cổng bị lỗi, khách có thể tạo lại phiên thanh toán |
| `refunded` | `refunded` | `returned` | Đã hoàn tất chuyển khoản tiền trả lại cho khách sau khi duyệt đổi trả |

---

## 4. DANH MỤC API SPECIFICATIONS MODULE PAYMENT (ZERO `/v1/`)

| Method | Endpoint | Quyền truy cập | Mục đích nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/shop` | Authenticated | Khởi tạo phiên thanh toán qua action `{ "type": "start_payment", "order_id": 105 }` |
| `POST` | `/api/payments/create` | Authenticated | Tạo phiên thanh toán payOS VietQR độc lập |
| `GET` | `/api/payments/{id}` | Authenticated (Chính chủ) | Tra cứu thông tin thanh toán theo Payment ID |
| `GET` | `/api/payments/{id}/sync` | Authenticated (Chính chủ) | Kích hoạt Backend đối soát trực tiếp với API payOS theo Payment ID |
| `GET` | `/api/payments/order/{orderId}` | Authenticated (Chính chủ) | Xem lịch sử các lần thanh toán của một đơn hàng |
| `GET` | `/api/payments/order/{orderId}/sync` | Authenticated (Chính chủ) | Kích hoạt Backend đối soát trực tiếp với API payOS theo Order ID |
| `GET` | `/api/payments/by-code/{code}` | Authenticated (Chính chủ) | Tra cứu theo mã nội bộ `PAY-yyyyMMdd-XXXXX` |
| `GET` | `/api/payments/by-payos-code/{code}` | Authenticated (Chính chủ) | Tra cứu theo mã số nguyên payOS (`payosOrderCode`) |
| `POST` | `/api/payments/{id}/cancel` | Authenticated (Chính chủ) | Hủy phiên thanh toán đang chờ |
| `POST` | `/api/payments/payos/webhook` | Public | Cổng payOS bắn Webhook thông báo kết quả (Xác thực chữ ký HMAC-SHA256) |
| `POST` | `/api/payments/payos/confirm-webhook` | `ROLE_ADMIN` | Gửi yêu cầu xác thực Webhook URL tới cổng payOS khi deploy |
