package fu.se.badmintonshop.modules.supportticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "replacement_variant_id")
    private Long replacementVariantId; // EXCHANGE only: same product, same price
}

