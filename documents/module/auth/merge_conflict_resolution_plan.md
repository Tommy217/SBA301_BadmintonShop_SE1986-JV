# Kế Hoạch Giải Quyết Xung Đột Merge & Chuẩn Hóa CustomUserDetails

> **Tài liệu tham chiếu**: Kế hoạch xử lý xung đột khi merge `origin/main` vào branch `feat/forgot-password`.  
> **Định hướng kiến trúc**: Chuẩn hóa 100% Security Principal sang `CustomUserDetails`, loại bỏ hoàn toàn Technical Debt `AuthService.User`.

---

## 1. Tổng Quan Tình Trạng Merge

Khi thực hiện lệnh `git merge origin/main` trên branch `feat/forgot-password`, phát sinh 4 xung đột:
1. `backend/.../config/SecurityConfig.java`: **Both modified** (Xung đột matcher endpoint & format code).
2. `backend/.../modules/shop/ShopController.java`: **Modify / Delete** (`origin/main` đã xóa, `HEAD` có chỉnh sửa).
3. `backend/.../modules/shop/ShopService.java`: **Modify / Delete** (`origin/main` đã xóa, `HEAD` có chỉnh sửa).
4. `backend/.../modules/supportticket/service/SupportActions.java`: **Both modified** (Xung đột signature kiểu tham số user & annotation).

### Quyết định kiến trúc (Hướng A):
- Loại bỏ dứt điểm `AuthService.User`, không khôi phục `record User` hay các method `user(id)`, `authenticate(header)`.
- Sử dụng `CustomUserDetails` (đã có sẵn các method record-style `.id()`, `.role()`, `.isActive()`) làm Single Source of Truth cho Principal trong toàn bộ hệ thống.
- Cập nhật đồng bộ các service/controller mới từ `origin/main` (`CurrentUser`, `CommerceValidation`, `OrderService`, `OrderServiceImpl`, `CartController`) sang `CustomUserDetails`.

---

## 2. Chi Tiết Kế Hoạch Xử Lý 4 Xung Đột Trực Tiếp

### Xung đột 1: `SecurityConfig.java`
* **Vị trí**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/config/SecurityConfig.java`
* **Nội dung xung đột**:
  * `HEAD`: Cấu hình permitAll cho 2 endpoint mới: `"/api/auth/forgot-password"`, `"/api/auth/reset-password"`. Vẫn giữ endpoint cũ `/api/shop`.
  * `origin/main`: Đã đổi `/api/shop` thành `/api/cart`, `/api/orders` theo kiến trúc module mới; format chuẩn indentation.
* **Cách giải quyết**:
  * Giữ nguyên formatting và các matcher mới của `origin/main`.
  * Giữ lại 2 endpoint `forgot-password` và `reset-password` của `HEAD` trong danh sách `POST permitAll`.
* **Đoạn code đích**:
  ```java
                  .authorizeHttpRequests(
                          auth ->
                                  auth.requestMatchers("/error")
                                          .permitAll()
                                          .requestMatchers(
                                                  HttpMethod.GET,
                                                  "/api/cart",
                                                  "/api/orders",
                                                  "/api/catalog")
                                          .permitAll()
                                          .requestMatchers(
                                                  HttpMethod.POST,
                                                  "/api/auth/login",
                                                  "/api/auth/register",
                                                  "/api/auth/logout",
                                                  "/api/auth/forgot-password",
                                                  "/api/auth/reset-password",
                                                  "/api/payments/payos/webhook",
                                                  "/api/webhooks/carriers/**")
                                          .permitAll()
                                          ...
  ```

---

### Xung đột 2: `ShopController.java`
* **Vị trí**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/shop/ShopController.java`
* **Bản chất**: `origin/main` đã xóa module `shop` để chuyển sang `CartController` và `OrderController`. `HEAD` có sửa đổi kiểu dữ liệu.
* **Cách giải quyết**:
  * Chấp nhận xóa file khỏi git tree để đồng bộ theo `origin/main`.
  * Lệnh: `git rm backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/shop/ShopController.java`

---

### Xung đột 3: `ShopService.java`
* **Vị trí**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/shop/ShopService.java`
* **Bản chất**: `origin/main` đã xóa và thay thế bằng `CartService`, `OrderService`.
* **Cách giải quyết**:
  * Chấp nhận xóa file khỏi git tree.
  * Lệnh: `git rm backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/shop/ShopService.java`

---

### Xung đột 4: `SupportActions.java`
* **Vị trí**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/supportticket/service/SupportActions.java`
* **Nội dung xung đột**:
  * `HEAD`: `void execute(String type, Map<String, Object> action, CustomUserDetails user)`
  * `origin/main`: 
    ```java
    @Transactional
    public void execute(String type, Map<String, Object> action, AuthService.User user) {
        require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
    ```
* **Cách giải quyết**:
  * Sử dụng `CustomUserDetails` làm kiểu tham số.
  * Giữ annotation `@Transactional`, phạm vi `public`, và dòng kiểm tra xác thực `require(user != null, ...)`.
* **Đoạn code đích**:
  ```java
  package fu.se.badmintonshop.modules.supportticket.service;

  import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
  ...

      @Transactional
      public void execute(String type, Map<String, Object> action, CustomUserDetails user) {
          require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
          if ("update_refund_threshold".equals(type)) {
              require(
                      user != null && Set.of("MANAGER", "ADMIN").contains(user.role()),
                      HttpStatus.FORBIDDEN,
                      "Chỉ quản lý được thiết lập ngưỡng phê duyệt hoàn tiền.");
              java.math.BigDecimal newThreshold =
                      new java.math.BigDecimal(String.valueOf(action.get("threshold")));
              tickets.updateApprovalThreshold(newThreshold, user.id());
              return;
          }
          ...
  ```

---

## 3. Kế Hoạch Chuẩn Hóa Các File Phụ Thuộc (Theo Hướng A)

Để hệ thống compile sạch và không còn dấu vết của `AuthService.User`, 5 file sau sẽ được cập nhật đồng bộ:

### 3.1. `CurrentUser.java`
* **Đường dẫn**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/common/service/CurrentUser.java`
* **Thay đổi**: Trả về trực tiếp `CustomUserDetails` từ `Authentication`.
```java
package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private CurrentUser() {}

    public static CustomUserDetails from(Authentication authentication) {
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof CustomUserDetails user) {
            return user;
        }
        return null;
    }
}
```

---

### 3.2. `CommerceValidation.java`
* **Đường dẫn**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/common/service/CommerceValidation.java`
* **Thay đổi**: Đổi `management(AuthService.User user)` sang `management(CustomUserDetails user)`.
```java
package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
...

    public static boolean management(CustomUserDetails user) {
        return user != null && Set.of("STAFF", "MANAGER", "ADMIN").contains(user.role());
    }
```

---

### 3.3. `CartController.java`
* **Đường dẫn**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/cart/controller/CartController.java`
* **Thay đổi**: Đổi kiểu của hàm private `user(Authentication)` và `updatedSnapshot(user)` sang `CustomUserDetails`.
```java
    private CustomUserDetails user(Authentication authentication) {
        var user = CurrentUser.from(authentication);
        if (user == null) throw new CartException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        return user;
    }

    private OrderSnapshotResponse updatedSnapshot(CustomUserDetails user) {
        var result = new LinkedHashMap<>(orders.snapshot(user));
        result.putAll(carts.snapshot(user.id()));
        return SnapshotResponseMapper.order(result);
    }
```

---

### 3.4. `OrderService.java`
* **Đường dẫn**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/order/service/OrderService.java`
* **Thay đổi**: Đổi toàn bộ chữ ký method nhận `AuthService.User` sang `CustomUserDetails`.
```java
package fu.se.badmintonshop.modules.order.service;

import java.util.Map;
import fu.se.badmintonshop.modules.order.dto.request.*;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;

public interface OrderService {
    Map<String, Object> snapshot(CustomUserDetails user);

    Map<String, Object> customerSnapshot(CustomUserDetails user);

    Map<String, Object> voucherWallet(CustomUserDetails user);

    void execute(Map<String, Object> action, CustomUserDetails user);

    void checkout(CheckoutRequest request, CustomUserDetails user);

    void cancelOrder(long id, CancelOrderRequest request, CustomUserDetails user);

    void updateStatus(long id, UpdateOrderStatusRequest request, CustomUserDetails user);

    void startPayment(long id, StartOrderPaymentRequest request, CustomUserDetails user);

    void cancelPayment(long id, CancelOrderRequest request, CustomUserDetails user);
}
```

---

### 3.5. `OrderServiceImpl.java`
* **Đường dẫn**: `backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/order/service/impl/OrderServiceImpl.java`
* **Thay đổi**: 
  * Thay `import fu.se.badmintonshop.modules.auth.service.AuthService;` bằng `import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;`.
  * Đổi tất cả tham số method từ `AuthService.User user` thành `CustomUserDetails user`.
  * **Lưu ý**: Vì `CustomUserDetails` đã có `.id()` và `.role()`, toàn bộ thân hàm bên trong giữ nguyên 100%, không bị ảnh hưởng logic.

---

### 3.6. Dọn dẹp phụ trợ (Cleanup)
* `CartService.java`: Xóa import thừa `fu.se.badmintonshop.modules.auth.service.AuthService;`.

---

## 4. Kế Hoạch Các Bước Thực Thi (Step-by-Step Action Checklist)

- [ ] **Bước 1: Loại bỏ 2 file xung đột Modify/Delete**
  ```powershell
  git rm backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/shop/ShopController.java
  git rm backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/shop/ShopService.java
  ```
- [ ] **Bước 2: Cập nhật `SecurityConfig.java`**
  - Xóa conflict marker.
  - Tích hợp 2 route `/api/auth/forgot-password` và `/api/auth/reset-password` vào cấu trúc của `main`.
  - Chạy `git add backend/badmintonshop/src/main/java/fu/se/badmintonshop/config/SecurityConfig.java`.
- [ ] **Bước 3: Cập nhật `SupportActions.java`**
  - Đổi package sang `modules.supportticket.service`.
  - Nhận tham số `CustomUserDetails user`.
  - Giữ `@Transactional` và `public`.
  - Chạy `git add backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/supportticket/service/SupportActions.java`.
- [ ] **Bước 4: Cập nhật 5 file phụ thuộc sang `CustomUserDetails`**
  - Cập nhật `CurrentUser.java`.
  - Cập nhật `CommerceValidation.java`.
  - Cập nhật `CartController.java`.
  - Cập nhật `OrderService.java`.
  - Cập nhật `OrderServiceImpl.java`.
  - Xóa import thừa ở `CartService.java`.
- [ ] **Bước 5: Kiểm tra biên dịch & Test Suite**
  ```powershell
  cd backend/badmintonshop
  mvn clean test-compile
  mvn test
  ```
- [ ] **Bước 6: Hoàn tất Merge Commit**
  ```powershell
  git add .
  git commit -m "merge: resolve conflicts with origin/main and standardize on CustomUserDetails"
  ```

---

## 5. Tiêu Chí Nghiệm Thu (Acceptance Criteria)
1. `git status` không còn bất kỳ unmerged path nào.
2. `mvn clean test-compile` hoàn thành thành công (`BUILD SUCCESS`), không có compilation error.
3. Toàn bộ unit test hiện có của `auth`, `order`, `payment`, `supportticket` đều PASS.
4. Hai endpoint `/api/auth/forgot-password` và `/api/auth/reset-password` hoạt động bình thường, không yêu cầu xác thực.
5. Không còn bất kỳ file nào trong dự án phụ thuộc vào `AuthService.User`.

