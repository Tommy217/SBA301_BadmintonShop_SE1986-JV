package fu.se.badmintonshop.modules.catalog.controller;

import fu.se.badmintonshop.modules.catalog.dto.request.CategoryRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.CategoryResponse;
import fu.se.badmintonshop.modules.catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping("")
    public List<CategoryResponse> listCategories() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping("")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Integer id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
