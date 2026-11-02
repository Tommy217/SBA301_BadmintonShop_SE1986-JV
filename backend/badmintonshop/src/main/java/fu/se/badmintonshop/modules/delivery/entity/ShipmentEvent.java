package fu.se.badmintonshop.modules.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/** Một mốc hành trình của vận đơn (bảng shipment_events). */
@Entity
@Table(name = "shipment_events", indexes = @Index(name = "idx_se_shipment", columnList = "shipment_id, occurred_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    //trang thai phia đoi tac tai thoi diem ghi nhan
    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Nationalized
    @Column(name = "title", length = 150)
    private String title;

    @Nationalized
    @Column(name = "description", length = 300)
    private String description;

    @Nationalized
    @Column(name = "location", length = 200)
    private String location;

    /** carrier | webhook | sync | manual */
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private String source = "carrier";

    @Column(name = "occurred_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime createdAt;
}