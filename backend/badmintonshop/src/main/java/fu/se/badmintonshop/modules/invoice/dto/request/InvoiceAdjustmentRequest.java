package fu.se.badmintonshop.modules.invoice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceAdjustmentRequest {
    @NotNull @Digits(integer = 12, fraction = 0)
    private BigDecimal amount;
    @Size(max = 255)
    private String description;
    @NotBlank @Size(max = 300)
    private String reason;
}
