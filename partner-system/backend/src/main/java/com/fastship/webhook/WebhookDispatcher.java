package com.fastship.webhook;

import com.fastship.service.WebhookOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// quet outbox moi 5 giay, gui cac webhook den han (lan dau hoac den luot gui lai)
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private final WebhookOutboxService outbox;

    public WebhookDispatcher(WebhookOutboxService outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${fastship.webhook.poll-ms:5000}", initialDelayString = "${fastship.webhook.initial-delay-ms:10000}")
    public void dispatch() {
        try {
            outbox.dispatchDue();
        } catch (RuntimeException e) {
            log.warn("[webhook] loi khi quet outbox: {}", e.toString());
        }
    }
}
