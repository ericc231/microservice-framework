package blog.eric231.examples.basicauthrest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:integrationtest",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BasicAuthRestIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpoint_ShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    void apiEndpoint_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/user/me", String.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void apiEndpoint_WithValidAuth_ShouldReturnUserInfo() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity("http://localhost:" + port + "/api/user/me", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("admin"));
    }

    @Test
    void adminEndpoint_WithAdminAuth_ShouldReturnAdminInfo() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity("http://localhost:" + port + "/api/user/admin", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Admin access granted"));
    }

    @Test
    void adminEndpoint_WithUserAuth_ShouldReturnForbidden() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity("http://localhost:" + port + "/api/user/admin", String.class);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void echoEndpoint_WithAuth_ShouldReturnEcho() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity("http://localhost:" + port + "/api/user/echo?message=test", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("test"));
    }
}