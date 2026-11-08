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

    /**
     * Xem ngưỡng phê duyệt hoàn tiền hiện tại
     */
    @GetMapping("/settings/threshold")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getRefundThreshold() {
        java.math.BigDecimal threshold = ticketService.getApprovalThreshold();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("threshold", threshold)));
    }

    /**
     * Cập nhật ngưỡng phê duyệt hoàn tiền (Chỉ Manager/Admin)
     */
    @PutMapping("/settings/threshold")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> updateRefundThreshold(
            @RequestBody java.util.Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) Long managerId) {
        Object thresholdObj = body.get("threshold");
        if (thresholdObj == null) {
            throw new IllegalArgumentException("Vui lòng cung cấp giá trị threshold.");
        }
        java.math.BigDecimal newThreshold = new java.math.BigDecimal(String.valueOf(thresholdObj));
        ticketService.updateApprovalThreshold(newThreshold, managerId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ngưỡng phê duyệt thành công", java.util.Map.of("threshold", newThreshold)));
    }
}

