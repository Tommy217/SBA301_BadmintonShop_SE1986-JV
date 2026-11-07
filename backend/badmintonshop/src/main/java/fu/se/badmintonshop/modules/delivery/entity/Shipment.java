package fu.se.badmintonshop.modules.delivery.entity;

import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_ship_tracking", columnList = "tracking_code"),
        @Index(name = "idx_ship_partner_ref", columnList = "partner_ref"),
        @Index(name = "idx_ship_order", columnList = "order_id"),
        @Index(name = "idx_ship_status", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    // id van don cua partner shipment
    @Column(name = "partner_ref", length = 64)
    private String partnerRef;

    // version document phia doi tac (tang moi lan cap nhat): bo qua webhook cu den muon
    @Column(name = "partner_version")
    private Long partnerVersion;

    @Column(name = "tracking_code", length = 80)
    private String trackingCode;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    /** standard | fast | express */
    @Column(name = "service_type", nullable = false, length = 20)
    @Builder.Default
    private String serviceType = "standard";

    // {@link ShipmentStatus}
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = ShipmentStatus.REQUESTED.getValue();

    @Nationalized
    @Column(name = "carrier_status_text", length = 300)
    private String carrierStatusText;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "cod_amount", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal codAmount = BigDecimal.ZERO;

    @Column(name = "cod_collected_at", columnDefinition = "datetime2(0)")
    private LocalDateTime codCollectedAt;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Nationalized
    @Column(name = "note", length = 500)
    private String note;

    @Nationalized
    @Column(name = "shipper_name", length = 150)
    private String shipperName;

    @Column(name = "shipper_phone", length = 20)
    private String shipperPhone;

    @Column(name = "estimated_delivery_date", columnDefinition = "datetime2(0)")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "shipped_at", columnDefinition = "datetime2(0)")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at", columnDefinition = "datetime2(0)")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at", columnDefinition = "datetime2(0)")
    private LocalDateTime cancelledAt;

    @Nationalized
    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Column(name = "customer_confirmed_at", columnDefinition = "datetime2(0)")
    private LocalDateTime customerConfirmedAt;

    @Column(name = "last_synced_at", columnDefinition = "datetime2(0)")
    private LocalDateTime lastSyncedAt;

    /** Staff tạo vận đơn (users.id). */
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt DESC")
    private List<ShipmentEvent> events = new ArrayList<>();

    public void addEvent(ShipmentEvent event) {
        event.setShipment(this);
        events.add(event);
    }
}
