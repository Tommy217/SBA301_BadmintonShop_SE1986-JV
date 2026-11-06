package fu.se.badmintonshop.modules.catalog;
import fu.se.badmintonshop.modules.catalog.entity.ImageBytesConverter;
import fu.se.badmintonshop.modules.catalog.exception.CatalogException;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
class ImageBytesConverterTest {
    private final ImageBytesConverter converter = new ImageBytesConverter();
    @Test void storesDecodedBytesAndRestoresDataUrl() {
        String data = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9p8AAAAASUVORK5CYII=";
        String url = "data:image/png;base64," + data;
        byte[] stored = converter.convertToDatabaseColumn(url);
        assertArrayEquals(Base64.getDecoder().decode(data), stored);
        assertEquals(url, converter.convertToEntityAttribute(stored));
    }
    @Test void preservesExistingUrlsAndEmptyLogo() {
        for (String url : new String[]{"https://example.com/logo.png", "/products/racket.svg"})
            assertEquals(url, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(url)));
        assertNull(converter.convertToDatabaseColumn(""));
        assertNull(converter.convertToEntityAttribute(null));
    }
    @Test void rejectsInvalidContentAndOversizedPayload() {
        assertThrows(CatalogException.class, () -> ImageBytesConverter.validate("data:image/png;base64,aGVsbG8="));
        assertThrows(CatalogException.class, () -> ImageBytesConverter.validate("data:image/png;base64,%%%"));
        assertThrows(CatalogException.class, () -> ImageBytesConverter.validate("data:image/svg+xml;base64,PHN2Zz4="));
        assertThrows(CatalogException.class, () -> ImageBytesConverter.validate("data:image/png;base64," + "A".repeat(6990509)));
    }
}
