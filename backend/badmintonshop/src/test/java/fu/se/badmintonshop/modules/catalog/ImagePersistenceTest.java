package fu.se.badmintonshop.modules.catalog;

import fu.se.badmintonshop.modules.catalog.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ImagePersistenceTest {
    @Autowired EntityManager em;
    @Test void urlAndUploadUseSeparateColumnsAndSurviveReload() {
        String url = "https://res.cloudinary.com/shop/image/upload/original.png";
        String image = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9p8AAAAASUVORK5CYII=";
        Brand brand = Brand.builder().name("Image test").slug("image-test").logoUrl(url).build();
        em.persist(brand);
        em.flush();
        assertEquals(url, em.createNativeQuery("select logo_url from brands where id = :id").setParameter("id", brand.getId()).getSingleResult());
        assertNull(brand.getLogoData());
        brand.setLogoUrl(image);
        em.flush();
        assertNull(em.createNativeQuery("select logo_url from brands where id = :id").setParameter("id", brand.getId()).getSingleResult());
        Integer id = brand.getId();
        em.clear();
        assertEquals(image, em.find(Brand.class, id).getLogoUrl());
        assertNotNull(em.find(Brand.class, id).getLogoData());
    }
}
