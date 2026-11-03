package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import org.junit.jupiter.api.Test;

import static fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentStatusTest {

    @Test
    void requestedCanBeApprovedRejectedOrCancelled() {
        assertTrue(REQUESTED.canTransitionTo(PENDING_PICKUP));
        assertTrue(REQUESTED.canTransitionTo(REJECTED));
        assertTrue(REQUESTED.canTransitionTo(CANCELLED));
        assertFalse(REQUESTED.canTransitionTo(IN_TRANSIT));
        assertFalse(REQUESTED.canTransitionTo(null));
    }

    @Test
    void finalStatusesHaveNoNextStep() {
        for (ShipmentStatus s : new ShipmentStatus[]{DELIVERED, RETURNED, CANCELLED, REJECTED}) {
            assertTrue(s.isFinal(), s + " phải là trạng thái cuối");
            for (ShipmentStatus next : values()) {
                assertFalse(s.canTransitionTo(next), s + " -> " + next);
            }
        }
    }

    @Test
    void onlyBeforePickupIsCancellable() {
        assertTrue(REQUESTED.isCancellable());
        assertTrue(PENDING_PICKUP.isCancellable());
        assertFalse(IN_TRANSIT.isCancellable());
        assertFalse(DELIVERY_FAILED.isCancellable());
        assertFalse(CANCELLED.isCancellable());
    }

    @Test
    void fromValueIsCaseInsensitive() {
        assertEquals(REQUESTED, fromValue("REQUESTED").orElseThrow());
        assertEquals(REJECTED, fromValue("rejected").orElseThrow());
        assertTrue(fromValue("unknown").isEmpty());
    }
}
