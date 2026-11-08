package fu.se.badmintonshop.modules.supportticket.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.supportticket.dto.CreateTicketRequest;
import fu.se.badmintonshop.modules.supportticket.dto.TicketResponse;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/support-tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerTicketController {

    private final SupportTicketService ticketService;

    /**
     * Khách hàng tạo phiếu yêu cầu Đổi hàng hoặc Hoàn tiền (UC15).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TicketResponse response = ticketService.createTicket(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu hỗ trợ thành công", response));
    }

    /**
     * Khách hàng xem danh sách phiếu hỗ trợ của mình.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<TicketResponse> list = ticketService.getMyTickets(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Khách hàng xem chi tiết 1 phiếu hỗ trợ.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TicketResponse response = ticketService.getTicketById(id, userId, false);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Khách hàng hủy yêu cầu hỗ trợ (chỉ khi đang ở trạng thái PENDING).
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TicketResponse>> cancelTicket(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TicketResponse response = ticketService.cancelTicket(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu hỗ trợ", response));
    }
}

