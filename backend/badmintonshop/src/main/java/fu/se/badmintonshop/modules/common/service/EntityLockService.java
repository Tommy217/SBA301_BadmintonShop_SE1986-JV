package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.supportticket.repository.SupportTicketRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityLockService {
    private final UserRepository users;
    private final OrderRepository orders;
    private final SupportTicketRepository tickets;
    private final EntityManager entityManager;

    public void lockUser(long id) {
        users.findLockedById(id)
                .ifPresent(e -> entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE));
    }

    public void lockOrder(long id) {
        orders.findLockedById(id)
                .ifPresent(e -> entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE));
    }

    public boolean lockTicket(long id) {
        var ticket = tickets.findLockedById(id);
        ticket.ifPresent(e -> entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE));
        return ticket.isPresent();
    }
}
