package blog.eric231.examples.ldapauthrest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.ldap.urls=ldap://localhost:8389",
    "spring.ldap.base=dc=springframework,dc=org"
})
class LdapAuthRestApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring application context loads successfully
        // Note: This test doesn't require an actual LDAP server to be running
        // as it only tests context loading
    }
}