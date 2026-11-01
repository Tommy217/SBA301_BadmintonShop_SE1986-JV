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
     * KhÃ¡ch hÃ ng táº¡o phiáº¿u yÃªu cáº§u Äá»•i hÃ ng hoáº·c HoÃ n tiá»n (UC15).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TicketResponse response = ticketService.createTicket(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Gá»­i yÃªu cáº§u há»— trá»£ thÃ nh cÃ´ng", response));
    }

    /**
     * KhÃ¡ch hÃ ng xem danh sÃ¡ch phiáº¿u há»— trá»£ cá»§a mÃ¬nh.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId) {
        List<TicketResponse> list = ticketService.getMyTickets(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * KhÃ¡ch hÃ ng xem chi tiáº¿t 1 phiáº¿u há»— trá»£.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TicketResponse response = ticketService.getTicketById(id, userId, false);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * KhÃ¡ch hÃ ng há»§y yÃªu cáº§u há»— trá»£ (chá»‰ khi Ä‘ang á»Ÿ tráº¡ng thÃ¡i PENDING).
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TicketResponse>> cancelTicket(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        TicketResponse response = ticketService.cancelTicket(id, userId);
        return ResponseEntity.ok(ApiResponse.success("ÄÃ£ há»§y yÃªu cáº§u há»— trá»£", response));
    }
}

