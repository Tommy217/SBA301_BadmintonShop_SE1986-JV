# Badminton Shop – Frontend

Giao diện web của Badminton Shop: trang mua hàng cho khách, trang quản trị người dùng cho System Admin và trang vận hành cửa hàng cho Manager/Staff.

- **Stack:** React 19, Vite 8, React Router 8, React-Bootstrap 2 (Bootstrap 5), Axios
- **Backend:** Spring Boot (dự kiến `http://localhost:8080`). Trong lúc backend chưa xong, frontend chạy với **mock API** có sẵn trong thư mục `mock/`.
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

Mở `http://localhost:5173`. Mock API chạy chung với Vite dev server ở đường dẫn `/api/*`, không cần mở thêm terminal.

### Các lệnh npm

| Lệnh                                                 | Tác dụng                                                                |
| ---------------------------------------------------- | ----------------------------------------------------------------------- |
| `npm run dev`                                        | Chạy dev server + mock API (port 5173)                                  |
| `npm run build`                                      | Build production vào `dist/`                                            |
| `npm run preview`                                    | Chạy thử bản build                                                      |
| `npm run lint`                                       | Kiểm tra ESLint, phải sạch trước khi tạo PR                             |
| `npm run format`                                     | Format toàn bộ code bằng Prettier                                       |
| `npm run format:check`                               | Kiểm tra format, không sửa file                                         |
| `npm run test:mock`                                  | Chạy test của mock API (`mock/tests/`)                                  |
| `npm run test:render`                                | Render thử các trang quản lý (`tests/`)                                 |
| `npm test`                                           | Chạy cả hai bộ test trên                                                |
| `npm run mock`                                       | Chạy riêng mock API ở port 3000 (thường không cần, `npm run dev` đã có) |
| `npm run seed:management` / `npm run seed:dashboard` | Nạp lại dữ liệu mẫu vào `mock/db.json`                                  |
| `npm run partner`                                    | Chạy hệ thống đối tác giao vận FastShip (`../partner-system`)           |

### Biến môi trường (`.env`)

| Biến               | Mặc định                | Ý nghĩa                                                                  |
| ------------------ | ----------------------- | ------------------------------------------------------------------------ |
| `VITE_API_URL`     | `/api`                  | Base URL mà `httpClient` gọi tới                                         |
| `VITE_CATALOG_API_URL` | `VITE_API_URL`       | Base URL riêng cho Catalog; đặt `http://localhost:8080/api` để gọi Java trực tiếp |
| `VITE_USE_MOCK`    | `true`                  | `true`: dùng mock trong `mock/`. `false`: proxy `/api` sang backend thật |
| `VITE_BACKEND_URL` | `http://localhost:8080` | Địa chỉ backend Spring Boot khi `VITE_USE_MOCK=false`                    |

Gọi Java trực tiếp từ trình duyệt cần backend cho phép CORS từ `http://localhost:5173`. Nếu không đặt `VITE_CATALOG_API_URL`, Catalog dùng `/api` qua Vite proxy.

Không commit file `.env`. Khi thêm biến mới thì thêm vào `.env.example` kèm giá trị mẫu.

### Tài khoản thử

Tài khoản lấy từ SQL Server (`users`, `roles`, `user_roles`), không còn user trong `mock/db.json`. Chạy backend Java ở cổng 8080 trước khi đăng nhập.

Bốn tài khoản trong SQL seed: `admin@gmail.com`, `manager@gmail.com`, `staff@gmail.com`, `customer@gmail.com`; mật khẩu seed: `123`.

`/api/auth/*` và `/api/users/*` luôn đi tới Java kể cả khi `VITE_USE_MOCK=true`. Backend kiểm tra BCrypt, trạng thái tài khoản và role trong database. Token có hạn 8 giờ, bị thu hồi khi đăng xuất; khởi động lại backend sẽ yêu cầu đăng nhập lại. Các API mock còn lại xác minh token qua `/api/auth/me`, không tự xác thực mật khẩu. Frontend chỉ giữ token, hiển thị và điều hướng theo thông tin backend trả về.

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
├── mock/                         # Mock API tạm thời, bỏ khi tích hợp backend
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
    │   │   └── shop/             # ShopProvider, useShop(), shop.api.js (tạm, gắn với mock)
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

Giai đoạn mock dùng endpoint gom `/api/shop`: `GET` trả về toàn bộ dữ liệu cửa hàng, `POST { type, ... }` chạy một thao tác và trả về dữ liệu mới. `ShopProvider` giữ dữ liệu đó, `useShop()` trả về `{ data, busy, act, refresh }`, còn `act(request)` chạy hàm API và cập nhật dữ liệu.

Riêng đăng nhập (`authApi.login` → `/api/auth/login`) và quản lý tài khoản (`adminApi` → `/api/users`) đã gọi theo kiểu REST.

Khi tích hợp backend, chỉ sửa thân hàm trong file `.api.js` sang REST (ví dụ `httpClient.post('/support-tickets', payload)`), rồi bỏ `ShopProvider`. Bảng endpoint dự kiến nằm trong plan của nhóm.

`httpClient` tự làm các việc sau, page không cần lặp lại:

- Trả thẳng `response.data`.
- Gắn header `Authorization: Bearer <token>`.
- Gặp 401 khi đang có token thì xóa phiên và phát sự kiện `auth-expired` để `AuthProvider` đăng xuất.
- Chuyển lỗi thành `Error` có `message` tiếng Việt lấy từ `response.data.message`.

## 5. Thêm một trang mới

1. Tạo `modules/<x>/pages/TenTrangPage.jsx`.
2. Nếu cần dữ liệu, thêm hàm vào `modules/<x>/api/<x>.api.js` (và hook trong `modules/<x>/hooks/` nếu logic dài).
3. Khai báo route trong file route của khu tương ứng: `app/router/public.routes.jsx`, `admin.routes.jsx` hoặc `management.routes.jsx`.
4. Nếu trang cần có trên menu, thêm link vào sidebar của layout đó (`app/layouts/<khu>/`).
5. Chạy `npm run format`, `npm run lint`, `npm run build`.

---

## 6. Giao vận (delivery) và đối tác FastShip

- Shop chỉ tạo đơn và gửi yêu cầu tạo vận đơn sang hệ thống đối tác FastShip.
- Khi đối tác cập nhật trạng thái, backend shop đồng bộ lại và đẩy cho trang quản lý giao hàng qua **WebSocket**.
- Frontend delivery chỉ nói chuyện với backend shop, không gọi thẳng FastShip. Riêng link “Tra cứu trên FastShip” mở trang `http://localhost:8082/track?code=...`.
- Trong giai đoạn mock, trạng thái được đồng bộ bằng nút “Đồng bộ” (action `sync_carrier_status`). Kết nối WebSocket sẽ thêm ở phase tích hợp backend.

---

## 7. Lộ trình

| Phase | Nội dung                                                                             | Trạng thái         |
| ----- | ------------------------------------------------------------------------------------ | ------------------ |
| 1     | Dọn cấu trúc, quy ước, lớp API chung, tài liệu                                       | Xong               |
| 2     | Tích hợp backend Spring Boot, bỏ mock và `ShopProvider`, thêm WebSocket cho delivery | Chờ leader yêu cầu |
| 3     | Cân nhắc TanStack Query thay cho custom hook                                         | Chờ leader yêu cầu |
