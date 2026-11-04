package fu.se.badmintonshop.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record StockAuditUpdateRequest(@NotEmpty List<@NotNull @Valid StockAuditItemRequest> items) {}
