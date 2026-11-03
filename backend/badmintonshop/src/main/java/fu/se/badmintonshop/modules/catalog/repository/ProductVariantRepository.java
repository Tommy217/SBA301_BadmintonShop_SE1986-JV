package fu.se.badmintonshop.modules.catalog.repository;

import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);
    boolean existsByProductIdAndColorAndSize(Long productId, String color, String size);
    boolean existsByProductIdAndColorAndSizeAndIdNot(Long productId, String color, String size, Long id);
    Optional<ProductVariant> findBySku(String sku);
}
