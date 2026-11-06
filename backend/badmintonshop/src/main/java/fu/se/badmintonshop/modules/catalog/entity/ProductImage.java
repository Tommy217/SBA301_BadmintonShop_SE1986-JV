package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = true)
    private ProductVariant variant;

    @Column(name = "url", columnDefinition = "varchar(max)")
    private String url;

    @Column(name = "image_data", columnDefinition = "varbinary(max)")
    private byte[] imageData;

    public String getUrl() {
        return imageData != null ? new ImageBytesConverter().convertToEntityAttribute(imageData) : url;
    }

    public void setUrl(String value) {
        ImageBytesConverter.validate(value);
        if (value != null && value.startsWith("data:")) {
            imageData = new ImageBytesConverter().convertToDatabaseColumn(value);
            url = null;
        } else {
            url = value;
            imageData = null;
        }
    }

    // Lombok builders bypass setters, so normalize builder input before persistence too.
    @PrePersist
    @PreUpdate
    private void prepareImage() {
        if (url != null && url.startsWith("data:")) setUrl(url);
    }

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
