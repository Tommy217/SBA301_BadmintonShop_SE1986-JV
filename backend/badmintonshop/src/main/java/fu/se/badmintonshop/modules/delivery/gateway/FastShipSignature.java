package fu.se.badmintonshop.modules.delivery.gateway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

// chu ky webhook FastShip: header X-FastShip-Signature = "sha256=" + hex(HMAC-SHA256(secret, body))
public final class FastShipSignature {

    public static final String HEADER = "X-FastShip-Signature";
    private static final String PREFIX = "sha256=";

    private FastShipSignature() {
    }

    public static String sign(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return PREFIX + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC-SHA256", e);
        }
    }

    // so sanh thoi gian hang so; chap nhan ca header khong co tien to "sha256="
    public static boolean verify(String secret, byte[] body, String header) {
        if (secret == null || secret.isBlank() || header == null || header.isBlank() || body == null) {
            return false;
        }
        String given = header.trim().toLowerCase(Locale.ROOT);
        if (!given.startsWith(PREFIX)) {
            given = PREFIX + given;
        }
        return MessageDigest.isEqual(
                sign(secret, body).getBytes(StandardCharsets.UTF_8),
                given.getBytes(StandardCharsets.UTF_8));
    }
}
