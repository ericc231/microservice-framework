package blog.eric231.examples.kafkakafkaredis.logic;

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
 * Second stage domain logic for storing processed messages to Redis.
 * 
 * This component receives processed messages from the intermediate Kafka topic,
 * performs final transformations, and stores them in Redis with proper
 * categorization and indexing.
 * 
 * Message flow: intermediate-topic -> MessageStorageLogic -> Redis
 */
@DL("kafka-message-storage")
@Component
public class MessageStorageLogic implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(MessageStorageLogic.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public MessageStorageLogic(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public JsonNode handle(JsonNode input) {
        logger.info("Storing processed Kafka message: {}", input);
        
        try {
            // Generate final storage metadata
            String storageId = UUID.randomUUID().toString();
            String storageTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Create final storage message
            ObjectNode finalMessage = mapper.createObjectNode();
            finalMessage.put("storageId", storageId);
            finalMessage.put("storedAt", storageTimestamp);
            finalMessage.put("stage", "storage");
            finalMessage.put("status", "stored");
            
            // Copy all processed data
            if (input.has("processingId")) {
                finalMessage.put("processingId", input.get("processingId").asText());
            }
            if (input.has("processedAt")) {
                finalMessage.put("processedAt", input.get("processedAt").asText());
            }
            if (input.has("userId")) {
                finalMessage.put("userId", input.get("userId").asText());
            }
            if (input.has("userCategory")) {
                finalMessage.put("userCategory", input.get("userCategory").asText());
            }
            if (input.has("eventType")) {
                finalMessage.put("eventType", input.get("eventType").asText());
            }
            if (input.has("priority")) {
                finalMessage.put("priority", input.get("priority").asText());
            }
            if (input.has("data")) {
                finalMessage.set("data", input.get("data"));
            }
            if (input.has("dataSize")) {
                finalMessage.put("dataSize", input.get("dataSize").asInt());
            }
            if (input.has("inputFields")) {
                finalMessage.put("inputFields", input.get("inputFields").asInt());
            }
            if (input.has("processingDuration")) {
                finalMessage.put("processingDuration", input.get("processingDuration").asLong());
            }
            
            // Calculate total processing time
            if (input.has("processedAt")) {
                long totalProcessingTime = calculateTotalProcessingTime(input.get("processedAt").asText());
                finalMessage.put("totalProcessingTime", totalProcessingTime);
            }
            
            // Store complete processing history
            finalMessage.set("processingHistory", input);
            
            // Store in Redis with multiple access patterns
            storeInRedis(finalMessage);
            
            logger.info("Successfully stored message with storage ID: {}", storageId);
            
            // Return storage confirmation
            ObjectNode response = mapper.createObjectNode();
            response.put("status", "success");
            response.put("storageId", storageId);
            response.put("storedAt", storageTimestamp);
            response.put("redisKeys", getRedisKeysUsed(finalMessage));
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error storing processed Kafka message: {}", e.getMessage(), e);
            
            // Return error response
            ObjectNode errorResponse = mapper.createObjectNode();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("stage", "storage");
            errorResponse.put("storedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            errorResponse.set("inputMessage", input);
            
            return errorResponse;
        }
    }

    /**
     * Store message in Redis with multiple indexing strategies
     */
    private void storeInRedis(ObjectNode finalMessage) {
        String storageId = finalMessage.get("storageId").asText();
        String messageData = finalMessage.toString();
        
        // 1. Store by storage ID (primary key)
        String primaryKey = "message:" + storageId;
        redisTemplate.opsForValue().set(primaryKey, messageData);
        redisTemplate.expire(primaryKey, java.time.Duration.ofHours(24));
        
        // 2. Store by processing ID (for tracing)
        if (finalMessage.has("processingId")) {
            String processingKey = "processing:" + finalMessage.get("processingId").asText();
            redisTemplate.opsForValue().set(processingKey, messageData);
            redisTemplate.expire(processingKey, java.time.Duration.ofHours(24));
        }
        
        // 3. Index by user category
        if (finalMessage.has("userCategory")) {
            String categoryKey = "category:" + finalMessage.get("userCategory").asText();
            redisTemplate.opsForList().leftPush(categoryKey, messageData);
            redisTemplate.opsForList().trim(categoryKey, 0, 99); // Keep last 100 messages
            redisTemplate.expire(categoryKey, java.time.Duration.ofHours(24));
        }
        
        // 4. Index by priority
        if (finalMessage.has("priority")) {
            String priorityKey = "priority:" + finalMessage.get("priority").asText();
            redisTemplate.opsForList().leftPush(priorityKey, messageData);
            redisTemplate.opsForList().trim(priorityKey, 0, 49); // Keep last 50 messages
            redisTemplate.expire(priorityKey, java.time.Duration.ofHours(12));
        }
        
        // 5. Index by event type
        if (finalMessage.has("eventType")) {
            String eventKey = "event:" + finalMessage.get("eventType").asText();
            redisTemplate.opsForList().leftPush(eventKey, messageData);
            redisTemplate.opsForList().trim(eventKey, 0, 29); // Keep last 30 messages
            redisTemplate.expire(eventKey, java.time.Duration.ofHours(6));
        }
        
        // 6. Add to recent messages list
        String recentKey = "recent-messages";
        redisTemplate.opsForList().leftPush(recentKey, messageData);
        redisTemplate.opsForList().trim(recentKey, 0, 199); // Keep last 200 messages
        redisTemplate.expire(recentKey, java.time.Duration.ofHours(48));
        
        // 7. Store metadata for analytics
        if (finalMessage.has("userId")) {
            String userKey = "user:" + finalMessage.get("userId").asText();
            redisTemplate.opsForList().leftPush(userKey, messageData);
            redisTemplate.opsForList().trim(userKey, 0, 19); // Keep last 20 user messages
            redisTemplate.expire(userKey, java.time.Duration.ofDays(7));
        }
    }

    /**
     * Get list of Redis keys that were used for this message
     */
    private String getRedisKeysUsed(ObjectNode finalMessage) {
        StringBuilder keys = new StringBuilder();
        
        if (finalMessage.has("storageId")) {
            keys.append("message:").append(finalMessage.get("storageId").asText()).append(", ");
        }
        if (finalMessage.has("processingId")) {
            keys.append("processing:").append(finalMessage.get("processingId").asText()).append(", ");
        }
        if (finalMessage.has("userCategory")) {
            keys.append("category:").append(finalMessage.get("userCategory").asText()).append(", ");
        }
        if (finalMessage.has("priority")) {
            keys.append("priority:").append(finalMessage.get("priority").asText()).append(", ");
        }
        if (finalMessage.has("eventType")) {
            keys.append("event:").append(finalMessage.get("eventType").asText()).append(", ");
        }
        if (finalMessage.has("userId")) {
            keys.append("user:").append(finalMessage.get("userId").asText()).append(", ");
        }
        
        keys.append("recent-messages");
        
        return keys.toString();
    }

    /**
     * Calculate total processing time from initial processing to storage
     */
    private long calculateTotalProcessingTime(String processedAtTime) {
        try {
            LocalDateTime processedAt = LocalDateTime.parse(processedAtTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            return java.time.Duration.between(processedAt, now).toMillis();
        } catch (Exception e) {
            logger.warn("Could not calculate total processing time: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public String getOperationName() {
        return "kafka-message-storage";
    }

    @Override
    public JsonNode getMetadata() {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("description", "Stores processed Kafka messages in Redis with multiple indexing strategies");
        metadata.put("version", "1.0");
        metadata.put("stage", "storage");
        metadata.put("inputSource", "intermediate-topic");
        metadata.put("outputTarget", "redis");
        metadata.put("indexingStrategies", "storageId, processingId, userCategory, priority, eventType, userId, recent");
        return metadata;
    }
}
