package blog.eric231.examples.basicrestredis.logic;

import blog.eric231.examples.basicrestredis.model.RedisData;
import blog.eric231.framework.infrastructure.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCreateLogicTest {

    @Mock
    private RedisAdapter redisAdapter;

    @InjectMocks
    private RedisCreateLogic redisCreateLogic;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redisCreateLogic = new RedisCreateLogic(redisAdapter, objectMapper);
    }

    @Test
    void testHandleSuccessfulCreate() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Test Name");
        input.put("description", "Test Description");
        input.put("value", "Test Value");
        input.put("category", "test-category");

        when(redisAdapter.exists("data:test-id")).thenReturn(false);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
        when(redisAdapter.sadd("category:test-category", "test-id")).thenReturn(true);

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("test-id", result.get("id").asText());
        assertEquals("Test Name", result.get("name").asText());
        assertEquals("Data created successfully", result.get("message").asText());

        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
        verify(redisAdapter).sadd("category:test-category", "test-id");
    }

    @Test
    void testHandleSuccessfulCreateWithTtl() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Test Name");
        input.put("ttlMinutes", 30);

        when(redisAdapter.exists("data:test-id")).thenReturn(false);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class), any(Duration.class));
        when(redisAdapter.sadd("category:default", "test-id")).thenReturn(true);

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals(30, result.get("ttlMinutes").asInt());

        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class), eq(Duration.ofMinutes(30)));
    }

    @Test
    void testHandleDataAlreadyExists() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "existing-id");
        input.put("name", "Test Name");

        when(redisAdapter.exists("data:existing-id")).thenReturn(true);

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertEquals("Data with id 'existing-id' already exists", result.get("error").asText());
        assertEquals("existing-id", result.get("id").asText());

        verify(redisAdapter, never()).set(anyString(), any());
    }

    @Test
    void testHandleInvalidInput() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        // Missing required 'id' field

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertEquals("Invalid input: missing required fields", result.get("error").asText());

        verify(redisAdapter, never()).set(anyString(), any());
    }

    @Test
    void testHandleRedisSetFailure() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Test Name");

        when(redisAdapter.exists("data:test-id")).thenReturn(false);
        doThrow(new RuntimeException("Redis failure")).when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Failed to create data"));
        // ID might not be present due to exception
    }

    @Test
    void testHandleWithMetadata() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Test Name");
        
        ObjectNode metadata = input.putObject("metadata");
        metadata.put("key1", "value1");
        metadata.put("key2", 123);

        when(redisAdapter.exists("data:test-id")).thenReturn(false);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
        when(redisAdapter.sadd("category:default", "test-id")).thenReturn(true);

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
    }

    @Test
    void testHandleException() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Test Name");

        when(redisAdapter.exists("data:test-id")).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        JsonNode result = redisCreateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Failed to create data: Redis connection error"));
    }

    @Test
    void testValidateInputValid() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Test Name");

        // Act
        boolean result = redisCreateLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputMissingId() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("name", "Test Name");

        // Act
        boolean result = redisCreateLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputMissingName() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");

        // Act
        boolean result = redisCreateLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputNull() {
        // Act
        boolean result = redisCreateLogic.validateInput(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetMetadata() {
        // Act
        JsonNode metadata = redisCreateLogic.getMetadata();

        // Assert
        assertEquals("redis-create", metadata.get("operation").asText());
        assertEquals("Create new data in Redis with indexing and TTL support", metadata.get("description").asText());
        assertEquals("1.0", metadata.get("version").asText());
        assertTrue(metadata.has("inputSchema"));
        assertTrue(metadata.has("outputSchema"));
    }
}