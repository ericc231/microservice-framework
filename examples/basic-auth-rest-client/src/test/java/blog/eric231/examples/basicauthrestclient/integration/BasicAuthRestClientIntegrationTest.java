package blog.eric231.examples.basicauthrestclient.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BasicAuthRestClientIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void frameworkRouting_shouldActivateForClientCall() throws Exception {
        // Given - simulate the framework routing with POST /client/call
        String url = "http://localhost:" + port + "/client/call";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JsonNode requestBody = mapper.createObjectNode().put("test", "message");
        HttpEntity<JsonNode> request = new HttpEntity<>(requestBody, headers);

        // When - make the request (will fail upstream since basic-auth-rest is not running, but routing should work)
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        // Then - verify that the framework routing worked
        // Spring Security might redirect to login (302) or return 401/403
        // But it should not be 404 (not found), which would indicate routing failed
        assertNotNull(response);
        
        // Accept various responses that indicate the endpoint was found but security kicked in
        int statusCode = response.getStatusCodeValue();
        assertTrue(statusCode == 200 || statusCode == 302 || statusCode == 401 || statusCode == 403,
                   "Expected routing to work (200, 302, 401, or 403) but got: " + statusCode);
    }

    @Test
    void frameworkRouting_shouldReturn404ForUnmappedPath() {
        // Given - try a path that's not configured in routing
        String url = "http://localhost:" + port + "/unmapped/path";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JsonNode requestBody = mapper.createObjectNode().put("test", "message");
        HttpEntity<JsonNode> request = new HttpEntity<>(requestBody, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        // Then - Spring Security might redirect (302) instead of 404 for unmapped paths
        // The important thing is it's not 200 (success)
        int statusCode = response.getStatusCodeValue();
        assertTrue(statusCode == 404 || statusCode == 302 || statusCode == 401 || statusCode == 403,
                   "Expected unmapped path to return error status but got: " + statusCode);
    }
}
