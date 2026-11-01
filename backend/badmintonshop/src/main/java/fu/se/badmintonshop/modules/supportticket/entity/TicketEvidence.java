package fu.se.badmintonshop.modules.supportticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_evidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 20)
    @Builder.Default
    private String fileType = "image"; // image, video, document

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}

