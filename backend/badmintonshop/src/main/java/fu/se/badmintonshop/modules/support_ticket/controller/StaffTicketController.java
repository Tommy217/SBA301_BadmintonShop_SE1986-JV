package fu.se.badmintonshop.modules.support_ticket.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.support_ticket.dto.*;
import fu.se.badmintonshop.modules.support_ticket.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/staff/support-tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StaffTicketController {

    private final SupportTicketService ticketService;

    /**
     * Nhân viên xem hàng đợi danh sách phiếu hỗ trợ (lọc theo status, type).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        List<TicketResponse> list = ticketService.getAllTicketsForStaff(status, type);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Nhân viên xem chi tiết 1 phiếu hỗ trợ.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse response = ticketService.getTicketById(id, null, true);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Nhân viên sơ duyệt tiếp nhận (SLA 48h):
     * - Duyệt (bắt buộc chọn cause: STORE_FAULT hoặc CUSTOMER_PREFERENCE) -> AWAITING_RETURN
     * - Từ chối (bắt buộc có rejectReason) -> REJECTED
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<TicketResponse>> reviewTicket(
            @PathVariable Long id,
            @Valid @RequestBody ReviewTicketRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        if (request.getStaffId() == null && staffId != null) {
            request.setStaffId(staffId);
        }
        TicketResponse response = ticketService.reviewTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận sơ duyệt thành công", response));
    }

    /**
     * Nhân viên ghi nhận biên bản thẩm định hàng trả về (PASSED hoặc FAILED).
     */
    @PostMapping("/{id}/inspection")
    public ResponseEntity<ApiResponse<TicketResponse>> inspectTicket(
            @PathVariable Long id,
            @Valid @RequestBody InspectionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        if (request.getStaffId() == null && staffId != null) {
            request.setStaffId(staffId);
        }
        TicketResponse response = ticketService.inspectTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận kết quả thẩm định", response));
    }

    /**
     * Nhân viên phê duyệt hoàn tiền (chỉ áp dụng với khoản hoàn dưới 3.000.000 VNĐ).
     */
    @PostMapping("/{id}/approve-refund")
    public ResponseEntity<ApiResponse<TicketResponse>> approveRefund(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long staffId) {
        TicketResponse response = ticketService.approveRefund(id, staffId);
        return ResponseEntity.ok(ApiResponse.success("Nhân viên đã phê duyệt hoàn tiền", response));
    }

    /**
     * Nhân viên xác nhận xuất kho gửi hàng thay thế (EXCHANGE thành công -> COMPLETED).
     */
    @PostMapping("/{id}/ship-exchange")
    public ResponseEntity<ApiResponse<TicketResponse>> shipExchange(
            @PathVariable Long id,
            @RequestBody ShipExchangeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        if (request.getStaffId() == null && staffId != null) {
            request.setStaffId(staffId);
        }
        TicketResponse response = ticketService.shipExchange(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận giao hàng thay thế", response));
    }

    /**
     * Nhân viên báo hết hàng thay thế:
     * - Nếu STORE_FAULT: Tự động rẽ nhánh sang luồng hoàn tiền.
     * - Nếu CUSTOMER_PREFERENCE: Chuyển sang CANCELLED, gửi trả hàng cũ, không hoàn tiền.
     */
    @PostMapping("/{id}/replacement-unavailable")
    public ResponseEntity<ApiResponse<TicketResponse>> replacementUnavailable(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Hết hàng thay thế") String note,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long staffId) {
        TicketResponse response = ticketService.replacementUnavailable(id, staffId, note);
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý trường hợp hết hàng thay thế", response));
    }

    /**
     * Nhân viên chuyển khoản thủ công qua quét mã QR của khách và lưu mã giao dịch ngân hàng (UC13).
     */
    @PostMapping("/{id}/refund-transfer")
    public ResponseEntity<ApiResponse<TicketResponse>> recordRefundTransfer(
            @PathVariable Long id,
            @Valid @RequestBody RefundTransferRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        if (request.getStaffId() == null && staffId != null) {
            request.setStaffId(staffId);
        }
        TicketResponse response = ticketService.recordRefundTransfer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận chuyển khoản hoàn tiền thành công", response));
    }
}

