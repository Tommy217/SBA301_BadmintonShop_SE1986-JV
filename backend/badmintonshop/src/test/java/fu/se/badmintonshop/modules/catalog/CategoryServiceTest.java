package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.dto.request.CategoryRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.CategoryResponse;
import fu.se.badmintonshop.modules.catalog.entity.Category;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.CategoryRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherScopeRepository;
import fu.se.badmintonshop.modules.catalog.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {
    CategoryRepository categoryRepository;
    ProductRepository productRepository;
    VoucherScopeRepository voucherScopeRepository;
    CategoryService service;

    @BeforeEach
    void setup() {
        categoryRepository = mock(CategoryRepository.class);
        productRepository = mock(ProductRepository.class);
        voucherScopeRepository = mock(VoucherScopeRepository.class);
        service = new CategoryService(categoryRepository, productRepository, voucherScopeRepository);
    }

    @Test
    void detectsCategoryCycles() {
        Category cat1 = Category.builder().id(1).name("Cat 1").slug("cat-1").build();
        Category cat2 = Category.builder().id(2).name("Cat 2").slug("cat-2").parent(cat1).build();

        when(categoryRepository.findById(1)).thenReturn(Optional.of(cat1));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(cat2));

        CategoryRequest request = CategoryRequest.builder()
                .name("Cat 1 update")
                .slug("cat-1")
                .parentId(2)
                .isActive(true)
                .build();

        assertThrows(CatalogException.class, () -> service.update(1, request));
    }

    @Test
    void createsCategorySuccessfully() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Vợt cầu lông")
                .slug("vot-cau-long")
                .isActive(true)
                .build();

        Category saved = Category.builder()
                .id(1)
                .name("Vợt cầu lông")
                .slug("vot-cau-long")
                .isActive(true)
                .build();

        when(categoryRepository.existsBySlugIgnoreCase("vot-cau-long")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = service.create(request);
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Vợt cầu lông", response.getName());
    }

    @Test
    void rejectsDeleteWhenHasProducts() {
        Category cat = Category.builder().id(1).name("Vợt").slug("vot").build();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(cat));
        when(productRepository.existsByCategoryId(1)).thenReturn(true);

        assertThrows(CatalogException.class, () -> service.delete(1));
        verify(categoryRepository, never()).delete(any());
    }
}
