package fu.se.badmintonshop.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record StockAdjustmentRequest(@NotNull @Positive Long variantId, @NotNull @PositiveOrZero Integer countedQty,
        @NotBlank @Size(max = 300) String note) {}
