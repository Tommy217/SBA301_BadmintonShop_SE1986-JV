# SBA301 Badminton Shop

Web bán đồ cầu lông của nhóm SE1986-JV (môn SBA301).

| Phần | Công nghệ | Thư mục |
| --- | --- | --- |
| Frontend | React 19, Vite, React Router, React Bootstrap | `frontend/` |
| Backend | Spring Boot REST API, chạy ở `http://localhost:8080` | `backend/` |
| Cơ sở dữ liệu | MS SQL Server | |
| Hệ thống giao hàng đối tác (giả lập) | FastShip: Spring Boot + MongoDB (8081), React (8082) | `partner-system/` (xem `partner-system/README.md`) |

Toàn bộ API cửa hàng dùng Spring Boot + SQL Server, bao gồm giỏ hàng, đặt hàng, dashboard, hỗ trợ, kho, hóa đơn và thanh toán. Cần chạy backend cổng 8080. Vite không chạy mock server và không đọc `frontend/mock/db.json`. Thư mục `frontend/mock/` chỉ lưu mã mẫu/test cũ để tham khảo, không tham gia luồng chạy ứng dụng.

Database được quản lý bằng Flyway trong `backend/badmintonshop/src/main/resources/db/migration`. Xem [hướng dẫn database và baseline DB cũ](documents/flyway.md) trước lần chạy đầu tiên. File `badmintonshop.sql` chỉ giữ làm bản script/seed cũ, có lệnh xóa database ở đầu; không dùng file đó để cập nhật DB đã được Flyway quản lý.

DB trống có thể chạy Flyway V1–V8 để tạo đầy đủ schema và dữ liệu demo tương đương script tổng, gồm 4 tài khoản mẫu (mật khẩu `123`), 32 sản phẩm, 95 biến thể, 54 đơn hàng và 52 hóa đơn. Không baseline DB trống. DB đã baseline V1/chạy V2 chỉ cần migrate tiếp để thêm các seed còn thiếu.

Thanh toán thật cần `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`; không tạo mã QR giả khi thiếu cấu hình. Giao vận FastShip cần backend đối tác tại `http://localhost:8081` (có thể đổi bằng `FASTSHIP_API_URL`). FastShip báo trạng thái vận đơn về shop qua webhook ký HMAC (`/api/webhooks/carriers/fastship`, khoá `FASTSHIP_WEBHOOK_SECRET`); nút "Đồng bộ" và job 5 phút đọc lại trạng thái phía đối tác khi webhook bị mất.

## Bắt đầu

```bash
cd frontend
npm install
npm run dev
```

## Tài liệu

- [Hướng dẫn sử dụng frontend](frontend/README.md)
- [Quy tắc đóng góp và clean code](frontend/CONTRIBUTING.md)
