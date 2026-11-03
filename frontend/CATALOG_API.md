# Quản lý Catalog qua Java

Chỉ chuyển màn hình `/management/catalog/*`. Các module khác giữ nguyên nguồn dữ liệu hiện tại.

## API

Bốn controller trong `modules.catalog.controller`, không còn `CatalogController` hay endpoint `/api/catalog`:

| Controller | Collection API |
| --- | --- |
| BrandController | `/api/brands` |
| CategoryController | `/api/categories` |
| ProductController | `/api/products`, `/api/products/variants`, `/api/products/images`, `/api/products/racket-specs`, `/api/products/shoe-specs`, `/api/products/apparel-specs`, `/api/products/price-history` |
| VoucherController | `/api/vouchers`, `/api/vouchers/scopes`, `/api/vouchers/usages` |

Mỗi collection hỗ trợ GET danh sách, GET `/{id}`, POST tạo (201), PUT `/{id}` cập nhật, DELETE `/{id}` xóa (204). Lịch sử giá và lượt dùng voucher chỉ có GET. Bộ thông số dùng `product_id` làm ID. POST/PUT nhận JSON trực tiếp và trả bản ghi đã lưu; GET trả mảng hoặc bản ghi trực tiếp.

Ví dụ: `POST /api/brands` với body `{"name":"Yonex","slug":"yonex","logo_url":null,"is_active":true}`. Dùng ID trả về để `PUT /api/brands/{id}` hoặc `DELETE /api/brands/{id}`.

JSON giữ trường snake_case tương ứng schema/form frontend; ID dạng chuỗi bảo toàn BIGINT. Ngày giờ trả offset +07:00, dữ liệu gửi được chuyển về giờ Việt Nam trước khi lưu DATETIME2. Client không được sửa lịch sử, lượt đã dùng hoặc người tạo.

Mỗi controller dùng service và repository tương ứng:

| Controller | Service | Repository |
| --- | --- | --- |
| BrandController | BrandService | BrandRepository |
| CategoryController | CategoryService | CategoryRepository |
| ProductController | ProductService | ProductRepository |
| VoucherController | VoucherService | VoucherRepository |

`CatalogService`, `CatalogRepository` và thư mục `api` đã được xóa. Quy tắc nghiệp vụ nằm trong từng service; kiểm tra tham chiếu dữ liệu nằm trong repository tương ứng. Biến thể, ảnh, thông số, lịch sử giá thuộc nhóm Product; phạm vi/lượt dùng thuộc nhóm Voucher. `ProductVariantRepository` JPA cũ được giữ nguyên cho module khác.

- `controller`: nhận và trả HTTP.
- `service`: nghiệp vụ theo từng nhóm.
- `repository`: truy cập dữ liệu theo từng nhóm.
- `entity`: entity JPA hiện có.
- `exception`: `CatalogException`, `CatalogExceptionHandler` (chỉ áp dụng cho bốn controller).
- `enums`: `CatalogTable`, metadata bảng nội bộ, không phải endpoint API.
- `validation`: kiểm tra kiểu, độ dài và định dạng dữ liệu.
- `service/support`, `repository/support`: lớp trừu tượng cho trình tự CRUD và thao tác JDBC dùng chung, không phải bean dịch vụ tổng hợp Catalog.

Lỗi trả `{ "message": "..." }`, xung đột constraint/đồng thời trả 409. Không có tên bảng tùy ý trên HTTP.

## Frontend và khởi động

Frontend đã có Axios. `httpClient.js` là Axios instance chung với base URL, token và xử lý lỗi. `catalog.api.js` ánh xạ từng bảng UI sang endpoint tương ứng, ghép kết quả GET cho CatalogProvider. CRUD gọi trực tiếp collection; không dùng mock làm dữ liệu thay thế.

1. Chuẩn bị SQL Server theo `badminton_shop.sql`; không chạy lại phần CREATE trên database đã có.
2. Chạy Java: `mvnw.cmd spring-boot:run`.
3. Chạy frontend: `npm run dev`. Giữ `VITE_USE_MOCK=true` cho module khác. Mặc định Catalog dùng Vite proxy nên request trên trình duyệt hiển thị cổng 5173. Để gọi Java trực tiếp, đặt `VITE_BACKEND_URL=http://localhost:8080/api`; backend phải cho phép CORS từ frontend.
4. Khởi động lại Java và Vite nếu đang chạy route cũ.

Vite dev/preview proxy riêng `/api/brands`, `/api/categories`, `/api/products`, `/api/vouchers` vào Java. Production reverse proxy phải ánh xạ tương tự.

## Nghiệp vụ giữ nguyên

- Kiểm tra trường bắt buộc, độ dài, enum, số tiền/số nguyên, URL ảnh, khóa ngoại và mã trùng.
- Chặn vòng lặp danh mục; thông số đúng loại sản phẩm; ảnh đúng biến thể.
- Không chuyển biến thể sang sản phẩm khác hoặc sửa tồn kho cũ qua Catalog. Tồn đầu được ghi sổ kho cùng transaction.
- Đổi giá tự ghi lịch sử cùng transaction và rollback khi lỗi.
- Kiểm tra thời hạn, mức giảm, giới hạn dùng và phạm vi voucher.
- Chặn xóa dữ liệu có tham chiếu/giao dịch/lịch sử hoặc còn tồn kho; hỗ trợ tạm ẩn qua `is_active`.

Mock dùng chung được giữ để không ảnh hưởng module khác theo phạm vi đã xác nhận; không tự đồng bộ với SQL.

Backend vẫn giữ `SecurityConfig.anyRequest().permitAll()` của nhóm. Cần tích hợp xác thực Java/quyền `catalog.manage` cho bốn nhóm API trước khi triển khai thực tế. Người tạo/thay đổi chưa có principal Java nên còn NULL.

## Kiểm thử

```powershell
# Backend dùng H2/mock, không dùng SQL Server thật
.\mvnw.cmd '-Dtest=BrandServiceTest,CategoryServiceTest,ProductServiceTest,VoucherServiceTest,ProductRepositoryTest,CatalogControllerTest' test
# Frontend
node --test tests/catalog-api.test.mjs tests/catalog-render.test.mjs
npm run build
```

Bao gồm từng route đọc/CRUD, lịch sử chỉ đọc, loại bỏ route cũ, ánh xạ lỗi, Unicode/ID, validation, tham chiếu và rollback lịch sử giá. Chưa kiểm thử end-to-end trực tiếp trên SQL Server đang chạy.
