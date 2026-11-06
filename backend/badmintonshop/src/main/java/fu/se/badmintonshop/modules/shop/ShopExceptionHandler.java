package fu.se.badmintonshop.modules.shop;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestControllerAdvice(assignableTypes = ShopController.class)
public class ShopExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", Objects.toString(error.getReason(), "Yêu cầu không thành công.")));
    }
    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class})
    public ResponseEntity<?> invalid(RuntimeException error) { return ResponseEntity.badRequest().body(Map.of("message", Objects.toString(error.getMessage(), "Dữ liệu không hợp lệ."))); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> conflict(IllegalStateException error) { return ResponseEntity.status(409).body(Map.of("message", Objects.toString(error.getMessage(), "Dữ liệu vừa thay đổi."))); }
}
