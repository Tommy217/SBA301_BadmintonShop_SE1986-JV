package fu.se.badmintonshop.modules.delivery.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// loi nghiep vu cua module delivery, mang theo HTTP status tra ve
@Getter
public class DeliveryException extends RuntimeException {
    private final HttpStatus status;

    public DeliveryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static DeliveryException notFound(String message) {
        return new DeliveryException(HttpStatus.NOT_FOUND, message);
    }

    public static DeliveryException badRequest(String message) {
        return new DeliveryException(HttpStatus.BAD_REQUEST, message);
    }

    public static DeliveryException conflict(String message) {
        return new DeliveryException(HttpStatus.CONFLICT, message);
    }
}
