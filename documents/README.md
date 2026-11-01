# TÀI LIỆU HỆ THỐNG - SBA301 BADMINTON SHOP
> **Nhóm thực hiện:** SE1986-JV (Môn SBA301 - FPT University)  
> **Dự án:** Badminton Shop Management System

---

## 1. MỤC LỤC TÀI LIỆU CÁC MODULE (SYSTEM MODULES)

Hệ thống được thiết kế theo kiến trúc module hóa (Modular Architecture). Tài liệu kỹ thuật chi tiết của từng phân hệ được tổ chức theo thư mục dưới đây:

| Phân hệ / Module | Đường dẫn tài liệu | Trạng thái | Mô tả tóm tắt |
| :--- | :--- | :---: | :--- |
| **Payment Module** | [**`module/payment/overview.md`**](module/payment/overview.md) | **Đã hoàn thành** | Luồng thanh toán 100% online payOS (VietQR 24/7), Hoàn tiền, bảo mật Webhook HMAC-SHA256 & phân quyền Staff / Manager. |
| **Support Ticket (UC15)** | [**`module/support-ticket/flow.md`**](module/support-ticket/flow.md) | **Đã hoàn thành** | Quy trình đổi trả & hoàn tiền thủ công quét QR, thẩm định hàng, phân quyền duyệt < 3M vs >= 3M (UC15). |
| **Order Module** | `module/order/` | Đang phát triển | Luồng quản lý giỏ hàng, đặt hàng, hủy đơn, chuyển trạng thái đơn hàng. |
| **Catalog & Products** | `module/catalog/` | Đang phát triển | Quản lý sản phẩm (vợt, giày, áo quần), biến thể, hình ảnh và voucher. |
| **Inventory Module** | `module/inventory/` | Đang phát triển | Quản lý kho, nhập hàng, kiểm kê, cảnh báo tồn kho thấp. |
| **Shipping Module** | `module/shipping/` | Đã có hệ thống đối tác FastShip | Tích hợp đơn vị vận chuyển, cập nhật tracking vận đơn (không thu hộ tiền mặt). |
| **Invoices Module** | `module/invoices/` | Đang phát triển | Xuất hóa đơn điện tử tự động sau khi thanh toán thành công. |

---

## 2. HỆ THỐNG CƠ SỞ DỮ LIỆU
* **Database Script:** [badminton_shop.sql] (Đã chuẩn hóa hỗ trợ đầy đủ các module và cổng thanh toán payOS).

---

## 3. HƯỚNG DẪN TRUY CẬP TÀI LIỆU PAYMENT
Để xem toàn bộ sơ đồ tuần tự (Sequence Diagram), quy trình kiểm thử và tích hợp chi tiết của Module Payment, vui lòng mở:
👉 **[documents/module/payment/flow.md](module/payment/flow.md)**

---

## 4. QUY CHUẨN ĐỒNG BỘ DATA FLOW (SKILLS)
Để đảm bảo tài liệu luôn đồng bộ 100% với mã nguồn sau mỗi lần cập nhật, hệ thống áp dụng quy chuẩn tại:
👉 **[documents/skills/SKILL.md](skills/SKILL.md)** (Skill tự động kiểm tra và cập nhật `flow.md` của các module theo code hiện tại).

