package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.delivery.scheduler.ShipmentSyncJob;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShipmentSyncJobTest {

    ShipmentRepository repository;
    ShipmentService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShipmentRepository.class);
        service = mock(ShipmentService.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncsOpenShipmentsAndSkipsFinalStatuses() {
        when(repository.findIdsForCarrierSync(anyCollection(), any(Pageable.class))).thenReturn(List.of(1L, 2L, 3L));
        // van don 2 loi nghiep vu: bo qua, van dong bo van don 3
        when(service.sync(2L, null)).thenThrow(DeliveryException.conflict("x"));

        new ShipmentSyncJob(repository, service, true).run();

        verify(service).sync(1L, null);
        verify(service).sync(2L, null);
        verify(service).sync(3L, null);
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findIdsForCarrierSync(captor.capture(), any(Pageable.class));
        assertTrue(captor.getValue().containsAll(List.of("delivered", "returned", "cancelled", "rejected")));
        assertFalse(captor.getValue().contains("in_transit"));
    }

    @Test
    void stopsAfterRepeatedGatewayErrors() {
        when(repository.findIdsForCarrierSync(anyCollection(), any(Pageable.class))).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(service.sync(anyLong(), isNull())).thenThrow(new DeliveryException(HttpStatus.BAD_GATEWAY, "down"));

        new ShipmentSyncJob(repository, service, true).run();

        verify(service, times(3)).sync(anyLong(), isNull());
    }

    @Test
    void disabledJobDoesNothing() {
        new ShipmentSyncJob(repository, service, false).run();
        verifyNoInteractions(repository, service);
    }
}
