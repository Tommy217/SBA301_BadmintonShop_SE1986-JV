package fu.se.badmintonshop.modules.catalog.service;

import fu.se.badmintonshop.modules.catalog.dto.request.VoucherRequest;
import fu.se.badmintonshop.modules.catalog.dto.request.VoucherScopeRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherResponse;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherScopeResponse;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherUsageResponse;
import fu.se.badmintonshop.modules.catalog.entity.Voucher;
import fu.se.badmintonshop.modules.catalog.entity.VoucherScope;
import fu.se.badmintonshop.modules.catalog.entity.VoucherUsage;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import fu.se.badmintonshop.modules.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final VoucherScopeRepository voucherScopeRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    private static final Set<String> ALLOWED_DISCOUNT_TYPES = Set.of("percent", "fixed");
    private static final Set<String> ALLOWED_APPLIES_TO = Set.of("all", "category", "brand", "product");
    private static final Set<String> ALLOWED_SCOPE_TYPES = Set.of("category", "brand", "product");

    // ==========================================
    // 1. VOUCHERS
    // ==========================================

    @Transactional(readOnly = true)
    public List<VoucherResponse> listVouchers() {
        return voucherRepository.findAll().stream().map(this::toVoucherResponse).toList();
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucher(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy voucher."));
        return toVoucherResponse(voucher);
    }

    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        String code = request.getCode().trim();
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new CatalogException(400, "Mã voucher đã tồn tại.");
        }

        validateVoucherFields(request, null);

        Voucher voucher = Voucher.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrder(request.getMinOrder() != null ? request.getMinOrder() : BigDecimal.ZERO)
                .maxDiscount(request.getMaxDiscount())
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1)
                .usedCount(0)
                .appliesTo(request.getAppliesTo() != null ? request.getAppliesTo() : "all")
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(request.getCreatedBy())
                .build();

        return toVoucherResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherResponse updateVoucher(Integer id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy voucher."));

        String code = request.getCode().trim();
        if (voucherRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new CatalogException(400, "Mã voucher đã tồn tại.");
        }

        validateVoucherFields(request, voucher);

        // If applies_to changed, verify existing scopes match
        if (request.getAppliesTo() != null && !request.getAppliesTo().equals(voucher.getAppliesTo())) {
            List<VoucherScope> scopes = voucherScopeRepository.findByVoucherId(id);
            for (VoucherScope scope : scopes) {
                if (!scope.getScopeType().equals(request.getAppliesTo())) {
                    throw new CatalogException(400, "Xóa phạm vi cũ trước khi đổi cách áp dụng.");
                }
            }
        }

        voucher.setCode(code);
        voucher.setName(request.getName().trim());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        if (request.getMinOrder() != null) voucher.setMinOrder(request.getMinOrder());
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setUsageLimit(request.getUsageLimit());
        if (request.getPerUserLimit() != null) voucher.setPerUserLimit(request.getPerUserLimit());
        if (request.getAppliesTo() != null) voucher.setAppliesTo(request.getAppliesTo());
        voucher.setStartsAt(request.getStartsAt());
        voucher.setEndsAt(request.getEndsAt());
        if (request.getIsActive() != null) voucher.setIsActive(request.getIsActive());

        return toVoucherResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public void deleteVoucher(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy voucher."));

        // If voucher has scopes, delete them or prevent delete
        List<VoucherScope> scopes = voucherScopeRepository.findByVoucherId(id);
        if (!scopes.isEmpty()) {
            voucherScopeRepository.deleteAll(scopes);
        }

        voucherRepository.delete(voucher);
    }

    private void validateVoucherFields(VoucherRequest request, Voucher existing) {
        if (!ALLOWED_DISCOUNT_TYPES.contains(request.getDiscountType())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: discount_type");
        }
        if (request.getAppliesTo() != null && !ALLOWED_APPLIES_TO.contains(request.getAppliesTo())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: applies_to");
        }
        if (request.getEndsAt() != null && request.getStartsAt() != null && !request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new CatalogException(400, "Ngày kết thúc phải sau ngày bắt đầu.");
        }
        BigDecimal discount = request.getDiscountValue();
        if (discount == null || discount.signum() <= 0 || ("percent".equals(request.getDiscountType()) && discount.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new CatalogException(400, "Mức giảm không hợp lệ.");
        }
        if (request.getPerUserLimit() != null && request.getPerUserLimit() <= 0) {
            throw new CatalogException(400, "Giới hạn mỗi khách phải lớn hơn 0.");
        }
        int used = existing == null ? 0 : existing.getUsedCount();
        if (request.getUsageLimit() != null && request.getUsageLimit() < used) {
            throw new CatalogException(400, "Giới hạn nhỏ hơn lượt đã dùng.");
        }
    }

    // ==========================================
    // 2. VOUCHER SCOPES
    // ==========================================

    @Transactional(readOnly = true)
    public List<VoucherScopeResponse> listScopes() {
        return voucherScopeRepository.findAll().stream().map(this::toScopeResponse).toList();
    }

    @Transactional(readOnly = true)
    public VoucherScopeResponse getScope(Integer id) {
        VoucherScope scope = voucherScopeRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy phạm vi voucher."));
        return toScopeResponse(scope);
    }

    @Transactional
    public VoucherScopeResponse createScope(VoucherScopeRequest request) {
        Voucher voucher = voucherRepository.findById(request.getVoucherId())
                .orElseThrow(() -> new CatalogException(400, "Voucher không tồn tại."));

        if (!ALLOWED_SCOPE_TYPES.contains(request.getScopeType())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: scope_type");
        }
        if (!voucher.getAppliesTo().equals(request.getScopeType())) {
            throw new CatalogException(400, "Loại phạm vi không khớp voucher.");
        }

        validateScopeTargetExists(request.getScopeType(), request.getScopeId());

        if (voucherScopeRepository.existsByVoucherIdAndScopeTypeAndScopeId(
                request.getVoucherId(), request.getScopeType(), request.getScopeId())) {
            throw new CatalogException(400, "Phạm vi voucher này đã tồn tại.");
        }

        VoucherScope scope = VoucherScope.builder()
                .voucher(voucher)
                .scopeType(request.getScopeType())
                .scopeId(request.getScopeId())
                .build();

        return toScopeResponse(voucherScopeRepository.save(scope));
    }

    @Transactional
    public VoucherScopeResponse updateScope(Integer id, VoucherScopeRequest request) {
        VoucherScope scope = voucherScopeRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy phạm vi voucher."));

        Voucher voucher = voucherRepository.findById(request.getVoucherId())
                .orElseThrow(() -> new CatalogException(400, "Voucher không tồn tại."));

        if (!ALLOWED_SCOPE_TYPES.contains(request.getScopeType())) {
            throw new CatalogException(400, "Giá trị không hợp lệ: scope_type");
        }
        if (!voucher.getAppliesTo().equals(request.getScopeType())) {
            throw new CatalogException(400, "Loại phạm vi không khớp voucher.");
        }

        validateScopeTargetExists(request.getScopeType(), request.getScopeId());

        if (voucherScopeRepository.existsByVoucherIdAndScopeTypeAndScopeIdAndIdNot(
                request.getVoucherId(), request.getScopeType(), request.getScopeId(), id)) {
            throw new CatalogException(400, "Phạm vi voucher này đã tồn tại.");
        }

        scope.setVoucher(voucher);
        scope.setScopeType(request.getScopeType());
        scope.setScopeId(request.getScopeId());

        return toScopeResponse(voucherScopeRepository.save(scope));
    }

    @Transactional
    public void deleteScope(Integer id) {
        VoucherScope scope = voucherScopeRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy phạm vi voucher."));
        voucherScopeRepository.delete(scope);
    }

    private void validateScopeTargetExists(String scopeType, Long scopeId) {
        switch (scopeType) {
            case "category" -> {
                if (!categoryRepository.existsById(scopeId.intValue())) {
                    throw new CatalogException(400, "Danh mục không tồn tại.");
                }
            }
            case "brand" -> {
                if (!brandRepository.existsById(scopeId.intValue())) {
                    throw new CatalogException(400, "Thương hiệu không tồn tại.");
                }
            }
            case "product" -> {
                if (!productRepository.existsById(scopeId)) {
                    throw new CatalogException(400, "Sản phẩm không tồn tại.");
                }
            }
            default -> throw new CatalogException(400, "Loại phạm vi không hợp lệ.");
        }
    }

    // ==========================================
    // 3. VOUCHER USAGES (READ-ONLY)
    // ==========================================

    @Transactional(readOnly = true)
    public List<VoucherUsageResponse> listUsages() {
        return voucherUsageRepository.findAll().stream().map(this::toUsageResponse).toList();
    }

    @Transactional(readOnly = true)
    public VoucherUsageResponse getUsage(Long id) {
        VoucherUsage usage = voucherUsageRepository.findById(id)
                .orElseThrow(() -> new CatalogException(404, "Không tìm thấy lượt sử dụng voucher."));
        return toUsageResponse(usage);
    }

    // ==========================================
    // MAPPERS
    // ==========================================

    public VoucherResponse toVoucherResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrder(voucher.getMinOrder())
                .maxDiscount(voucher.getMaxDiscount())
                .usageLimit(voucher.getUsageLimit())
                .perUserLimit(voucher.getPerUserLimit())
                .usedCount(voucher.getUsedCount())
                .appliesTo(voucher.getAppliesTo())
                .startsAt(voucher.getStartsAt())
                .endsAt(voucher.getEndsAt())
                .isActive(voucher.getIsActive())
                .createdBy(voucher.getCreatedBy())
                .build();
    }

    public VoucherScopeResponse toScopeResponse(VoucherScope scope) {
        return VoucherScopeResponse.builder()
                .id(scope.getId())
                .voucherId(scope.getVoucher() != null ? scope.getVoucher().getId() : null)
                .scopeType(scope.getScopeType())
                .scopeId(scope.getScopeId())
                .build();
    }

    public VoucherUsageResponse toUsageResponse(VoucherUsage usage) {
        return VoucherUsageResponse.builder()
                .id(usage.getId())
                .voucherId(usage.getVoucherId())
                .userId(usage.getUserId())
                .orderId(usage.getOrderId())
                .discountAmount(usage.getDiscountAmount())
                .usedAt(usage.getUsedAt())
                .build();
    }
}
