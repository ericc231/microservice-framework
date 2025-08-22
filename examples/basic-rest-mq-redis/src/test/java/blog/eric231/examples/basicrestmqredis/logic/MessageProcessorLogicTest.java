package blog.eric231.examples.basicrestmqredis.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageProcessorLogic
 */
@ExtendWith(MockitoExtension.class)
class MessageProcessorLogicTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    private MessageProcessorLogic messageProcessorLogic;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        messageProcessorLogic = new MessageProcessorLogic(redisTemplate);
        mapper = new ObjectMapper();

        // Setup Redis template mocks
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void testHandle_StoreOperation_Success() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "store");
        input.put("clientId", "test-client");
        input.set("data", mapper.createObjectNode().put("key", "value"));

        // Mock Redis operations
        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(1L), eq(TimeUnit.HOURS));
        doNothing().when(setOperations).add(anyString(), anyString());
        doNothing().when(redisTemplate).expire(anyString(), eq(24L), eq(TimeUnit.HOURS));
        when(valueOperations.get(anyString())).thenReturn(new HashMap<>());

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("store");
        assertThat(result.has("storageId")).isTrue();
        assertThat(result.has("redisKey")).isTrue();
        assertThat(result.get("verified").asBoolean()).isTrue();
        assertThat(result.get("message").asText()).isEqualTo("Data stored successfully in Redis");
        assertThat(result.get("ttl").asInt()).isEqualTo(3600);
        assertThat(result.has("processingTime")).isTrue();

        // Verify Redis interactions
        verify(valueOperations, times(2)).set(anyString(), any(), eq(1L), eq(TimeUnit.HOURS));
        verify(setOperations).add(anyString(), anyString());
        verify(redisTemplate).expire(anyString(), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).get(anyString());
    }

    @Test
    void testHandle_StoreOperation_RedisException() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "store");

        // Mock Redis exception
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), any(Map.class), eq(1L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("error");
        assertThat(result.get("operation").asText()).isEqualTo("store");
        assertThat(result.get("errorMessage").asText()).contains("Redis connection failed");
    }

    @Test
    void testHandle_RetrieveOperation_ByStorageId_Found() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "retrieve");
        input.put("storageId", "test-storage-id");

        Map<String, Object> storedData = new HashMap<>();
        storedData.put("requestId", "test-request-id");
        storedData.put("payload", "test data");

        when(valueOperations.get("data:test-storage-id")).thenReturn(storedData);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("retrieve");
        assertThat(result.get("found").asBoolean()).isTrue();
        assertThat(result.get("redisKey").asText()).isEqualTo("data:test-storage-id");
        assertThat(result.has("retrievedData")).isTrue();

        verify(valueOperations).get("data:test-storage-id");
    }

    @Test
    void testHandle_RetrieveOperation_ByRequestId_Found() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "retrieve");

        // Mock index lookup
        when(valueOperations.get("request:test-request-id")).thenReturn("test-storage-id");
        
        Map<String, Object> storedData = new HashMap<>();
        storedData.put("requestId", "test-request-id");
        storedData.put("payload", "test data");
        when(valueOperations.get("data:test-storage-id")).thenReturn(storedData);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("retrieve");
        assertThat(result.get("found").asBoolean()).isTrue();
        assertThat(result.get("redisKey").asText()).isEqualTo("data:test-storage-id");

        verify(valueOperations).get("request:test-request-id");
        verify(valueOperations).get("data:test-storage-id");
    }

    @Test
    void testHandle_RetrieveOperation_NotFound() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "retrieve");
        input.put("storageId", "non-existent-id");

        when(valueOperations.get("data:non-existent-id")).thenReturn(null);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("retrieve");
        assertThat(result.get("found").asBoolean()).isFalse();
        assertThat(result.get("message").asText()).isEqualTo("No data found in Redis for the specified key");
    }

    @Test
    void testHandle_RetrieveOperation_NoValidKey() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "retrieve");
        // No storageId or requestId to retrieve

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("error");
        assertThat(result.get("operation").asText()).isEqualTo("retrieve");
        assertThat(result.get("errorMessage").asText()).isEqualTo("No valid key found for retrieval");
    }

    @Test
    void testHandle_UpdateOperation_Success() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "update");
        input.put("storageId", "test-storage-id");
        input.set("data", mapper.createObjectNode().put("updatedKey", "updatedValue"));

        Map<String, Object> existingData = new HashMap<>();
        existingData.put("requestId", "original-request-id");
        existingData.put("payload", "original data");

        when(redisTemplate.hasKey("data:test-storage-id")).thenReturn(true);
        when(valueOperations.get("data:test-storage-id")).thenReturn(existingData);
        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(1L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("update");
        assertThat(result.get("redisKey").asText()).isEqualTo("data:test-storage-id");
        assertThat(result.get("message").asText()).isEqualTo("Data updated successfully in Redis");

        verify(redisTemplate).hasKey("data:test-storage-id");
        verify(valueOperations).get("data:test-storage-id");
        verify(valueOperations).set(eq("data:test-storage-id"), any(Map.class), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testHandle_UpdateOperation_NotFound() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "update");
        input.put("storageId", "non-existent-id");

        when(redisTemplate.hasKey("data:non-existent-id")).thenReturn(false);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("error");
        assertThat(result.get("operation").asText()).isEqualTo("update");
        assertThat(result.get("errorMessage").asText()).isEqualTo("No existing data found for update");
    }

    @Test
    void testHandle_DeleteOperation_Success() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "delete");
        input.put("storageId", "test-storage-id");

        when(redisTemplate.delete("data:test-storage-id")).thenReturn(true);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("delete");
        assertThat(result.get("deleted").asBoolean()).isTrue();
        assertThat(result.get("redisKey").asText()).isEqualTo("data:test-storage-id");
        assertThat(result.get("message").asText()).isEqualTo("Data deleted successfully from Redis");

        verify(redisTemplate).delete("data:test-storage-id");
    }

    @Test
    void testHandle_DeleteOperation_ByRequestId() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "delete");

        when(valueOperations.get("request:test-request-id")).thenReturn("test-storage-id");
        when(redisTemplate.delete("data:test-storage-id")).thenReturn(true);
        when(redisTemplate.delete("request:test-request-id")).thenReturn(true);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("delete");
        assertThat(result.get("deleted").asBoolean()).isTrue();

        verify(valueOperations).get("request:test-request-id");
        verify(redisTemplate).delete("data:test-storage-id");
        verify(redisTemplate).delete("request:test-request-id");
    }

    @Test
    void testHandle_DeleteOperation_NotFound() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "delete");
        input.put("storageId", "non-existent-id");

        when(redisTemplate.delete("data:non-existent-id")).thenReturn(false);

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("delete");
        assertThat(result.get("deleted").asBoolean()).isFalse();
        assertThat(result.get("message").asText()).isEqualTo("No data found to delete");
    }

    @Test
    void testHandle_DefaultOperation_Success() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("clientId", "test-client");
        input.put("requestSize", 100);
        input.put("inputFields", 5);

        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(2L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("default-enriched");
        assertThat(result.has("storageId")).isTrue();
        assertThat(result.get("redisKey").asText()).startsWith("enriched:");
        assertThat(result.get("enrichmentApplied").asBoolean()).isTrue();
        assertThat(result.get("ttl").asInt()).isEqualTo(7200);

        verify(valueOperations, times(2)).set(anyString(), any(), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    void testHandle_DefaultOperation_WithoutOptionalFields() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        // No clientId, requestSize, inputFields

        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(2L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("default-enriched");
        assertThat(result.get("enrichmentApplied").asBoolean()).isTrue();
    }

    @Test
    void testHandle_NoRequestId_GeneratesUUID() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("operation", "store");

        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(1L), eq(TimeUnit.HOURS));
        when(valueOperations.get(anyString())).thenReturn(new HashMap<>());

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.has("processingTime")).isTrue();
        assertThat(result.get("processedBy").asText()).isEqualTo("message-processor");
    }

    @Test
    void testHandle_UnknownOperation_UsesDefault() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "unknown-operation");

        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(2L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("default-enriched");
    }

    @Test
    void testHandle_ProcessingException() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "store");

        doThrow(new RuntimeException("Unexpected Redis error"))
            .when(valueOperations).set(anyString(), any(Map.class), eq(1L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("error");
        assertThat(result.get("errorMessage").asText()).contains("Unexpected Redis error");
        assertThat(result.get("processedBy").asText()).isEqualTo("message-processor");
        assertThat(result.has("processingTime")).isTrue();
        assertThat(result.get("requestId").asText()).isEqualTo("test-request-id");
    }

    @Test
    void testGetOperationName() {
        // When & Then
        assertThat(messageProcessorLogic.getOperationName()).isEqualTo("message-processor");
    }

    @Test
    void testGetMetadata() {
        // When
        JsonNode metadata = messageProcessorLogic.getMetadata();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.get("description").asText())
            .isEqualTo("Processes messages from RabbitMQ and interacts with Redis");
        assertThat(metadata.get("version").asText()).isEqualTo("1.0");
        assertThat(metadata.get("stage").asText()).isEqualTo("message-processing");
        assertThat(metadata.get("inputSource").asText()).isEqualTo("RabbitMQ");
        assertThat(metadata.get("outputTarget").asText()).isEqualTo("Redis");
        assertThat(metadata.get("supportedOperations").asText()).isEqualTo("store,retrieve,update,delete,default");
    }

    @Test
    void testHandle_UpdateOperation_WithNonMapExistingData() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("requestId", "test-request-id");
        input.put("operation", "update");
        input.put("storageId", "test-storage-id");
        input.set("data", mapper.createObjectNode().put("updatedKey", "updatedValue"));

        // Existing data is not a Map
        String existingData = "simple string data";

        when(redisTemplate.hasKey("data:test-storage-id")).thenReturn(true);
        when(valueOperations.get("data:test-storage-id")).thenReturn(existingData);
        doNothing().when(valueOperations).set(anyString(), any(Map.class), eq(1L), eq(TimeUnit.HOURS));

        // When
        JsonNode result = messageProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.get("operation").asText()).isEqualTo("update");

        // Should create new Map since existing data wasn't a Map
        verify(valueOperations).set(eq("data:test-storage-id"), argThat(updatedData -> {
            Map<String, Object> data = (Map<String, Object>) updatedData;
            return data.containsKey("lastUpdated") && data.containsKey("updateRequestId");
        }), eq(1L), eq(TimeUnit.HOURS));
    }
}
