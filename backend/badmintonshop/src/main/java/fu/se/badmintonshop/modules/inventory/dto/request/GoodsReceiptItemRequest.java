package fu.se.badmintonshop.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record GoodsReceiptItemRequest(@NotNull @Positive Long variantId, @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal unitCost) {}
