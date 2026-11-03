package fu.se.badmintonshop.modules.catalog.controller;

import fu.se.badmintonshop.modules.catalog.dto.request.BrandRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.BrandResponse;
import fu.se.badmintonshop.modules.catalog.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService service;

    @GetMapping("")
    public List<BrandResponse> listBrands() {
        return service.list();
    }

    @GetMapping("/{id}")
    public BrandResponse getBrand(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping("")
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public BrandResponse updateBrand(@PathVariable Integer id, @Valid @RequestBody BrandRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
