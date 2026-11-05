package fu.se.badmintonshop.modules.payment.service;

import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface PayOSService {

    Map<String, String> createPaymentLink(
            Long orderCode,
            BigDecimal amount,
            String description,
            String buyerName,
            String buyerEmail,
            String buyerPhone,
            String returnUrl,
            String cancelUrl
    );

    boolean verifyWebhookSignature(PayOSWebhookRequest webhookRequest);

    void cancelPaymentLink(Long orderCode, String cancellationReason);

    void confirmWebhook(String webhookUrl);
}

