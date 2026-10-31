---
name: sync-flow-documentation
description: Quy chuẩn và hướng dẫn bắt buộc để tự động cập nhật tài liệu Data Flow, Sequence Diagram, State Transitions và API Specs vào documents/module/<module_name>/flow.md mỗi khi mã nguồn hoặc luồng nghiệp vụ được cập nhật.
---

# SKILL: ĐỒNG BỘ & CẬP NHẬT DATA FLOW THEO MÃ NGUỒN HIỆN TẠI

## 1. MỤC ĐÍCH & NGUYÊN TẮC CỐT LÕI

Kỹ năng này định nghĩa quy trình chuẩn để AI Assistant và Developer bắt buộc tuân theo: **Bất cứ khi nào có sự thay đổi, bổ sung hoặc tái cấu trúc mã nguồn liên quan đến luồng nghiệp vụ, tài liệu Data Flow trong `documents/module/<module_name>/flow.md` phải được cập nhật đồng bộ ngay lập tức.**

### Nguyên tắc bắt buộc:
1. **Single Source of Truth (Code là chân lý):** Tài liệu trong `flow.md` phải phản ánh chính xác 100% logic đang chạy trong mã nguồn thực tế (Controllers, Services, Repositories, Entities, Database Constraints). Tuyệt đối không mô tả giả định hay giữ lại các luồng code cũ đã bị xóa/thay thế.
2. **Đồng bộ nguyên tử (Atomic Update):** Mỗi lần commit hoặc thay đổi chức năng, việc cập nhật `flow.md` phải diễn ra cùng lúc với việc viết code và kiểm thử, không để tồn đọng kỹ thuật (technical debt).
3. **Trực quan hóa (Visualization First):** Mọi luồng dữ liệu chính đều phải có sơ đồ tuần tự (**Mermaid Sequence Diagram**) có đánh số bước (`autonumber`) để người đọc dễ dàng hình dung.

---

## 2. QUY TRÌNH CẬP NHẬT TỪNG BƯỚC (STEP-BY-STEP WORKFLOW)

Khi nhận yêu cầu cập nhật hoặc sửa đổi chức năng của một module (ví dụ: `payment`, `order`, `inventory`, `catalog`...), hãy thực hiện theo 5 bước sau:

```
[Mã nguồn thay đổi] 
       │
       ▼
[Bước 1: Xác định phạm vi tác động] (API, Service, Entity, Webhook, Event)
       │
       ▼
[Bước 2: Đối chiếu file flow.md của module] (documents/module/<name>/flow.md)
       │
       ▼
[Bước 3: Cập nhật sơ đồ Mermaid & Logic nghiệp vụ]
       │
       ▼
[Bước 4: Cập nhật API Specifications & Mock Test cURL]
       │
       ▼
[Bước 5: Đồng bộ lại bảng mục lục tại documents/README.md]
```

### Bước 1: Xác định phạm vi tác động từ mã nguồn
Đọc và phân tích các tệp tin vừa thay đổi trong backend/frontend:
* **Thay đổi Entity / Database:** Có trường mới nào không? Có bảng nào đổi quan hệ foreign key không? Ràng buộc `status` có thêm giá trị mới không?
* **Thay đổi Service / Logic:** Các bước xử lý trong hàm có thay đổi thứ tự không? Có thêm bước gọi API bên ngoài (Third-party) không?
* **Thay đổi Controller:** Có thêm/bớt/sửa endpoint, HTTP Method, Request Body hay Response DTO không?
* **Sự kiện liên module (Events/Listeners):** Có phát sinh `ApplicationEvent` mới hoặc thay đổi listener không?

### Bước 2: Mở và cập nhật file `documents/module/<module_name>/flow.md`
Nếu thư mục hoặc file chưa tồn tại, hãy tạo mới theo định dạng: `documents/module/<module_name>/flow.md`.

### Bước 3: Cấu trúc bắt buộc của một file `flow.md`
Mỗi file `flow.md` phải đảm bảo đầy đủ các phần sau:

1. **Tổng quan module:** Tóm tắt ngắn gọn vai trò và các phương thức/chức năng chính.
2. **Sơ đồ tuần tự (Mermaid Sequence Diagrams):**
   * Sử dụng cú pháp `sequenceDiagram` có `autonumber`.
   * Định danh rõ các bên tham gia: `actor User`, `participant FE`, `participant BE`, `participant ThirdParty`, `participant DB`, `participant EventPublisher`.
   * Thể hiện rõ các nhánh điều kiện bằng khối `alt ... else ... end`.
3. **Ma trận ánh xạ trạng thái dữ liệu (State Transitions):**
   * Bảng mô tả sự thay đổi trạng thái của các Entity chính qua từng bước (ví dụ: `PaymentStatus` chuyển từ `pending` $\rightarrow$ `paid`, `OrderStatus` chuyển từ `pending` $\rightarrow$ `confirmed`).
4. **Cơ chế kỹ thuật nâng cao:**
   * Cơ chế xác thực chữ ký (Signature/Checksum), chống tấn công giả mạo.
   * Cơ chế chống xử lý trùng lặp (Idempotency Key / Status Check).
   * Cơ chế hết hạn phiên (Timeout / Expiration) và xử lý ngoại lệ (Exception Handling).
5. **Danh mục API & Lệnh Test Mock:**
   * Bảng tổng hợp tất cả endpoints (Method, URL, Quyền truy cập, Mục đích).
   * Kèm ít nhất 1 lệnh `curl` mẫu có đầy đủ Request Body và Response JSON tương ứng với code hiện tại.

### Bước 4: Kiểm tra tính nhất quán với Unit/Mock Tests
* So sánh các test case trong thư mục `src/test/java/...` với tài liệu `flow.md`.
* Đảm bảo các kịch bản thành công (Happy Path) và thất bại (Edge Cases) trong test đều được mô tả đúng trong tài liệu.

### Bước 5: Cập nhật Mục lục trung tâm (`documents/README.md`)
* Kiểm tra xem bảng mục lục tại [documents/README.md](file:///d:/_Ky7-FPTU/SBA301/gr_project/SBA301_BadmintonShop_SE1986-JV/documents/README.md) đã có link dẫn đến `flow.md` của module vừa cập nhật chưa.
* Cập nhật cột **Trạng thái** và **Mô tả tóm tắt** tương ứng.

---

## 3. CHECKLIST KIỂM TRA TRƯỚC KHI HOÀN THÀNH (DEFINITION OF DONE)

Mỗi khi agent hoặc developer hoàn tất cập nhật code, hãy tự kiểm tra theo checklist này:

- [ ] Đã kiểm tra code hiện tại và đảm bảo `flow.md` phản ánh 100% logic mới nhất.
- [ ] Sơ đồ Mermaid không có lỗi cú pháp, hiển thị rõ luồng request/response và database operations.
- [ ] Bảng trạng thái (`status`, `payment_status`...) đồng nhất hoàn toàn với các enum trong Java code và CHECK constraint trong file SQL.
- [ ] Tất cả cURL mẫu trong `flow.md` đều có thể chạy thử nghiệm được trên môi trường dev/mock.
- [ ] File [documents/README.md](file:///d:/_Ky7-FPTU/SBA301/gr_project/SBA301_BadmintonShop_SE1986-JV/documents/README.md) đã được dẫn link đầy đủ.

