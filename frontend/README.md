# Badminton Shop – Frontend

Giao diện web của Badminton Shop: trang mua hàng cho khách, trang quản trị người dùng cho System Admin và trang vận hành cửa hàng cho Manager/Staff.

- **Stack:** React 19, Vite 8, React Router 8, React-Bootstrap 2 (Bootstrap 5), Axios
- **Backend:** Spring Boot tại `http://localhost:8080` + SQL Server. Toàn bộ API chạy ở Java; frontend không bật mock server.
- **Quy tắc đóng góp code:** xem [CONTRIBUTING.md](./CONTRIBUTING.md). Đọc file đó trước khi tạo PR.

---

## 1. Cài đặt và chạy

Yêu cầu: Node.js 20 trở lên, npm 10 trở lên.

```bash
cd frontend
npm install
cp .env.example .env      # Windows: copy .env.example .env
npm run dev
```

Mở `http://localhost:5173`. Vite chuyển `/api/*` sang backend Java cổng 8080; cần chạy backend và SQL Server trước.

### Các lệnh npm

| Lệnh                   | Tác dụng                                                          |
| ---------------------- | ----------------------------------------------------------------- |
| `npm run dev`          | Chạy Vite (port 5173), proxy `/api` sang Java                     |
| `npm run build`        | Build production vào `dist/`                                      |
| `npm run preview`      | Chạy thử bản build                                                |
| `npm run lint`         | Kiểm tra ESLint, phải sạch trước khi tạo PR                       |
| `npm run format`       | Format toàn bộ code bằng Prettier                                 |
| `npm run format:check` | Kiểm tra format, không sửa file                                   |
| `npm test`             | Chạy test frontend trong `tests/`; backend có bộ test Maven riêng |

### Biến môi trường (`.env`)

| Biến                   | Mặc định                | Ý nghĩa                                                                           |
| ---------------------- | ----------------------- | --------------------------------------------------------------------------------- |
| `VITE_API_URL`         | `/api`                  | Base URL mà `httpClient` gọi tới                                                  |
| `VITE_CATALOG_API_URL` | `VITE_API_URL`          | Base URL riêng cho Catalog; đặt `http://localhost:8080/api` để gọi Java trực tiếp |
| `VITE_BACKEND_URL`     | `http://localhost:8080` | Địa chỉ backend Spring Boot                                                       |

Gọi Java trực tiếp từ trình duyệt cần backend cho phép CORS từ `http://localhost:5173`. Nếu không đặt `VITE_CATALOG_API_URL`, Catalog dùng `/api` qua Vite proxy.

Không commit file `.env`. Khi thêm biến mới thì thêm vào `.env.example` kèm giá trị mẫu.

### Tài khoản thử

Tài khoản lấy từ SQL Server (`users`, `roles`, `user_roles`), không còn user trong `mock/db.json`. Chạy backend Java ở cổng 8080 trước khi đăng nhập.

Bốn tài khoản trong SQL seed: `admin@gmail.com`, `manager@gmail.com`, `staff@gmail.com`, `customer@gmail.com`; mật khẩu seed: `123`.

Mọi API đi tới Java. Backend kiểm tra BCrypt, trạng thái tài khoản và role trong database. Token có hạn 8 giờ, bị thu hồi khi đăng xuất; khởi động lại backend sẽ yêu cầu đăng nhập lại. Frontend chỉ giữ token, hiển thị và điều hướng theo thông tin backend trả về. `/api/shop` trả dữ liệu theo người dùng trong token; khách không được xem đơn hàng của người khác.

### Các cổng trong toàn hệ thống

| Thành phần                         | Port |
| ---------------------------------- | ---- |
| Shop frontend (thư mục này)        | 5173 |
| Shop backend Spring Boot           | 8080 |
| FastShip backend (đối tác giả lập) | 8081 |
| FastShip trang tra cứu             | 8082 |

---

## 2. Phân khu theo vai trò

| Khu                 | Đường dẫn                                                                           | Role                         | Layout             |
| ------------------- | ----------------------------------------------------------------------------------- | ---------------------------- | ------------------ |
| Cửa hàng            | `/`, `/products`, `/cart`, `/order`, `/orders`, `/payments/*`, `/support-tickets/*` | Khách vãng lai, `CUSTOMER`   | `PublicLayout`     |
| Quản trị hệ thống   | `/admin/*`                                                                          | `ADMIN` (quản lý người dùng) | `AdminLayout`      |
| Vận hành cửa hàng   | `/management/*`                                                                     | `MANAGER`, `STAFF`           | `ManagementLayout` |
| Đăng nhập / đăng ký | `/login`, `/register`                                                               | Ai cũng vào được             | Không dùng layout  |

Sau khi đăng nhập, người dùng được chuyển về trang chủ của role mình (`shared/auth/rolePaths.js`). Route cần đăng nhập được bọc trong `ProtectedRoute`, có thể giới hạn role bằng `allowedRoles`.

---

## 3. Cấu trúc thư mục

```
frontend/
├── .env.example
├── .editorconfig  .prettierrc.json  eslint.config.js  jsconfig.json  vite.config.js
├── mock/                         # Mã mẫu/test cũ, không còn được Vite hay ứng dụng chạy
│   ├── server.mjs                # Handler /api/* gắn vào Vite (shop, auth/login, users)
│   ├── actions/                  # Nghiệp vụ mock: catalog, management, inventory, invoice
│   ├── seed/                     # Script nạp dữ liệu mẫu
│   ├── tests/                    # Test của mock API
│   └── db.json                   # Dữ liệu mẫu
├── tests/                        # Test render các trang quản lý
├── public/
└── src/
    ├── main.jsx
    ├── app/
    │   ├── App.jsx
    │   ├── providers/
    │   │   ├── auth/             # AuthProvider, AuthContext + useAuth()
    │   │   └── shop/             # ShopProvider, useShop(), shop.api.js (snapshot từ Java)
    │   ├── router/               # routes.js, public/admin/management.routes.jsx, ProtectedRoute
    │   └── layouts/
    │       ├── public/           # PublicLayout, PublicHeader, PublicSidebar
    │       ├── admin/            # AdminLayout, AdminHeader, AdminSidebar, AdminFooter, AdminLayout.css
    │       └── management/       # ManagementLayout, ManagementSidebar
    ├── shared/
    │   ├── api/httpClient.js     # Axios dùng chung: gắn token, xử lý 401, chuẩn hóa lỗi
    │   ├── auth/                 # session.js (getToken, getRoles...), rolePaths.js
    │   ├── components/           # BrandLogo, Icon, QuantityControl, PublicFooter, HomeButton,
    │   │                         # ManagerControls, Loading, EmptyState
    │   ├── constants/roles.js
    │   ├── hooks/useAsync.js
    │   ├── utils/format.js       # formatMoney, formatNumber, formatDate, formatDateTime
    │   └── styles/               # ui.css (biến CSS + style nền), app.css (khung trang cửa hàng)
    └── modules/
        ├── auth/                 # Đăng nhập, đăng ký
        ├── shop/                 # Trang chủ, sản phẩm, giỏ hàng, đặt hàng, đơn của tôi
        ├── payment/              # Thanh toán
        ├── support/              # Phiếu hỗ trợ (khách + quản lý)
        ├── delivery/             # Giao vận (khách tra cứu + quản lý)
        ├── admin/                # Quản lý tài khoản (System Admin)
        └── management/           # Dashboard, đơn hàng, kho, hóa đơn, danh mục (Manager/Staff)
```

Mỗi module có cùng một khuôn:

```
modules/<ten-module>/
├── api/<ten-module>.api.js     # Mọi lời gọi API của module
├── pages/                      # Component gắn vào route
├── components/                 # Component chỉ module này dùng
├── hooks/                      # useXxx của module
├── utils/                      # Hàm thuần (tính toán, map dữ liệu)
├── constants.js
└── <ten-module>.css
```

Không cần tạo thư mục rỗng. Module nhỏ có thể chỉ có `api/` và `pages/`.

---

## 4. Gọi API

Page **không** gọi `fetch` hay `axios` trực tiếp. Luồng luôn là:

```
Page → (hook) → modules/<x>/api/<x>.api.js → shared/api/httpClient.js → /api/...
```

Ví dụ trong module support:

```js
// modules/support/api/support.api.js
import { sendShopAction } from '@/app/providers/shop/shop.api';

export const supportApi = {
  createTicket: (payload) => sendShopAction({ type: 'create_support_ticket', ...payload }),
};

// modules/support/pages/CreateSupportTicketPage.jsx
const { act } = useShop();
await act(() =>
  supportApi.createTicket({ order_id: orderId, ticket_type: ticketType, reason, description }),
);
```

Spring Boot cung cấp `/api/shop`: `GET` trả snapshot theo quyền và người dùng, `POST { type, ... }` ghi thao tác vào SQL Server rồi trả dữ liệu mới. Giá, voucher, tồn kho và quyền sở hữu được kiểm tra lại ở backend trong transaction. `ShopProvider` giữ snapshot; `useShop()` trả `{ data, busy, act, refresh }`.

Catalog tải qua một request `GET /api/catalog`, thay cho 11–12 request riêng trước đây. Luồng cửa hàng dùng `view=shop` và tải song song với `/api/shop` (tổng cộng 2 request). Backend chỉ trả lịch sử giá và lượt dùng voucher cho người có quyền quản lý; dữ liệu này không được cache chung giữa người dùng.

Riêng đăng nhập (`authApi.login` → `/api/auth/login`) và quản lý tài khoản (`adminApi` → `/api/users`) đã gọi theo kiểu REST.

Các thao tác phiếu hỗ trợ sử dụng service Java hiện có; kho, catalog và hóa đơn dùng các endpoint REST riêng. Local và bản deploy sử dụng cùng nguồn dữ liệu SQL Server.

`httpClient` tự làm các việc sau, page không cần lặp lại:

- Trả thẳng `response.data`.
- Gắn header `Authorization: Bearer <token>`.
- Gặp 401 khi đang có token thì xóa phiên và phát sự kiện `auth-expired` để `AuthProvider` đăng xuất.
- Chuyển lỗi thành `Error` có `message` tiếng Việt lấy từ `response.data.message`.
- Giới hạn chờ mặc định 15 giây, riêng thao tác `/api/shop` là 30 giây. Không tự gửi lại thao tác ghi khi timeout; cần kiểm tra trạng thái trước khi gửi lại.
- Lỗi mạng khi kiểm tra phiên đăng nhập giữ token và hiện nút thử lại; chỉ hiển thị ứng dụng sau khi xác thực thành công.

Khi cập nhật bản deploy, chạy lại backend mới trước, sau đó build và tải `dist/` lên Pages. Xem [hướng dẫn Cloudflare](../deploy/README.md).

## 5. Thêm một trang mới

1. Tạo `modules/<x>/pages/TenTrangPage.jsx`.
2. Nếu cần dữ liệu, thêm hàm vào `modules/<x>/api/<x>.api.js` (và hook trong `modules/<x>/hooks/` nếu logic dài).
3. Khai báo route trong file route của khu tương ứng: `app/router/public.routes.jsx`, `admin.routes.jsx` hoặc `management.routes.jsx`.
4. Nếu trang cần có trên menu, thêm link vào sidebar của layout đó (`app/layouts/<khu>/`).
5. Chạy `npm run format`, `npm run lint`, `npm run build`.

---

## 6. Giao vận (delivery) và đối tác FastShip

- Shop chỉ tạo đơn và gửi yêu cầu tạo vận đơn sang hệ thống đối tác FastShip.
- Nút “Tạo vận đơn FastShip” trong chi tiết đơn gọi backend shop để gửi yêu cầu sang backend đối tác (`http://localhost:8081`).
- Frontend delivery chỉ nói chuyện với backend shop, không gọi thẳng FastShip. Riêng link “Tra cứu trên FastShip” mở trang `http://localhost:8082/track?code=...`.
- Nút “Đồng bộ” đọc trạng thái và hành trình từ backend FastShip, lưu vào SQL Server. Nút này không tự tăng trạng thái giao hàng. WebSocket chưa được triển khai.

---

## 7. Lộ trình

| Phase | Nội dung                                                                    | Trạng thái                     |
| ----- | --------------------------------------------------------------------------- | ------------------------------ |
| 1     | Dọn cấu trúc, quy ước, lớp API chung, tài liệu                              | Xong                           |
| 2     | Dùng API Spring Boot cho toàn bộ luồng; giữ ShopProvider làm state frontend | Đã tích hợp; WebSocket chưa có |
| 3     | Cân nhắc TanStack Query thay cho custom hook                                | Chờ leader yêu cầu             |
