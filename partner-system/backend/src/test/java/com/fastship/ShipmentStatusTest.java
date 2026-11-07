package com.fastship;

import com.fastship.domain.ShipmentStatus;
import org.junit.jupiter.api.Test;

import static com.fastship.domain.ShipmentStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentStatusTest {

    @Test
    void requestedNeedsApproval() {
        assertTrue(REQUESTED.canTransitionTo(PENDING_PICKUP));
        assertTrue(REQUESTED.canTransitionTo(REJECTED));
        assertTrue(REQUESTED.canTransitionTo(CANCELLED));
        assertFalse(REQUESTED.canTransitionTo(IN_TRANSIT));
    }

    @Test
    void finalStatusesStop() {
        for (ShipmentStatus s : new ShipmentStatus[]{DELIVERED, RETURNED, CANCELLED, REJECTED}) {
            assertTrue(s.isFinal());
            assertTrue(s.allowedNext().isEmpty(), s.name());
        }
    }

    @Test
    void courierCannotApproveOrCancel() {
        assertFalse(COURIER_UPDATES.contains(PENDING_PICKUP));
        assertFalse(COURIER_UPDATES.contains(REJECTED));
        assertFalse(COURIER_UPDATES.contains(CANCELLED));
        assertTrue(COURIER_UPDATES.contains(RETURNED));
    }
}
