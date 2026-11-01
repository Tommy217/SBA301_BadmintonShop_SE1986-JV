package fu.se.badmintonshop.modules.supportticket.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.supportticket.dto.ManagerDecisionRequest;
import fu.se.badmintonshop.modules.supportticket.dto.TicketResponse;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/manager/support-tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ManagerTicketController {

    private final SupportTicketService ticketService;

    /**
     * Quáº£n lÃ½ (Manager) tháº©m Ä‘á»‹nh vÃ  ra quyáº¿t Ä‘á»‹nh phÃª duyá»‡t hoáº·c tá»« chá»‘i yÃªu cáº§u hoÃ n tiá»n tá»« 3.000.000 VNÄ trá»Ÿ lÃªn:
     * - Approve: Chuyá»ƒn sang PENDING_TRANSFER.
     * - Reject: Báº¯t buá»™c cÃ³ rejectReason -> Chuyá»ƒn sang REJECTED (gá»­i tráº£ láº¡i sáº£n pháº©m cho khÃ¡ch).
     */
    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<TicketResponse>> managerDecision(
            @PathVariable Long id,
            @Valid @RequestBody ManagerDecisionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long managerId) {
        if (request.getManagerId() == null && managerId != null) {
            request.setManagerId(managerId);
        }
        TicketResponse response = ticketService.managerDecision(id, request);
        return ResponseEntity.ok(ApiResponse.success("Quáº£n lÃ½ Ä‘Ã£ ghi nháº­n quyáº¿t Ä‘á»‹nh thÃ nh cÃ´ng", response));
    }
}

