package fu.se.badmintonshop.modules.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One HTTP request and one persistence context for the catalog needed by React. */
@Service
@RequiredArgsConstructor
public class CatalogSnapshotService {
    private final BrandService brands;
    private final CategoryService categories;
    private final ProductService products;
    private final VoucherService vouchers;

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(boolean management) {
        var result = new LinkedHashMap<String, Object>();
        result.put("brands", brands.list());
        result.put("categories", categories.list());
        result.put("products", products.listProducts());
        result.put("product_variants", products.listVariants());
        result.put("product_images", products.listImages());
        result.put("racket_specs", products.listRacketSpecs());
        result.put("shoe_specs", products.listShoeSpecs());
        result.put("apparel_specs", products.listApparelSpecs());
        result.put("vouchers", vouchers.listVouchers());
        result.put("voucher_scopes", vouchers.listScopes());
        result.put("product_price_history", management ? products.listPriceHistory() : List.of());
        result.put("voucher_usages", management ? vouchers.listUsages() : List.of());
        return result;
    }
}
