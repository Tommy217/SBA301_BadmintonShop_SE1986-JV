package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "brands", uniqueConstraints = {@UniqueConstraint(columnNames = "name"), @UniqueConstraint(columnNames = "slug")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "logo_url", columnDefinition = "varchar(max)")
    private String logoUrl;

    @Column(name = "logo_data", columnDefinition = "varbinary(max)")
    private byte[] logoData;

    public String getLogoUrl() {
        return logoData != null ? new ImageBytesConverter().convertToEntityAttribute(logoData) : logoUrl;
    }

    public void setLogoUrl(String value) {
        ImageBytesConverter.validate(value);
        if (value != null && value.startsWith("data:")) {
            logoData = new ImageBytesConverter().convertToDatabaseColumn(value);
            logoUrl = null;
        } else {
            logoUrl = value;
            logoData = null;
        }
    }

    // Lombok builders bypass setters, so normalize builder input before persistence too.
    @PrePersist
    @PreUpdate
    private void prepareImage() {
        if (logoUrl != null && logoUrl.startsWith("data:")) setLogoUrl(logoUrl);
    }

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();
}
