# LUỒNG HOẠT ĐỘNG MODULE TRẢ HÀNG & HOÀN TIỀN (UC15: RETURNS & REFUNDS)
> **Dự án:** SBA301_BadmintonShop_SE1986-JV  
> **Phân hệ:** Support Ticket / Returns & Refunds (UC15)  
> **Kiến trúc:** Java 21, Spring Boot, Spring Data JPA, SQL Server, React Vite Frontend

---

## 1. TỔNG QUAN NGHIỆP VỤ & QUY TẮC CỐT LÕI

Phân hệ Trả hàng & Hoàn tiền (UC15) xử lý khiếu nại sau bán hàng cho các đơn hàng trực tuyến đã giao thành công, tuân thủ các nguyên tắc thiết kế sau:

1. **Loại bỏ COD & Ví điện tử:** Tất cả các đơn hàng hoàn tiền đều là các giao dịch đã thanh toán trực tuyến qua Cổng thanh toán (payOS).
2. **Hoàn tiền chuyển khoản thủ công qua mã QR ngân hàng (100% Manual):**
   * Khách hàng **bắt buộc tải lên ảnh mã QR tài khoản ngân hàng** nhận tiền khi tạo yêu cầu.
   * Không lưu thông tin số tài khoản / tên ngân hàng dạng text (loại bỏ lỗi chính tả của khách).
   * Nhân viên quét ảnh QR của khách bằng ứng dụng Mobile Banking ngoài đời thực, thực hiện chuyển tiền, sau đó **nhập Mã tham chiếu giao dịch ngân hàng (`bank_reference`)** vào hệ thống để đối soát.
3. **Phạm vi nghiệp vụ:** Hệ thống chỉ xử lý **Trả hàng & Hoàn tiền (REFUND)**. Khách hàng có nhu cầu mua sản phẩm khác có thể thực hiện đặt đơn hàng mới sau khi nhận hoàn tiền.
4. **Chính sách hoàn tiền chỉ áp dụng khi lỗi từ phía cửa hàng (`STORE_FAULT`):**
   * Cửa hàng **chỉ chấp nhận hoàn tiền khi lỗi phát sinh từ phía shop / nhà sản xuất** (`STORE_FAULT`: giao sai mẫu, sai kích cỡ, sản phẩm lỗi nứt/bong tróc, hư hại khi mở hộp, thiếu phụ kiện...). Cửa hàng **hoàn lại cả tiền hàng và phí vận chuyển ban đầu**.
   * Trường hợp hàng hoàn về kiểm tra đạt chuẩn (hàng nguyên vẹn không lỗi) nhưng nguyên nhân xuất phát từ **nhu cầu cá nhân / khách hàng đổi ý không muốn nhận (`CUSTOMER_PREFERENCE`)** $\rightarrow$ Hệ thống sẽ **Từ chối phiếu (`REJECTED`)**, gửi trả lại sản phẩm cho khách và **không hoàn tiền**.
   * **Quy tắc không cộng lại kho (`No Restock`):** Do cửa hàng chỉ tiếp nhận hàng lỗi do shop để tiến hành bảo hành/thu hồi/hoàn tiền, các sản phẩm lỗi này không được đưa trở lại bán đại trà, do đó hệ thống **không thực hiện tự động cộng lại tồn kho**.
5. **Hạn mức phê duyệt 3.000.000 VNĐ:**
   * Số tiền hoàn **dưới 3.000.000 VNĐ**: Nhân viên (Staff) có thẩm quyền phê duyệt trực tiếp sang trạng thái chờ chuyển tiền (`PENDING_TRANSFER`).
   * Số tiền hoàn **từ 3.000.000 VNĐ trở lên**: Bắt buộc chuyển sang trạng thái chờ Quản lý phê duyệt (`PENDING_APPROVAL`). Chỉ tài khoản có vai trò `MANAGER` mới được phép duyệt hoặc từ chối.
6. **Ràng buộc duy nhất:** Mỗi đơn hàng chỉ được tạo tối đa 1 phiếu hỗ trợ (`UNIQUE order_id`).
7. **Quy trình gửi trả hàng:** Khách hàng đóng gói sản phẩm và gửi về địa chỉ kho của cửa hàng theo thông tin hướng dẫn trên hệ thống. Nhân viên nhận hàng tại kho và tiến hành kiểm định trực tiếp.

---

## 2. SƠ ĐỒ TUẦN TỰ HOẠT ĐỘNG (SEQUENCE DIAGRAM)

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

    Customer->>FE: Chọn đơn hàng đã giao (<= 7 ngày), chọn sản phẩm cần hoàn tiền
    Customer->>FE: Tải lên ảnh mã QR tài khoản nhận tiền & ảnh/video lỗi
    FE->>BE: POST /api/support-tickets (type='REFUND', items, qrUrl, evidenceUrls)
    Note over BE: Kiểm tra 1 ticket/order, thời hạn 7 ngày, QR bắt buộc
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
        alt Nguyên nhân là CUSTOMER_PREFERENCE (Khách đổi ý/không muốn)
            Note over BE: Hàng đạt chuẩn nhưng do khách đổi ý -> Từ chối phiếu
            BE->>DB: UPDATE status='REJECTED' (Gửi trả hàng, không hoàn tiền)
        else Nguyên nhân là STORE_FAULT (Lỗi do cửa hàng)
            Note over BE: Tính số tiền hoàn = tiền hàng + ship ban đầu
            alt Số tiền hoàn < 3.000.000 VNĐ
                BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=staffId
            else Số tiền hoàn >= 3.000.000 VNĐ
                BE->>DB: UPDATE status='PENDING_APPROVAL'
                Manager->>FE: Quản lý xem xét biên bản & bấm Duyệt
                FE->>BE: POST /api/manager/support-tickets/{id}/decision (decision='approve')
                BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=managerId
            end
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

## 3. MA TRẬN CÁC TRẠNG THÁI VÒNG ĐỜI (STATE TRANSITIONS)

| Trạng thái | Tên tiếng Việt | Quyền hạn thao tác | Ý nghĩa & Điều kiện chuyển tiếp |
| :--- | :--- | :--- | :--- |
| `PENDING` | Chờ tiếp nhận sơ duyệt | Khách hàng, Staff | Phiếu vừa tạo. Khách có thể bấm **Hủy (`CANCELLED`)**. Staff duyệt (chọn `cause` $\rightarrow$ `AWAITING_RETURN`) hoặc từ chối (`REJECTED`). |
| `REJECTED` | Bị từ chối | - | Trạng thái kết thúc. Bị Staff từ chối ở bước sơ duyệt, hoặc bị từ chối sau thẩm định vì hàng đạt chuẩn nhưng nguyên nhân là do khách đổi ý (`CUSTOMER_PREFERENCE`), hoặc Quản lý từ chối duyệt hoàn tiền. |
| `AWAITING_RETURN` | Chờ gửi lại hàng | Khách hàng, Staff | Khách đóng gói gửi hàng về địa chỉ kho. Staff nhận hàng và ghi nhận biên bản kiểm tra. |
| `INSPECTION_FAILED`| Kiểm tra không đạt | - | Trạng thái kết thúc. Hàng hoàn về không đủ điều kiện (bị can thiệp, hư hại do người dùng), hàng gửi trả lại khách, chặn hoàn tiền. |
| `PENDING_APPROVAL` | Chờ Quản lý duyệt | Quản lý (Manager) | Áp dụng khi số tiền hoàn $\ge 3.000.000$ VNĐ sau khi kiểm tra đạt. Chỉ Quản lý mới có quyền duyệt hoặc từ chối. |
| `PENDING_TRANSFER` | Chờ chuyển tiền | Nhân viên (Staff) | Khoản hoàn đã được duyệt (Staff duyệt nếu $< 3$ triệu, Quản lý duyệt nếu $\ge 3$ triệu). Nhân viên quét QR để chuyển tiền. |
| `REFUNDED` | Đã hoàn tiền | - | Trạng thái kết thúc. Nhân viên nhập mã tham chiếu ngân hàng thành công. Ghi nhận `refund_records`. |
| `CANCELLED` | Đã hủy | - | Trạng thái kết thúc. Khách chủ động hủy khi phiếu còn ở trạng thái `PENDING`. |

---

## 4. BẢNG DANH MỤC API SPECIFICATIONS

| Method | Endpoint | Quyền truy cập | Mục đích |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/support-tickets` | Khách hàng | Tạo yêu cầu trả hàng & hoàn tiền kèm ảnh QR và bằng chứng |
| `GET` | `/api/support-tickets` | Khách hàng | Danh sách phiếu hỗ trợ của khách hàng đang đăng nhập |
| `GET` | `/api/support-tickets/{id}` | Khách hàng / Staff | Xem chi tiết tiến trình phiếu hỗ trợ |
| `POST` | `/api/support-tickets/{id}/cancel` | Khách hàng | Khách hủy yêu cầu (chỉ khi còn `PENDING`) |
| `GET` | `/api/staff/support-tickets` | Staff / Manager | Hàng đợi danh sách ticket (lọc theo status) |
| `POST` | `/api/staff/support-tickets/{id}/review` | Staff | Sơ duyệt tiếp nhận: chọn `cause` hoặc từ chối |
| `POST` | `/api/staff/support-tickets/{id}/inspection` | Staff | Ghi nhận biên bản kiểm định hàng (`PASSED` / `FAILED`) |
| `POST` | `/api/staff/support-tickets/{id}/approve-refund`| Staff | Duyệt hoàn tiền cho đơn dưới 3.000.000 VNĐ |
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
    "note": "Hàng trả về đúng lỗi khai báo, đủ điều kiện tiếp nhận"
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
