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
    @Test
    void storefrontFiltersAndSortsByDisplayedPriceBeforePagination() {
        Brand brand = brandRepository.save(Brand.builder().name("Filter Brand").slug("filter-brand").build());
        Category category = categoryRepository.save(Category.builder().name("Filter Category").slug("filter-category").build());
        // Reverse names and prices so each sorting mode differs from ID order.
        for (int i = 0; i < 10; i++) {
            Product product = productRepository.save(Product.builder().brand(brand).category(category)
                    .name("Filter " + (char) ('J' - i)).slug("filter-" + i).productType("racket")
                    .basePrice(BigDecimal.valueOf(1000)).build());
            productVariantRepository.save(ProductVariant.builder().productId(product.getId())
                    .sku("filter-active-" + i).color("Black").size("4U")
                    .price(BigDecimal.valueOf((10 - i) * 100)).stockQuantity(1).build());
            productVariantRepository.save(ProductVariant.builder().productId(product.getId())
                    .sku("filter-out-of-stock-" + i).color("White").size("4U")
                    .price(BigDecimal.ONE).stockQuantity(0).build());
            productVariantRepository.save(ProductVariant.builder().productId(product.getId())
                    .sku("filter-inactive-" + i).color("Red").size("4U")
                    .price(BigDecimal.ZERO).isActive(false).stockQuantity(10).build());
        }
        var service = new fu.se.badmintonshop.modules.catalog.service.ProductService(
                productRepository, null, null, null, null, null, null,
                brandRepository, categoryRepository, null, null, null);
        var filtered = service.pageProducts(0, "filter", "racket", BigDecimal.valueOf(200), BigDecimal.valueOf(400), "price_asc");
        assertEquals(3L, filtered.get("totalElements"));
        assertEquals(java.util.List.of("Filter B", "Filter C", "Filter D"), names(filtered));
        assertEquals(java.util.List.of("Filter B", "Filter A"),
                names(service.pageProducts(1, "filter", "", null, null, "price_desc")));
        assertEquals("Filter A", names(service.pageProducts(0, "filter", "", null, null, "name_asc")).getFirst());
        assertEquals("Filter J", names(service.pageProducts(0, "filter", "", null, null, "name_desc")).getFirst());
        assertThrows(fu.se.badmintonshop.modules.catalog.exception.CatalogException.class,
                () -> service.pageProducts(0, "", "", BigDecimal.TEN, BigDecimal.ONE, "default"));
        assertThrows(fu.se.badmintonshop.modules.catalog.exception.CatalogException.class,
                () -> service.pageProducts(0, "", "", BigDecimal.valueOf(-1), null, "default"));
        assertThrows(fu.se.badmintonshop.modules.catalog.exception.CatalogException.class,
                () -> service.pageProducts(0, "", "", null, null, "invalid"));
    }

    @Test
    void storefrontPriceFallsBackToSoldOutVariantThenBasePrice() {
        Brand brand = brandRepository.save(Brand.builder().name("Fallback").slug("fallback").build());
        Category category = categoryRepository.save(Category.builder().name("Fallback").slug("fallback").build());
        Product soldOut = productRepository.save(Product.builder().brand(brand).category(category)
                .name("Fallback sold out").slug("fallback-sold-out").productType("shoes").basePrice(BigDecimal.valueOf(900)).build());
        productVariantRepository.save(ProductVariant.builder().productId(soldOut.getId()).sku("fallback-sold-out")
                .price(BigDecimal.valueOf(200)).stockQuantity(0).build());
        productRepository.save(Product.builder().brand(brand).category(category)
                .name("Fallback base").slug("fallback-base").productType("shoes").basePrice(BigDecimal.valueOf(300)).build());
        var service = new fu.se.badmintonshop.modules.catalog.service.ProductService(
                productRepository, null, null, null, null, null, null,
                brandRepository, categoryRepository, null, null, null);
        assertEquals(java.util.List.of("Fallback sold out", "Fallback base"),
                names(service.pageProducts(0, "fallback", "shoes", BigDecimal.valueOf(200), BigDecimal.valueOf(300), "price_asc")));
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> names(java.util.Map<String, Object> page) {
        return ((java.util.List<fu.se.badmintonshop.modules.catalog.dto.response.ProductResponse>) page.get("content"))
                .stream().map(fu.se.badmintonshop.modules.catalog.dto.response.ProductResponse::getName).toList();
    }
}
