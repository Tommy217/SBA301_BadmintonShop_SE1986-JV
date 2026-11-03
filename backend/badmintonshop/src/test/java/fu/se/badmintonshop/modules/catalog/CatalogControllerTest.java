package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.controller.*;
import fu.se.badmintonshop.modules.catalog.dto.request.BrandRequest;
import fu.se.badmintonshop.modules.catalog.dto.request.CategoryRequest;
import fu.se.badmintonshop.modules.catalog.dto.request.ProductRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.exception.CatalogExceptionHandler;
import fu.se.badmintonshop.modules.catalog.service.BrandService;
import fu.se.badmintonshop.modules.catalog.service.CategoryService;
import fu.se.badmintonshop.modules.catalog.service.ProductService;
import fu.se.badmintonshop.modules.catalog.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogControllerTest {
    MockMvc mvc;
    BrandService brandService;
    CategoryService categoryService;
    ProductService productService;
    VoucherService voucherService;

    @BeforeEach
    void setup() {
        brandService = mock(BrandService.class);
        categoryService = mock(CategoryService.class);
        productService = mock(ProductService.class);
        voucherService = mock(VoucherService.class);

        mvc = MockMvcBuilders.standaloneSetup(
                        new BrandController(brandService),
                        new CategoryController(categoryService),
                        new ProductController(productService),
                        new VoucherController(voucherService)
                )
                .setControllerAdvice(new CatalogExceptionHandler())
                .build();
    }

    @Test
    void brandsRoutes() throws Exception {
        BrandResponse res = BrandResponse.builder().id(2).name("Yonex").slug("yonex").isActive(true).build();
        when(brandService.list()).thenReturn(List.of(res));
        when(brandService.get(2)).thenReturn(res);
        when(brandService.create(any())).thenReturn(res);
        when(brandService.update(eq(2), any())).thenReturn(res);

        mvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Yonex"));

        mvc.perform(get("/api/brands/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        mvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Yonex\",\"slug\":\"yonex\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));

        mvc.perform(put("/api/brands/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Yonex\",\"slug\":\"yonex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        mvc.perform(delete("/api/brands/2"))
                .andExpect(status().isNoContent());

        verify(brandService).delete(2);
    }

    @Test
    void categoriesRoutes() throws Exception {
        CategoryResponse res = CategoryResponse.builder().id(2).name("Vợt").slug("vot").isActive(true).build();
        when(categoryService.list()).thenReturn(List.of(res));
        when(categoryService.get(2)).thenReturn(res);
        when(categoryService.create(any())).thenReturn(res);
        when(categoryService.update(eq(2), any())).thenReturn(res);

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));

        mvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vợt\",\"slug\":\"vot\"}"))
                .andExpect(status().isCreated());

        mvc.perform(put("/api/categories/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vợt\",\"slug\":\"vot\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/categories/2"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(2);
    }

    @Test
    void productsRoutes() throws Exception {
        ProductResponse res = ProductResponse.builder().id(2L).name("Astrox").build();
        when(productService.listProducts()).thenReturn(List.of(res));
        when(productService.getProduct(2L)).thenReturn(res);
        when(productService.createProduct(any())).thenReturn(res);
        when(productService.updateProduct(eq(2L), any())).thenReturn(res);

        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/products/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Astrox\",\"category_id\":1,\"brand_id\":1,\"product_type\":\"racket\",\"base_price\":100000}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Astrox\",\"category_id\":1,\"brand_id\":1,\"product_type\":\"racket\",\"base_price\":100000}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/products/2")).andExpect(status().isNoContent());
        verify(productService).deleteProduct(2L);
    }

    @Test
    void product_variantsRoutes() throws Exception {
        ProductVariantResponse res = ProductVariantResponse.builder().id(2L).sku("TEST").build();
        when(productService.listVariants()).thenReturn(List.of(res));
        when(productService.getVariant(2L)).thenReturn(res);
        when(productService.createVariant(any())).thenReturn(res);
        when(productService.updateVariant(eq(2L), any())).thenReturn(res);

        mvc.perform(get("/api/products/variants")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/products/variants/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":1,\"sku\":\"TEST\",\"price\":100000}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/variants/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":1,\"sku\":\"TEST\",\"price\":100000}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/products/variants/2")).andExpect(status().isNoContent());
        verify(productService).deleteVariant(2L);
    }

    @Test
    void product_imagesRoutes() throws Exception {
        ProductImageResponse res = ProductImageResponse.builder().id(2L).url("/img.jpg").build();
        when(productService.listImages()).thenReturn(List.of(res));
        when(productService.getImage(2L)).thenReturn(res);
        when(productService.createImage(any())).thenReturn(res);
        when(productService.updateImage(eq(2L), any())).thenReturn(res);

        mvc.perform(get("/api/products/images")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/products/images/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":1,\"url\":\"/img.jpg\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/images/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":1,\"url\":\"/img.jpg\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/products/images/2")).andExpect(status().isNoContent());
        verify(productService).deleteImage(2L);
    }

    @Test
    void racket_specsRoutes() throws Exception {
        RacketSpecResponse res = RacketSpecResponse.builder().productId(2L).balanceType("head_heavy").build();
        when(productService.listRacketSpecs()).thenReturn(List.of(res));
        when(productService.getRacketSpec(2L)).thenReturn(res);
        when(productService.createRacketSpec(any())).thenReturn(res);
        when(productService.updateRacketSpec(eq(2L), any())).thenReturn(res);

        mvc.perform(get("/api/products/racket-specs")).andExpect(status().isOk()).andExpect(jsonPath("$[0].product_id").value(2));
        mvc.perform(get("/api/products/racket-specs/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products/racket-specs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":2,\"balance_type\":\"head_heavy\",\"flex\":\"stiff\",\"play_style\":\"attack\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/racket-specs/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":2,\"balance_type\":\"head_heavy\",\"flex\":\"stiff\",\"play_style\":\"attack\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/products/racket-specs/2")).andExpect(status().isNoContent());
        verify(productService).deleteRacketSpec(2L);
    }

    @Test
    void shoe_specsRoutes() throws Exception {
        ShoeSpecResponse res = ShoeSpecResponse.builder().productId(2L).gender("unisex").build();
        when(productService.listShoeSpecs()).thenReturn(List.of(res));
        when(productService.getShoeSpec(2L)).thenReturn(res);
        when(productService.createShoeSpec(any())).thenReturn(res);
        when(productService.updateShoeSpec(eq(2L), any())).thenReturn(res);

        mvc.perform(get("/api/products/shoe-specs")).andExpect(status().isOk()).andExpect(jsonPath("$[0].product_id").value(2));
        mvc.perform(get("/api/products/shoe-specs/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products/shoe-specs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":2,\"gender\":\"unisex\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/shoe-specs/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":2,\"gender\":\"unisex\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/products/shoe-specs/2")).andExpect(status().isNoContent());
        verify(productService).deleteShoeSpec(2L);
    }

    @Test
    void apparel_specsRoutes() throws Exception {
        ApparelSpecResponse res = ApparelSpecResponse.builder().productId(2L).gender("unisex").build();
        when(productService.listApparelSpecs()).thenReturn(List.of(res));
        when(productService.getApparelSpec(2L)).thenReturn(res);
        when(productService.createApparelSpec(any())).thenReturn(res);
        when(productService.updateApparelSpec(eq(2L), any())).thenReturn(res);

        mvc.perform(get("/api/products/apparel-specs")).andExpect(status().isOk()).andExpect(jsonPath("$[0].product_id").value(2));
        mvc.perform(get("/api/products/apparel-specs/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products/apparel-specs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":2,\"gender\":\"unisex\",\"apparel_kind\":\"shirt\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/apparel-specs/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":2,\"gender\":\"unisex\",\"apparel_kind\":\"shirt\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/products/apparel-specs/2")).andExpect(status().isNoContent());
        verify(productService).deleteApparelSpec(2L);
    }

    @Test
    void product_price_historyRoutes() throws Exception {
        ProductPriceHistoryResponse res = ProductPriceHistoryResponse.builder().id(2L).oldPrice(BigDecimal.TEN).newPrice(BigDecimal.valueOf(20)).build();
        when(productService.listPriceHistory()).thenReturn(List.of(res));
        when(productService.getPriceHistory(2L)).thenReturn(res);

        mvc.perform(get("/api/products/price-history")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/products/price-history/2")).andExpect(status().isOk());
        mvc.perform(post("/api/products/price-history").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void vouchersRoutes() throws Exception {
        VoucherResponse res = VoucherResponse.builder().id(2).code("VOUCHER").build();
        when(voucherService.listVouchers()).thenReturn(List.of(res));
        when(voucherService.getVoucher(2)).thenReturn(res);
        when(voucherService.createVoucher(any())).thenReturn(res);
        when(voucherService.updateVoucher(eq(2), any())).thenReturn(res);

        mvc.perform(get("/api/vouchers")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/vouchers/2")).andExpect(status().isOk());
        mvc.perform(post("/api/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"VOUCHER\",\"name\":\"Voucher\",\"discount_type\":\"percent\",\"discount_value\":10,\"starts_at\":\"2026-01-01T00:00:00\",\"ends_at\":\"2026-12-31T00:00:00\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/vouchers/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"VOUCHER\",\"name\":\"Voucher\",\"discount_type\":\"percent\",\"discount_value\":10,\"starts_at\":\"2026-01-01T00:00:00\",\"ends_at\":\"2026-12-31T00:00:00\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/vouchers/2")).andExpect(status().isNoContent());
        verify(voucherService).deleteVoucher(2);
    }

    @Test
    void voucher_scopesRoutes() throws Exception {
        VoucherScopeResponse res = VoucherScopeResponse.builder().id(2).voucherId(1).build();
        when(voucherService.listScopes()).thenReturn(List.of(res));
        when(voucherService.getScope(2)).thenReturn(res);
        when(voucherService.createScope(any())).thenReturn(res);
        when(voucherService.updateScope(eq(2), any())).thenReturn(res);

        mvc.perform(get("/api/vouchers/scopes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/vouchers/scopes/2")).andExpect(status().isOk());
        mvc.perform(post("/api/vouchers/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voucher_id\":1,\"scope_type\":\"category\",\"scope_id\":1}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/vouchers/scopes/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voucher_id\":1,\"scope_type\":\"category\",\"scope_id\":1}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/vouchers/scopes/2")).andExpect(status().isNoContent());
        verify(voucherService).deleteScope(2);
    }

    @Test
    void voucher_usagesRoutes() throws Exception {
        VoucherUsageResponse res = VoucherUsageResponse.builder().id(2L).voucherId(1).build();
        when(voucherService.listUsages()).thenReturn(List.of(res));
        when(voucherService.getUsage(2L)).thenReturn(res);

        mvc.perform(get("/api/vouchers/usages")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(2));
        mvc.perform(get("/api/vouchers/usages/2")).andExpect(status().isOk());
        mvc.perform(post("/api/vouchers/usages").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void oldGenericRouteIsRemovedAndErrorsAreMapped() throws Exception {
        mvc.perform(get("/api/catalog")).andExpect(status().isNotFound());
        mvc.perform(get("/api/brands/not-an-id")).andExpect(status().isBadRequest());

        when(brandService.create(any())).thenThrow(new CatalogException(400, "Missing name"));
        mvc.perform(post("/api/brands").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Yonex\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Missing name"));

        doThrow(new DataIntegrityViolationException("private SQL")).when(brandService).delete(1);
        mvc.perform(delete("/api/brands/1")).andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("private SQL"))));
    }
}
