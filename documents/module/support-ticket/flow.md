# LUỒNG HOẠT ĐỘNG MODULE HỖ TRỢ ĐỔI TRẢ (SUPPORT TICKET: RETURNS & REFUNDS)
> **Dự án:** Badminton Shop Management System (SBA301_BadmintonShop_SE1986-JV)  
> **Phân hệ:** Support Ticket / Returns & Refunds (Khiếu nại, Trả hàng & Hoàn tiền)  
> **Công nghệ:** Java 21, Spring Boot 3, Spring Data JPA, Spring Security, SQL Server, React (Vite)  
> **Chuẩn kiến trúc:** RESTful Zero `/v1/` (`/api/support-tickets`, `/api/staff/support-tickets`, `/api/manager/support-tickets`)

---

## 1. NGUYÊN TẮC CỐT LÕI & QUY TẮC NGHIỆP VỤ

1. **Phạm vi xử lý (Chỉ Trả hàng & Hoàn tiền - REFUND):**  
   * Hệ thống chỉ tiếp nhận yêu cầu **Trả hàng & Hoàn tiền (`REFUND`)** đối với các đơn hàng đã giao thành công và đã thanh toán (`payment_status = 'paid'`).
   * Thời hạn yêu cầu: Trong vòng **7 ngày** kể từ thời điểm nhận hàng.
   * Khách có nhu cầu đổi sang mẫu hoặc size khác sẽ nhận tiền hoàn và thực hiện đặt đơn hàng mới.
   * Ràng buộc duy nhất: Mỗi đơn hàng chỉ được tạo tối đa 1 phiếu hỗ trợ còn hiệu lực (`UNIQUE order_id`).
2. **Bắt buộc tải ảnh mã QR ngân hàng nhận tiền:**  
   * Khách hàng **bắt buộc tải lên ảnh mã VietQR tài khoản ngân hàng** nhận tiền khi tạo ticket.
   * Không cho nhập số tài khoản / tên ngân hàng dạng text tự do nhằm loại bỏ hoàn toàn rủi ro sai sót thông tin thụ hưởng.
   * Bắt buộc đính kèm tối thiểu 1 hình ảnh hoặc video bằng chứng rõ nét về lỗi sản phẩm.
3. **Chính sách hoàn tiền chỉ áp dụng cho lỗi từ Cửa hàng (`STORE_FAULT`):**  
   * Cửa hàng **chỉ chấp nhận hoàn tiền khi phát sinh lỗi từ phía shop / nhà sản xuất** (`STORE_FAULT`: giao sai mẫu, sai kích thước, bong tróc, nứt gãy, hư hại do vận chuyển...). Cửa hàng **hoàn lại 100% tiền hàng và phí vận chuyển ban đầu**.
   * Trường hợp hàng hoàn về kiểm định đạt chuẩn (hàng nguyên vẹn không lỗi) nhưng nguyên nhân do **khách đổi ý cá nhân (`CUSTOMER_PREFERENCE`)** $\rightarrow$ Hệ thống sẽ **Từ chối phiếu (`REJECTED`)**, gửi trả lại sản phẩm cho khách và **không hoàn tiền**.
   * **Không nhập lại kho (`No Restock`):** Hàng lỗi thu hồi được chuyển bảo hành hãng hoặc tiêu hủy, không tự động cộng lại tồn kho bán đại trà.
4. **Phân quyền duyệt hoàn tiền theo hạn mức 3.000.000 VNĐ:**  
   * Số tiền hoàn **dưới 3.000.000 VNĐ**: Nhân viên CSKH (`STAFF`) có thẩm quyền phê duyệt trực tiếp sang trạng thái chờ chuyển tiền (`PENDING_TRANSFER`).
   * Số tiền hoàn **từ 3.000.000 VNĐ trở lên**: Chuyển sang trạng thái chờ Quản lý phê duyệt (`PENDING_APPROVAL`). Chỉ tài khoản có vai trò `MANAGER` hoặc `ADMIN` mới được phép ra quyết định.
5. **Hoàn tiền thủ công qua quét mã QR (Manual QR Transfer):**  
   * Nhân viên CSKH mở App ngân hàng quét trực tiếp ảnh QR do khách upload, chuyển đúng số tiền đã duyệt, sau đó **nhập Mã giao dịch ngân hàng (`bank_reference`)** vào hệ thống để đối soát và kết thúc phiếu (`REFUNDED`). Không dùng Payout API tự động trừ tiền.
6. **Bảo mật & Cô lập dữ liệu (Data Isolation):**  
   * Khách hàng chỉ xem và thao tác trên các phiếu hỗ trợ do tài khoản mình tạo. Header `X-User-Id` được trích xuất an toàn từ JWT Token của người đăng nhập.

---

## 2. SƠ ĐỒ LUỒNG HOẠT ĐỘNG (FLOWCHART & SEQUENCE DIAGRAM)

### 2.1. Sơ đồ tổng quan quy trình Trả hàng & Hoàn tiền (Flowchart)

```mermaid
flowchart TD
    Start([Khách mở Đơn hàng đã giao <= 7 ngày]) --> ClickCreate[Khách bấm nút 'Hỗ trợ / Đổi trả' trên OrdersPage]
    ClickCreate --> FormCreate[Điền lý do, chọn sản phẩm lỗi + Upload ảnh bằng chứng & ảnh QR ngân hàng]
    FormCreate --> SubmitTicket[POST /api/support-tickets -> Tạo phiếu status='PENDING']
    
    SubmitTicket --> StaffReview{Nhân viên sơ duyệt SLA 48h}
    StaffReview -->|Từ chối sơ duyệt| RejectReview([status='REJECTED' - Kết thúc phiếu])
    StaffReview -->|Chấp thuận| ApproveReview[Chọn nguyên nhân: STORE_FAULT hoặc CUSTOMER_PREFERENCE]
    ApproveReview --> StatusAwaiting[UPDATE status='AWAITING_RETURN' - Hướng dẫn gửi hàng]
    
    StatusAwaiting --> ShipBack[Khách đóng gói gửi hàng về địa chỉ kho shop]
    ShipBack --> WarehouseInspect[Nhân viên kho nhận kiện hàng & thẩm định chất lượng]
    WarehouseInspect --> InspectResult{Kết quả kiểm tra hàng?}
    
    InspectResult -->|FAILED - Hàng rách/hỏng do khách| FailedInspect([status='INSPECTION_FAILED' - Gửi trả hàng cho khách])
    InspectResult -->|PASSED - Hàng chuẩn| CheckCause{Nguyên nhân lỗi ban đầu?}
    
    CheckCause -->|CUSTOMER_PREFERENCE - Khách đổi ý| RejectPref([status='REJECTED' - Gửi trả hàng, không hoàn tiền])
    CheckCause -->|STORE_FAULT - Lỗi từ phía shop| CheckAmount{Số tiền hoàn}
    
    CheckAmount -->|< 3.000.000 VNĐ| StaffPass[Staff phê duyệt trực tiếp]
    CheckAmount -->|>= 3.000.000 VNĐ| ManagerReview[Chuyển PENDING_APPROVAL -> Quản lý thẩm định]
    ManagerReview -->|Manager duyệt| ManagerPass[Chuyển sang PENDING_TRANSFER]
    ManagerReview -->|Manager từ chối| ManagerReject([status='REJECTED' - Gửi trả hàng])
    
    StaffPass --> TransferStep[Chuyển sang PENDING_TRANSFER]
    ManagerPass --> TransferStep
    
    TransferStep --> ScanQR[Staff mở App ngân hàng quét ảnh QR của khách & chuyển tiền hoàn]
    ScanQR --> RecordRef[Staff nhập mã giao dịch ngân hàng bankReference]
    RecordRef --> DoneTicket([status='REFUNDED' - Hoàn tất phiếu & cập nhật orders, payments])
```

---

### 2.2. Sơ đồ tuần tự chi tiết (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng
    actor Staff as Nhân viên CSKH / Kho
    actor Manager as Quản lý (Manager)
    participant FE as Frontend (React Vite)
    participant BE as Backend (SupportTicketController & Service)
    participant DB as SQL Server (badminton_shop)
    participant BankApp as App Ngân hàng ngoài đời thực

    Customer->>FE: Bấm "Hỗ trợ / Đổi trả" trên đơn hàng đã thanh toán (/orders)
    FE->>Customer: Mở form /support-tickets/create?orderId=... (tự nạp đơn & sản phẩm)
    Customer->>FE: Chọn sản phẩm lỗi, nhập lý do, upload ảnh bằng chứng + ảnh QR nhận tiền
    FE->>BE: POST /api/support-tickets (orderId, items, refundQrUrl, evidenceUrls)
    Note over BE: Kiểm tra quyền sở hữu đơn, thời hạn 7 ngày, bắt buộc có ảnh QR
    BE->>DB: INSERT support_tickets (status='PENDING') & ticket_items, ticket_evidence
    BE-->>FE: HTTP 200 (Tạo phiếu hỗ trợ thành công)

    Note over Staff: Bước 1: Sơ duyệt tiếp nhận (SLA 48h)
    Staff->>FE: Mở phiếu PENDING, xem xét lý do và bằng chứng lỗi
    alt Sơ duyệt không hợp lệ
        Staff->>BE: POST /api/staff/support-tickets/{id}/review (action='reject', rejectReason)
        BE->>DB: UPDATE status='REJECTED'
    else Chấp thuận tiếp nhận
        Staff->>BE: POST /api/staff/support-tickets/{id}/review (action='approve', cause)
        BE->>DB: UPDATE status='AWAITING_RETURN', cause=cause
    end

    Note over Customer: Bước 2: Khách đóng gói gửi hàng về kho theo hướng dẫn
    Customer->>Staff: Kiện hàng hoàn trả về tới kho
    Staff->>FE: Lập biên bản kiểm định hàng tại kho (PASSED hoặc FAILED)
    Staff->>BE: POST /api/staff/support-tickets/{id}/inspection (result, note)
    BE->>DB: INSERT ticket_inspection

    alt Thẩm định Hỏng/Rách do người dùng (FAILED)
        BE->>DB: UPDATE status='INSPECTION_FAILED' (gửi trả hàng cho khách)
    else Thẩm định ĐẠT (PASSED)
        alt Khách đổi ý (CUSTOMER_PREFERENCE)
            BE->>DB: UPDATE status='REJECTED' (Hàng chuẩn không lỗi, gửi trả khách, không hoàn tiền)
        else Lỗi từ cửa hàng (STORE_FAULT)
            Note over BE: Tính số tiền hoàn = giá trị hàng + phí ship ban đầu
            alt Số tiền hoàn < 3.000.000 VNĐ
                Staff->>BE: POST /api/staff/support-tickets/{id}/approve-refund
                BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=staffId
            else Số tiền hoàn >= 3.000.000 VNĐ
                BE->>DB: UPDATE status='PENDING_APPROVAL'
                Manager->>FE: Quản lý xem xét biên bản & bấm Duyệt
                FE->>BE: POST /api/manager/support-tickets/{id}/decision (decision='approve')
                BE->>DB: UPDATE status='PENDING_TRANSFER', approved_by=managerId
            end
        end
    end

    Note over Staff: Bước 3: Thực hiện chuyển khoản thủ công
    Staff->>FE: Mở ảnh QR của khách và số tiền đã được duyệt
    Staff->>BankApp: Quét mã QR & thực hiện chuyển khoản thành công
    Staff->>BE: POST /api/staff/support-tickets/{id}/refund-transfer (bankReference, note)
    Note over BE: 1. Kiểm tra mã giao dịch duy nhất<br/>2. INSERT refund_records<br/>3. UPDATE support_tickets SET status='REFUNDED'<br/>4. UPDATE orders/payments status='refunded'
    BE->>DB: Lưu các bản ghi hoàn tất
    BE-->>FE: Hoàn tất quy trình hoàn tiền!
```

---

## 3. MA TRẬN 8 TRẠNG THÁI VÒNG ĐỜI SUPPORT TICKET (STATE TRANSITIONS)

```
[Khách tạo phiếu]
       │
       ▼
    PENDING ──(Khách chủ động hủy)──> CANCELLED
       │
       ├──(Staff từ chối sơ duyệt)──> REJECTED
       │
       └──(Staff chấp thuận sơ duyệt)──> AWAITING_RETURN
                                               │
               ┌───────────────────────────────┴───────────────────────────────┐
               ▼                                                               ▼
        [Kiểm định FAILED]                                              [Kiểm định PASSED]
               │                                                               │
        INSPECTION_FAILED                                     ┌────────────────┴────────────────┐
                                                              ▼                                 ▼
                                                   [Khách đổi ý/không lỗi]             [Lỗi do shop STORE_FAULT]
                                                              │                                 │
                                                           REJECTED                 ┌───────────┴───────────┐
                                                                                    ▼                       ▼
                                                                             [Tiền hoàn < 3M]       [Tiền hoàn >= 3M]
                                                                                    │                       │
                                                                           (Staff duyệt ngay)        PENDING_APPROVAL
                                                                                    │                       │
                                                                                    │               (Manager duyệt)
                                                                                    │                       │
                                                                                    ▼                       ▼
                                                                            PENDING_TRANSFER <──────────────┘
                                                                                    │
                                                                           (Staff quét QR chuyển tiền)
                                                                                    │
                                                                                    ▼
                                                                                 REFUNDED
```

| Trạng thái | Tên tiếng Việt | Quyền thao tác | Ý nghĩa & Điều kiện chuyển tiếp |
| :--- | :--- | :--- | :--- |
| `PENDING` | Chờ tiếp nhận sơ duyệt | Khách hàng, Staff | Phiếu vừa tạo. Khách có thể bấm **Hủy (`CANCELLED`)**. Staff duyệt (`AWAITING_RETURN`) hoặc từ chối (`REJECTED`). |
| `AWAITING_RETURN` | Chờ khách gửi lại hàng | Khách hàng, Staff | Khách đóng gói gửi hàng về kho shop. Nhân viên kho nhận hàng và lập biên bản kiểm định. |
| `INSPECTION_FAILED`| Kiểm tra hàng không đạt | - | Trạng thái kết thúc. Hàng rách/hỏng do lỗi người dùng. Gửi trả lại khách, chặn hoàn tiền. |
| `PENDING_APPROVAL` | Chờ Quản lý duyệt | Quản lý (`MANAGER`) | Hàng đạt chuẩn do lỗi shop nhưng số tiền hoàn $\ge 3.000.000$ VNĐ. Quản lý duyệt hoặc từ chối. |
| `PENDING_TRANSFER` | Chờ chuyển tiền | Nhân viên (`STAFF`) | Khoản hoàn đã được duyệt (Staff duyệt nếu $< 3$M, Quản lý duyệt nếu $\ge 3$M). Chờ Staff quét QR chuyển tiền. |
| `REFUNDED` | Đã hoàn tiền | - | Trạng thái kết thúc. Staff nhập mã giao dịch ngân hàng thành công. Ghi nhận bảng `refund_records`. |
| `REJECTED` | Bị từ chối | - | Trạng thái kết thúc. Bị từ chối ở bước sơ duyệt, hoặc do khách đổi ý (`CUSTOMER_PREFERENCE`), hoặc do Quản lý từ chối. |
| `CANCELLED` | Khách đã hủy | - | Trạng thái kết thúc. Khách chủ động hủy khi phiếu còn ở trạng thái `PENDING`. |

---

## 4. DANH MỤC API SPECIFICATIONS MODULE SUPPORT TICKET (ZERO `/v1/`)

### 4.1. Dành cho Khách hàng (`/api/support-tickets`)

| Method | Endpoint | Quyền truy cập | Mục đích nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/support-tickets` | Authenticated | Khách hàng tạo phiếu yêu cầu Trả hàng & Hoàn tiền kèm ảnh QR và bằng chứng |
| `GET` | `/api/support-tickets` | Authenticated | Khách hàng xem danh sách phiếu hỗ trợ của tài khoản mình |
| `GET` | `/api/support-tickets/{id}` | Authenticated (Chính chủ) | Khách hàng xem chi tiết tiến trình xử lý phiếu hỗ trợ |
| `POST` | `/api/support-tickets/{id}/cancel` | Authenticated (Chính chủ) | Khách hàng hủy yêu cầu hỗ trợ (chỉ khi phiếu đang ở trạng thái `PENDING`) |

### 4.2. Dành cho Nhân viên CSKH & Kho (`/api/staff/support-tickets`)

| Method | Endpoint | Quyền truy cập | Mục đích nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/staff/support-tickets` | `STAFF`, `MANAGER`, `ADMIN` | Xem hàng đợi danh sách ticket cần xử lý (lọc theo `status`, `type`) |
| `GET` | `/api/staff/support-tickets/{id}` | `STAFF`, `MANAGER`, `ADMIN` | Xem chi tiết thông tin và biên bản kiểm định của 1 phiếu |
| `POST` | `/api/staff/support-tickets/{id}/review` | `STAFF`, `MANAGER`, `ADMIN` | Sơ duyệt tiếp nhận (SLA 48h): duyệt chọn `cause` hoặc từ chối |
| `POST` | `/api/staff/support-tickets/{id}/inspection` | `STAFF`, `MANAGER`, `ADMIN` | Ghi nhận biên bản thẩm định hàng tại kho (`PASSED` / `FAILED`) |
| `POST` | `/api/staff/support-tickets/{id}/approve-refund` | `STAFF`, `MANAGER`, `ADMIN` | Nhân viên phê duyệt hoàn tiền (áp dụng cho khoản hoàn dưới 3.000.000 VNĐ) |
| `POST` | `/api/staff/support-tickets/{id}/refund-transfer` | `STAFF`, `MANAGER`, `ADMIN` | Nhân viên quét ảnh QR chuyển tiền và lưu mã giao dịch `bankReference` |

### 4.3. Dành cho Quản lý (`/api/manager/support-tickets`)

| Method | Endpoint | Quyền truy cập | Mục đích nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/manager/support-tickets/{id}/decision` | `MANAGER`, `ADMIN` | Quản lý phê duyệt (`approve`) hoặc từ chối (`reject`) khoản hoàn $\ge 3.000.000$ VNĐ |
| `GET` | `/api/manager/support-tickets/settings/threshold` | `MANAGER`, `ADMIN` | Xem ngưỡng phê duyệt hoàn tiền hiện tại của hệ thống (mặc định 3.000.000 VNĐ) |
| `PUT` | `/api/manager/support-tickets/settings/threshold` | `MANAGER`, `ADMIN` | Cập nhật ngưỡng phê duyệt hoàn tiền mới |
