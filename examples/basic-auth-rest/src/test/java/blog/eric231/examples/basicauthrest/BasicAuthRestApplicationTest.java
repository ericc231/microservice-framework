package blog.eric231.examples.basicauthrest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BasicAuthRestApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring application context loads successfully
    }
}