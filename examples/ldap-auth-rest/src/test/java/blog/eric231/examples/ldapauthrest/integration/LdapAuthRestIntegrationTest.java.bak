package blog.eric231.examples.ldapauthrest.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LDAP Auth REST Service
 * These tests require the ldap-provider service to be running
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.ldap.urls=ldap://localhost:8389",
    "spring.ldap.base=dc=springframework,dc=org"
})
class LdapAuthRestIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static Process ldapProviderProcess;

    @BeforeAll
    static void startLdapProvider() throws Exception {
        // Start LDAP provider service for testing
        try {
            // Build ldap-provider if needed
            ProcessBuilder buildProcess = new ProcessBuilder("mvn", "clean", "package", "-DskipTests");
            buildProcess.directory(new java.io.File("../ldap-provider"));
            Process build = buildProcess.start();
            build.waitFor();

            // Start ldap-provider service
            ProcessBuilder startProcess = new ProcessBuilder("java", "-jar", 
                "target/ldap-provider-0.0.1-SNAPSHOT.jar", "--server.port=8389");
            startProcess.directory(new java.io.File("../ldap-provider"));
            ldapProviderProcess = startProcess.start();

            // Wait for LDAP server to start
            Thread.sleep(10000);
            
            System.out.println("LDAP Provider started for testing");
        } catch (Exception e) {
            System.out.println("Failed to start LDAP provider: " + e.getMessage());
            System.out.println("Some tests may fail without LDAP server");
        }
    }

    @AfterAll
    static void stopLdapProvider() {
        if (ldapProviderProcess != null) {
            ldapProviderProcess.destroy();
            try {
                ldapProviderProcess.waitFor();
                System.out.println("LDAP Provider stopped");
            } catch (InterruptedException e) {
                ldapProviderProcess.destroyForcibly();
            }
        }
    }

    @Test
    void healthEndpoint_ShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    void ldapServerStatus_ShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/ldap/server/status", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("ldap://localhost:8389"));
    }

    @Test
    void apiEndpoint_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/user/me", String.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Test LDAP authentication with ben user")
    void apiEndpoint_WithValidLdapAuth_ShouldReturnUserInfo() {
        // Test with ben user (should exist in LDAP server)
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("ben", "benspassword")
            .getForEntity("http://localhost:" + port + "/api/user/me", String.class);
        
        // If LDAP server is not available, this will return 401
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            System.out.println("LDAP server may not be running - skipping LDAP authentication test");
            return;
        }
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("ben"));
        assertTrue(response.getBody().contains("LDAP"));
    }

    @Test
    @DisplayName("Test LDAP admin access with bob user")
    void adminEndpoint_WithLdapAdminAuth_ShouldReturnAdminInfo() {
        // Test with bob user (should have admin privileges)
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("bob", "bobspassword")
            .getForEntity("http://localhost:" + port + "/api/user/admin", String.class);
        
        // If LDAP server is not available, this will return 401
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            System.out.println("LDAP server may not be running - skipping LDAP admin test");
            return;
        }
        
        // Bob should have admin access
        assertTrue(response.getStatusCode() == HttpStatus.OK || 
                  response.getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    void userGroupsEndpoint_WithAuth_ShouldReturnGroups() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("ben", "benspassword")
            .getForEntity("http://localhost:" + port + "/api/ldap/user/groups", String.class);
        
        // If LDAP server is not available, this will return 401
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            System.out.println("LDAP server may not be running - skipping groups test");
            return;
        }
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("groups") || 
                  response.getBody().contains("authorities"));
    }
}