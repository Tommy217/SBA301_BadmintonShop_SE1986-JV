package fu.se.badmintonshop.modules.catalog.repository;

import fu.se.badmintonshop.modules.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = {"brand", "category", "images"})
    List<Product> findAll();

    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);
    boolean existsByBrandId(Integer brandId);
    boolean existsByCategoryId(Integer categoryId);
    Optional<Product> findBySlug(String slug);
}
