package fu.se.badmintonshop.modules.invoice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class InvoiceDeliveryRequest {
    @NotNull @Pattern(regexp = "email|download")
    private String channel;
    @Email @Size(max = 190)
    private String recipient;
}
