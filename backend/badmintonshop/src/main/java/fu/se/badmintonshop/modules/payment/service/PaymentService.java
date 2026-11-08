package fu.se.badmintonshop.modules.payment.service;

import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.response.CreatePaymentResponse;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentDetailResponse;

import java.util.List;

public interface PaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    PaymentDetailResponse getPaymentById(Long id);

    PaymentDetailResponse getPaymentByCode(String paymentCode);

    PaymentDetailResponse getPaymentByPayosOrderCode(Long payosOrderCode);

    List<PaymentDetailResponse> getPaymentsByOrderId(Long orderId);

    PaymentDetailResponse cancelPayment(Long paymentId, String reason);

    void handlePayOSWebhook(PayOSWebhookRequest webhookRequest);

    void confirmWebhook(String webhookUrl);

    int processExpiredPayments();

    PaymentDetailResponse syncPaymentStatus(Long paymentId);

    PaymentDetailResponse syncPaymentStatusByOrderId(Long orderId);

    void syncPendingPayments();

    int getPaymentTimeoutMinutes();

    void updatePaymentTimeoutMinutes(int minutes, Long managerId);
}

