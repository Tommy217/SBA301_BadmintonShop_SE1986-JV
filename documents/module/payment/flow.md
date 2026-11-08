# LUỒNG HOẠT ĐỘNG TOÀN DIỆN: MODULE THANH TOÁN (PAYMENT) & HỖ TRỢ ĐỔI TRẢ (SUPPORT TICKET)
> **Dự án:** Badminton Shop Management System (SBA301_BadmintonShop_SE1986-JV)  
> **Công nghệ:** Java 21, Spring Boot 3, Spring Data JPA, Spring Security, SQL Server, React (Vite), payOS (VietQR Napas 24/7)  
> **Phạm vi tài liệu:** Hợp nhất luồng nghiệp vụ khép kín từ khâu Đặt hàng & Thanh toán trực tuyến (payOS) đến Hỗ trợ sau bán hàng, Thẩm định hàng trả về & Hoàn tiền thủ công (UC15: Returns & Refunds).

---

## 1. NGUYÊN TẮC CỐT LÕI & QUYẾT ĐỊNH NGHIỆP VỤ

1. **Thanh toán trực tuyến 100% (Loại bỏ COD):**  
   * Tất cả đơn hàng trực tuyến bắt buộc thanh toán trả trước qua cổng **payOS (VietQR Napas 24/7)** trước khi xác nhận đơn và xuất kho.
   * Phí vận chuyển áp dụng chính sách **0đ (Freeship toàn quốc)** cho mọi đơn hàng.
2. **Cơ chế đối soát kép (Dual Verification):**  
   * **Kênh 1 - Webhook Push (Thụ động):** Cổng payOS tự động gửi tín hiệu POST kèm chữ ký số HMAC-SHA256 về Backend khi tiền vào tài khoản. Đơn hàng chuyển sang `paid` ngay tức thì (< 1s).
   * **Kênh 2 - Active Polling Sync (Chủ động):** Frontend mở màn hình QR tự động polling 3s/lần gọi về Backend để Backend tra cứu API payOS `GET /v2/payment-requests/{orderCode}`. Đảm bảo giao dịch luôn cập nhật thành công ngay cả khi chạy trên Localhost không có ngrok hoặc khi Webhook bị nghẽn mạng trên Production.
   * **Idempotency:** Kiểm tra chống xử lý trùng lặp giao dịch trên toàn hệ thống.
3. **Thời hạn thanh toán 15 phút (Timeout Scheduler):**  
   * Mỗi mã VietQR có hiệu lực trong 15 phút. Hệ thống quét định kỳ mỗi 60 giây; nếu quá hạn mà chưa thanh toán thì tự động chuyển `payments.status = 'expired'`, hủy đơn hàng (`orders.status = 'cancelled'`) và nhả tồn kho về cho sản phẩm.
4. **Phạm vi đổi trả sau bán hàng (Chỉ Trả hàng & Hoàn tiền - REFUND):**  
   * Khách hàng tạo phiếu trực tiếp từ đơn hàng đã thanh toán (`payment_status = 'paid'`) trong vòng **7 ngày** kể từ khi nhận hàng.
   * **Bắt buộc upload ảnh mã QR tài khoản ngân hàng nhận tiền** + ảnh/video bằng chứng (không nhập số tài khoản text để loại bỏ sai sót).
   * **Chính sách chỉ hoàn tiền khi lỗi từ cửa hàng (`STORE_FAULT`):** Giao sai mẫu, sai kích thước, lỗi sản xuất, hư hại khi vận chuyển. Cửa hàng hoàn 100% tiền hàng và phí ship ban đầu.
   * Nếu hàng hoàn về đạt chuẩn nhưng do **khách đổi ý cá nhân (`CUSTOMER_PREFERENCE`)** $\rightarrow$ Hệ thống **từ chối phiếu (`REJECTED`)**, gửi trả lại sản phẩm cho khách và **không hoàn tiền**.
   * **Không nhập lại kho (`No Restock`):** Hàng lỗi thu hồi được cách ly để chuyển bảo hành hãng hoặc tiêu hủy, không cộng lại tồn kho bán đại trà.
5. **Phân quyền duyệt hoàn tiền 2 cấp độ (Ngưỡng 3.000.000 VNĐ):**  
   * Khoản hoàn **< 3.000.000 VNĐ**: Nhân viên CSKH (`STAFF`) phê duyệt trực tiếp sang trạng thái chờ chuyển tiền (`PENDING_TRANSFER`).
   * Khoản hoàn **$\ge$ 3.000.000 VNĐ**: Chuyển Quản lý (`MANAGER`) thẩm định và ra quyết định phê duyệt (`PENDING_APPROVAL`).
6. **Thực hiện hoàn tiền thủ công (Manual QR Transfer - An toàn tài chính):**  
   * Nhân viên mở App ngân hàng quét trực tiếp ảnh QR do khách upload, chuyển đúng số tiền đã duyệt, sau đó **nhập Mã giao dịch ngân hàng (`bank_reference`)** vào hệ thống để kết thúc quy trình (`REFUNDED`). Không sử dụng Payout API tự động trừ tiền tài khoản.

---

## 2. SƠ ĐỒ LUỒNG HOẠT ĐỘNG TỔNG THỂ (END-TO-END FLOW)

```mermaid
flowchart TD
    Start([Khách hàng đặt hàng]) --> Checkout[Khởi tạo đơn hàng - Phí ship 0đ]
    Checkout --> CreatePay[Backend tạo link payOS & mã VietQR]
    CreatePay --> ShowQR[Hiển thị VietQR động trên web - Hạn 15 phút]
    
    ShowQR --> CustomerTransfer[Khách mở App ngân hàng quét mã VietQR & chuyển khoản]
    
    CustomerTransfer -.->|Kênh 1: Webhook Push| WH[payOS bắn Webhook POST]
    WH --> HMAC{Xác thực HMAC-SHA256 & Idempotency}
    HMAC -->|Hợp lệ| ConfirmPaid[Cập nhật: payment_status='paid', status='confirmed']
    
    CustomerTransfer -.->|Kênh 2: Active Polling Pull| Poll[FE Polling 3s/lần hoặc bấm Kiểm tra kết quả]
    Poll --> AskPayOS[Backend gọi GET /v2/payment-requests/orderCode]
    AskPayOS -->|Status='PAID'| ConfirmPaid
    
    ShowQR -.->|Quá 15 phút không thanh toán| Expired[Timeout Scheduler: Hủy đơn & Nhả tồn kho]
    
    ConfirmPaid --> Warehouse[Kho đóng gói & Vận chuyển giao hàng]
    Warehouse --> Delivered[Khách nhận hàng thành công]
    
    Delivered --> ReturnCheck{Khách có khiếu nại trong 7 ngày?}
    ReturnCheck -->|Không| OrderDone([Đơn hàng Hoàn tất])
    ReturnCheck -->|Có| CreateTicket[Khách ấn nút Tạo Ticket từ Lịch sử đơn hàng + Upload ảnh QR nhận tiền]
    
    CreateTicket --> ReviewStaff{Nhân viên sơ duyệt SLA 48h}
    ReviewStaff -->|Từ chối sơ duyệt| TicketRejected([Phiếu REJECTED - Kết thúc])
    ReviewStaff -->|Chấp thuận| AwaitingReturn[Chuyển AWAITING_RETURN - Khách gửi hàng về kho]
    
    AwaitingReturn --> WarehouseReceive[Kho nhận hàng hoàn & Kiểm tra chất lượng]
    WarehouseReceive --> InspectCheck{Kết quả thẩm định}
    
    InspectCheck -->|FAILED - Hàng rách/hỏng do khách| InspectFailed([INSPECTION_FAILED - Gửi trả hàng cho khách])
    InspectCheck -->|PASSED| CauseCheck{Nguyên nhân lỗi}
    
    CauseCheck -->|CUSTOMER_PREFERENCE - Đổi ý| RejectPref([REJECTED - Hàng chuẩn, gửi trả khách, không hoàn tiền])
    CauseCheck -->|STORE_FAULT - Lỗi do Shop| AmountCheck{Số tiền hoàn}
    
    AmountCheck -->|< 3.000.000 VNĐ| StaffApprove[Staff phê duyệt trực tiếp]
    AmountCheck -->|>= 3.000.000 VNĐ| ManagerApprove[Chuyển Quản lý thẩm định & phê duyệt]
    
    StaffApprove --> TransferReady[Chuyển sang PENDING_TRANSFER]
    ManagerApprove --> TransferReady
    
    TransferReady --> StaffTransfer[Staff quét ảnh QR chuyển khoản tiền hoàn]
    StaffTransfer --> RecordBankRef[Staff nhập mã giao dịch bankReference]
    RecordBankRef --> RefundCompleted([Hoàn tất: status='REFUNDED', payment='refunded'])
```

---

## 3. CHI TIẾT SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAMS)

### 3.1. Luồng 1: Thanh toán trực tuyến payOS VietQR (Dual Verification)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    participant FE as Frontend (React)
    participant BE as Backend (Shop & Payment)
    participant PayOS as Cổng thanh toán payOS
    participant BankApp as App Ngân hàng (Napas 24/7)
    participant DB as SQL Server

    Customer->>FE: Bấm "Đặt hàng / Thanh toán"
    FE->>BE: POST /api/shop (action: 'start_payment') hoặc POST /api/payments/create
    Note over BE: 1. Lấy total_amount từ orders<br/>2. Sinh mã payosOrderCode ngẫu nhiên<br/>3. Ký số HMAC-SHA256 request
    BE->>PayOS: POST /v2/payment-requests (orderCode, amount, returnUrl, cancelUrl)
    PayOS-->>BE: Trả về checkoutUrl, qrCodeUrl
    BE->>DB: INSERT/UPDATE payments (status='pending', expires_at = now + 15m)
    BE-->>FE: Trả về dữ liệu phiên thanh toán
    FE->>Customer: Điều hướng tới /payments/qr, hiển thị mã VietQR động
    FE->>BE: Tự động kích hoạt Polling 3s/lần (GET /api/shop)

    Customer->>BankApp: Mở App Ngân hàng quét VietQR & chuyển tiền
    BankApp->>PayOS: Khách chuyển tiền Napas thành công

    par Kênh 1: Webhook Push (Khi có Public Domain / ngrok)
        PayOS->>BE: POST /api/payments/payos/webhook (code="00", data, signature)
        Note over BE: 1. Xác thực chữ ký HMAC-SHA256<br/>2. Kiểm tra Idempotency<br/>3. Cập nhật paid
        BE->>DB: UPDATE payments SET status='paid' & orders SET payment_status='paid', status='confirmed'
        BE-->>PayOS: HTTP 200 {"code": "00", "message": "Processed"}
    and Kênh 2: Active Polling Sync (Chủ động đối soát - Fail-safe)
        FE->>BE: Polling gọi GET /api/shop (hoặc bấm nút "Kiểm tra kết quả")
        BE->>PayOS: GET /v2/payment-requests/{orderCode} (Header: x-client-id, x-api-key)
        PayOS-->>BE: Trả về trạng thái thực tế: status="PAID"
        BE->>DB: UPDATE payments (paid) & orders (paid, confirmed)
    end

    FE->>BE: Nhịp Polling kế tiếp phát hiện order.payment_status='paid'
    FE->>Customer: Tự động chuyển hướng sang /payments/result (Báo thanh toán thành công!)
```

---

### 3.2. Luồng 2: Hỗ trợ sau bán hàng & Hoàn tiền đổi trả (Support Ticket & Manual Refund)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    actor Staff as Nhân viên CSKH / Kho
    actor Manager as Quản lý (Manager)
    participant FE as Frontend React
    participant BE as Backend Support / Payment
    participant DB as SQL Server
    participant BankApp as App Ngân hàng ngoài đời

    Customer->>FE: Bấm "Hỗ trợ / Đổi trả" trên đơn hàng đã thanh toán (/orders)
    FE->>Customer: Mở form /support-tickets/create?orderId=... (tự nạp đơn & sản phẩm)
    Customer->>FE: Chọn sản phẩm lỗi, tải ảnh bằng chứng + ảnh mã QR ngân hàng nhận tiền
    FE->>BE: POST /api/support-tickets (orderId, items, refundQrUrl, evidenceUrls)
    Note over BE: Kiểm tra quyền sở hữu đơn hàng, hạn 7 ngày, bắt buộc có ảnh QR
    BE->>DB: INSERT support_tickets (status='PENDING') & items, evidence
    BE-->>FE: Phiếu hỗ trợ tạo thành công

    Staff->>FE: Xem chi tiết phiếu PENDING, lý do và ảnh bằng chứng
    alt Sơ duyệt không hợp lệ
        Staff->>BE: POST /api/staff/support-tickets/{id}/review (action='reject', rejectReason)
        BE->>DB: UPDATE status='REJECTED'
    else Chấp thuận sơ duyệt
        Staff->>BE: POST /api/staff/support-tickets/{id}/review (action='approve', cause)
        BE->>DB: UPDATE status='AWAITING_RETURN'
    end

    Customer->>Staff: Khách đóng gói gửi hàng về địa chỉ kho của shop
    Staff->>FE: Kiểm định kiện hàng tại kho (PASSED hoặc FAILED)
    Staff->>BE: POST /api/staff/support-tickets/{id}/inspection (result, note)
    BE->>DB: INSERT ticket_inspection

    alt Thẩm định Hỏng/Rách do khách (FAILED)
        BE->>DB: UPDATE status='INSPECTION_FAILED' (gửi trả hàng cho khách)
    else Thẩm định ĐẠT (PASSED)
        alt Khách đổi ý (CUSTOMER_PREFERENCE)
            BE->>DB: UPDATE status='REJECTED' (Hàng chuẩn, gửi trả khách, không hoàn tiền)
        else Lỗi cửa hàng (STORE_FAULT)
            alt Số tiền hoàn < 3.000.000 VNĐ
                Staff->>BE: POST /api/staff/support-tickets/{id}/approve-refund
                BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=staffId
            else Số tiền hoàn >= 3.000.000 VNĐ
                BE->>DB: UPDATE status='PENDING_APPROVAL'
                Manager->>BE: POST /api/manager/support-tickets/{id}/decision (decision='approve')
                BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=managerId
            end
        end
    end

    Note over Staff: Thực hiện chuyển khoản hoàn tiền
    Staff->>FE: Mở ảnh QR của khách và số tiền đã duyệt
    Staff->>BankApp: Quét mã QR & chuyển khoản thành công
    Staff->>BE: POST /api/staff/support-tickets/{id}/refund-transfer (bankReference, note)
    Note over BE: 1. INSERT refund_records<br/>2. UPDATE support_tickets SET status='REFUNDED'<br/>3. UPDATE payments/orders SET status='refunded'
    BE->>DB: Lưu các bản ghi cập nhật
    BE-->>FE: Hoàn tất quy trình hoàn tiền!
```

---

## 4. MA TRẬN CHUYỂN ĐỔI TRẠNG THÁI (STATE MACHINE)

### 4.1. Bảng đồng bộ trạng thái Thanh toán & Đơn hàng

| `payments.status` | `orders.payment_status` | `orders.status` | Ý nghĩa nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `pending` | `unpaid` | `pending` | Khách vừa đặt hàng, đang chờ quét VietQR trong 15 phút |
| `paid` | `paid` | `confirmed` | Đã nhận thanh toán thành công. Đơn hàng sẵn sàng đóng gói & giao |
| `expired` | `unpaid` | `cancelled` | Quá 15 phút không thanh toán. Tự động hủy đơn và nhả lại tồn kho |
| `cancelled` | `unpaid` | `cancelled` | Khách hàng chủ động hủy phiên thanh toán |
| `failed` | `unpaid` | `pending` | Giao dịch cổng lỗi, khách có thể tạo lại phiên thanh toán |
| `refunded` | `refunded` | `returned` | Đã chuyển khoản hoàn tiền thành công cho khách sau khi duyệt ticket |

---

### 4.2. Bảng vòng đời trạng thái Phiếu hỗ trợ (Support Ticket Lifecycle)

| Trạng thái | Tên hiển thị | Vai trò xử lý | Điều kiện chuyển tiếp |
| :--- | :--- | :--- | :--- |
| `PENDING` | Chờ tiếp nhận sơ duyệt | Khách hàng, Staff | Phiếu vừa tạo. Khách có thể bấm **Hủy (`CANCELLED`)**. Staff duyệt (`AWAITING_RETURN`) hoặc từ chối (`REJECTED`). |
| `AWAITING_RETURN` | Chờ khách gửi lại hàng | Khách hàng, Staff | Khách đóng gói gửi hàng về kho. Nhân viên nhận hàng và ghi biên bản kiểm định. |
| `INSPECTION_FAILED`| Kiểm tra hàng không đạt | - | Trạng thái kết thúc. Hàng bị rách/hỏng do lỗi người dùng. Gửi trả lại khách, chặn hoàn tiền. |
| `PENDING_APPROVAL` | Chờ Quản lý duyệt | Quản lý (`MANAGER`) | Hàng đạt chuẩn do lỗi shop nhưng số tiền hoàn $\ge 3.000.000$ VNĐ. Quản lý duyệt hoặc từ chối. |
| `PENDING_TRANSFER` | Chờ chuyển tiền | Nhân viên (`STAFF`) | Khoản hoàn đã được duyệt (Staff duyệt nếu $< 3$M, Quản lý duyệt nếu $\ge 3$M). Chờ Staff quét QR chuyển khoản. |
| `REFUNDED` | Đã hoàn tiền | - | Trạng thái kết thúc. Staff nhập mã giao dịch ngân hàng thành công. Ghi nhận `refund_records`. |
| `REJECTED` | Bị từ chối | - | Trạng thái kết thúc. Bị từ chối ở bước sơ duyệt, hoặc do khách đổi ý (`CUSTOMER_PREFERENCE`), hoặc do Quản lý từ chối. |
| `CANCELLED` | Khách đã hủy | - | Trạng thái kết thúc. Khách chủ động hủy khi phiếu còn ở `PENDING`. |

---

## 5. DANH MỤC API ĐẶC TẢ CHÍNH THỨC (API SPECIFICATIONS)

### 5.1. Nhóm API Thanh toán (`/api/payments` & `/api/shop`)

| Method | Endpoint | Quyền truy cập | Chức năng nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/shop` | Authenticated | Khởi tạo thanh toán VietQR với action `{ "type": "start_payment", "order_id": 105 }` |
| `POST` | `/api/payments/create` | Authenticated | Tạo phiên thanh toán payOS VietQR độc lập |
| `GET` | `/api/payments/{id}` | Authenticated (Chính chủ) | Tra cứu trạng thái thanh toán theo Payment ID |
| `GET` | `/api/payments/order/{orderId}` | Authenticated (Chính chủ) | Tra cứu lịch sử thanh toán theo Order ID |
| `GET` | `/api/payments/{id}/sync` | Authenticated (Chính chủ) | Kích hoạt Backend đối soát trực tiếp với API payOS theo Payment ID |
| `GET` | `/api/payments/order/{orderId}/sync` | Authenticated (Chính chủ) | Kích hoạt Backend đối soát trực tiếp với API payOS theo Order ID |
| `POST` | `/api/payments/{id}/cancel` | Authenticated (Chính chủ) | Hủy phiên thanh toán đang chờ |
| `POST` | `/api/payments/payos/webhook` | Public | payOS bắn tín hiệu Webhook tự động (Xác thực chữ ký HMAC-SHA256) |
| `POST` | `/api/payments/payos/confirm-webhook` | `ROLE_ADMIN` | Gửi yêu cầu xác thực Webhook URL tới cổng payOS |

### 5.2. Nhóm API Hỗ trợ Đổi trả (`/api/support-tickets`)

| Method | Endpoint | Quyền truy cập | Chức năng nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/support-tickets` | Authenticated (Chính chủ) | Khách tạo yêu cầu Trả hàng & Hoàn tiền kèm ảnh QR và bằng chứng |
| `GET` | `/api/support-tickets` | Authenticated (Chính chủ) | Khách xem danh sách phiếu hỗ trợ của tài khoản mình |
| `GET` | `/api/support-tickets/{id}` | Authenticated (Chính chủ/NV) | Xem chi tiết tiến trình phiếu hỗ trợ |
| `POST` | `/api/support-tickets/{id}/cancel` | Authenticated (Chính chủ) | Khách hủy yêu cầu hỗ trợ (chỉ khi còn ở trạng thái `PENDING`) |
| `GET` | `/api/staff/support-tickets` | `STAFF`, `MANAGER`, `ADMIN` | Hàng đợi danh sách phiếu hỗ trợ cần xử lý |
| `POST` | `/api/staff/support-tickets/{id}/review` | `STAFF`, `MANAGER`, `ADMIN` | Sơ duyệt tiếp nhận: chọn `cause` hoặc từ chối phiếu |
| `POST` | `/api/staff/support-tickets/{id}/inspection` | `STAFF`, `MANAGER`, `ADMIN` | Ghi nhận biên bản kiểm định hàng tại kho (`PASSED` / `FAILED`) |
| `POST` | `/api/staff/support-tickets/{id}/approve-refund` | `STAFF`, `MANAGER`, `ADMIN` | Nhân viên duyệt hoàn tiền cho khoản hoàn < 3.000.000 VNĐ |
| `POST` | `/api/manager/support-tickets/{id}/decision` | `MANAGER`, `ADMIN` | Quản lý phê duyệt hoặc từ chối khoản hoàn $\ge 3.000.000$ VNĐ |
| `POST` | `/api/staff/support-tickets/{id}/refund-transfer` | `STAFF`, `MANAGER`, `ADMIN` | Nhân viên nhập `bankReference` xác nhận đã chuyển khoản manual thành công |
