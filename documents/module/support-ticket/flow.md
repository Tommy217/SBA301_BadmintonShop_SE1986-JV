# LUỒNG HOẠT ĐỘNG MODULE ĐỔI TRẢ & HOÀN TIỀN (UC15: EXCHANGES & REFUNDS)
> **Dự án:** SBA301_BadmintonShop_SE1986-JV  
> **Phân hệ:** Support Ticket / Exchanges & Refunds (UC15)  
> **Kiến trúc:** Java 21, Spring Boot, Spring Data JPA, SQL Server, React Vite Frontend

---

## 1. TỔNG QUAN NGHIỆP VỤ & QUY TẮC CỐT LÕI

Phân hệ Đổi trả & Hoàn tiền (UC15) xử lý khiếu nại sau bán hàng cho các đơn hàng trực tuyến đã giao thành công, tuân thủ các nguyên tắc thiết kế sau:

1. **Loại bỏ COD & Ví điện tử:** Tất cả các đơn hàng hoàn tiền đều là các giao dịch đã thanh toán trực tuyến qua Cổng thanh toán (payOS).
2. **Hoàn tiền chuyển khoản thủ công qua mã QR ngân hàng (100% Manual):**
   * Khách hàng **bắt buộc tải lên ảnh mã QR tài khoản ngân hàng** nhận tiền khi tạo ticket (áp dụng cho cả `REFUND` và `EXCHANGE`).
   * Không lưu thông tin số tài khoản / tên ngân hàng dạng text (loại bỏ lỗi chính tả của khách).
   * Nhân viên quét ảnh QR của khách bằng ứng dụng Mobile Banking ngoài đời thực, thực hiện chuyển tiền, sau đó **nhập Mã tham chiếu giao dịch ngân hàng (`bank_reference`)** vào hệ thống để đối soát.
3. **Đổi hàng 1:1 cùng loại, cùng giá tiền:**
   * Chỉ cho phép đổi sang biến thể khác (kích thước, màu sắc) của **cùng sản phẩm gốc** và có **đơn giá bằng chính xác đơn giá sản phẩm đã mua** (`CR-BR-05`).
   * Hệ thống không thu thêm phụ phí chênh lệch giá hoặc phí vận chuyển đổi hàng.
4. **Xác định nguyên nhân (`cause`) khi sơ duyệt:**
   * `STORE_FAULT`: Lỗi từ phía cửa hàng / nhà sản xuất (hàng lỗi, giao nhầm mã, thiếu phụ kiện). Cửa hàng **hoàn lại cả tiền phí vận chuyển ban đầu**.
   * `CUSTOMER_PREFERENCE`: Nhu cầu cá nhân của khách hàng (đổi size, đổi màu, đổi ý). Khách hàng tự chịu chi phí vận chuyển.
5. **Hạn mức phê duyệt 3.000.000 VNĐ:**
   * Số tiền hoàn **dưới 3.000.000 VNĐ**: Nhân viên (Staff) có thẩm quyền phê duyệt trực tiếp sang trạng thái chờ chuyển tiền (`PENDING_TRANSFER`).
   * Số tiền hoàn **từ 3.000.000 VNĐ trở lên**: Bắt buộc chuyển sang trạng thái chờ Quản lý phê duyệt (`PENDING_APPROVAL`). Chỉ tài khoản có vai trò `MANAGER` mới được phép duyệt hoặc từ chối.
6. **Ràng buộc duy nhất:** Mỗi đơn hàng chỉ được tạo tối đa 1 phiếu hỗ trợ (`UNIQUE order_id`).

---

## 2. SƠ ĐỒ TUẦN TỰ HOẠT ĐỘNG (SEQUENCE DIAGRAMS)

### 2.1. Luồng Trả hàng & Hoàn tiền (Refund Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    participant FE as Frontend (React)
    participant BE as Backend (Customer/Staff/Manager Controller)
    participant DB as SQL Server (badminton_shop)
    actor Staff as Nhân viên (Staff)
    actor Manager as Quản lý (Manager)
    participant BankApp as App Ngân hàng ngoài đời thực

    Customer->>FE: Chọn đơn hàng đã giao (<= 7 ngày), chọn sản phẩm cần hoàn
    Customer->>FE: Tải lên ảnh mã QR tài khoản nhận tiền & ảnh/video lỗi
    FE->>BE: POST /api/support-tickets (type='REFUND', items, qrUrl, evidenceUrls)
    Note over BE: Kiểm tra 1 ticket/order, 7 ngày, QR bắt buộc
    BE->>DB: INSERT support_tickets (status='PENDING') & ticket_items, ticket_evidence
    BE-->>FE: HTTP 200 (Ticket created)

    Note over Staff: Sơ duyệt tiếp nhận (SLA 48h)
    Staff->>FE: Mở phiếu PENDING, xem xét lý do và bằng chứng
    alt Từ chối sơ duyệt
        Staff->>FE: Bấm "Từ chối" & nhập lý do
        FE->>BE: POST /api/staff/support-tickets/{id}/review (action='reject', rejectReason)
        BE->>DB: UPDATE status='REJECTED'
    else Chấp thuận tiếp nhận
        Staff->>FE: Bấm "Chấp thuận" & chọn cause ('STORE_FAULT' hoặc 'CUSTOMER_PREFERENCE')
        FE->>BE: POST /api/staff/support-tickets/{id}/review (action='approve', cause)
        BE->>DB: UPDATE status='AWAITING_RETURN', cause=cause
    end

    Note over Customer: Khách đóng gói gửi hàng về kho theo hướng dẫn
    Customer->>Staff: Kiện hàng hoàn về tới kho

    Note over Staff: Thẩm định hàng hoàn về
    Staff->>FE: Lập biên bản kiểm tra hàng (PASSED hoặc FAILED)
    FE->>BE: POST /api/staff/support-tickets/{id}/inspection (result, note)
    BE->>DB: INSERT ticket_inspection

    alt Thẩm định KHÔNG ĐẠT (FAILED)
        BE->>DB: UPDATE status='INSPECTION_FAILED' (gửi trả hàng cho khách)
    else Thẩm định ĐẠT (PASSED)
        Note over BE: Tính số tiền hoàn = tiền hàng + ship (nếu STORE_FAULT)
        alt Số tiền hoàn < 3.000.000 VNĐ
            BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=staffId
        else Số tiền hoàn >= 3.000.000 VNĐ
            BE->>DB: UPDATE status='PENDING_APPROVAL'
            Manager->>FE: Quản lý xem xét biên bản & bấm Duyệt
            FE->>BE: POST /api/manager/support-tickets/{id}/decision (decision='approve')
            BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=managerId
        end
    end

    Note over Staff: Thực hiện chuyển khoản thủ công
    Staff->>FE: Xem ảnh QR của khách & số tiền cần hoàn
    Staff->>BankApp: Quét mã QR & chuyển khoản thành công
    Staff->>FE: Nhập mã giao dịch ngân hàng (bankReference)
    FE->>BE: POST /api/staff/support-tickets/{id}/refund-transfer (bankReference)
    Note over BE: 1. Kiểm tra mã giao dịch duy nhất<br/>2. INSERT refund_records<br/>3. UPDATE status='REFUNDED'<br/>4. UPDATE orders/payments status
    BE->>DB: UPDATE support_tickets, orders, payments
    BE-->>FE: Hoàn tiền hoàn tất!
```

---

### 2.2. Luồng Đổi sản phẩm (Exchange Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    participant FE as Frontend
    participant BE as Backend
    participant DB as SQL Server
    actor Staff as Nhân viên

    Customer->>FE: Chọn đơn hàng, chọn biến thể đổi cùng giá (CR-BR-05) & tải lên QR
    FE->>BE: POST /api/support-tickets (type='EXCHANGE', replacementVariantId, qrUrl)
    BE->>DB: INSERT support_tickets (status='PENDING')

    Staff->>FE: Sơ duyệt chọn cause (STORE_FAULT hoặc CUSTOMER_PREFERENCE)
    FE->>BE: POST /api/staff/support-tickets/{id}/review (action='approve')
    BE->>DB: UPDATE status='AWAITING_RETURN'

    Customer->>Staff: Gửi kiện hàng về kho
    Staff->>FE: Thẩm định hàng ĐẠT (PASSED)
    FE->>BE: POST /api/staff/support-tickets/{id}/inspection (result='PASSED')
    
    alt Hàng thay thế CÒN TỒN KHO
        Staff->>FE: Bấm "Giao hàng thay thế" & nhập mã vận đơn
        FE->>BE: POST /api/staff/support-tickets/{id}/ship-exchange (trackingCode)
        BE->>DB: UPDATE status='COMPLETED' (trừ kho sản phẩm thay thế)
    else Hàng thay thế HẾT HÀNG (Unavailable)
        Staff->>FE: Bấm "Hết hàng thay thế"
        FE->>BE: POST /api/staff/support-tickets/{id}/replacement-unavailable
        alt Nguyên nhân là STORE_FAULT
            Note over BE: Tự động rẽ nhánh sang luồng Hoàn tiền (dùng QR đã có sẵn)
            BE->>DB: Chuyển sang PENDING_TRANSFER hoặc PENDING_APPROVAL
        else Nguyên nhân là CUSTOMER_PREFERENCE
            Note over BE: Hủy yêu cầu, gửi trả hàng cũ, KHÔNG hoàn tiền
            BE->>DB: UPDATE status='CANCELLED'
        end
    end
```

---

## 3. MA TRẬN 9 TRẠNG THÁI VÒNG ĐỜI (STATE TRANSITIONS)

| Trạng thái | Tên tiếng Việt | Quyền hạn thao tác | Ý nghĩa & Điều kiện chuyển tiếp |
| :--- | :--- | :--- | :--- |
| `PENDING` | Chờ tiếp nhận sơ duyệt | Khách hàng, Staff | Phiếu vừa tạo. Khách có thể bấm **Hủy (`CANCELLED`)**. Staff duyệt (chọn `cause` $\rightarrow$ `AWAITING_RETURN`) hoặc từ chối (`REJECTED`). |
| `REJECTED` | Bị từ chối | - | Trạng thái kết thúc. Bị Staff từ chối ở bước sơ duyệt hoặc Quản lý từ chối duyệt hoàn tiền. |
| `AWAITING_RETURN` | Chờ gửi lại hàng | Khách hàng, Staff | Khách đóng gói gửi hàng về địa chỉ kho. Staff nhận hàng và ghi nhận biên bản kiểm tra. |
| `INSPECTION_FAILED`| Kiểm tra không đạt | - | Trạng thái kết thúc. Hàng hoàn về không đủ điều kiện (bị can thiệp, hư hại do người dùng), hàng gửi trả lại khách, chặn hoàn tiền. |
| `PENDING_APPROVAL` | Chờ Quản lý duyệt | Quản lý (Manager) | Áp dụng khi số tiền hoàn $\ge 3.000.000$ VNĐ sau khi kiểm tra đạt. Chỉ Quản lý mới có quyền duyệt hoặc từ chối. |
| `PENDING_TRANSFER` | Chờ chuyển tiền | Nhân viên (Staff) | Khoản hoàn đã được duyệt (Staff duyệt nếu $< 3$ triệu, Quản lý duyệt nếu $\ge 3$ triệu). Nhân viên quét QR để chuyển tiền. |
| `REFUNDED` | Đã hoàn tiền | - | Trạng thái kết thúc. Nhân viên nhập mã tham chiếu ngân hàng thành công. Ghi nhận `refund_records`. |
| `COMPLETED` | Đã hoàn tất đổi hàng | - | Trạng thái kết thúc. Hàng thay thế còn tồn kho và đã được xuất kho giao cho khách. |
| `CANCELLED` | Đã hủy | - | Trạng thái kết thúc. Khách chủ động hủy khi còn `PENDING` hoặc hết hàng thay thế do `CUSTOMER_PREFERENCE`. |

---

## 4. BẢNG DANH MỤC API SPECIFICATIONS

| Method | Endpoint | Quyền truy cập | Mục đích |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/support-tickets` | Khách hàng | Tạo yêu cầu đổi hàng / hoàn tiền kèm ảnh QR và bằng chứng |
| `GET` | `/api/support-tickets` | Khách hàng | Danh sách phiếu hỗ trợ của khách hàng đang đăng nhập |
| `GET` | `/api/support-tickets/{id}` | Khách hàng / Staff | Xem chi tiết tiến trình phiếu hỗ trợ |
| `POST` | `/api/support-tickets/{id}/cancel` | Khách hàng | Khách hủy yêu cầu (chỉ khi còn `PENDING`) |
| `GET` | `/api/staff/support-tickets` | Staff / Manager | Hàng đợi danh sách ticket (lọc theo status/type) |
| `POST` | `/api/staff/support-tickets/{id}/review` | Staff | Sơ duyệt tiếp nhận: chọn `cause` hoặc từ chối |
| `POST` | `/api/staff/support-tickets/{id}/inspection` | Staff | Ghi nhận biên bản kiểm định hàng (`PASSED` / `FAILED`) |
| `POST` | `/api/staff/support-tickets/{id}/approve-refund`| Staff | Duyệt hoàn tiền cho đơn dưới 3.000.000 VNĐ |
| `POST` | `/api/staff/support-tickets/{id}/ship-exchange` | Staff | Xác nhận gửi hàng thay thế khi kho còn hàng |
| `POST` | `/api/staff/support-tickets/{id}/replacement-unavailable` | Staff | Báo hết hàng thay thế (tự động rẽ nhánh theo `cause`) |
| `POST` | `/api/staff/support-tickets/{id}/refund-transfer` | Staff | Nhập `bankReference` xác nhận đã chuyển khoản manual |
| `POST` | `/api/manager/support-tickets/{id}/decision` | Manager | Quản lý phê duyệt / từ chối khoản hoàn $\ge 3.000.000$ VNĐ |

---

## 5. LỆNH TEST MOCK (CURL EXAMPLES)

### 5.1. Khách hàng tạo phiếu hoàn tiền (`REFUND`)
```bash
curl -X POST http://localhost:8080/api/support-tickets \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10" \
  -d '{
    "orderId": 1,
    "ticketType": "REFUND",
    "reason": "Vợt có dấu hiệu nứt ở khớp nối chữ T",
    "description": "Mở hộp kiểm tra phát hiện vết nứt dài 2cm",
    "refundQrUrl": "https://img.vietqr.io/image/970422-0901234567-compact2.jpg",
    "evidenceUrls": ["https://res.cloudinary.com/demo/image/upload/sample_defect.jpg"],
    "items": [
      {
        "orderItemId": 101,
        "quantity": 1
      }
    ]
  }'
```

### 5.2. Nhân viên chấp thuận sơ duyệt kèm nguyên nhân (`STORE_FAULT`)
```bash
curl -X POST http://localhost:8080/api/staff/support-tickets/1/review \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "action": "approve",
    "cause": "STORE_FAULT",
    "note": "Xác nhận lỗi từ phía nhà sản xuất"
  }'
```

### 5.3. Nhân viên lưu kết quả kiểm tra hàng đạt (`PASSED`)
```bash
curl -X POST http://localhost:8080/api/staff/support-tickets/1/inspection \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "result": "PASSED",
    "note": "Hàng trả về còn nguyên tem bảo hành, phụ kiện đầy đủ"
  }'
```

### 5.4. Nhân viên xác nhận chuyển tiền hoàn và lưu mã giao dịch
```bash
curl -X POST http://localhost:8080/api/staff/support-tickets/1/refund-transfer \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "bankReference": "FT26103188991234",
    "note": "Đã chuyển khoản qua app Vietcombank"
  }'
```

