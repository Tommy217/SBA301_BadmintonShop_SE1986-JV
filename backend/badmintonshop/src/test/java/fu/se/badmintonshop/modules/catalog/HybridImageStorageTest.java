package fu.se.badmintonshop.modules.catalog;
import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.catalog.dto.request.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HybridImageStorageTest {
    private final String image = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9p8AAAAASUVORK5CYII=";
    @Test void keepsCloudinaryUrlAsText() {
        String url = "https://res.cloudinary.com/shop/image/upload/" + "a".repeat(600) + ".png";
        Brand brand = new Brand(); brand.setLogoUrl(url);
        ProductImage product = new ProductImage(); product.setUrl(url);
        assertEquals(url, brand.getLogoUrl()); assertNull(brand.getLogoData());
        assertEquals(url, product.getUrl()); assertNull(product.getImageData());
    }
    @Test void uploadedImagesUseBytesAndRoundTrip() {
        Brand brand = new Brand(); brand.setLogoUrl(image);
        ProductImage product = new ProductImage(); product.setUrl(image);
        assertNotNull(brand.getLogoData()); assertEquals(image, brand.getLogoUrl());
        assertNotNull(product.getImageData()); assertEquals(image, product.getUrl());
        brand.setLogoUrl(null); assertNull(brand.getLogoData()); assertNull(brand.getLogoUrl());
        product.setUrl("https://res.cloudinary.com/shop/image.png"); assertNull(product.getImageData());
    }
    @Test void requestValidationDoesNotApplyOld500CharacterLimit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            String upload = image + "A".repeat(1000);
            var brand = BrandRequest.builder().name("Brand").logoUrl(upload).build();
            var product = ProductImageRequest.builder().productId(1L).url(upload).build();
            assertTrue(factory.getValidator().validate(brand).isEmpty());
            assertTrue(factory.getValidator().validate(product).isEmpty());
        }
    }
}
