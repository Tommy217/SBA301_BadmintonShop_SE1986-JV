package fu.se.badmintonshop.modules.invoice.controller;

import fu.se.badmintonshop.modules.invoice.dto.request.*;
import fu.se.badmintonshop.modules.invoice.dto.response.*;
import fu.se.badmintonshop.modules.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService service;

    @GetMapping
    public List<InvoiceResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) { return service.create(request); }

    @PostMapping("/{id}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse adjust(@PathVariable Long id, @Valid @RequestBody InvoiceAdjustmentRequest request) {
        return service.adjust(id, request);
    }

    @PostMapping("/{id}/replace")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse replace(@PathVariable Long id) { return service.replace(id); }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable Long id) { return service.cancel(id); }

    @PostMapping("/{id}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDeliveryResponse deliver(@PathVariable Long id, @Valid @RequestBody InvoiceDeliveryRequest request) {
        return service.deliver(id, request);
    }
}
