package blog.eric231.examples.kafkaredis.logic;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessingLogicTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    private MessageProcessingLogic messageProcessingLogic;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        messageProcessingLogic = new MessageProcessingLogic(redisTemplate);
        mapper = new ObjectMapper();
    }

    @Test
    void testHandleMessageWithId() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("id", "test-123");
        inputMessage.put("message", "Hello World");
        inputMessage.put("timestamp", "2023-01-01T10:00:00");

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertNotNull(result);
        assertEquals("success", result.get("status").asText());
        assertNotNull(result.get("messageId"));
        assertNotNull(result.get("processedAt"));
        assertEquals("kafka-message:test-123", result.get("redisKey").asText());

        // Verify Redis operations
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture());
        verify(redisTemplate, times(2)).expire(any(String.class), eq(Duration.ofHours(24)));
        verify(listOperations).leftPush(eq("recent-messages"), any(String.class));
        verify(listOperations).trim("recent-messages", 0, 99);

        assertEquals("kafka-message:test-123", keyCaptor.getValue());
        
        // Parse stored value and verify it contains expected fields
        String storedValue = valueCaptor.getValue();
        JsonNode storedMessage = mapper.readTree(storedValue);
        assertNotNull(storedMessage.get("id"));
        assertEquals(true, storedMessage.get("processed").asBoolean());
        assertEquals(inputMessage, storedMessage.get("originalMessage"));
    }

    @Test
    void testHandleMessageWithKey() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("key", "user-key-456");
        inputMessage.put("data", "Some data");

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        assertEquals("kafka-message:user-key-456", result.get("redisKey").asText());
    }

    @Test
    void testHandleMessageWithUserId() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("userId", "789");
        inputMessage.put("action", "login");

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        assertEquals("kafka-message:user-789", result.get("redisKey").asText());
    }

    @Test
    void testHandleMessageWithoutSpecialKeys() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("message", "Some random message");
        inputMessage.put("type", "notification");

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        String expectedHashKey = String.valueOf("Some random message".hashCode());
        assertEquals("kafka-message:" + expectedHashKey, result.get("redisKey").asText());
    }

    @Test
    void testHandleMessageFallbackKey() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("randomField", "randomValue");

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        assertTrue(result.get("redisKey").asText().startsWith("kafka-message:"));
    }

    @Test
    void testHandleMessageRedisException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("id", "test-error");
        
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(any(String.class), any(String.class));

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertEquals("error", result.get("status").asText());
        assertEquals("Redis connection failed", result.get("message").asText());
        assertEquals(inputMessage.toString(), result.get("input").asText());
    }

    @Test
    void testGetOperationName() {
        assertEquals("kafka-redis-message-processing", messageProcessingLogic.getOperationName());
    }

    @Test
    void testGetMetadata() {
        JsonNode metadata = messageProcessingLogic.getMetadata();
        
        assertNotNull(metadata);
        assertEquals("Processes Kafka messages and stores them in Redis", 
                     metadata.get("description").asText());
        assertEquals("1.0", metadata.get("version").asText());
        assertEquals("JSON message processing", metadata.get("supports").asText());
    }

    @Test
    void testRedisOperationsSequence() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("id", "sequence-test");

        // Act
        messageProcessingLogic.handle(inputMessage);

        // Assert - verify the correct sequence of Redis operations
        verify(valueOperations).set(eq("kafka-message:sequence-test"), any(String.class));
        verify(redisTemplate).expire(eq("kafka-message:sequence-test"), eq(Duration.ofHours(24)));
        verify(listOperations).leftPush(eq("recent-messages"), any(String.class));
        verify(listOperations).trim("recent-messages", 0, 99);
        verify(redisTemplate).expire(eq("recent-messages"), eq(Duration.ofHours(24)));
    }
}
