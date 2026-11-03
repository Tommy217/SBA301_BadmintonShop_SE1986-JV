package fu.se.badmintonshop.modules.catalog.exception;

import fu.se.badmintonshop.modules.catalog.controller.*;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {BrandController.class, CategoryController.class, ProductController.class, VoucherController.class})
public class CatalogExceptionHandler {

    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<?> handleCatalogException(CatalogException e) {
        return ResponseEntity.status(e.status).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dữ liệu gửi lên không đúng định dạng.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<?> handleConflict(Exception e) {
        return ResponseEntity.status(409).body(Map.of("message", "Dữ liệu trùng, đang được sử dụng hoặc vừa thay đổi. Vui lòng tải lại."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<?> handleMalformed(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu gửi lên không đúng định dạng."));
    }
}
