package fu.se.badmintonshop.modules.inventory.service;

import fu.se.badmintonshop.modules.inventory.dto.request.SupplierRequest;
import fu.se.badmintonshop.modules.inventory.dto.response.SupplierResponse;
import fu.se.badmintonshop.modules.inventory.entity.Supplier;
import fu.se.badmintonshop.modules.inventory.exception.InventoryException;
import fu.se.badmintonshop.modules.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {
    private final SupplierRepository suppliers;
    private final GoodsReceiptRepository receipts;

    public List<SupplierResponse> list() {
        return suppliers.findAll().stream().map(SupplierResponse::from).toList();
    }
    public SupplierResponse get(Integer id) { return SupplierResponse.from(find(id)); }

    @Transactional
    public SupplierResponse create(SupplierRequest request) { return save(new Supplier(), request); }

    @Transactional
    public SupplierResponse update(Integer id, SupplierRequest request) { return save(find(id), request); }

    @Transactional
    public void delete(Integer id) {
        var supplier = find(id);
        if (receipts.existsBySupplierId(id)) {
            throw new InventoryException(409, "Nhà cung cấp đã có phiếu nhập, không thể xóa.");
        }
        suppliers.delete(supplier);
    }
    private SupplierResponse save(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        return SupplierResponse.from(suppliers.save(supplier));
    }
    private Supplier find(Integer id) {
        return suppliers.findById(id).orElseThrow(() -> new InventoryException(404, "Không tìm thấy nhà cung cấp."));
    }
}
