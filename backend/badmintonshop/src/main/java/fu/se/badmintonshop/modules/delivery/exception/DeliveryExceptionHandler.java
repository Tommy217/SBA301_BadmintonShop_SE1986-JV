package fu.se.badmintonshop.modules.delivery.exception;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.delivery.controller.CarrierController;
import fu.se.badmintonshop.modules.delivery.controller.CarrierWebhookController;
import fu.se.badmintonshop.modules.delivery.controller.CustomerShipmentController;
import fu.se.badmintonshop.modules.delivery.controller.ManagerShipmentController;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// chi ap cho controller delivery, khong doi format loi cua module khac
@RestControllerAdvice(assignableTypes = {
        ManagerShipmentController.class,
        CarrierController.class,
        CustomerShipmentController.class,
        CarrierWebhookController.class
})
public class DeliveryExceptionHandler {

    @ExceptionHandler(DeliveryException.class)
    public ResponseEntity<ApiResponse<Void>> handleDelivery(DeliveryException e) {
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dữ liệu gửi lên không hợp lệ.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformed(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không đúng định dạng.");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(Exception e) {
        return error(HttpStatus.CONFLICT, "Dữ liệu vừa bị thay đổi hoặc bị trùng. Vui lòng tải lại.");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message));
    }
}
