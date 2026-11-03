package fu.se.badmintonshop.modules.catalog.service;

import fu.se.badmintonshop.modules.catalog.dto.request.BrandRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.BrandResponse;
import fu.se.badmintonshop.modules.catalog.entity.Brand;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.BrandRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherScopeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final VoucherScopeRepository voucherScopeRepository;

    private static final String URL_REGEX = "(?i)^(https?://[^\\s]+|/(?!/)[^\\s]*)$";

    @Transactional(readOnly = true)
    public List<BrandResponse> list() {
        return brandRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse get(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thương hiệu."));
        return toResponse(brand);
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? SlugHelper.toSlug(request.getName())
                : request.getSlug().trim();

        if (brandRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new CatalogException(400, "Tên thương hiệu đã tồn tại.");
        }
        if (brandRepository.existsBySlugIgnoreCase(slug)) {
            throw new CatalogException(400, "Slug thương hiệu đã tồn tại.");
        }
        validateLogoUrl(request.getLogoUrl());

        Brand brand = Brand.builder()
                .name(request.getName().trim())
                .slug(slug)
                .logoUrl(request.getLogoUrl() != null && !request.getLogoUrl().isBlank() ? request.getLogoUrl().trim() : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse update(Integer id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thương hiệu."));

        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? SlugHelper.toSlug(request.getName())
                : request.getSlug().trim();

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new CatalogException(400, "Tên thương hiệu đã tồn tại.");
        }
        if (brandRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new CatalogException(400, "Slug thương hiệu đã tồn tại.");
        }
        validateLogoUrl(request.getLogoUrl());

        brand.setName(request.getName().trim());
        brand.setSlug(slug);
        brand.setLogoUrl(request.getLogoUrl() != null && !request.getLogoUrl().isBlank() ? request.getLogoUrl().trim() : null);
        if (request.getIsActive() != null) {
            brand.setIsActive(request.getIsActive());
        }

        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void delete(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy thương hiệu."));

        if (voucherScopeRepository.existsByScopeTypeAndScopeId("brand", id.longValue())) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }
        if (productRepository.existsByBrandId(id)) {
            throw new CatalogException(400, "Record is in use; deactivate it or remove dependent records first.");
        }

        brandRepository.delete(brand);
    }

    private void validateLogoUrl(String url) {
        if (url != null && !url.isBlank() && !url.matches(URL_REGEX)) {
            throw new CatalogException(400, "URL ảnh không hợp lệ.");
        }
    }

    public BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .isActive(brand.getIsActive())
                .build();
    }
}
