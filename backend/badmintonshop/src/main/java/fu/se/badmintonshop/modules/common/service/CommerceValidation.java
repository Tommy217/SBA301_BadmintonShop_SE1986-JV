package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import fu.se.badmintonshop.modules.common.exception.BusinessException;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CommerceValidation {
    private CommerceValidation() {}

    public static BusinessException notFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu hoặc bạn không có quyền truy cập.");
    }

    public static boolean management(AuthService.User user) {
        return user != null && Set.of("STAFF", "MANAGER", "ADMIN").contains(user.role());
    }

    public static long id(Object value) {
        try {
            long id = Long.parseLong(Objects.toString(value, ""));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Mã dữ liệu không hợp lệ.");
    }

    public static int positiveInt(Object value) {
        try {
            int quantity = new BigDecimal(Objects.toString(value, "")).intValueExact();
            if (quantity > 0) return quantity;
        } catch (ArithmeticException | NumberFormatException ignored) {
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Số lượng phải là số nguyên dương.");
    }

    public static int number(Object value) {
        return ((Number) value).intValue();
    }

    public static BigDecimal money(Object value) {
        return new BigDecimal(Objects.toString(value, "0"));
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static String text(Map<String, Object> a, String key) {
        return Objects.toString(a.get(key), "").trim();
    }

    public static String bounded(Map<String, Object> a, String key, int min, int max) {
        String value = text(a, key);
        require(
                value.length() >= min && value.length() <= max,
                HttpStatus.BAD_REQUEST,
                "Trường " + key + " cần từ " + min + " đến " + max + " ký tự.");
        return value;
    }

    public static void require(boolean condition, HttpStatus status, String message) {
        if (!condition) throw new BusinessException(status, message);
    }
}
