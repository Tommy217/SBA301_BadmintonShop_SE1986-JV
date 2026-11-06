package fu.se.badmintonshop.modules.auth;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
            .body(Map.of("message", exception.getReason() == null ? "Yêu cầu không hợp lệ." : exception.getReason()));
    }
}
