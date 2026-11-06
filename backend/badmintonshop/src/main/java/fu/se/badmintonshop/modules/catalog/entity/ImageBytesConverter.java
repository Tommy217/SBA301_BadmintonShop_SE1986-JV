package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;

/** Validates and encodes uploaded image data; existing remote URLs remain strings in entities. */
@Converter
public class ImageBytesConverter implements AttributeConverter<String, byte[]> {
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    public static void validate(String value) {
        new ImageBytesConverter().convertToDatabaseColumn(value);
    }

    @Override
    public byte[] convertToDatabaseColumn(String value) {
        if (value == null || value.isBlank()) return null;
        value = value.trim();
        if (!value.startsWith("data:")) {
            if (value.matches("(?i)^(https?://[^\\s]+|/(?!/)[^\\s]*)$"))
                return value.getBytes(StandardCharsets.UTF_8);
            throw new CatalogException(400, "Invalid image URL or image data.");
        }
        int comma = value.indexOf(',');
        if (comma < 0 || comma > 40 || value.length() - comma - 1 > 4 * ((MAX_BYTES + 2) / 3))
            throw new CatalogException(400, "Image must be at most 5 MB.");
        String header = value.substring(0, comma);
        if (!header.matches("data:image/(png|jpeg|gif|webp);base64"))
            throw new CatalogException(400, "Only PNG, JPG, GIF and WebP images are supported.");
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(value.substring(comma + 1)); }
        catch (IllegalArgumentException ex) { throw new CatalogException(400, "Invalid Base64 image."); }
        String mime = mime(bytes);
        if (bytes.length == 0 || bytes.length > MAX_BYTES || mime == null || !header.equals("data:" + mime + ";base64"))
            throw new CatalogException(400, "Invalid image content or size (maximum 5 MB).");
        return bytes;
    }

    @Override
    public String convertToEntityAttribute(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        String type = mime(bytes);
        // Existing URLs are migrated as UTF-8 bytes without downloading remote resources.
        if (type == null) return new String(bytes, StandardCharsets.UTF_8);
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private static String mime(byte[] bytes) {
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (bytes.length >= 3 && bytes[0] == (byte)255 && bytes[1] == (byte)216 && bytes[2] == (byte)255) return "image/jpeg";
        if (bytes.length >= 6) {
            String sig = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            if (sig.equals("GIF87a") || sig.equals("GIF89a")) return "image/gif";
        }
        if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        return null;
    }
}
