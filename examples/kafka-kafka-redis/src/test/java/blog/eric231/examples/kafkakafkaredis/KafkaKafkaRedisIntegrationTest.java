package blog.eric231.examples.kafkakafkaredis;

import blog.eric231.examples.kafkakafkaredis.logic.MessageProcessorLogic;
import blog.eric231.examples.kafkakafkaredis.logic.MessageStorageLogic;
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
class KafkaKafkaRedisIntegrationTest {

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
    private MessageProcessorLogic messageProcessorLogic;

    @Autowired
    private MessageStorageLogic messageStorageLogic;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testCompleteMessagePipeline() {
        // Arrange
        String inputTopic = "input-messages";
        ObjectNode originalMessage = mapper.createObjectNode();
        originalMessage.put("userId", "admin_123");
        originalMessage.put("eventType", "login");
        originalMessage.put("data", "user login information");

        // Act - Send message to input topic (this will trigger the entire pipeline)
        kafkaTemplate.send(inputTopic, "test-key", originalMessage.toString());

        // Assert - Wait for complete pipeline processing and Redis storage
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    // Check that final message is stored in Redis with proper categorization
                    
                    // Should find messages in recent-messages list
                    Long recentSize = redisTemplate.opsForList().size("recent-messages");
                    assertNotNull(recentSize);
                    assertTrue(recentSize > 0, "Recent messages should contain processed message");
                    
                    // Should find message in admin category
                    Long adminCategorySize = redisTemplate.opsForList().size("category:admin");
                    assertNotNull(adminCategorySize);
                    assertTrue(adminCategorySize > 0, "Admin category should contain message");
                    
                    // Should find message in high priority list
                    Long highPrioritySize = redisTemplate.opsForList().size("priority:high");
                    assertNotNull(highPrioritySize);
                    assertTrue(highPrioritySize > 0, "High priority list should contain message");
                    
                    // Should find message in login event list
                    Long loginEventSize = redisTemplate.opsForList().size("event:login");
                    assertNotNull(loginEventSize);
                    assertTrue(loginEventSize > 0, "Login event list should contain message");
                    
                    // Should find message in user-specific list
                    Long userMessagesSize = redisTemplate.opsForList().size("user:admin_123");
                    assertNotNull(userMessagesSize);
                    assertTrue(userMessagesSize > 0, "User-specific list should contain message");
                    
                    // Verify the stored message structure
                    String recentMessage = (String) redisTemplate.opsForList().index("recent-messages", 0);
                    assertNotNull(recentMessage);
                    
                    JsonNode storedMessage = mapper.readTree(recentMessage);
                    assertNotNull(storedMessage.get("storageId"));
                    assertNotNull(storedMessage.get("storedAt"));
                    assertEquals("storage", storedMessage.get("stage").asText());
                    assertEquals("stored", storedMessage.get("status").asText());
                    assertEquals("admin_123", storedMessage.get("userId").asText());
                    assertEquals("admin", storedMessage.get("userCategory").asText());
                    assertEquals("login", storedMessage.get("eventType").asText());
                    assertEquals("high", storedMessage.get("priority").asText());
                    
                    // Verify processing history is preserved
                    assertNotNull(storedMessage.get("processingHistory"));
                    JsonNode processingHistory = storedMessage.get("processingHistory");
                    assertEquals("processor", processingHistory.get("stage").asText());
                    assertEquals("processed", processingHistory.get("status").asText());
                    assertEquals(originalMessage, processingHistory.get("originalMessage"));
                });
    }

    @Test
    void testMultipleMessageTypes() {
        // Test different message types through the pipeline
        String inputTopic = "input-messages";
        
        // VIP payment message
        ObjectNode vipMessage = mapper.createObjectNode();
        vipMessage.put("userId", "vip_456");
        vipMessage.put("eventType", "payment");
        ObjectNode paymentData = mapper.createObjectNode();
        paymentData.put("amount", 500.0);
        vipMessage.set("data", paymentData);
        
        // Regular user view message
        ObjectNode regularMessage = mapper.createObjectNode();
        regularMessage.put("userId", "12345");
        regularMessage.put("eventType", "view");
        regularMessage.put("data", "product page view");
        
        // Error message
        ObjectNode errorMessage = mapper.createObjectNode();
        errorMessage.put("userId", "guest_789");
        errorMessage.put("eventType", "error");
        errorMessage.put("data", "system error occurred");

        // Send all messages
        kafkaTemplate.send(inputTopic, "vip-key", vipMessage.toString());
        kafkaTemplate.send(inputTopic, "regular-key", regularMessage.toString());
        kafkaTemplate.send(inputTopic, "error-key", errorMessage.toString());

        // Assert all messages are processed correctly
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    // Check VIP category
                    Long vipCategorySize = redisTemplate.opsForList().size("category:vip");
                    assertTrue(vipCategorySize != null && vipCategorySize > 0);
                    
                    // Check regular category
                    Long regularCategorySize = redisTemplate.opsForList().size("category:regular");
                    assertTrue(regularCategorySize != null && regularCategorySize > 0);
                    
                    // Check guest category
                    Long guestCategorySize = redisTemplate.opsForList().size("category:guest");
                    assertTrue(guestCategorySize != null && guestCategorySize > 0);
                    
                    // Check different priorities
                    Long criticalPrioritySize = redisTemplate.opsForList().size("priority:critical");
                    assertTrue(criticalPrioritySize != null && criticalPrioritySize > 0);
                    
                    Long lowPrioritySize = redisTemplate.opsForList().size("priority:low");
                    assertTrue(lowPrioritySize != null && lowPrioritySize > 0);
                    
                    Long urgentPrioritySize = redisTemplate.opsForList().size("priority:urgent");
                    assertTrue(urgentPrioritySize != null && urgentPrioritySize > 0);
                    
                    // Check recent messages contains all 3
                    Long recentSize = redisTemplate.opsForList().size("recent-messages");
                    assertTrue(recentSize != null && recentSize >= 3);
                });
    }

    @Test
    void testDomainLogicDirectlyInSequence() throws Exception {
        // Test the two domain logic components in sequence directly
        
        // Step 1: Test processor logic
        ObjectNode originalMessage = mapper.createObjectNode();
        originalMessage.put("userId", "vip_999");
        originalMessage.put("eventType", "purchase");
        originalMessage.put("data", "product purchase");

        JsonNode processedMessage = messageProcessorLogic.handle(originalMessage);
        
        // Verify processing results
        assertEquals("processed", processedMessage.get("status").asText());
        assertEquals("processor", processedMessage.get("stage").asText());
        assertEquals("vip", processedMessage.get("userCategory").asText());
        assertEquals("critical", processedMessage.get("priority").asText());

        // Step 2: Test storage logic with processed message
        JsonNode storageResult = messageStorageLogic.handle(processedMessage);
        
        // Verify storage results
        assertEquals("success", storageResult.get("status").asText());
        assertNotNull(storageResult.get("storageId"));
        
        // Check Redis storage patterns
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Long vipCategorySize = redisTemplate.opsForList().size("category:vip");
                    assertTrue(vipCategorySize != null && vipCategorySize > 0);
                    
                    Long criticalPrioritySize = redisTemplate.opsForList().size("priority:critical");
                    assertTrue(criticalPrioritySize != null && criticalPrioritySize > 0);
                    
                    Long purchaseEventSize = redisTemplate.opsForList().size("event:purchase");
                    assertTrue(purchaseEventSize != null && purchaseEventSize > 0);
                });
    }

    @Test
    void testRedisKeyExpiration() {
        // Test that Redis keys have proper TTL set
        ObjectNode message = mapper.createObjectNode();
        message.put("userId", "test_user");
        message.put("eventType", "test");
        
        // Process through both stages
        JsonNode processed = messageProcessorLogic.handle(message);
        messageStorageLogic.handle(processed);
        
        // Check TTL on various keys
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Recent messages should have 48-hour TTL
                    Long recentTtl = redisTemplate.getExpire("recent-messages");
                    assertTrue(recentTtl != null && recentTtl > 172000); // Close to 48 hours
                    
                    // Priority should have 12-hour TTL  
                    Long priorityTtl = redisTemplate.getExpire("priority:medium");
                    assertTrue(priorityTtl != null && priorityTtl > 43000); // Close to 12 hours
                    
                    // User should have 7-day TTL
                    Long userTtl = redisTemplate.getExpire("user:test_user");
                    assertTrue(userTtl != null && userTtl > 604000); // Close to 7 days
                });
    }
}
