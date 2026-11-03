package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.dto.request.BrandRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.BrandResponse;
import fu.se.badmintonshop.modules.catalog.entity.Brand;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.BrandRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherScopeRepository;
import fu.se.badmintonshop.modules.catalog.service.BrandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BrandServiceTest {
    BrandRepository brandRepository;
    ProductRepository productRepository;
    VoucherScopeRepository voucherScopeRepository;
    BrandService service;

    @BeforeEach
    void setup() {
        brandRepository = mock(BrandRepository.class);
        productRepository = mock(ProductRepository.class);
        voucherScopeRepository = mock(VoucherScopeRepository.class);
        service = new BrandService(brandRepository, productRepository, voucherScopeRepository);
    }

    @Test
    void rejectsDuplicateSlugIgnoringCase() {
        when(brandRepository.existsBySlugIgnoreCase("yonex")).thenReturn(true);
        BrandRequest request = BrandRequest.builder()
                .name("Yonex")
                .slug("YONEX")
                .isActive(true)
                .build();
        assertThrows(CatalogException.class, () -> service.create(request));
    }

    @Test
    void createsBrandSuccessfully() {
        BrandRequest request = BrandRequest.builder()
                .name("Yonex")
                .slug("yonex")
                .logoUrl("https://example.com/logo.png")
                .isActive(true)
                .build();

        Brand saved = Brand.builder()
                .id(1)
                .name("Yonex")
                .slug("yonex")
                .logoUrl("https://example.com/logo.png")
                .isActive(true)
                .build();

        when(brandRepository.existsByNameIgnoreCase("Yonex")).thenReturn(false);
        when(brandRepository.existsBySlugIgnoreCase("yonex")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(saved);

        BrandResponse response = service.create(request);
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Yonex", response.getName());
    }

    @Test
    void rejectsDeleteWhenInUse() {
        Brand brand = Brand.builder().id(1).name("Yonex").slug("yonex").build();
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
        when(productRepository.existsByBrandId(1)).thenReturn(true);

        assertThrows(CatalogException.class, () -> service.delete(1));
        verify(brandRepository, never()).delete(any());
    }
}
