package fu.se.badmintonshop.modules.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrderException extends RuntimeException {
    private final int status;

    public OrderException(int status, String message) {
        super(message);
        this.status = status;
    }

    public OrderException(HttpStatus status, String message) {
        this(status.value(), message);
    }
}
