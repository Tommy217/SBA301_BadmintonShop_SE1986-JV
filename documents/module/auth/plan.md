# KẾ HOẠCH TRIỂN KHAI PHÂN HỆ AUTHENTICATION, ACCESS CONTROL & PROFILE
> **Phân hệ:** Authentication, User Administration & Access Control, Profile Management  
> **Dự án:** SBA301 Badminton Shop (SE1986-JV)  
> **Kiến trúc tham chiếu:** `clonecuaco` (`talenthub_be` - Spring Boot 3/4, Spring Security, JJWT 0.12.x, Spring Data JPA)  
> **Trạng thái:** Kế hoạch thực thi chính thức (Implementation Plan)

---

## 1. MỤC TIÊU & NGUYÊN TẮC THỰC THI

1. **Xóa bỏ Anti-pattern:** Dọn sạch các file nén phẳng và thư mục rỗng trong `modules/auth`, loại bỏ hoàn toàn việc gọi `JdbcTemplate` viết SQL thô trực tiếp trong Controller/Service.
2. **Chuẩn hóa tầng dữ liệu (Data Access Layer):** Chuyển sang Spring Data JPA, sử dụng `@EntityGraph` để triệt tiêu lỗi $N+1$ query khi truy vấn quan hệ `User` - `Role`.
3. **Thay thế cơ chế Session RAM bằng Stateless JWT:** Loại bỏ `ConcurrentHashMap` lưu UUID tạm bợ trong bộ nhớ máy chủ; tích hợp JJWT (HMAC-SHA ký số) chuẩn theo phong cách `talenthub_be`.
4. **Loại bỏ lỗ hổng `X-User-Id`:** Bỏ cơ chế bọc `HttpServletRequestWrapper` ép đè header; xóa bỏ `defaultValue = "1"` ở các controller lân cận (Support Ticket, Shipment), chuyển toàn bộ sang lấy user từ `SecurityContext` / `@AuthenticationPrincipal`.
5. **Hoàn thiện tính năng còn thiếu:** Bổ sung API Đăng ký khách hàng (`Register`), Quản lý hồ sơ cá nhân (`Profile`), Đổi mật khẩu (`Change Password`) và Sổ địa chỉ giao hàng (`Addresses`).

---

## 2. LỘ TRÌNH TRIỂN KHAI CHI TIẾT (4 GIAI ĐOẠN)

```mermaid
flowchart LR
    P1["Giai đoạn 1\nKiến trúc & JPA"] --> P2["Giai đoạn 2\nJWT & Auth Engine"]
    P2 --> P3["Giai đoạn 3\nProfile & Address Book"]
    P3 --> P4["Giai đoạn 4\nUser Admin & Dọn mã cũ"]
```

### Giai đoạn 1: Chuẩn hóa kiến trúc phân tầng & JPA Entity (Đã hoàn thành)
* **Trạng thái:** ✅ **Đã hoàn thành 100%**
* **Nhiệm vụ trọng tâm:**
  1. **Tổ chức lại package `modules/auth`:**
     - `controller`, `dto/request`, `dto/response`, `entity`, `enums`, `exception`, `repository`, `security`, `service/impl`.
  2. **Khai báo JPA Entities:**
     - `User`: Ánh xạ bảng `users` (`id`, `email`, `password_hash`, `full_name`, `phone`, `gender`, `birth_date`, `avatar_url`, `status`, `last_login_at`).
     - `Role`: Ánh xạ bảng `roles` (`id`, `code`, `name`).
     - `Address`: Ánh xạ bảng `addresses` (`id`, `user_id`, `receiver_name`, `phone`, `province`, `district`, `ward`, `street`, `is_default`).
     - `UserStatus`: Enum `ACTIVE`, `INACTIVE`, `LOCKED`.
  3. **Xây dựng Spring Data Repositories:**
     - `UserRepository`: Bổ sung `@EntityGraph(attributePaths = {"roles"}) Optional<User> findByEmail(String email)` và `boolean existsByEmail(String email)`.
     - `RoleRepository`: `Optional<Role> findByCode(String code)`.
     - `AddressRepository`: `List<Address> findByUserIdOrderByIsDefaultDesc(Long userId)`.

---

### Giai đoạn 2: Động cơ JWT & Phân hệ Authentication (Đã hoàn thành)
* **Trạng thái:** ✅ **Đã hoàn thành 100%**
* **Nhiệm vụ trọng tâm:**
  1. **Thêm dependency JJWT vào `pom.xml`:** `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (phiên bản `0.12.7`).
  2. **Xây dựng module `security`:**
     - `CustomUserDetails`: Implements `UserDetails`, chứa `id`, `email`, `fullName`, `authorities` (ROLE_ADMIN, ROLE_STAFF,...).
     - `UserDetailsServiceImpl`: Nạp thông tin user qua `UserRepository`, chuyển `Role` thành `SimpleGrantedAuthority`.
     - `JwtProperties`: Cấu hình secret-key, thời gian sống của token (TTL).
     - `JwtService`: Tạo Access Token (chứa claims: `userId`, `email`, `roles`), trích xuất claims, xác thực token hợp lệ.
     - `JwtAuthenticationFilter`: Filter `OncePerRequestFilter` trích xuất Bearer token từ header `Authorization`, xác thực và gán vào `SecurityContextHolder`.
  3. **Cập nhật `SecurityConfig`:**
     - Tích hợp `AuthenticationManager`, `DaoAuthenticationProvider`, `BCryptPasswordEncoder`.
     - Cấu hình phân quyền endpoint:
       - Public: `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/shop/**`, `/api/catalog/**`.
       - Yêu cầu đăng nhập: `/api/v1/auth/me`, `/api/v1/auth/logout`, `/api/v1/profile/**`.
       - Quyền Admin: `/api/v1/users/**`.
     - Xử lý lỗi trả về JSON chuẩn qua `AuthenticationEntryPoint` (401) và `AccessDeniedHandler` (403).
  4. **Triển khai `AuthService` & `AuthController`:**
     - `POST /api/v1/auth/register`: Đăng ký tài khoản khách hàng mới (mặc định role `CUSTOMER`, trạng thái `active`).
     - `POST /api/v1/auth/login`: Xác thực credentials qua `AuthenticationManager`, trả về token và thông tin cơ bản.
     - `POST /api/v1/auth/logout`: Xóa session context.
     - `GET /api/v1/auth/me`: Trả về thông tin danh tính người dùng hiện tại từ context.
  5. **Tích hợp Frontend:** Nối form tại [`RegisterPage.jsx`](file:///d:/SBA301_BadmintonShop_SE1986-JV/frontend/src/modules/auth/pages/RegisterPage.jsx) vào API `register`.

---

### Giai đoạn 3: Phân hệ Profile Management & Sổ địa chỉ (Đã hoàn thành)
* **Trạng thái:** ✅ **DONE (100%)**
* **Thời lượng dự kiến:** 2 ngày.
* **Nhiệm vụ trọng tâm:**
  - [x] ✅ **Tạo `ProfileService` & `ProfileController` (`/api/v1/profile`):**
     - `GET /api/v1/profile`: Lấy đầy đủ thông tin cá nhân của user đang đăng nhập.
     - `PUT /api/v1/profile`: Cập nhật họ tên, số điện thoại, ngày sinh, giới tính, avatar URL.
     - `POST /api/v1/profile/change-password`: Kiểm tra mật khẩu hiện tại bằng `passwordEncoder.matches`, cập nhật mật khẩu mới nếu hợp lệ.
  - [x] ✅ **Quản lý sổ địa chỉ giao hàng (`/api/v1/profile/addresses`):**
     - `GET /api/v1/profile/addresses`: Lấy danh sách địa chỉ nhận hàng của user.
     - `POST /api/v1/profile/addresses`: Thêm địa chỉ mới; nếu đánh dấu `is_default = true`, tự động hủy cờ mặc định của các địa chỉ cũ.
     - `PUT /api/v1/profile/addresses/{id}`: Chỉnh sửa thông tin địa chỉ (chỉ được sửa địa chỉ của chính mình).
     - `DELETE /api/v1/profile/addresses/{id}`: Xóa địa chỉ nhận hàng.
  - [x] ✅ **Bộ kiểm thử tự động (Unit & Integration Tests):**
     - `ProfileServiceTest`: 13/13 tests kiểm tra logic cập nhật profile, đổi mật khẩu, sổ địa chỉ và chuyển cờ mặc định.
     - `ProfileControllerTest`: 8/8 tests kiểm tra xác thực JWT, mã trạng thái HTTP (200, 201, 401, 404).

---

### Giai đoạn 4: User Administration & Dọn dẹp mã cũ (Ưu tiên 2 & 3)
* **Thời lượng dự kiến:** 2 ngày.
* **Nhiệm vụ trọng tâm:**
  1. **Tái cấu trúc `UserController` & `UserService` (`/api/v1/users`):**
     - `GET /api/v1/users`: Phân trang danh sách người dùng (`Pageable`), hỗ trợ lọc theo từ khóa tìm kiếm (`keyword`) và vai trò (`role`).
     - `GET /api/v1/users/{id}`: Xem chi tiết tài khoản.
     - `POST /api/v1/users`: Admin tạo tài khoản nhân viên / quản lý, cấp quyền trực tiếp.
     - `PATCH /api/v1/users/{id}`: Cập nhật thông tin, thay đổi vai trò hoặc thay đổi trạng thái (`active` / `locked`).
  2. **Khắc phục triệt để lỗ hổng `X-User-Id`:**
     - Xóa bỏ lớp bọc `HttpServletRequestWrapper` giả lập header trong `TokenFilter`.
     - Thay thế toàn bộ `@RequestHeader(value = "X-User-Id", defaultValue = "1")` tại `CustomerTicketController`, `StaffTicketController`, `CustomerShipmentController` sang `@AuthenticationPrincipal CustomUserDetails`.
  3. **Cập nhật Unit & Integration Tests:**
     - Cập nhật `AuthServiceTest` và `AuthSecurityTest` sang cấu hình MockMvc với JWT token thật thay cho mock chuỗi thô.

---

## 3. BẢNG TỔNG HỢP API & DATABASE MAPPING

| Phân hệ | Phương thức | Endpoint | Phân quyền | Bảng CSDL tác động |
| :--- | :---: | :--- | :---: | :--- |
| **Auth** | `POST` | `/api/v1/auth/register` | Public | `users`, `user_roles` | ✅ **DONE** |
| **Auth** | `POST` | `/api/v1/auth/login` | Public | `users` (`last_login_at`) | ✅ **DONE** |
| **Auth** | `POST` | `/api/v1/auth/logout` | Authenticated | Không | ✅ **DONE** |
| **Auth** | `GET` | `/api/v1/auth/me` | Authenticated | `users`, `user_roles`, `roles` | ✅ **DONE** |
| **Profile** | `GET` | `/api/v1/profile` | Authenticated | `users` | ✅ **DONE** |
| **Profile** | `PUT` | `/api/v1/profile` | Authenticated | `users` | ✅ **DONE** |
| **Profile** | `POST` | `/api/v1/profile/change-password` | Authenticated | `users` (`password_hash`) | ✅ **DONE** |
| **Profile** | `GET` | `/api/v1/profile/addresses` | Authenticated | `addresses` | ✅ **DONE** |
| **Profile** | `POST` | `/api/v1/profile/addresses` | Authenticated | `addresses` | ✅ **DONE** |
| **Profile** | `PUT` | `/api/v1/profile/addresses/{id}` | Authenticated | `addresses` | ✅ **DONE** |
| **Profile** | `DELETE` | `/api/v1/profile/addresses/{id}` | Authenticated | `addresses` | ✅ **DONE** |
| **User Admin** | `GET` | `/api/v1/users` | `ROLE_ADMIN` | `users`, `user_roles`, `roles` | ⏳ *Chưa làm* |
| **User Admin** | `GET` | `/api/v1/users/{id}` | `ROLE_ADMIN` | `users`, `user_roles`, `roles` | ⏳ *Chưa làm* |
| **User Admin** | `POST` | `/api/v1/users` | `ROLE_ADMIN` | `users`, `user_roles` | ⏳ *Chưa làm* |
| **User Admin** | `PATCH` | `/api/v1/users/{id}` | `ROLE_ADMIN` | `users`, `user_roles` | ⏳ *Chưa làm* |

---

## 4. TIÊU CHÍ NGHIỆM THU (DEFINITION OF DONE)

1. **Kiến trúc:** 100% code auth tuân thủ Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Entity. Không còn bất kỳ câu lệnh SQL thô `JdbcTemplate` nào trong module Auth.
2. **Bảo mật:** Đăng nhập sinh token JWT hợp lệ chuẩn JJWT; tắt/bật lại server không làm mất session của token còn hạn; không còn lỗ hổng inject ID qua header `X-User-Id`.
3. **Chức năng:** Khách hàng đăng ký được tài khoản từ giao diện React, đăng nhập nhận token, cập nhật được thông tin cá nhân, đổi mật khẩu và quản lý danh sách địa chỉ nhận hàng.
4. **Kiểm thử:** Toàn bộ test suite backend (`mvn test`) vượt qua màu xanh với cơ chế xác thực JWT mới.

