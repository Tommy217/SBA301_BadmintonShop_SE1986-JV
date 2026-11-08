package fu.se.badmintonshop.modules.catalog.service;

import fu.se.badmintonshop.modules.catalog.dto.request.*;
import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.*;
import fu.se.badmintonshop.modules.inventory.entity.InventoryMovement;
import fu.se.badmintonshop.modules.inventory.repository.InventoryMovementRepository;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final RacketSpecRepository racketSpecRepository;
    private final ShoeSpecRepository shoeSpecRepository;
    private final ApparelSpecRepository apparelSpecRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final VoucherScopeRepository voucherScopeRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    private static final Set<String> ALLOWED_PRODUCT_TYPES = Set.of("racket", "shoes", "apparel");
    private static final Set<String> ALLOWED_BALANCE_TYPES = Set.of("head_heavy", "even", "head_light");
    private static final Set<String> ALLOWED_FLEX = Set.of("stiff", "medium", "flexible");
    private static final Set<String> ALLOWED_PLAY_STYLES = Set.of("attack", "defense", "all_round");
    private static final Set<String> ALLOWED_SHOE_GENDERS = Set.of("male", "female", "unisex");
    private static final Set<String> ALLOWED_FOOT_WIDTHS = Set.of("narrow", "regular", "wide");
    private static final Set<String> ALLOWED_APPAREL_GENDERS = Set.of("male", "female", "unisex", "kids");
    private static final Set<String> ALLOWED_APPAREL_KINDS = Set.of("shirt", "shorts", "skirt", "dress", "jacket", "socks", "other");

    // ==========================================
    // 1. PRODUCTS
    // ==========================================

    @Transactional(readOnly = true)
    public Map<String, Object> pageProducts(int page, String search, String type) {
        return pageProducts(page, search, type, null, null, "default");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> pageProducts(int page, String search, String type,
                                          BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        if (page < 0) throw new CatalogException(400, "Trang phải lớn hơn hoặc bằng 0.");
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0))
            throw new CatalogException(400, "Giá phải lớn hơn hoặc bằng 0.");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw new CatalogException(400, "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu.");
        if (sort == null || !Set.of("default", "price_asc", "price_desc", "name_asc", "name_desc").contains(sort))
            throw new CatalogException(400, "Thứ tự sắp xếp không hợp lệ.");
        if (type != null && !type.isBlank() && !ALLOWED_PRODUCT_TYPES.contains(type))
            throw new CatalogException(400, "Loại sản phẩm không hợp lệ.");
        Set<String> demoSlugs = Set.of("yonex-astrox-88d-play-1", "victor-thruster-k-220h-2", "li-ning-windstorm-72-3", "yonex-power-cushion-39-4", "victor-a170-ii-5", "ao-cau-long-training-essential-6", "demo-product-racket", "demo-product-shoes", "demo-product-apparel");
        Specification<Product> filter = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.isTrue(root.join("brand").get("isActive")));
            predicates.add(cb.isTrue(root.join("category").get("isActive")));
            predicates.add(cb.not(root.get("slug").in(demoSlugs)));
            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + term + "%", '!'));
            }
            if (type != null && !type.isBlank()) predicates.add(cb.equal(root.get("productType"), type));
            // Match the card price: cheapest in-stock active variant, then any active variant,
            // then base price when there are no active variants. Filter before pagination.
            jakarta.persistence.criteria.Expression<BigDecimal> price = root.get("basePrice");
            if (minPrice != null || maxPrice != null || sort.startsWith("price_")) {
                var inStock = query.subquery(BigDecimal.class);
                var availableVariant = inStock.from(ProductVariant.class);
                inStock.select(cb.min(availableVariant.<BigDecimal>get("price"))).where(
                        cb.equal(availableVariant.get("productId"), root.get("id")),
                        cb.isTrue(availableVariant.get("isActive")),
                        cb.greaterThan(availableVariant.get("stockQuantity"), 0));
                var active = query.subquery(BigDecimal.class);
                var variant = active.from(ProductVariant.class);
                active.select(cb.min(variant.<BigDecimal>get("price"))).where(
                        cb.equal(variant.get("productId"), root.get("id")),
                        cb.isTrue(variant.get("isActive")));
                price = cb.<BigDecimal>coalesce().value(inStock).value(active).value(root.get("basePrice"));
            }
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(price, minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(price, maxPrice));
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var order = switch (sort) {
                    case "price_asc" -> cb.asc(price);
                    case "price_desc" -> cb.desc(price);
                    case "name_asc" -> cb.asc(cb.lower(root.get("name")));
                    case "name_desc" -> cb.desc(cb.lower(root.get("name")));
                    default -> cb.asc(root.get("id"));
                };
                if (sort.equals("default")) query.orderBy(order);
                else query.orderBy(order, cb.asc(root.get("id")));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = productRepository.findAll(filter, PageRequest.of(page, 8));
        return Map.of("content", result.getContent().stream().map(this::toProductResponse).toList(),
                "number", result.getNumber(), "size", result.getSize(),
                "totalElements", result.getTotalElements(), "totalPages", result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return productRepository.findAll().stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy sản phẩm."));
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (!ALLOWED_PRODUCT_TYPES.contains(request.getProductType())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: product_type");
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CatalogException(400, "Danh mục không tồn tại."));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new CatalogException(400, "Thương hiệu không tồn tại."));

        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? SlugHelper.toSlug(request.getName())
                : request.getSlug().trim();

        if (productRepository.existsBySlugIgnoreCase(slug)) {
            throw new CatalogException(400, "Slug sản phẩm đã tồn tại.");
        }

        Product product = Product.builder()
                .category(category)
                .brand(brand)
                .productType(request.getProductType())
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(request.getCreatedBy())
                .build();

        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy sản phẩm."));

        if (!ALLOWED_PRODUCT_TYPES.contains(request.getProductType())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: product_type");
        }

        // If product_type changed, make sure old specs are deleted first
        if (!product.getProductType().equals(request.getProductType())) {
            boolean hasOldSpec = racketSpecRepository.existsById(id)
                    || shoeSpecRepository.existsById(id)
                    || apparelSpecRepository.existsById(id);
            if (hasOldSpec) {
                throw new CatalogException(400, "Xóa thông số cũ trước khi đổi loại sản phẩm.");
            }
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CatalogException(400, "Danh mục không tồn tại."));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new CatalogException(400, "Thương hiệu không tồn tại."));

        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? SlugHelper.toSlug(request.getName())
                : request.getSlug().trim();

        if (productRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new CatalogException(400, "Slug sản phẩm đã tồn tại.");
        }

        product.setCategory(category);
        product.setBrand(brand);
        product.setProductType(request.getProductType());
        product.setName(request.getName().trim());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy sản phẩm."));

        if (voucherScopeRepository.existsByScopeTypeAndScopeId("product", id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (productVariantRepository.existsByProductId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (productImageRepository.existsByProductId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (orderItemRepository.existsByProductId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }

        // Delete specs if exist
        if (racketSpecRepository.existsById(id)) racketSpecRepository.deleteById(id);
        if (shoeSpecRepository.existsById(id)) shoeSpecRepository.deleteById(id);
        if (apparelSpecRepository.existsById(id)) apparelSpecRepository.deleteById(id);

        productRepository.delete(product);
    }

    // ==========================================
    // 2. PRODUCT VARIANTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> listVariants() {
        return productVariantRepository.findAll().stream().map(this::toVariantResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getVariant(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy biến thể sản phẩm."));
        return toVariantResponse(variant);
    }

    @Transactional
    public ProductVariantResponse createVariant(ProductVariantRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CatalogException(400, "Sản phẩm không tồn tại."));

        String sku = request.getSku().trim();
        if (productVariantRepository.existsBySkuIgnoreCase(sku)) {
            throw new CatalogException(400, "Mã SKU đã tồn tại.");
        }

        String barcode = request.getBarcode() != null && !request.getBarcode().isBlank() ? request.getBarcode().trim() : null;
        if (barcode != null && productVariantRepository.existsByBarcodeIgnoreCase(barcode)) {
            throw new CatalogException(400, "Mã vạch đã tồn tại.");
        }

        String color = request.getColor() != null && !request.getColor().isBlank() ? request.getColor().trim() : null;
        String size = request.getSize() != null && !request.getSize().isBlank() ? request.getSize().trim() : null;
        if (productVariantRepository.existsByProductIdAndColorAndSize(request.getProductId(), color, size)) {
            throw new CatalogException(400, "Biến thể với màu và kích cỡ này đã tồn tại.");
        }

        int stock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        if (stock < 0) {
            throw new CatalogException(400, "Giá trị số không hợp lệ: stock_quantity");
        }

        ProductVariant variant = ProductVariant.builder()
                .productId(product.getId())
                .product(product)
                .sku(sku)
                .barcode(barcode)
                .color(color)
                .size(size)
                .price(request.getPrice())
                .costPrice(request.getCostPrice() != null ? request.getCostPrice() : BigDecimal.ZERO)
                .stockQuantity(stock)
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 5)
                .weightGrams(request.getWeightGrams() != null ? request.getWeightGrams() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        ProductVariant saved = productVariantRepository.save(variant);

        // Record initial stock movement if stock > 0
        if (stock > 0) {
            InventoryMovement movement = InventoryMovement.builder()
                    .variant(saved)
                    .movementType("manual_adjust")
                    .quantity(stock)
                    .balanceAfter(stock)
                    .refType("manual")
                    .refId(saved.getId())
                    .note("Tồn đầu khi tạo biến thể Catalog")
                    .build();
            inventoryMovementRepository.save(movement);
        }

        return toVariantResponse(saved);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long id, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy biến thể sản phẩm."));

        if (request.getProductId() != null && !request.getProductId().equals(variant.getProductId())) {
            throw new CatalogException(400, "Không thể chuyển biến thể sang sản phẩm khác.");
        }

        if (request.getStockQuantity() != null && !request.getStockQuantity().equals(variant.getStockQuantity())) {
            throw new CatalogException(400, "Điều chỉnh tồn kho tại nghiệp vụ kho, không qua Catalog.");
        }

        String sku = request.getSku().trim();
        if (productVariantRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new CatalogException(400, "Mã SKU đã tồn tại.");
        }

        String barcode = request.getBarcode() != null && !request.getBarcode().isBlank() ? request.getBarcode().trim() : null;
        if (barcode != null && productVariantRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode, id)) {
            throw new CatalogException(400, "Mã vạch đã tồn tại.");
        }

        String color = request.getColor() != null && !request.getColor().isBlank() ? request.getColor().trim() : null;
        String size = request.getSize() != null && !request.getSize().isBlank() ? request.getSize().trim() : null;
        if (productVariantRepository.existsByProductIdAndColorAndSizeAndIdNot(variant.getProductId(), color, size, id)) {
            throw new CatalogException(400, "Biến thể với màu và kích cỡ này đã tồn tại.");
        }

        // Check if price changed -> record in ProductPriceHistory
        if (request.getPrice() != null && variant.getPrice().compareTo(request.getPrice()) != 0) {
            ProductPriceHistory history = ProductPriceHistory.builder()
                    .variant(variant)
                    .oldPrice(variant.getPrice())
                    .newPrice(request.getPrice())
                    .build();
            productPriceHistoryRepository.save(history);
            variant.setPrice(request.getPrice());
        }

        variant.setSku(sku);
        variant.setBarcode(barcode);
        variant.setColor(color);
        variant.setSize(size);
        if (request.getCostPrice() != null) variant.setCostPrice(request.getCostPrice());
        if (request.getLowStockThreshold() != null) variant.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getWeightGrams() != null) variant.setWeightGrams(request.getWeightGrams());
        if (request.getIsActive() != null) variant.setIsActive(request.getIsActive());

        return toVariantResponse(productVariantRepository.save(variant));
    }

    @Transactional
    public void deleteVariant(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy biến thể sản phẩm."));

        if (variant.getStockQuantity() > 0) {
            throw new CatalogException(400, "Cannot delete stock on hand.");
        }
        if (orderItemRepository.existsByVariantId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (productImageRepository.existsByVariantId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }

        productVariantRepository.delete(variant);
    }

    // ==========================================
    // 3. PRODUCT IMAGES
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProductImageResponse> listImages() {
        return productImageRepository.findAll().stream().map(this::toImageResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductImageResponse getImage(Long id) {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy hình ảnh sản phẩm."));
        return toImageResponse(image);
    }

    @Transactional
    public ProductImageResponse createImage(ProductImageRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CatalogException(400, "Sản phẩm không tồn tại."));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new CatalogException(400, "Biến thể không tồn tại."));
            if (!variant.getProductId().equals(product.getId())) {
                throw new CatalogException(400, "Biến thể không thuộc sản phẩm đã chọn.");
            }
        }

        validateUrl(request.getUrl());

        ProductImage image = ProductImage.builder()
                .product(product)
                .variant(variant)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        image.setUrl(request.getUrl().trim());
        return toImageResponse(productImageRepository.save(image));
    }

    @Transactional
    public ProductImageResponse updateImage(Long id, ProductImageRequest request) {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy hình ảnh sản phẩm."));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CatalogException(400, "Sản phẩm không tồn tại."));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new CatalogException(400, "Biến thể không tồn tại."));
            if (!variant.getProductId().equals(product.getId())) {
                throw new CatalogException(400, "Biến thể không thuộc sản phẩm đã chọn.");
            }
        }

        validateUrl(request.getUrl());

        image.setProduct(product);
        image.setVariant(variant);
        image.setUrl(request.getUrl().trim());
        if (request.getSortOrder() != null) {
            image.setSortOrder(request.getSortOrder());
        }

        return toImageResponse(productImageRepository.save(image));
    }

    @Transactional
    public void deleteImage(Long id) {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy hình ảnh sản phẩm."));
        productImageRepository.delete(image);
    }

    // ==========================================
    // 4. RACKET SPECS
    // ==========================================

    @Transactional(readOnly = true)
    public List<RacketSpecResponse> listRacketSpecs() {
        return racketSpecRepository.findAll().stream().map(this::toRacketSpecResponse).toList();
    }

    @Transactional(readOnly = true)
    public RacketSpecResponse getRacketSpec(Long productId) {
        RacketSpec spec = racketSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số vợt."));
        return toRacketSpecResponse(spec);
    }

    @Transactional
    public RacketSpecResponse createRacketSpec(RacketSpecRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CatalogException(400, "Sản phẩm không tồn tại."));

        if (!"racket".equals(product.getProductType())) {
            throw new CatalogException(400, "Thông số không đúng loại sản phẩm.");
        }
        if (racketSpecRepository.existsById(product.getId())) {
            throw new CatalogException(400, "Sản phẩm đã có thông số.");
        }

        validateRacketEnums(request);

        RacketSpec spec = RacketSpec.builder()
                .product(product)
                .productId(product.getId())
                .balanceType(request.getBalanceType())
                .flex(request.getFlex())
                .playStyle(request.getPlayStyle())
                .maxTensionLbs(request.getMaxTensionLbs())
                .material(request.getMaterial())
                .shaftDiameter(request.getShaftDiameter())
                .build();

        return toRacketSpecResponse(racketSpecRepository.save(spec));
    }

    @Transactional
    public RacketSpecResponse updateRacketSpec(Long productId, RacketSpecRequest request) {
        RacketSpec spec = racketSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số vợt."));

        if (request.getProductId() != null && !request.getProductId().equals(productId)) {
            throw new CatalogException(400, "Không thể đổi sản phẩm của bộ thông số.");
        }

        validateRacketEnums(request);

        spec.setBalanceType(request.getBalanceType());
        spec.setFlex(request.getFlex());
        spec.setPlayStyle(request.getPlayStyle());
        spec.setMaxTensionLbs(request.getMaxTensionLbs());
        spec.setMaterial(request.getMaterial());
        spec.setShaftDiameter(request.getShaftDiameter());

        return toRacketSpecResponse(racketSpecRepository.save(spec));
    }

    @Transactional
    public void deleteRacketSpec(Long productId) {
        RacketSpec spec = racketSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số vợt."));
        racketSpecRepository.delete(spec);
    }

    private void validateRacketEnums(RacketSpecRequest request) {
        if (!ALLOWED_BALANCE_TYPES.contains(request.getBalanceType())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: balance_type");
        }
        if (!ALLOWED_FLEX.contains(request.getFlex())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: flex");
        }
        if (!ALLOWED_PLAY_STYLES.contains(request.getPlayStyle())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: play_style");
        }
    }

    // ==========================================
    // 5. SHOE SPECS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ShoeSpecResponse> listShoeSpecs() {
        return shoeSpecRepository.findAll().stream().map(this::toShoeSpecResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShoeSpecResponse getShoeSpec(Long productId) {
        ShoeSpec spec = shoeSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số giày."));
        return toShoeSpecResponse(spec);
    }

    @Transactional
    public ShoeSpecResponse createShoeSpec(ShoeSpecRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CatalogException(400, "Sản phẩm không tồn tại."));

        if (!"shoes".equals(product.getProductType())) {
            throw new CatalogException(400, "Thông số không đúng loại sản phẩm.");
        }
        if (shoeSpecRepository.existsById(product.getId())) {
            throw new CatalogException(400, "Sản phẩm đã có thông số.");
        }

        validateShoeEnums(request);

        ShoeSpec spec = ShoeSpec.builder()
                .product(product)
                .productId(product.getId())
                .gender(request.getGender())
                .soleType(request.getSoleType())
                .technology(request.getTechnology())
                .footWidth(request.getFootWidth() != null ? request.getFootWidth() : "regular")
                .build();

        return toShoeSpecResponse(shoeSpecRepository.save(spec));
    }

    @Transactional
    public ShoeSpecResponse updateShoeSpec(Long productId, ShoeSpecRequest request) {
        ShoeSpec spec = shoeSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số giày."));

        if (request.getProductId() != null && !request.getProductId().equals(productId)) {
            throw new CatalogException(400, "Không thể đổi sản phẩm của bộ thông số.");
        }

        validateShoeEnums(request);

        spec.setGender(request.getGender());
        spec.setSoleType(request.getSoleType());
        spec.setTechnology(request.getTechnology());
        if (request.getFootWidth() != null) {
            spec.setFootWidth(request.getFootWidth());
        }

        return toShoeSpecResponse(shoeSpecRepository.save(spec));
    }

    @Transactional
    public void deleteShoeSpec(Long productId) {
        ShoeSpec spec = shoeSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số giày."));
        shoeSpecRepository.delete(spec);
    }

    private void validateShoeEnums(ShoeSpecRequest request) {
        if (!ALLOWED_SHOE_GENDERS.contains(request.getGender())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: gender");
        }
        if (request.getFootWidth() != null && !ALLOWED_FOOT_WIDTHS.contains(request.getFootWidth())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: foot_width");
        }
    }

    // ==========================================
    // 6. APPAREL SPECS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ApparelSpecResponse> listApparelSpecs() {
        return apparelSpecRepository.findAll().stream().map(this::toApparelSpecResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApparelSpecResponse getApparelSpec(Long productId) {
        ApparelSpec spec = apparelSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số trang phục."));
        return toApparelSpecResponse(spec);
    }

    @Transactional
    public ApparelSpecResponse createApparelSpec(ApparelSpecRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CatalogException(400, "Sản phẩm không tồn tại."));

        if (!"apparel".equals(product.getProductType())) {
            throw new CatalogException(400, "Thông số không đúng loại sản phẩm.");
        }
        if (apparelSpecRepository.existsById(product.getId())) {
            throw new CatalogException(400, "Sản phẩm đã có thông số.");
        }

        validateApparelEnums(request);

        ApparelSpec spec = ApparelSpec.builder()
                .product(product)
                .productId(product.getId())
                .gender(request.getGender())
                .apparelKind(request.getApparelKind())
                .material(request.getMaterial())
                .build();

        return toApparelSpecResponse(apparelSpecRepository.save(spec));
    }

    @Transactional
    public ApparelSpecResponse updateApparelSpec(Long productId, ApparelSpecRequest request) {
        ApparelSpec spec = apparelSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số trang phục."));

        if (request.getProductId() != null && !request.getProductId().equals(productId)) {
            throw new CatalogException(400, "Không thể đổi sản phẩm của bộ thông số.");
        }

        validateApparelEnums(request);

        spec.setGender(request.getGender());
        spec.setApparelKind(request.getApparelKind());
        spec.setMaterial(request.getMaterial());

        return toApparelSpecResponse(apparelSpecRepository.save(spec));
    }

    @Transactional
    public void deleteApparelSpec(Long productId) {
        ApparelSpec spec = apparelSpecRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thông số trang phục."));
        apparelSpecRepository.delete(spec);
    }

    private void validateApparelEnums(ApparelSpecRequest request) {
        if (!ALLOWED_APPAREL_GENDERS.contains(request.getGender())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: gender");
        }
        if (!ALLOWED_APPAREL_KINDS.contains(request.getApparelKind())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: apparel_kind");
        }
    }

    // ==========================================
    // 7. PRICE HISTORY (READ-ONLY)
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProductPriceHistoryResponse> listPriceHistory() {
        return productPriceHistoryRepository.findAll().stream().map(this::toPriceHistoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductPriceHistoryResponse getPriceHistory(Long id) {
        ProductPriceHistory history = productPriceHistoryRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy lịch sử giá."));
        return toPriceHistoryResponse(history);
    }

    // ==========================================
    // MAPPERS & UTILS
    // ==========================================

    private void validateUrl(String url) {
        fu.se.badmintonshop.modules.catalog.entity.ImageBytesConverter.validate(url);
    }

    public ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brand(product.getBrand() != null ? product.getBrand().getName() : null)
                .productType(product.getProductType())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .images(product.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(ProductImage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toImageResponse).toList())
                .basePrice(product.getBasePrice())
                .isActive(product.getIsActive())
                .createdBy(product.getCreatedBy())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductVariantResponse toVariantResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProductId())
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .costPrice(variant.getCostPrice())
                .stockQuantity(variant.getStockQuantity())
                .lowStockThreshold(variant.getLowStockThreshold())
                .weightGrams(variant.getWeightGrams())
                .isActive(variant.getIsActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    public ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .productId(image.getProduct() != null ? image.getProduct().getId() : null)
                .variantId(image.getVariant() != null ? image.getVariant().getId() : null)
                .url(image.getUrl())
                .sortOrder(image.getSortOrder())
                .build();
    }

    public RacketSpecResponse toRacketSpecResponse(RacketSpec spec) {
        return RacketSpecResponse.builder()
                .productId(spec.getProductId())
                .balanceType(spec.getBalanceType())
                .flex(spec.getFlex())
                .playStyle(spec.getPlayStyle())
                .maxTensionLbs(spec.getMaxTensionLbs())
                .material(spec.getMaterial())
                .shaftDiameter(spec.getShaftDiameter())
                .build();
    }

    public ShoeSpecResponse toShoeSpecResponse(ShoeSpec spec) {
        return ShoeSpecResponse.builder()
                .productId(spec.getProductId())
                .gender(spec.getGender())
                .soleType(spec.getSoleType())
                .technology(spec.getTechnology())
                .footWidth(spec.getFootWidth())
                .build();
    }

    public ApparelSpecResponse toApparelSpecResponse(ApparelSpec spec) {
        return ApparelSpecResponse.builder()
                .productId(spec.getProductId())
                .gender(spec.getGender())
                .apparelKind(spec.getApparelKind())
                .material(spec.getMaterial())
                .build();
    }

    public ProductPriceHistoryResponse toPriceHistoryResponse(ProductPriceHistory history) {
        return ProductPriceHistoryResponse.builder()
                .id(history.getId())
                .variantId(history.getVariant() != null ? history.getVariant().getId() : null)
                .oldPrice(history.getOldPrice())
                .newPrice(history.getNewPrice())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }
}
