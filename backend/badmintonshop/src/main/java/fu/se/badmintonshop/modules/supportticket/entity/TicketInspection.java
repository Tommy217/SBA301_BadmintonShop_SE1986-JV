package fu.se.badmintonshop.modules.supportticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_inspection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "result", nullable = false, length = 20)
    private String result; // PASSED or FAILED

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @CreationTimestamp
    @Column(name = "inspected_at", nullable = false, updatable = false)
    private LocalDateTime inspectedAt;
}

