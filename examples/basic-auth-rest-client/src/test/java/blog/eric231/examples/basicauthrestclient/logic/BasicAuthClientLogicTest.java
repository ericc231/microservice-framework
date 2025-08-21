package blog.eric231.examples.basicauthrestclient.logic;

import blog.eric231.examples.basicauthrestclient.config.ClientProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicAuthClientLogicTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private ClientProperties properties;
    private BasicAuthClientLogic logic;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new ClientProperties();
        properties.setBaseUrl("http://localhost:8081");
        properties.setEndpointPath("/api/basic-auth");
        properties.setUsername("testuser");
        properties.setPassword("testpass");

        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        logic = new BasicAuthClientLogic(properties, restTemplateBuilder);
    }

    @Test
    void handle_successfulCall_returnsWrappedResponse() throws Exception {
        // Given
        JsonNode input = mapper.createObjectNode().put("message", "test");
        JsonNode responseBody = mapper.createObjectNode().put("result", "success");
        ResponseEntity<JsonNode> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(response);

        // When
        JsonNode result = logic.handle(input);

        // Then
        assertNotNull(result);
        assertEquals(200, result.get("status").asInt());
        assertEquals("success", result.get("upstream").get("result").asText());
        assertEquals("http://localhost:8081/api/basic-auth", result.get("target").asText());
    }

    @Test
    void handle_nullInput_handlesGracefully() throws Exception {
        // Given
        JsonNode responseBody = mapper.createObjectNode().put("result", "success");
        ResponseEntity<JsonNode> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(response);

        // When
        JsonNode result = logic.handle(null);

        // Then
        assertNotNull(result);
        assertEquals(200, result.get("status").asInt());
    }

    @Test
    void handle_exceptionOccurs_returnsErrorResponse() {
        // Given
        JsonNode input = mapper.createObjectNode().put("message", "test");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        // When
        JsonNode result = logic.handle(input);

        // Then
        assertNotNull(result);
        assertEquals("Failed to call basic-auth-rest", result.get("error").asText());
        assertEquals("Connection failed", result.get("message").asText());
    }

    @Test
    void handle_verifyBasicAuthHeader() throws Exception {
        // Given
        JsonNode input = mapper.createObjectNode().put("message", "test");
        JsonNode responseBody = mapper.createObjectNode().put("result", "success");
        ResponseEntity<JsonNode> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(anyString(), entityCaptor.capture(), eq(JsonNode.class)))
                .thenReturn(response);

        // When
        logic.handle(input);

        // Then
        HttpEntity<JsonNode> capturedEntity = entityCaptor.getValue();
        String authHeader = capturedEntity.getHeaders().getFirst("Authorization");
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
    }
}
