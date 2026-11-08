package fu.se.badmintonshop.modules.common.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fu.se.badmintonshop.modules.cart.dto.response.CartSnapshotResponse;
import fu.se.badmintonshop.modules.order.dto.response.OrderSnapshotResponse;
import fu.se.badmintonshop.modules.order.dto.response.VoucherWalletResponse;

import java.util.Map;

/** Adapts the existing snapshot data to typed response DTOs. DTOs contain data only. */
public final class SnapshotResponseMapper {
    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private SnapshotResponseMapper() {}

    public static CartSnapshotResponse cart(Map<String, Object> data) {
        return MAPPER.convertValue(data, CartSnapshotResponse.class);
    }

    public static OrderSnapshotResponse order(Map<String, Object> data) {
        return MAPPER.convertValue(data, OrderSnapshotResponse.class);
    }

    public static VoucherWalletResponse wallet(Map<String, Object> data) {
        return MAPPER.convertValue(data, VoucherWalletResponse.class);
    }
}
