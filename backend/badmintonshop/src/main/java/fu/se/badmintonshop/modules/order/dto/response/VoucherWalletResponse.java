package fu.se.badmintonshop.modules.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fu.se.badmintonshop.modules.cart.dto.response.*;
import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.supportticket.dto.response.*;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoucherWalletResponse {
    @JsonProperty("vouchers")
    private List<VoucherSnapshotResponse> vouchers;

    @JsonProperty("voucher_usages")
    private List<VoucherUsageSnapshotResponse> voucherUsages;
}
