package fu.se.badmintonshop.modules.invoice.exception;

import lombok.Getter;

@Getter
public class InvoiceException extends RuntimeException {
    private final int status;
    public InvoiceException(int status, String message) {
        super(message);
        this.status = status;
    }
}
