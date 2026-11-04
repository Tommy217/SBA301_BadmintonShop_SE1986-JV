package fu.se.badmintonshop.modules.inventory.repository;

import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InventoryVariantRepository extends JpaRepository<ProductVariant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v order by v.id")
    List<ProductVariant> findAllLocked();

    @Query("select coalesce(sum(i.quantity), 0) from OrderItem i where i.order.status in ('pending', 'confirmed', 'preparing', 'shipping')")
    Long allocatedQuantity();
}
