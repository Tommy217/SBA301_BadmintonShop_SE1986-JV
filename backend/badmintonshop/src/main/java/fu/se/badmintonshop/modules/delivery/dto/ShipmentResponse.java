package fu.se.badmintonshop.modules.delivery.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// events chi co o API chi tiet; danh sach de null (bi an khoi JSON)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentResponse {
    private Long id;

    private Long orderId;
    private String orderCode;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    private String carrierCode;
    private String carrierName;
    private String carrierHotline;

    private String partnerRef;
    private String trackingCode;
    private String trackingUrl;
    private String serviceType;
    private String status;
    private String statusLabel;
    private String carrierStatusText;
    private Boolean cancellable;

    private BigDecimal shippingFee;
    private Integer weightGrams;
    private String note;
    private String shipperName;
    private String shipperPhone;

    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime lastSyncedAt;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ShipmentEventResponse> events;
}
