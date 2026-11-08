package fu.se.badmintonshop.modules.order.exception;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

@RestControllerAdvice(basePackages = "fu.se.badmintonshop.modules.order.controller")
public class OrderExceptionHandler {
    @ExceptionHandler(OrderException.class)
    public ResponseEntity<?> business(OrderException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var error = e.getBindingResult().getFieldErrors().getFirst();
        return ResponseEntity.badRequest()
                .body(Map.of("message", error.getField() + ": " + error.getDefaultMessage()));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<?> conflict(Exception e) {
        return ResponseEntity.status(409)
                .body(
                        Map.of(
                                "message",
                                "Dữ liệu đang được sử dụng hoặc vừa thay đổi. Vui lòng tải lại."));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class,
        ArithmeticException.class
    })
    public ResponseEntity<?> malformed(Exception e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Dữ liệu không hợp lệ hoặc vượt giới hạn số lượng."));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        Map.of(
                                "message",
                                java.util.Objects.toString(
                                        e.getReason(), "Yêu cầu không thành công.")));
    }
}
