package fu.se.badmintonshop.modules.catalog.service;

import fu.se.badmintonshop.modules.catalog.dto.request.CategoryRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.CategoryResponse;
import fu.se.badmintonshop.modules.catalog.entity.Category;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.CategoryRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherScopeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final VoucherScopeRepository voucherScopeRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy danh mục."));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? SlugHelper.toSlug(request.getName())
                : request.getSlug().trim();

        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new CatalogException(400, "Slug danh mục đã tồn tại.");
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CatalogException(400, "Danh mục cha không tồn tại."));
        }

        Category category = Category.builder()
                .parent(parent)
                .name(request.getName().trim())
                .slug(slug)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy danh mục."));

        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? SlugHelper.toSlug(request.getName())
                : request.getSlug().trim();

        if (categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new CatalogException(400, "Slug danh mục đã tồn tại.");
        }

        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new CatalogException(400, "Danh mục cha tạo thành vòng lặp.");
            }
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CatalogException(400, "Danh mục cha không tồn tại."));

            // Check ancestor cycle
            Set<Integer> visited = new HashSet<>();
            visited.add(id);
            Category current = parent;
            while (current != null) {
                if (!visited.add(current.getId())) {
                    throw new CatalogException(400, "Danh mục cha tạo thành vòng lặp.");
                }
                current = current.getParent();
            }
        }

        category.setParent(parent);
        category.setName(request.getName().trim());
        category.setSlug(slug);
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy danh mục."));

        if (voucherScopeRepository.existsByScopeTypeAndScopeId("category", id.longValue())) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (categoryRepository.existsByParentId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }

        categoryRepository.delete(category);
    }

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .name(category.getName())
                .slug(category.getSlug())
                .isActive(category.getIsActive())
                .build();
    }
}
