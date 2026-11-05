package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.entity.Brand;
import fu.se.badmintonshop.modules.catalog.entity.Category;
import fu.se.badmintonshop.modules.catalog.entity.Product;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import fu.se.badmintonshop.modules.catalog.repository.BrandRepository;
import fu.se.badmintonshop.modules.catalog.repository.CategoryRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    BrandRepository brandRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    @Test
    void persistsBrandAndFindsByIdAndSlug() {
        Brand brand = Brand.builder()
                .name("Yonex Japan")
                .slug("yonex-japan")
                .isActive(true)
                .build();
        Brand saved = brandRepository.save(brand);

        assertNotNull(saved.getId());
        assertEquals("Yonex Japan", saved.getName());
        assertTrue(brandRepository.existsBySlugIgnoreCase("yonex-japan"));
        assertTrue(brandRepository.existsBySlugIgnoreCase("YONEX-JAPAN"));

        Optional<Brand> found = brandRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("yonex-japan", found.get().getSlug());
    }

    @Test
    void persistsProductAndVariants() {
        Brand brand = brandRepository.save(Brand.builder().name("Victor").slug("victor").isActive(true).build());
        Category category = categoryRepository.save(Category.builder().name("Vợt").slug("vot").isActive(true).build());

        Product product = Product.builder()
                .brand(brand)
                .category(category)
                .productType("racket")
                .name("Victor Auraspeed 90K")
                .slug("victor-auraspeed-90k")
                .basePrice(new BigDecimal("2500000"))
                .isActive(true)
                .build();
        Product savedProduct = productRepository.save(product);
        assertNotNull(savedProduct.getId());

        ProductVariant variant = ProductVariant.builder()
                .productId(savedProduct.getId())
                .product(savedProduct)
                .sku("TK-90K-4U")
                .price(new BigDecimal("2500000"))
                .stockQuantity(10)
                .color("Trắng")
                .size("4U-G5")
                .isActive(true)
                .build();
        ProductVariant savedVariant = productVariantRepository.save(variant);

        assertNotNull(savedVariant.getId());
        assertEquals(savedProduct.getId(), savedVariant.getProductId());
        assertTrue(productVariantRepository.existsBySkuIgnoreCase("TK-90K-4U"));
        assertEquals(1, productVariantRepository.findByProductId(savedProduct.getId()).size());
    }

    @Test
    void storefrontPaginatesInDatabaseAndFiltersBeforeCounting() {
        Brand brand = brandRepository.save(Brand.builder().name("Paging Brand").slug("paging-brand").build());
        Category category = categoryRepository.save(Category.builder().name("Paging Category").slug("paging-category").build());
        for (int i = 0; i < 12; i++) {
            productRepository.save(Product.builder().brand(brand).category(category)
                    .productType(i == 11 ? "shoes" : "racket").name("Paging Item " + i)
                    .slug(i == 10 ? "demo-product-racket" : "paging-item-" + i)
                    .basePrice(BigDecimal.valueOf(100000)).build());
        }
        var service = new fu.se.badmintonshop.modules.catalog.service.ProductService(
                productRepository, null, null, null, null, null, null,
                brandRepository, categoryRepository, null, null, null);
        var first = service.pageProducts(0, "Paging", "racket");
        var second = service.pageProducts(1, "Paging", "racket");
        assertEquals(8, ((java.util.List<?>) first.get("content")).size());
        assertEquals(2, ((java.util.List<?>) second.get("content")).size());
        assertEquals(10L, first.get("totalElements"));
        assertEquals(2, first.get("totalPages"));
        assertEquals(8, first.get("size"));
        assertEquals(1L, service.pageProducts(0, "Paging Item 1", "racket").get("totalElements"));
        assertEquals(0L, service.pageProducts(0, "Missing", "").get("totalElements"));
        assertTrue(((java.util.List<?>) service.pageProducts(5, "Paging", "racket").get("content")).isEmpty());
        assertThrows(fu.se.badmintonshop.modules.catalog.exception.CatalogException.class,
                () -> service.pageProducts(-1, "", ""));
    }
}
