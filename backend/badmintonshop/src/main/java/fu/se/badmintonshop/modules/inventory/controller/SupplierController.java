package fu.se.badmintonshop.modules.inventory.controller;

import fu.se.badmintonshop.modules.inventory.dto.request.SupplierRequest;
import fu.se.badmintonshop.modules.inventory.dto.response.SupplierResponse;
import fu.se.badmintonshop.modules.inventory.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService service;
    @GetMapping
    public List<SupplierResponse> list() { return service.list(); }
    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Integer id) { return service.get(id); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Integer id, @Valid @RequestBody SupplierRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) { service.delete(id); }
}
