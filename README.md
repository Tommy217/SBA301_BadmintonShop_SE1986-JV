# SBA301 Badminton Shop

Web bán đồ cầu lông của nhóm SE1986-JV (môn SBA301).

| Phần | Công nghệ | Thư mục |
| --- | --- | --- |
| Frontend | React 19, Vite, React Router, React Bootstrap | `frontend/` |
| Backend | Spring Boot REST API, chạy ở `http://localhost:8080` | `backend/` |
| Cơ sở dữ liệu | MS SQL Server | |
| Hệ thống giao hàng đối tác (giả lập) | FastShip | `partner-system/` (chạy bằng `npm run partner` trong `frontend/`) |

Toàn bộ API cửa hàng dùng Spring Boot + SQL Server, bao gồm giỏ hàng, đặt hàng, dashboard, hỗ trợ, kho, hóa đơn và thanh toán. Cần chạy backend cổng 8080. Vite không chạy mock server và không đọc `frontend/mock/db.json`. Thư mục `frontend/mock/` chỉ lưu mã mẫu/test cũ để tham khảo, không tham gia luồng chạy ứng dụng.

Database đang có dữ liệu: chạy phần cuối `badmintonshop.sql` từ marker `UPDATE 2026-11-06: JAVA SHOP API (BO MOCK SERVER)` để cập nhật schema. Phần đầu file vẫn xóa và tạo lại database; chỉ chạy toàn bộ khi muốn tạo DB mới.

Thanh toán thật cần `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`; không tạo mã QR giả khi thiếu cấu hình. Giao vận FastShip cần backend đối tác tại `http://localhost:8081` (có thể đổi bằng `FASTSHIP_API_URL`). Nút đồng bộ chỉ đọc trạng thái phía đối tác, không tự chuyển trạng thái giao hàng.

## Bắt đầu

```bash
cd frontend
npm install
npm run dev
```

## Tài liệu

- [Hướng dẫn sử dụng frontend](frontend/README.md)
- [Quy tắc đóng góp và clean code](frontend/CONTRIBUTING.md)
