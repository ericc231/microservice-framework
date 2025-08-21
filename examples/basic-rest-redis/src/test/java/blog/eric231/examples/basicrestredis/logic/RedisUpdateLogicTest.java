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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisUpdateLogicTest {

    @Mock
    private RedisAdapter redisAdapter;

    @InjectMocks
    private RedisUpdateLogic redisUpdateLogic;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redisUpdateLogic = new RedisUpdateLogic(redisAdapter, objectMapper);
    }

    @Test
    void testHandlePatchUpdateSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Updated Name");
        input.put("description", "Updated Description");
        input.put("operation", "patch");

        RedisData existingData = createMockRedisData("test-id", "Old Name", "test-category");
        existingData.setVersion(1L);

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("test-id", result.get("id").asText());
        assertEquals("patch", result.get("operation").asText());
        assertEquals(1L, result.get("previousVersion").asLong());
        assertEquals(2L, result.get("newVersion").asLong());
        assertTrue(result.has("updatedData"));

        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
    }

    @Test
    void testHandleFullUpdateSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "New Name");
        input.put("description", "New Description");
        input.put("value", "New Value");
        input.put("category", "new-category");
        input.put("operation", "update");

        RedisData existingData = createMockRedisData("test-id", "Old Name", "old-category");
        existingData.setVersion(2L);

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
        when(redisAdapter.srem("category:old-category", "test-id")).thenReturn(true);
        when(redisAdapter.sadd("category:new-category", "test-id")).thenReturn(true);

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("update", result.get("operation").asText());
        assertEquals(2L, result.get("previousVersion").asLong());
        assertEquals(3L, result.get("newVersion").asLong());

        // Verify category change
        verify(redisAdapter).srem("category:old-category", "test-id");
        verify(redisAdapter).sadd("category:new-category", "test-id");
    }

    @Test
    void testHandleWithTtlUpdate() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Updated Name");
        input.put("ttlMinutes", 45);

        RedisData existingData = createMockRedisData("test-id", "Old Name", "test-category");

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class), eq(Duration.ofMinutes(45)));

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals(45, result.get("ttlMinutes").asInt());

        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class), eq(Duration.ofMinutes(45)));
    }

    @Test
    void testHandleVersionMismatch() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Updated Name");
        input.put("version", 1);

        RedisData existingData = createMockRedisData("test-id", "Old Name", "test-category");
        existingData.setVersion(2L); // Different version

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Version mismatch"));
        assertEquals(2L, result.get("currentVersion").asLong());

        verify(redisAdapter, never()).set(anyString(), any(RedisData.class));
    }

    @Test
    void testHandleDataNotFound() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "nonexistent-id");
        input.put("name", "Updated Name");

        when(redisAdapter.exists("data:nonexistent-id")).thenReturn(false);

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertEquals("Data with id 'nonexistent-id' not found", result.get("error").asText());
        assertEquals("nonexistent-id", result.get("id").asText());

        verify(redisAdapter, never()).set(anyString(), any(RedisData.class));
    }

    @Test
    void testHandleWithMetadata() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Updated Name");
        
        ObjectNode metadata = input.putObject("metadata");
        metadata.put("key1", "newValue1");
        metadata.put("newKey", "newValue");

        RedisData existingData = createMockRedisData("test-id", "Old Name", "test-category");
        existingData.addMetadata("key1", "oldValue1");
        existingData.addMetadata("key2", "value2");

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
    }

    @Test
    void testHandleException() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Updated Name");

        when(redisAdapter.exists("data:test-id")).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        JsonNode result = redisUpdateLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Failed to update data: Redis connection error"));
    }

    @Test
    void testValidateInputValid() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("name", "Updated Name");

        // Act
        boolean result = redisUpdateLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputMissingId() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("name", "Updated Name");

        // Act
        boolean result = redisUpdateLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputNoUpdateFields() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        // No update fields provided

        // Act
        boolean result = redisUpdateLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputNull() {
        // Act
        boolean result = redisUpdateLogic.validateInput(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetMetadata() {
        // Act
        JsonNode metadata = redisUpdateLogic.getMetadata();

        // Assert
        assertEquals("redis-update", metadata.get("operation").asText());
        assertEquals("Update existing data in Redis with version control", metadata.get("description").asText());
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
        data.setCreatedAt(LocalDateTime.now().minusDays(1));
        data.setUpdatedAt(LocalDateTime.now());
        return data;
    }
}