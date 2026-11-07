package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.controller.CarrierController;
import fu.se.badmintonshop.modules.delivery.controller.CustomerShipmentController;
import fu.se.badmintonshop.modules.delivery.controller.ManagerShipmentController;
import fu.se.badmintonshop.modules.delivery.dto.CarrierResponse;
import fu.se.badmintonshop.modules.delivery.dto.PageResponse;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryExceptionHandler;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeliveryControllerTest {

    MockMvc mvc;
    ShipmentService service;

    @BeforeEach
    void setUp() {
        service = mock(ShipmentService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ManagerShipmentController(service),
                        new CarrierController(service),
                        new CustomerShipmentController(service))
                .setControllerAdvice(new DeliveryExceptionHandler())
                .build();
    }

    private ShipmentResponse sample() {
        return ShipmentResponse.builder().id(5L).orderId(10L).status("requested").carrierCode("fastship").build();
    }

    @Test
    void listPassesFiltersAndWrapsPage() throws Exception {
        when(service.list("requested", "fastship", "ORD", 10L, 1, 10))
                .thenReturn(PageResponse.<ShipmentResponse>builder()
                        .content(List.of(sample())).page(1).size(10).totalElements(11).totalPages(2).build());

        mvc.perform(get("/api/manager/shipments")
                        .param("status", "requested").param("carrier", "fastship").param("q", "ORD").param("orderId", "10")
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(11));
    }

    @Test
    void createReturns201AndPassesStaffHeader() throws Exception {
        when(service.create(any(), eq(3L))).thenReturn(sample());

        mvc.perform(post("/api/manager/shipments")
                        .header("X-User-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"carrierCode\":\"fastship\",\"serviceType\":\"fast\",\"weightGrams\":800}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.status").value("requested"));
    }

    @Test
    void createValidatesBody() throws Exception {
        mvc.perform(post("/api/manager/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierCode\":\"fastship\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mvc.perform(post("/api/manager/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"carrierCode\":\"fastship\",\"serviceType\":\"rocket\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/manager/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void businessErrorsKeepStatusAndMessage() throws Exception {
        when(service.cancel(eq(5L), any(), any())).thenThrow(DeliveryException.conflict("cannot-cancel"));
        mvc.perform(post("/api/manager/shipments/5/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Khách đổi ý\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("cannot-cancel"));

        when(service.get(99L)).thenThrow(DeliveryException.notFound("Không tìm thấy vận đơn #99"));
        mvc.perform(get("/api/manager/shipments/99"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/manager/shipments/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void syncAndResendPassStaffHeader() throws Exception {
        when(service.sync(5L, 3L)).thenReturn(sample());
        mvc.perform(post("/api/manager/shipments/5/sync").header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));

        when(service.resend(5L, 3L)).thenThrow(new DeliveryException(org.springframework.http.HttpStatus.BAD_GATEWAY, "partner-down"));
        mvc.perform(post("/api/manager/shipments/5/resend").header("X-User-Id", "3"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("partner-down"));
    }

    @Test
    void cancelRequiresReason() throws Exception {
        mvc.perform(post("/api/manager/shipments/5/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void carriersAndCustomerShipment() throws Exception {
        when(service.activeCarriers()).thenReturn(List.of(CarrierResponse.builder().code("fastship").name("FastShip").build()));
        mvc.perform(get("/api/carriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("fastship"));

        when(service.getForCustomer(10L, 7L)).thenReturn(Optional.of(sample()));
        mvc.perform(get("/api/orders/10/shipment").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));

        when(service.getForCustomer(11L, 7L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/orders/11/shipment").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        when(service.listForCustomer(7L)).thenReturn(List.of(sample()));
        mvc.perform(get("/api/my/shipments").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(5));

        when(service.syncForCustomer(10L, 7L)).thenReturn(sample());
        mvc.perform(post("/api/orders/10/shipment/sync").header("X-User-Id", "7"))
                .andExpect(status().isOk());
    }
}
