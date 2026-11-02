package fu.se.badmintonshop.modules.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "carriers", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Nationalized
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "api_base_url", length = 255)
    private String apiBaseUrl;

    /** Link tra cứu, chứa {code} để thay bằng mã vận đơn. */
    @Column(name = "tracking_url_template", length = 500)
    private String trackingUrlTemplate;

    @Column(name = "hotline", length = 20)
    private String hotline;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public String buildTrackingUrl(String trackingCode) {
        if (trackingUrlTemplate == null || trackingCode == null) {
            return null;
        }
        return trackingUrlTemplate.replace("{code}", java.net.URLEncoder.encode(trackingCode, java.nio.charset.StandardCharsets.UTF_8));
    }
}