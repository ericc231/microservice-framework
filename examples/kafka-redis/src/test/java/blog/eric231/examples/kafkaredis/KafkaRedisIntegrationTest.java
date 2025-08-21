package blog.eric231.examples.kafkaredis;

import blog.eric231.examples.kafkaredis.logic.MessageProcessingLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class KafkaRedisIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("redis.host", redis::getHost);
        registry.add("redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MessageProcessingLogic messageProcessingLogic;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testKafkaToRedisIntegration() {
        // Arrange
        String topic = "test-messages";
        String messageKey = "test-key-123";
        ObjectNode message = mapper.createObjectNode();
        message.put("id", messageKey);
        message.put("message", "Integration test message");
        message.put("userId", "user123");

        // Act
        kafkaTemplate.send(topic, messageKey, message.toString());

        // Assert - Wait for message to be processed and stored in Redis
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    String redisKey = "kafka-message:" + messageKey;
                    String storedMessage = (String) redisTemplate.opsForValue().get(redisKey);
                    assertNotNull(storedMessage, "Message should be stored in Redis");

                    // Verify stored message content
                    JsonNode storedJson = mapper.readTree(storedMessage);
                    assertNotNull(storedJson.get("id"));
                    assertEquals(true, storedJson.get("processed").asBoolean());
                    assertNotNull(storedJson.get("timestamp"));
                    assertEquals(message, storedJson.get("originalMessage"));
                });

        // Verify the message is also in recent messages list
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Long listSize = redisTemplate.opsForList().size("recent-messages");
                    assertNotNull(listSize);
                    assertTrue(listSize > 0, "Recent messages list should contain at least one message");
                });
    }

    @Test
    void testMultipleMessagesProcessing() {
        // Arrange
        String topic = "user-events";
        
        // Send multiple messages
        for (int i = 1; i <= 5; i++) {
            ObjectNode message = mapper.createObjectNode();
            message.put("id", "msg-" + i);
            message.put("action", "test-action-" + i);
            message.put("userId", "user" + i);
            
            kafkaTemplate.send(topic, "key-" + i, message.toString());
        }

        // Assert - Wait for all messages to be processed
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    // Check that all 5 messages are stored individually
                    for (int i = 1; i <= 5; i++) {
                        String redisKey = "kafka-message:msg-" + i;
                        String storedMessage = (String) redisTemplate.opsForValue().get(redisKey);
                        assertNotNull(storedMessage, "Message " + i + " should be stored in Redis");
                    }
                    
                    // Check recent messages list contains all messages
                    Long listSize = redisTemplate.opsForList().size("recent-messages");
                    assertNotNull(listSize);
                    assertTrue(listSize >= 5, "Recent messages list should contain at least 5 messages");
                });
    }

    @Test
    void testDomainLogicDirectCall() throws Exception {
        // Test domain logic directly without Kafka
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("key", "direct-test-key");
        inputMessage.put("message", "Direct test message");

        // Act
        JsonNode result = messageProcessingLogic.handle(inputMessage);

        // Assert
        assertEquals("success", result.get("status").asText());
        assertNotNull(result.get("messageId"));
        assertEquals("kafka-message:direct-test-key", result.get("redisKey").asText());

        // Verify message is stored in Redis
        String storedMessage = (String) redisTemplate.opsForValue().get("kafka-message:direct-test-key");
        assertNotNull(storedMessage);
        
        JsonNode storedJson = mapper.readTree(storedMessage);
        assertEquals(inputMessage, storedJson.get("originalMessage"));
    }

    @Test
    void testMessageKeyExtraction() throws Exception {
        // Test different key extraction scenarios
        
        // Test with userId
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("userId", "456");
        userMessage.put("action", "login");
        
        JsonNode userResult = messageProcessingLogic.handle(userMessage);
        assertEquals("kafka-message:user-456", userResult.get("redisKey").asText());
        
        // Test with message field
        ObjectNode messageFieldMessage = mapper.createObjectNode();
        messageFieldMessage.put("message", "content for hashing");
        messageFieldMessage.put("type", "notification");
        
        JsonNode messageResult = messageProcessingLogic.handle(messageFieldMessage);
        String expectedKey = "kafka-message:" + "content for hashing".hashCode();
        assertEquals(expectedKey, messageResult.get("redisKey").asText());
    }

    @Test
    void testRedisExpiration() {
        // Test that Redis keys have proper expiration set
        ObjectNode message = mapper.createObjectNode();
        message.put("id", "expiration-test");
        
        // Process message
        messageProcessingLogic.handle(message);
        
        // Check that TTL is set (should be around 24 hours = 86400 seconds)
        Long ttl = redisTemplate.getExpire("kafka-message:expiration-test");
        assertNotNull(ttl);
        assertTrue(ttl > 86000, "TTL should be close to 24 hours"); // Allow some margin
        assertTrue(ttl <= 86400, "TTL should not exceed 24 hours");
        
        // Check recent messages list also has expiration
        Long recentTtl = redisTemplate.getExpire("recent-messages");
        assertNotNull(recentTtl);
        assertTrue(recentTtl > 86000, "Recent messages TTL should be close to 24 hours");
    }
}
