package fu.se.badmintonshop.modules.catalog.repository;

import fu.se.badmintonshop.modules.catalog.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
    List<ProductPriceHistory> findByVariantId(Long variantId);
}
