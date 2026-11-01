package fu.se.badmintonshop.modules.supportticket.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.supportticket.dto.*;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
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
     * NhÃ¢n viÃªn xem hÃ ng Ä‘á»£i danh sÃ¡ch phiáº¿u há»— trá»£ (lá»c theo status, type).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        List<TicketResponse> list = ticketService.getAllTicketsForStaff(status, type);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * NhÃ¢n viÃªn xem chi tiáº¿t 1 phiáº¿u há»— trá»£.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse response = ticketService.getTicketById(id, null, true);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * NhÃ¢n viÃªn sÆ¡ duyá»‡t tiáº¿p nháº­n (SLA 48h):
     * - Duyá»‡t (báº¯t buá»™c chá»n cause: STORE_FAULT hoáº·c CUSTOMER_PREFERENCE) -> AWAITING_RETURN
     * - Tá»« chá»‘i (báº¯t buá»™c cÃ³ rejectReason) -> REJECTED
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
        return ResponseEntity.ok(ApiResponse.success("ÄÃ£ ghi nháº­n sÆ¡ duyá»‡t thÃ nh cÃ´ng", response));
    }

    /**
     * NhÃ¢n viÃªn ghi nháº­n biÃªn báº£n tháº©m Ä‘á»‹nh hÃ ng tráº£ vá» (PASSED hoáº·c FAILED).
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
        return ResponseEntity.ok(ApiResponse.success("ÄÃ£ ghi nháº­n káº¿t quáº£ tháº©m Ä‘á»‹nh", response));
    }

    /**
     * NhÃ¢n viÃªn phÃª duyá»‡t hoÃ n tiá»n (chá»‰ Ã¡p dá»¥ng vá»›i khoáº£n hoÃ n dÆ°á»›i 3.000.000 VNÄ).
     */
    @PostMapping("/{id}/approve-refund")
    public ResponseEntity<ApiResponse<TicketResponse>> approveRefund(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long staffId) {
        TicketResponse response = ticketService.approveRefund(id, staffId);
        return ResponseEntity.ok(ApiResponse.success("NhÃ¢n viÃªn Ä‘Ã£ phÃª duyá»‡t hoÃ n tiá»n", response));
    }

    /**
     * NhÃ¢n viÃªn xÃ¡c nháº­n xuáº¥t kho gá»­i hÃ ng thay tháº¿ (EXCHANGE thÃ nh cÃ´ng -> COMPLETED).
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
        return ResponseEntity.ok(ApiResponse.success("ÄÃ£ xÃ¡c nháº­n giao hÃ ng thay tháº¿", response));
    }

    /**
     * NhÃ¢n viÃªn bÃ¡o háº¿t hÃ ng thay tháº¿:
     * - Náº¿u STORE_FAULT: Tá»± Ä‘á»™ng ráº½ nhÃ¡nh sang luá»“ng hoÃ n tiá»n.
     * - Náº¿u CUSTOMER_PREFERENCE: Chuyá»ƒn sang CANCELLED, gá»­i tráº£ hÃ ng cÅ©, khÃ´ng hoÃ n tiá»n.
     */
    @PostMapping("/{id}/replacement-unavailable")
    public ResponseEntity<ApiResponse<TicketResponse>> replacementUnavailable(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Háº¿t hÃ ng thay tháº¿") String note,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long staffId) {
        TicketResponse response = ticketService.replacementUnavailable(id, staffId, note);
        return ResponseEntity.ok(ApiResponse.success("ÄÃ£ xá»­ lÃ½ trÆ°á»ng há»£p háº¿t hÃ ng thay tháº¿", response));
    }

    /**
     * NhÃ¢n viÃªn chuyá»ƒn khoáº£n thá»§ cÃ´ng qua quÃ©t mÃ£ QR cá»§a khÃ¡ch vÃ  lÆ°u mÃ£ giao dá»‹ch ngÃ¢n hÃ ng (UC13).
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
        return ResponseEntity.ok(ApiResponse.success("ÄÃ£ ghi nháº­n chuyá»ƒn khoáº£n hoÃ n tiá»n thÃ nh cÃ´ng", response));
    }
}

