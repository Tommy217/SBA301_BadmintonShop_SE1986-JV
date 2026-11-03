package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.dto.request.*;
import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.*;
import fu.se.badmintonshop.modules.catalog.service.ProductService;
import fu.se.badmintonshop.modules.inventory.entity.InventoryMovement;
import fu.se.badmintonshop.modules.inventory.repository.InventoryMovementRepository;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    ProductRepository productRepository;
    ProductVariantRepository productVariantRepository;
    ProductImageRepository productImageRepository;
    RacketSpecRepository racketSpecRepository;
    ShoeSpecRepository shoeSpecRepository;
    ApparelSpecRepository apparelSpecRepository;
    ProductPriceHistoryRepository productPriceHistoryRepository;
    BrandRepository brandRepository;
    CategoryRepository categoryRepository;
    VoucherScopeRepository voucherScopeRepository;
    OrderItemRepository orderItemRepository;
    InventoryMovementRepository inventoryMovementRepository;
    ProductService service;

    @BeforeEach
    void setup() {
        productRepository = mock(ProductRepository.class);
        productVariantRepository = mock(ProductVariantRepository.class);
        productImageRepository = mock(ProductImageRepository.class);
        racketSpecRepository = mock(RacketSpecRepository.class);
        shoeSpecRepository = mock(ShoeSpecRepository.class);
        apparelSpecRepository = mock(ApparelSpecRepository.class);
        productPriceHistoryRepository = mock(ProductPriceHistoryRepository.class);
        brandRepository = mock(BrandRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        voucherScopeRepository = mock(VoucherScopeRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        inventoryMovementRepository = mock(InventoryMovementRepository.class);

        service = new ProductService(
                productRepository, productVariantRepository, productImageRepository,
                racketSpecRepository, shoeSpecRepository, apparelSpecRepository,
                productPriceHistoryRepository, brandRepository, categoryRepository,
                voucherScopeRepository, orderItemRepository, inventoryMovementRepository
        );
    }

    @Test
    void validatesSpecAndImageOwnership() {
        Product racket = Product.builder().id(1L).productType("racket").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(racket));

        ShoeSpecRequest shoeReq = ShoeSpecRequest.builder()
                .productId(1L)
                .gender("unisex")
                .footWidth("regular")
                .build();
        assertThrows(CatalogException.class, () -> service.createShoeSpec(shoeReq));

        ProductVariant variant3 = ProductVariant.builder().id(2L).productId(3L).build();
        when(productVariantRepository.findById(2L)).thenReturn(Optional.of(variant3));

        ProductImageRequest imageReq = ProductImageRequest.builder()
                .productId(1L)
                .variantId(2L)
                .url("/image.svg")
                .sortOrder(0)
                .build();
        assertThrows(CatalogException.class, () -> service.createImage(imageReq));
    }

    @Test
    void priceChangeRecordsHistoryAndStockCannotBeEdited() {
        Product product = Product.builder().id(1L).name("Vợt").build();
        ProductVariant oldVariant = ProductVariant.builder()
                .id(2L)
                .productId(1L)
                .product(product)
                .sku("TEST-4U")
                .price(new BigDecimal("100000"))
                .costPrice(new BigDecimal("50000"))
                .stockQuantity(0)
                .lowStockThreshold(5)
                .isActive(true)
                .build();

        when(productVariantRepository.findById(2L)).thenReturn(Optional.of(oldVariant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariantRequest changePrice = ProductVariantRequest.builder()
                .productId(1L)
                .sku("TEST-4U")
                .price(new BigDecimal("120000"))
                .stockQuantity(0)
                .build();

        service.updateVariant(2L, changePrice);
        verify(productPriceHistoryRepository).save(any(ProductPriceHistory.class));

        ProductVariantRequest changeStock = ProductVariantRequest.builder()
                .productId(1L)
                .sku("TEST-4U")
                .price(new BigDecimal("120000"))
                .stockQuantity(3)
                .build();

        assertThrows(CatalogException.class, () -> service.updateVariant(2L, changeStock));
    }

    @Test
    void preventsDeletingUsedProductsWithoutCascading() {
        Product product = Product.builder().id(1L).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsByProductId(1L)).thenReturn(true);

        assertThrows(CatalogException.class, () -> service.deleteProduct(1L));
        verify(productRepository, never()).delete(any());
    }

    @Test
    void createsInitialStockLedgerWhenStockGreaterThanZero() {
        Product product = Product.builder().id(1L).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productVariantRepository.existsBySkuIgnoreCase("TEST-SKU")).thenReturn(false);

        ProductVariant savedVariant = ProductVariant.builder()
                .id(2L)
                .productId(1L)
                .sku("TEST-SKU")
                .price(new BigDecimal("100000"))
                .stockQuantity(5)
                .build();

        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);

        ProductVariantRequest request = ProductVariantRequest.builder()
                .productId(1L)
                .sku("TEST-SKU")
                .price(new BigDecimal("100000"))
                .stockQuantity(5)
                .build();

        service.createVariant(request);
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }
}
