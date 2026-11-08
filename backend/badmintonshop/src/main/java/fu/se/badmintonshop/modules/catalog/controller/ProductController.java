package fu.se.badmintonshop.modules.catalog.controller;

import fu.se.badmintonshop.modules.catalog.dto.request.*;
import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    // --- Products ---

    @GetMapping("")
    public List<ProductResponse> listProducts() {
        return service.listProducts();
    }

    @GetMapping("/page")
    public Map<String, Object> pageProducts(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "") String search,
                                          @RequestParam(defaultValue = "") String type,
                                          @RequestParam(required = false) java.math.BigDecimal minPrice,
                                          @RequestParam(required = false) java.math.BigDecimal maxPrice,
                                          @RequestParam(defaultValue = "default") String sort) {
        return service.pageProducts(page, search, type, minPrice, maxPrice, sort);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return service.getProduct(id);
    }

    @PostMapping("")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProduct(request));
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // --- Product Variants ---

    @GetMapping("/variants")
    public List<ProductVariantResponse> listVariants() {
        return service.listVariants();
    }

    @GetMapping("/variants/{id}")
    public ProductVariantResponse getVariant(@PathVariable Long id) {
        return service.getVariant(id);
    }

    @PostMapping("/variants")
    public ResponseEntity<ProductVariantResponse> createVariant(@Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVariant(request));
    }

    @PutMapping("/variants/{id}")
    public ProductVariantResponse updateVariant(@PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {
        return service.updateVariant(id, request);
    }

    @DeleteMapping("/variants/{id}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        service.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }

    // --- Product Images ---

    @GetMapping("/images")
    public List<ProductImageResponse> listImages() {
        return service.listImages();
    }

    @GetMapping("/images/{id}")
    public ProductImageResponse getImage(@PathVariable Long id) {
        return service.getImage(id);
    }

    @PostMapping("/images")
    public ResponseEntity<ProductImageResponse> createImage(@Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createImage(request));
    }

    @PutMapping("/images/{id}")
    public ProductImageResponse updateImage(@PathVariable Long id, @Valid @RequestBody ProductImageRequest request) {
        return service.updateImage(id, request);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        service.deleteImage(id);
        return ResponseEntity.noContent().build();
    }

    // --- Racket Specs ---

    @GetMapping("/racket-specs")
    public List<RacketSpecResponse> listRacketSpecs() {
        return service.listRacketSpecs();
    }

    @GetMapping("/racket-specs/{id}")
    public RacketSpecResponse getRacketSpec(@PathVariable Long id) {
        return service.getRacketSpec(id);
    }

    @PostMapping("/racket-specs")
    public ResponseEntity<RacketSpecResponse> createRacketSpec(@Valid @RequestBody RacketSpecRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRacketSpec(request));
    }

    @PutMapping("/racket-specs/{id}")
    public RacketSpecResponse updateRacketSpec(@PathVariable Long id, @Valid @RequestBody RacketSpecRequest request) {
        return service.updateRacketSpec(id, request);
    }

    @DeleteMapping("/racket-specs/{id}")
    public ResponseEntity<Void> deleteRacketSpec(@PathVariable Long id) {
        service.deleteRacketSpec(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shoe Specs ---

    @GetMapping("/shoe-specs")
    public List<ShoeSpecResponse> listShoeSpecs() {
        return service.listShoeSpecs();
    }

    @GetMapping("/shoe-specs/{id}")
    public ShoeSpecResponse getShoeSpec(@PathVariable Long id) {
        return service.getShoeSpec(id);
    }

    @PostMapping("/shoe-specs")
    public ResponseEntity<ShoeSpecResponse> createShoeSpec(@Valid @RequestBody ShoeSpecRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createShoeSpec(request));
    }

    @PutMapping("/shoe-specs/{id}")
    public ShoeSpecResponse updateShoeSpec(@PathVariable Long id, @Valid @RequestBody ShoeSpecRequest request) {
        return service.updateShoeSpec(id, request);
    }

    @DeleteMapping("/shoe-specs/{id}")
    public ResponseEntity<Void> deleteShoeSpec(@PathVariable Long id) {
        service.deleteShoeSpec(id);
        return ResponseEntity.noContent().build();
    }

    // --- Apparel Specs ---

    @GetMapping("/apparel-specs")
    public List<ApparelSpecResponse> listApparelSpecs() {
        return service.listApparelSpecs();
    }

    @GetMapping("/apparel-specs/{id}")
    public ApparelSpecResponse getApparelSpec(@PathVariable Long id) {
        return service.getApparelSpec(id);
    }

    @PostMapping("/apparel-specs")
    public ResponseEntity<ApparelSpecResponse> createApparelSpec(@Valid @RequestBody ApparelSpecRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createApparelSpec(request));
    }

    @PutMapping("/apparel-specs/{id}")
    public ApparelSpecResponse updateApparelSpec(@PathVariable Long id, @Valid @RequestBody ApparelSpecRequest request) {
        return service.updateApparelSpec(id, request);
    }

    @DeleteMapping("/apparel-specs/{id}")
    public ResponseEntity<Void> deleteApparelSpec(@PathVariable Long id) {
        service.deleteApparelSpec(id);
        return ResponseEntity.noContent().build();
    }

    // --- Product Price History (Read-only) ---

    @GetMapping("/price-history")
    public List<ProductPriceHistoryResponse> listPriceHistory() {
        return service.listPriceHistory();
    }

    @GetMapping("/price-history/{id}")
    public ProductPriceHistoryResponse getPriceHistory(@PathVariable Long id) {
        return service.getPriceHistory(id);
    }
}
