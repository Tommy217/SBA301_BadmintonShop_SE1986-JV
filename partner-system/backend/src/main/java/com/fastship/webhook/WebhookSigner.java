package com.fastship.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

// header X-FastShip-Signature = "sha256=" + hex(HMAC-SHA256(secret, body)); shop kiem lai bang cung cong thuc
public final class WebhookSigner {

    public static final String SIGNATURE_HEADER = "X-FastShip-Signature";
    public static final String EVENT_HEADER = "X-FastShip-Event-Id";

    private WebhookSigner() {
    }

    public static String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC-SHA256", e);
        }
    }
}
