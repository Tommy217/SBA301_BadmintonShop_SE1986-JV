package fu.se.badmintonshop.modules.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fu.se.badmintonshop.modules.cart.dto.response.*;
import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentSnapshotResponse;
import fu.se.badmintonshop.modules.supportticket.dto.response.*;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSnapshotResponse {
    @JsonProperty("orders")
    private List<OrderResponse> orders;

    @JsonProperty("order_items")
    private List<OrderItemResponse> orderItems;

    @JsonProperty("payments")
    private List<PaymentSnapshotResponse> payments;

    @JsonProperty("order_status_history")
    private List<OrderStatusHistoryResponse> orderStatusHistory;

    @JsonProperty("support_tickets")
    private List<SupportTicketSnapshotResponse> supportTickets;

    @JsonProperty("ticket_items")
    private List<SupportTicketItemSnapshotResponse> ticketItems;

    @JsonProperty("ticket_status_history")
    private List<TicketStatusHistorySnapshotResponse> ticketStatusHistory;

    @JsonProperty("ticket_inspections")
    private List<TicketInspectionSnapshotResponse> ticketInspections;

    @JsonProperty("refund_records")
    private List<RefundRecordSnapshotResponse> refundRecords;

    @JsonProperty("carts")
    private List<CartResponse> carts;

    @JsonProperty("cart_items")
    private List<CartItemResponse> cartItems;

    @JsonProperty("refund_approval_threshold")
    private BigDecimal refundApprovalThreshold;

    @JsonProperty("payment_qr_timeout_minutes")
    private Integer paymentQrTimeoutMinutes;
}
