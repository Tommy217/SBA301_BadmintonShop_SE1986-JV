package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.Supplier;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SupplierResponse(Integer id, String name, String phone, String email, String address) {
    public static SupplierResponse from(Supplier value) {
        return new SupplierResponse(value.getId(), value.getName(), value.getPhone(), value.getEmail(), value.getAddress());
    }
}
