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

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisReadLogicTest {

    @Mock
    private RedisAdapter redisAdapter;

    @InjectMocks
    private RedisReadLogic redisReadLogic;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redisReadLogic = new RedisReadLogic(redisAdapter, objectMapper);
    }

    @Test
    void testHandleSingleReadSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");

        RedisData mockData = createMockRedisData("test-id", "Test Name", "test-category");
        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(mockData);

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("test-id", result.get("data").get("id").asText());
        assertEquals("Test Name", result.get("data").get("name").asText());
        assertEquals("test-category", result.get("data").get("category").asText());
    }

    @Test
    void testHandleSingleReadNotFound() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "nonexistent-id");
        input.put("operation", "single");

        when(redisAdapter.exists("data:nonexistent-id")).thenReturn(false);

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertEquals("Data not found", result.get("error").asText());
        assertEquals("nonexistent-id", result.get("id").asText());
    }

    @Test
    void testHandleCategoryReadSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("category", "test-category");
        input.put("operation", "category");

        Set<Object> mockIds = Set.of("id1", "id2", "id3");
        RedisData mockData1 = createMockRedisData("id1", "Name1", "test-category");
        RedisData mockData2 = createMockRedisData("id2", "Name2", "test-category");
        RedisData mockData3 = createMockRedisData("id3", "Name3", "test-category");

        when(redisAdapter.smembers("category:test-category")).thenReturn(mockIds);
        when(redisAdapter.get("data:id1", RedisData.class)).thenReturn(mockData1);
        when(redisAdapter.get("data:id2", RedisData.class)).thenReturn(mockData2);
        when(redisAdapter.get("data:id3", RedisData.class)).thenReturn(mockData3);

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("test-category", result.get("category").asText());
        assertEquals(3, result.get("count").asInt());
        assertTrue(result.get("data").isArray());
        assertEquals(3, result.get("data").size());
    }

    @Test
    void testHandleCategoryReadEmpty() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("category", "empty-category");
        input.put("operation", "category");

        when(redisAdapter.smembers("category:empty-category")).thenReturn(Set.of());

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        if (result.has("category")) {
            assertEquals("empty-category", result.get("category").asText());
        }
        assertEquals(0, result.get("count").asInt());
        assertTrue(result.get("data").isArray());
        assertEquals(0, result.get("data").size());
    }

    @Test
    void testHandleAllReadSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "all");

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("Retrieved all active data", result.get("message").asText());
        assertEquals(0, result.get("count").asInt()); // No data mocked
    }

    @Test
    void testHandleKeysOperation() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "keys");

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("Retrieved all data keys", result.get("message").asText());
        assertEquals(0, result.get("count").asInt()); // No data mocked
    }

    @Test
    void testHandleInvalidOperation() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "invalid");

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertEquals("Unsupported operation: invalid", result.get("error").asText());
    }

    @Test
    void testHandleException() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");

        when(redisAdapter.exists("data:test-id")).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        JsonNode result = redisReadLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Failed to read data: Redis connection error"));
    }

    @Test
    void testValidateInputSingle() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");

        // Act
        boolean result = redisReadLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputCategory() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("category", "test-category");
        input.put("operation", "category");

        // Act
        boolean result = redisReadLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputAll() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "all");

        // Act
        boolean result = redisReadLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputMissingId() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "single");
        // Missing 'id' field

        // Act
        boolean result = redisReadLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputMissingCategory() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "category");
        // Missing 'category' field

        // Act
        boolean result = redisReadLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputNull() {
        // Act
        boolean result = redisReadLogic.validateInput(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetMetadata() {
        // Act
        JsonNode metadata = redisReadLogic.getMetadata();

        // Assert
        assertEquals("redis-read", metadata.get("operation").asText());
        assertEquals("Read data from Redis with various query options", metadata.get("description").asText());
        assertEquals("1.0", metadata.get("version").asText());
        assertTrue(metadata.has("inputSchema"));
        assertTrue(metadata.has("outputSchema"));
    }

    private RedisData createMockRedisData(String id, String name, String category) {
        RedisData data = new RedisData();
        data.setId(id);
        data.setName(name);
        data.setDescription("Test description for " + id);
        data.setValue("Test value for " + id);
        data.setCategory(category);
        data.setStatus("active");
        data.setVersion(1L);
        data.setCreatedAt(LocalDateTime.now());
        data.setUpdatedAt(LocalDateTime.now());
        return data;
    }
}