# TÀI LIỆU HỆ THỐNG - SBA301 BADMINTON SHOP
> **Nhóm thực hiện:** SE1986-JV (Môn SBA301 - FPT University)  
> **Dự án:** Badminton Shop Management System

---

## 1. MỤC LỤC TÀI LIỆU CÁC MODULE (SYSTEM MODULES)

Hệ thống được thiết kế theo kiến trúc module hóa (Modular Architecture). Tài liệu kỹ thuật chi tiết của từng phân hệ được tổ chức theo thư mục dưới đây:

| Phân hệ / Module | Đường dẫn tài liệu | Trạng thái | Mô tả tóm tắt |
| :--- | :--- | :--- :---: | :--- |
| **Auth & Access Control** | [**`module/auth/plan.md`**](module/auth/plan.md) | **Kế hoạch triển khai** | Kiến trúc Clean Architecture, JJWT 0.12.x, JPA Entities, Phân quyền RBAC, Quản lý Profile & Sổ địa chỉ. |
| **Payment Module** | [**`module/payment/flow.md`**](module/payment/flow.md) | **Đã hoàn thành** | Luồng thanh toán trực tuyến payOS (VietQR 24/7), Đối soát kép (Dual Verification: Webhook & Polling Sync), tích hợp đổi trả. |
| **Support Ticket** | [**`module/support-ticket/flow.md`**](module/support-ticket/flow.md) | **Đã hoàn thành** | Quy trình đổi trả & hoàn tiền thủ công quét QR, thẩm định hàng, phân quyền duyệt < 3M vs >= 3M. |
| **Order Module** | `module/order/` | Đang phát triển | Luồng quản lý giỏ hàng, đặt hàng, hủy đơn, chuyển trạng thái đơn hàng. |
| **Catalog & Products** | `module/catalog/` | Đang phát triển | Quản lý sản phẩm (vợt, giày, áo quần), biến thể, hình ảnh và voucher. |
| **Inventory Module** | `module/inventory/` | Đang phát triển | Quản lý kho, nhập hàng, kiểm kê, cảnh báo tồn kho thấp. |
| **Shipping Module** | [**`module/shipping/flow.md`**](module/shipping/flow.md) | Hoàn tất (GĐ5) · [kịch bản test](module/shipping/test-scenarios.md) | Gửi yêu cầu / huỷ vận đơn FastShip (FastShip duyệt, cấp mã); nhận trạng thái qua webhook ký HMAC, dự phòng bằng nút đồng bộ + job 5 phút; tự chuyển trạng thái đơn hàng, hoàn kho khi chuyển hoàn. Không thu hộ (COD). |
| **Invoices Module** | `module/invoices/` | Đang phát triển | Xuất hóa đơn điện tử tự động sau khi thanh toán thành công. |

---

## 2. HỆ THỐNG CƠ SỞ DỮ LIỆU
* **Database Script:** [badminton_shop.sql] (Đã chuẩn hóa hỗ trợ đầy đủ các module và cổng thanh toán payOS).

---

## 3. HƯỚNG DẪN TRUY CẬP TÀI LIỆU PAYMENT & SUPPORT TICKET
Tài liệu luồng hoạt động, sơ đồ tuần tự và đặc tả API chi tiết cho 2 module được tổ chức tách biệt tại:
* 👉 **[documents/module/payment/flow.md](module/payment/flow.md)**: Luồng hoạt động chi tiết Module Payment (payOS VietQR, Đối soát kép Dual Verification).
* 👉 **[documents/module/support-ticket/flow.md](module/support-ticket/flow.md)**: Luồng hoạt động chi tiết Module Support Ticket (Khiếu nại, Trả hàng & Hoàn tiền).

---

## 4. QUY CHUẨN ĐỒNG BỘ DATA FLOW (SKILLS)
Để đảm bảo tài liệu luôn đồng bộ 100% với mã nguồn sau mỗi lần cập nhật, hệ thống áp dụng quy chuẩn tại:
👉 **[documents/skills/SKILL.md](skills/SKILL.md)** (Skill tự động kiểm tra và cập nhật `flow.md` của các module theo code hiện tại).
