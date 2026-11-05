package fu.se.badmintonshop.modules.invoice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class InvoiceRequest {
    @NotNull @Positive
    private Long orderId;
    @Size(max = 150)
    private String buyerName;
    @Email @Size(max = 190)
    private String buyerEmail;
    @Size(max = 500)
    private String buyerAddress;
    @Size(max = 30)
    private String buyerTaxCode;
}
