package blog.eric231.examples.kafkakafkaredis.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageStorageLogicTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    private MessageStorageLogic messageStorageLogic;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        messageStorageLogic = new MessageStorageLogic(redisTemplate);
        mapper = new ObjectMapper();
    }

    @Test
    void testHandleProcessedMessage() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = createProcessedMessage();

        // Act
        JsonNode result = messageStorageLogic.handle(inputMessage);

        // Assert
        assertNotNull(result);
        assertEquals("success", result.get("status").asText());
        assertNotNull(result.get("storageId"));
        assertNotNull(result.get("storedAt"));
        assertTrue(result.get("redisKeys").asText().contains("message:"));
        assertTrue(result.get("redisKeys").asText().contains("processing:"));
        assertTrue(result.get("redisKeys").asText().contains("category:admin"));
        assertTrue(result.get("redisKeys").asText().contains("priority:high"));
        assertTrue(result.get("redisKeys").asText().contains("event:login"));
        assertTrue(result.get("redisKeys").asText().contains("user:admin_123"));
        assertTrue(result.get("redisKeys").asText().contains("recent-messages"));

        // Verify Redis operations
        verify(valueOperations, times(2)).set(anyString(), anyString()); // message and processing keys
        verify(redisTemplate, times(7)).expire(anyString(), any(Duration.class)); // All 7 keys
        verify(listOperations, times(5)).leftPush(anyString(), anyString()); // 5 list operations
        verify(listOperations, times(5)).trim(anyString(), anyLong(), anyLong()); // 5 trim operations
    }

    @Test
    void testStorageWithAllFields() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = createCompleteProcessedMessage();

        // Act
        JsonNode result = messageStorageLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        assertNotNull(result.get("storageId"));
        
        // Verify all Redis key patterns are created
        String redisKeys = result.get("redisKeys").asText();
        assertTrue(redisKeys.contains("message:"));
        assertTrue(redisKeys.contains("processing:"));
        assertTrue(redisKeys.contains("category:vip"));
        assertTrue(redisKeys.contains("priority:critical"));
        assertTrue(redisKeys.contains("event:payment"));
        assertTrue(redisKeys.contains("user:vip_456"));
        assertTrue(redisKeys.contains("recent-messages"));
    }

    @Test
    void testStorageWithMinimalFields() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("someField", "someValue");

        // Act
        JsonNode result = messageStorageLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        assertNotNull(result.get("storageId"));
        
        // Should only have basic storage and recent messages
        String redisKeys = result.get("redisKeys").asText();
        assertTrue(redisKeys.contains("message:"));
        assertTrue(redisKeys.contains("recent-messages"));
        assertFalse(redisKeys.contains("processing:"));
        assertFalse(redisKeys.contains("category:"));
        assertFalse(redisKeys.contains("priority:"));
        assertFalse(redisKeys.contains("event:"));
        assertFalse(redisKeys.contains("user:"));
    }

    @Test
    void testRedisOperationsSequence() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = createProcessedMessage();

        // Act
        messageStorageLogic.handle(inputMessage);

        // Assert - verify Redis operations are called correctly
        
        // Primary storage operations
        verify(valueOperations).set(startsWith("message:"), anyString());
        verify(valueOperations).set(startsWith("processing:"), anyString());
        verify(redisTemplate).expire(startsWith("message:"), eq(Duration.ofHours(24)));
        verify(redisTemplate).expire(startsWith("processing:"), eq(Duration.ofHours(24)));
        
        // List operations for indexing
        verify(listOperations).leftPush(eq("category:admin"), anyString());
        verify(listOperations).trim(eq("category:admin"), eq(0L), eq(99L));
        verify(redisTemplate).expire(eq("category:admin"), eq(Duration.ofHours(24)));
        
        verify(listOperations).leftPush(eq("priority:high"), anyString());
        verify(listOperations).trim(eq("priority:high"), eq(0L), eq(49L));
        verify(redisTemplate).expire(eq("priority:high"), eq(Duration.ofHours(12)));
        
        verify(listOperations).leftPush(eq("event:login"), anyString());
        verify(listOperations).trim(eq("event:login"), eq(0L), eq(29L));
        verify(redisTemplate).expire(eq("event:login"), eq(Duration.ofHours(6)));
        
        verify(listOperations).leftPush(eq("recent-messages"), anyString());
        verify(listOperations).trim(eq("recent-messages"), eq(0L), eq(199L));
        verify(redisTemplate).expire(eq("recent-messages"), eq(Duration.ofHours(48)));
        
        verify(listOperations).leftPush(eq("user:admin_123"), anyString());
        verify(listOperations).trim(eq("user:admin_123"), eq(0L), eq(19L));
        verify(redisTemplate).expire(eq("user:admin_123"), eq(Duration.ofDays(7)));
    }

    @Test
    void testRedisException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), anyString());

        ObjectNode inputMessage = createProcessedMessage();

        // Act
        JsonNode result = messageStorageLogic.handle(inputMessage);

        // Assert
        assertEquals("error", result.get("status").asText());
        assertEquals("Redis connection failed", result.get("error").asText());
        assertEquals("storage", result.get("stage").asText());
        assertNotNull(result.get("storedAt"));
        assertEquals(inputMessage, result.get("inputMessage"));
    }

    @Test
    void testTotalProcessingTimeCalculation() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        String pastTime = LocalDateTime.now().minusSeconds(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        ObjectNode inputMessage = createProcessedMessage();
        inputMessage.put("processedAt", pastTime);

        // Act
        JsonNode result = messageStorageLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        
        // Verify total processing time was calculated and stored
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, atLeastOnce()).set(anyString(), messageCaptor.capture());
        
        String storedMessage = messageCaptor.getValue();
        JsonNode storedJson = mapper.readTree(storedMessage);
        assertTrue(storedJson.has("totalProcessingTime"));
        assertTrue(storedJson.get("totalProcessingTime").asLong() >= 0);
    }

    @Test
    void testGetOperationName() {
        assertEquals("kafka-message-storage", messageStorageLogic.getOperationName());
    }

    @Test
    void testGetMetadata() {
        JsonNode metadata = messageStorageLogic.getMetadata();
        
        assertNotNull(metadata);
        assertEquals("Stores processed Kafka messages in Redis with multiple indexing strategies", 
                     metadata.get("description").asText());
        assertEquals("1.0", metadata.get("version").asText());
        assertEquals("storage", metadata.get("stage").asText());
        assertEquals("intermediate-topic", metadata.get("inputSource").asText());
        assertEquals("redis", metadata.get("outputTarget").asText());
        assertEquals("storageId, processingId, userCategory, priority, eventType, userId, recent", 
                     metadata.get("indexingStrategies").asText());
    }

    private ObjectNode createProcessedMessage() {
        ObjectNode message = mapper.createObjectNode();
        message.put("processingId", "proc-123");
        message.put("processedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        message.put("userId", "admin_123");
        message.put("userCategory", "admin");
        message.put("eventType", "login");
        message.put("priority", "high");
        message.put("data", "login data");
        message.put("dataSize", 10);
        message.put("inputFields", 3);
        message.put("processingDuration", 1L);
        return message;
    }

    private ObjectNode createCompleteProcessedMessage() {
        ObjectNode message = mapper.createObjectNode();
        message.put("processingId", "proc-456");
        message.put("processedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        message.put("userId", "vip_456");
        message.put("userCategory", "vip");
        message.put("eventType", "payment");
        message.put("priority", "critical");
        
        ObjectNode data = mapper.createObjectNode();
        data.put("amount", 100.0);
        message.set("data", data);
        
        message.put("dataSize", 15);
        message.put("inputFields", 4);
        message.put("processingDuration", 2L);
        return message;
    }
}
