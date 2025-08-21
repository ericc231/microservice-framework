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
class RedisDeleteLogicTest {

    @Mock
    private RedisAdapter redisAdapter;

    @InjectMocks
    private RedisDeleteLogic redisDeleteLogic;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redisDeleteLogic = new RedisDeleteLogic(redisAdapter, objectMapper);
    }

    @Test
    void testHandleSingleDeleteSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");

        RedisData existingData = createMockRedisData("test-id", "Test Name", "test-category");
        
        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);
        when(redisAdapter.delete("data:test-id")).thenReturn(true);
        when(redisAdapter.srem("category:test-category", "test-id")).thenReturn(true);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("Data deleted successfully", result.get("message").asText());
        assertEquals("test-id", result.get("id").asText());
        assertEquals("data:test-id", result.get("key").asText());
        assertTrue(result.get("cleanupPerformed").asBoolean());

        verify(redisAdapter).delete("data:test-id");
        verify(redisAdapter).srem("category:test-category", "test-id");
    }

    @Test
    void testHandleSingleDeleteNotFound() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "nonexistent-id");
        input.put("operation", "single");

        when(redisAdapter.exists("data:nonexistent-id")).thenReturn(false);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertEquals("Data not found", result.get("error").asText());
        assertEquals("nonexistent-id", result.get("id").asText());

        verify(redisAdapter, never()).delete(anyString());
    }

    @Test
    void testHandleSingleDeleteArchivedWithoutForce() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "archived-id");
        input.put("operation", "single");

        RedisData archivedData = createMockRedisData("archived-id", "Archived Data", "test-category");
        archivedData.setStatus("archived");

        when(redisAdapter.exists("data:archived-id")).thenReturn(true);
        when(redisAdapter.get("data:archived-id", RedisData.class)).thenReturn(archivedData);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Data is archived"));
        assertEquals("archived", result.get("status").asText());

        verify(redisAdapter, never()).delete(anyString());
    }

    @Test
    void testHandleSingleDeleteArchivedWithForce() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "archived-id");
        input.put("operation", "single");
        input.put("force", true);

        RedisData archivedData = createMockRedisData("archived-id", "Archived Data", "test-category");
        archivedData.setStatus("archived");

        when(redisAdapter.exists("data:archived-id")).thenReturn(true);
        when(redisAdapter.get("data:archived-id", RedisData.class)).thenReturn(archivedData);
        when(redisAdapter.delete("data:archived-id")).thenReturn(true);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("archived-id", result.get("id").asText());

        verify(redisAdapter).delete("data:archived-id");
    }

    @Test
    void testHandleCategoryDeleteSuccess() {
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
        when(redisAdapter.delete("data:id1")).thenReturn(true);
        when(redisAdapter.delete("data:id2")).thenReturn(true);
        when(redisAdapter.delete("data:id3")).thenReturn(true);
        when(redisAdapter.delete("category:test-category")).thenReturn(true);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("Category delete operation completed", result.get("message").asText());
        assertEquals("test-category", result.get("category").asText());
        assertEquals(3, result.get("totalItems").asInt());
        assertEquals(3, result.get("deletedCount").asInt());
        assertEquals(0, result.get("failedCount").asInt());

        verify(redisAdapter).delete("category:test-category");
    }

    @Test
    void testHandleCategoryDeleteEmpty() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("category", "empty-category");
        input.put("operation", "category");

        when(redisAdapter.smembers("category:empty-category")).thenReturn(Set.of());

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("No data found in category: empty-category", result.get("message").asText());
        assertEquals(0, result.get("deletedCount").asInt());
    }

    @Test
    void testHandleArchiveOperationSuccess() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "archive");

        RedisData existingData = createMockRedisData("test-id", "Test Name", "test-category");
        existingData.setStatus("active");

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.get("data:test-id", RedisData.class)).thenReturn(existingData);
        doNothing().when(redisAdapter).set(eq("data:test-id"), any(RedisData.class));

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("Data archived successfully", result.get("message").asText());
        assertEquals("test-id", result.get("id").asText());
        assertEquals("archived", result.get("newStatus").asText());

        verify(redisAdapter).set(eq("data:test-id"), any(RedisData.class));
        verify(redisAdapter, never()).delete("data:test-id");
    }

    @Test
    void testHandleArchiveOperationAlreadyArchived() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "archived-id");
        input.put("operation", "archive");

        RedisData archivedData = createMockRedisData("archived-id", "Test Name", "test-category");
        archivedData.setStatus("archived");

        when(redisAdapter.exists("data:archived-id")).thenReturn(true);
        when(redisAdapter.get("data:archived-id", RedisData.class)).thenReturn(archivedData);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertEquals("Data is already archived", result.get("message").asText());
        assertEquals("archived", result.get("status").asText());

        verify(redisAdapter, never()).set(anyString(), any(RedisData.class));
    }

    @Test
    void testHandleWithoutCleanup() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");
        input.put("cleanup", false);

        when(redisAdapter.exists("data:test-id")).thenReturn(true);
        when(redisAdapter.delete("data:test-id")).thenReturn(true);

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertTrue(result.get("success").asBoolean());
        assertFalse(result.get("cleanupPerformed").asBoolean());

        verify(redisAdapter).delete("data:test-id");
        verify(redisAdapter, never()).get(anyString(), eq(RedisData.class));
    }

    @Test
    void testHandleException() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");

        when(redisAdapter.exists("data:test-id")).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        JsonNode result = redisDeleteLogic.handle(input);

        // Assert
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("error").asText().contains("Failed to delete data: Redis connection error"));
    }

    @Test
    void testValidateInputSingle() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "single");

        // Act
        boolean result = redisDeleteLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputArchive() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", "test-id");
        input.put("operation", "archive");

        // Act
        boolean result = redisDeleteLogic.validateInput(input);

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
        boolean result = redisDeleteLogic.validateInput(input);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateInputMissingId() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "single");

        // Act
        boolean result = redisDeleteLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputMissingCategory() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "category");

        // Act
        boolean result = redisDeleteLogic.validateInput(input);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateInputNull() {
        // Act
        boolean result = redisDeleteLogic.validateInput(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetMetadata() {
        // Act
        JsonNode metadata = redisDeleteLogic.getMetadata();

        // Assert
        assertEquals("redis-delete", metadata.get("operation").asText());
        assertEquals("Delete data from Redis with cleanup options", metadata.get("description").asText());
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