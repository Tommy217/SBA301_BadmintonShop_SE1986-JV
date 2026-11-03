package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.dto.request.VoucherRequest;
import fu.se.badmintonshop.modules.catalog.dto.request.VoucherScopeRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherResponse;
import fu.se.badmintonshop.modules.catalog.entity.Voucher;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.*;
import fu.se.badmintonshop.modules.catalog.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoucherServiceTest {
    VoucherRepository voucherRepository;
    VoucherScopeRepository voucherScopeRepository;
    VoucherUsageRepository voucherUsageRepository;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;
    ProductRepository productRepository;
    VoucherService service;

    @BeforeEach
    void setup() {
        voucherRepository = mock(VoucherRepository.class);
        voucherScopeRepository = mock(VoucherScopeRepository.class);
        voucherUsageRepository = mock(VoucherUsageRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        brandRepository = mock(BrandRepository.class);
        productRepository = mock(ProductRepository.class);

        service = new VoucherService(
                voucherRepository, voucherScopeRepository, voucherUsageRepository,
                categoryRepository, brandRepository, productRepository
        );
    }

    @Test
    void validatesVoucherScopeAgainstVoucherAndTarget() {
        Voucher voucher = Voucher.builder().id(1).code("V1").appliesTo("brand").build();
        when(voucherRepository.findById(1)).thenReturn(Optional.of(voucher));

        // Applies to brand, but scopeType is product -> must throw exception
        VoucherScopeRequest request = VoucherScopeRequest.builder()
                .voucherId(1)
                .scopeType("product")
                .scopeId(2L)
                .build();

        assertThrows(CatalogException.class, () -> service.createScope(request));
    }

    @Test
    void createsVoucherSuccessfully() {
        LocalDateTime now = LocalDateTime.now();
        VoucherRequest request = VoucherRequest.builder()
                .code("DISCOUNT10")
                .name("Giảm 10%")
                .discountType("percent")
                .discountValue(new BigDecimal("10"))
                .startsAt(now)
                .endsAt(now.plusDays(7))
                .appliesTo("all")
                .isActive(true)
                .build();

        Voucher saved = Voucher.builder()
                .id(1)
                .code("DISCOUNT10")
                .name("Giảm 10%")
                .discountType("percent")
                .discountValue(new BigDecimal("10"))
                .startsAt(now)
                .endsAt(now.plusDays(7))
                .appliesTo("all")
                .isActive(true)
                .build();

        when(voucherRepository.existsByCodeIgnoreCase("DISCOUNT10")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(saved);

        VoucherResponse response = service.createVoucher(request);
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("DISCOUNT10", response.getCode());
    }

    @Test
    void rejectsInvalidDiscountPercent() {
        LocalDateTime now = LocalDateTime.now();
        VoucherRequest request = VoucherRequest.builder()
                .code("DISCOUNT200")
                .name("Giảm 200%")
                .discountType("percent")
                .discountValue(new BigDecimal("200"))
                .startsAt(now)
                .endsAt(now.plusDays(7))
                .build();

        assertThrows(CatalogException.class, () -> service.createVoucher(request));
    }
}
