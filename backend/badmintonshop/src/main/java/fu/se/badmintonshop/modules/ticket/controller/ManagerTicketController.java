package fu.se.badmintonshop.modules.ticket.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.ticket.dto.ManagerDecisionRequest;
import fu.se.badmintonshop.modules.ticket.dto.TicketResponse;
import fu.se.badmintonshop.modules.ticket.service.SupportTicketService;
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
     * Quản lý (Manager) thẩm định và ra quyết định phê duyệt hoặc từ chối yêu cầu hoàn tiền từ 3.000.000 VNĐ trở lên:
     * - Approve: Chuyển sang PENDING_TRANSFER.
     * - Reject: Bắt buộc có rejectReason -> Chuyển sang REJECTED (gửi trả lại sản phẩm cho khách).
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
        return ResponseEntity.ok(ApiResponse.success("Quản lý đã ghi nhận quyết định thành công", response));
    }
}

