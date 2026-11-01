package fu.se.badmintonshop.modules.ticket.service;

import fu.se.badmintonshop.modules.ticket.dto.*;

import java.util.List;

public interface SupportTicketService {

    // Customer operations
    TicketResponse createTicket(CreateTicketRequest request, Long currentUserId);

    TicketResponse getTicketById(Long id, Long currentUserId, boolean isStaffOrManager);

    List<TicketResponse> getMyTickets(Long currentUserId);

    TicketResponse cancelTicket(Long id, Long currentUserId);

    // Staff operations
    List<TicketResponse> getAllTicketsForStaff(String status, String type);

    TicketResponse reviewTicket(Long id, ReviewTicketRequest request);

    TicketResponse inspectTicket(Long id, InspectionRequest request);

    TicketResponse approveRefund(Long id, Long staffId);

    TicketResponse shipExchange(Long id, ShipExchangeRequest request);

    TicketResponse replacementUnavailable(Long id, Long staffId, String note);

    TicketResponse recordRefundTransfer(Long id, RefundTransferRequest request);

    // Manager operations
    TicketResponse managerDecision(Long id, ManagerDecisionRequest request);
}

