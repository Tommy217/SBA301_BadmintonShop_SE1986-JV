package fu.se.badmintonshop.modules.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record GoodsReceiptRequest(Integer supplierId, @Size(max = 300) String note,
        @NotEmpty List<@NotNull @Valid GoodsReceiptItemRequest> items) {}
