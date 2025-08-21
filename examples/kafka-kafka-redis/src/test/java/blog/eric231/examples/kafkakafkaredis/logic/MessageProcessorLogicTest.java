package blog.eric231.examples.kafkakafkaredis.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageProcessorLogicTest {

    private MessageProcessorLogic messageProcessorLogic;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        messageProcessorLogic = new MessageProcessorLogic();
        mapper = new ObjectMapper();
    }

    @Test
    void testHandleMessageWithUserIdAndEventType() throws Exception {
        // Arrange
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("userId", "admin_123");
        inputMessage.put("eventType", "login");
        inputMessage.put("data", "user login data");

        // Act
        JsonNode result = messageProcessorLogic.handle(inputMessage);

        // Assert
        assertNotNull(result);
        assertEquals("processed", result.get("status").asText());
        assertEquals("processor", result.get("stage").asText());
        assertNotNull(result.get("processingId"));
        assertNotNull(result.get("processedAt"));
        
        assertEquals("admin_123", result.get("userId").asText());
        assertEquals("admin", result.get("userCategory").asText());
        assertEquals("login", result.get("eventType").asText());
        assertEquals("high", result.get("priority").asText());
        assertEquals("user login data", result.get("data").asText());
        assertEquals(17, result.get("dataSize").asInt()); // "user login data".length()
        assertEquals(3, result.get("inputFields").asInt());
        assertEquals(1, result.get("processingDuration").asLong());
        assertEquals(inputMessage, result.get("originalMessage"));
    }

    @Test
    void testUserCategorization() throws Exception {
        // Test admin user
        ObjectNode adminMessage = mapper.createObjectNode();
        adminMessage.put("userId", "admin_user");
        JsonNode adminResult = messageProcessorLogic.handle(adminMessage);
        assertEquals("admin", adminResult.get("userCategory").asText());

        // Test VIP user
        ObjectNode vipMessage = mapper.createObjectNode();
        vipMessage.put("userId", "vip_user");
        JsonNode vipResult = messageProcessorLogic.handle(vipMessage);
        assertEquals("vip", vipResult.get("userCategory").asText());

        // Test regular user (numeric)
        ObjectNode regularMessage = mapper.createObjectNode();
        regularMessage.put("userId", "12345");
        JsonNode regularResult = messageProcessorLogic.handle(regularMessage);
        assertEquals("regular", regularResult.get("userCategory").asText());

        // Test guest user
        ObjectNode guestMessage = mapper.createObjectNode();
        guestMessage.put("userId", "guest_user");
        JsonNode guestResult = messageProcessorLogic.handle(guestMessage);
        assertEquals("guest", guestResult.get("userCategory").asText());
    }

    @Test
    void testPriorityCalculation() throws Exception {
        // Test high priority
        ObjectNode loginMessage = mapper.createObjectNode();
        loginMessage.put("eventType", "login");
        JsonNode loginResult = messageProcessorLogic.handle(loginMessage);
        assertEquals("high", loginResult.get("priority").asText());

        // Test critical priority
        ObjectNode purchaseMessage = mapper.createObjectNode();
        purchaseMessage.put("eventType", "purchase");
        JsonNode purchaseResult = messageProcessorLogic.handle(purchaseMessage);
        assertEquals("critical", purchaseResult.get("priority").asText());

        // Test low priority
        ObjectNode viewMessage = mapper.createObjectNode();
        viewMessage.put("eventType", "view");
        JsonNode viewResult = messageProcessorLogic.handle(viewMessage);
        assertEquals("low", viewResult.get("priority").asText());

        // Test urgent priority
        ObjectNode errorMessage = mapper.createObjectNode();
        errorMessage.put("eventType", "error");
        JsonNode errorResult = messageProcessorLogic.handle(errorMessage);
        assertEquals("urgent", errorResult.get("priority").asText());

        // Test medium priority (default)
        ObjectNode unknownMessage = mapper.createObjectNode();
        unknownMessage.put("eventType", "unknown");
        JsonNode unknownResult = messageProcessorLogic.handle(unknownMessage);
        assertEquals("medium", unknownResult.get("priority").asText());
    }

    @Test
    void testMessageWithComplexData() throws Exception {
        // Arrange
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("userId", "vip_456");
        inputMessage.put("eventType", "payment");
        
        ObjectNode complexData = mapper.createObjectNode();
        complexData.put("amount", 100.50);
        complexData.put("currency", "USD");
        complexData.put("method", "credit_card");
        inputMessage.set("data", complexData);

        // Act
        JsonNode result = messageProcessorLogic.handle(inputMessage);

        // Assert
        assertEquals("processed", result.get("status").asText());
        assertEquals("vip", result.get("userCategory").asText());
        assertEquals("critical", result.get("priority").asText());
        assertEquals(complexData, result.get("data"));
        assertEquals(complexData.toString().length(), result.get("dataSize").asInt());
    }

    @Test
    void testMessageWithoutSpecificFields() throws Exception {
        // Arrange
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("someField", "someValue");
        inputMessage.put("anotherField", 123);

        // Act
        JsonNode result = messageProcessorLogic.handle(inputMessage);

        // Assert
        assertEquals("processed", result.get("status").asText());
        assertEquals("processor", result.get("stage").asText());
        assertFalse(result.has("userId"));
        assertFalse(result.has("userCategory"));
        assertFalse(result.has("eventType"));
        assertFalse(result.has("priority"));
        
        // Should wrap entire input as data
        assertEquals(inputMessage, result.get("data"));
        assertEquals(inputMessage.toString().length(), result.get("dataSize").asInt());
        assertEquals(2, result.get("inputFields").asInt());
    }

    @Test
    void testGetOperationName() {
        assertEquals("kafka-message-processor", messageProcessorLogic.getOperationName());
    }

    @Test
    void testGetMetadata() {
        JsonNode metadata = messageProcessorLogic.getMetadata();
        
        assertNotNull(metadata);
        assertEquals("Processes initial Kafka messages and enriches them for downstream processing", 
                     metadata.get("description").asText());
        assertEquals("1.0", metadata.get("version").asText());
        assertEquals("processor", metadata.get("stage").asText());
        assertEquals("input-topic", metadata.get("inputSource").asText());
        assertEquals("intermediate-topic", metadata.get("outputTarget").asText());
    }

    @Test
    void testProcessingFields() throws Exception {
        // Arrange
        ObjectNode inputMessage = mapper.createObjectNode();
        inputMessage.put("userId", "123");
        inputMessage.put("eventType", "view");

        // Act
        JsonNode result = messageProcessorLogic.handle(inputMessage);

        // Assert
        assertNotNull(result.get("processingId"));
        assertNotNull(result.get("processedAt"));
        assertEquals("processor", result.get("stage").asText());
        assertEquals("processed", result.get("status").asText());
        assertTrue(result.get("processingDuration").asLong() >= 0);
        assertEquals(inputMessage, result.get("originalMessage"));
    }
}
