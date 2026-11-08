package fu.se.badmintonshop.modules.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    boolean existsByProductIdAndColorAndSize(Long productId, String color, String size);

    boolean existsByProductIdAndColorAndSizeAndIdNot(
            Long productId, String color, String size, Long id);

    Optional<ProductVariant> findBySku(String sku);

    @Query(
            "SELECT v FROM ProductVariant v JOIN v.product p JOIN p.brand b JOIN p.category c WHERE v.id=?1 AND v.isActive=true AND p.isActive=true AND b.isActive=true AND c.isActive=true")
    Optional<ProductVariant> findAvailableById(Long id);

    @Query(
            "UPDATE ProductVariant SET stockQuantity=stockQuantity-?1 WHERE id=?2 AND isActive=true AND stockQuantity>=?3")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int deductStock(Integer quantity, Long id, Integer requiredStock);

    @Query("UPDATE ProductVariant SET stockQuantity=stockQuantity+?1 WHERE id=?2")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int restoreStock(Integer quantity, Long id);
}
