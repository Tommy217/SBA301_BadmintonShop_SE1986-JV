package fu.se.badmintonshop.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record SupplierRequest(@NotBlank @Size(max = 150) String name, @Size(max = 20) String phone,
        @Email @Size(max = 190) String email, @Size(max = 300) String address) {}
