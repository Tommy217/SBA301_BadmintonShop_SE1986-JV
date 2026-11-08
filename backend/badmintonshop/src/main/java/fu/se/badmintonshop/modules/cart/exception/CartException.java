package fu.se.badmintonshop.modules.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CartException extends RuntimeException {
    private final int status;

    public CartException(int status, String message) {
        super(message);
        this.status = status;
    }

    public CartException(HttpStatus status, String message) {
        this(status.value(), message);
    }
}
