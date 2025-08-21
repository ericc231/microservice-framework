package blog.eric231.examples.kafkaredis.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Domain logic for processing Kafka messages and storing them in Redis.
 * 
 * This class demonstrates the @DL pattern where Kafka messages are routed
 * to this domain logic and the processed data is stored in Redis.
 */
@DL("kafka-redis.message-processing")
@Component
public class MessageProcessingLogic implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessingLogic.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public MessageProcessingLogic(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public JsonNode handle(JsonNode input) {
        logger.info("Processing Kafka message: {}", input);
        
        try {
            // Generate unique ID for this message
            String messageId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Create processed message object
            ObjectNode processedMessage = mapper.createObjectNode();
            processedMessage.put("id", messageId);
            processedMessage.put("timestamp", timestamp);
            processedMessage.put("processed", true);
            processedMessage.set("originalMessage", input);
            
            // Extract key information from the message
            String messageKey = extractMessageKey(input);
            String redisKey = "kafka-message:" + messageKey;
            
            // Store in Redis with expiration (24 hours)
            redisTemplate.opsForValue().set(redisKey, processedMessage.toString());
            redisTemplate.expire(redisKey, java.time.Duration.ofHours(24));
            
            // Also store in a list for recent messages
            String recentMessagesKey = "recent-messages";
            redisTemplate.opsForList().leftPush(recentMessagesKey, processedMessage.toString());
            redisTemplate.opsForList().trim(recentMessagesKey, 0, 99); // Keep only last 100 messages
            redisTemplate.expire(recentMessagesKey, java.time.Duration.ofHours(24));
            
            logger.info("Successfully stored message with ID {} in Redis", messageId);
            
            // Return success response
            ObjectNode response = mapper.createObjectNode();
            response.put("status", "success");
            response.put("messageId", messageId);
            response.put("redisKey", redisKey);
            response.put("processedAt", timestamp);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing Kafka message: {}", e.getMessage(), e);
            
            // Return error response
            ObjectNode errorResponse = mapper.createObjectNode();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("input", input.toString());
            
            return errorResponse;
        }
    }

    /**
     * Extract a key from the message for Redis storage
     */
    private String extractMessageKey(JsonNode message) {
        // Try to extract a meaningful key from the message
        if (message.has("id")) {
            return message.get("id").asText();
        } else if (message.has("key")) {
            return message.get("key").asText();
        } else if (message.has("userId")) {
            return "user-" + message.get("userId").asText();
        } else if (message.has("message")) {
            // Use hash of message content if no specific key found
            return String.valueOf(message.get("message").asText().hashCode());
        } else {
            // Use hash of entire message as fallback
            return String.valueOf(message.toString().hashCode());
        }
    }

    @Override
    public String getOperationName() {
        return "kafka-redis-message-processing";
    }

    @Override
    public JsonNode getMetadata() {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("description", "Processes Kafka messages and stores them in Redis");
        metadata.put("version", "1.0");
        metadata.put("supports", "JSON message processing");
        return metadata;
    }
}
