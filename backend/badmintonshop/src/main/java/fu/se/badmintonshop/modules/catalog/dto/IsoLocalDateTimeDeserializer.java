package fu.se.badmintonshop.modules.catalog.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public class IsoLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();
        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(DEFAULT_ZONE).toLocalDateTime().withNano(0);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Instant.parse(text).atZone(DEFAULT_ZONE).toLocalDateTime().withNano(0);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(text).withNano(0);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Định dạng ngày giờ không hợp lệ: " + text, e);
        }
    }
}
