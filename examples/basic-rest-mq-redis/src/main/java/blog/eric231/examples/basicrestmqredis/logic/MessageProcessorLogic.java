package blog.eric231.examples.basicrestmqredis.logic;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Second stage domain logic for processing messages from RabbitMQ and storing to Redis.
 * 
 * This component consumes messages from RabbitMQ, performs business logic operations,
 * interacts with Redis for data storage and retrieval, and sends replies back to the MQ.
 * 
 * Message flow: RabbitMQ -> MessageProcessorLogic -> Redis
 *               RabbitMQ <- MessageProcessorLogic <- Redis
 */
@DL("message-processor")
@Component
public class MessageProcessorLogic implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessorLogic.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public MessageProcessorLogic(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public JsonNode handle(JsonNode input) {
        logger.info("Processing message from MQ: {}", input);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract request information
            String requestId = input.has("requestId") ? input.get("requestId").asText() : UUID.randomUUID().toString();
            String operation = input.has("operation") ? input.get("operation").asText() : "default";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            logger.info("Processing operation '{}' for request ID: {}", operation, requestId);
            
            // Generate storage ID for Redis
            String storageId = UUID.randomUUID().toString();
            
            // Process based on operation type
            ObjectNode result;
            switch (operation.toLowerCase()) {
                case "store":
                    result = handleStoreOperation(input, requestId, storageId, timestamp);
                    break;
                case "retrieve":
                    result = handleRetrieveOperation(input, requestId, timestamp);
                    break;
                case "update":
                    result = handleUpdateOperation(input, requestId, timestamp);
                    break;
                case "delete":
                    result = handleDeleteOperation(input, requestId, timestamp);
                    break;
                default:
                    result = handleDefaultOperation(input, requestId, storageId, timestamp);
                    break;
            }
            
            // Add processing metadata
            result.put("processingTime", System.currentTimeMillis() - startTime);
            result.put("processedBy", "message-processor");
            result.put("processedAt", timestamp);
            
            logger.info("Successfully processed message for request ID: {} in {}ms", 
                       requestId, result.get("processingTime").asLong());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing message from MQ: {}", e.getMessage(), e);
            return createErrorResponse(input, e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Handle store operation - store data to Redis
     */
    private ObjectNode handleStoreOperation(JsonNode input, String requestId, String storageId, String timestamp) {
        ObjectNode result = mapper.createObjectNode();
        
        try {
            // Create data object to store
            Map<String, Object> dataToStore = new HashMap<>();
            dataToStore.put("requestId", requestId);
            dataToStore.put("storageId", storageId);
            dataToStore.put("timestamp", timestamp);
            dataToStore.put("operation", "store");
            
            // Extract and store input data
            if (input.has("data")) {
                dataToStore.put("payload", input.get("data").toString());
            } else {
                dataToStore.put("payload", input.toString());
            }
            
            if (input.has("clientId")) {
                dataToStore.put("clientId", input.get("clientId").asText());
            }
            
            // Store main data
            String mainKey = "data:" + storageId;
            redisTemplate.opsForValue().set(mainKey, dataToStore, 1, TimeUnit.HOURS);
            
            // Create index entries
            String requestIndexKey = "request:" + requestId;
            redisTemplate.opsForValue().set(requestIndexKey, storageId, 1, TimeUnit.HOURS);
            
            String timestampIndexKey = "timestamp:" + timestamp.substring(0, 10); // Date only
            redisTemplate.opsForSet().add(timestampIndexKey, storageId);
            redisTemplate.expire(timestampIndexKey, 24, TimeUnit.HOURS);
            
            // Verify storage
            Object stored = redisTemplate.opsForValue().get(mainKey);
            boolean verified = stored != null;
            
            result.put("status", "success");
            result.put("operation", "store");
            result.put("storageId", storageId);
            result.put("redisKey", mainKey);
            result.put("verified", verified);
            result.put("message", "Data stored successfully in Redis");
            result.put("ttl", 3600); // 1 hour
            
            logger.info("Data stored to Redis with key: {} for request: {}", mainKey, requestId);
            
        } catch (Exception e) {
            logger.error("Error storing data to Redis: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("operation", "store");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }

    /**
     * Handle retrieve operation - get data from Redis
     */
    private ObjectNode handleRetrieveOperation(JsonNode input, String requestId, String timestamp) {
        ObjectNode result = mapper.createObjectNode();
        
        try {
            String retrieveKey = null;
            
            // Determine what to retrieve
            if (input.has("storageId")) {
                retrieveKey = "data:" + input.get("storageId").asText();
            } else if (input.has("requestId")) {
                String lookupRequestId = input.get("requestId").asText();
                String indexKey = "request:" + lookupRequestId;
                Object storageId = redisTemplate.opsForValue().get(indexKey);
                if (storageId != null) {
                    retrieveKey = "data:" + storageId.toString();
                }
            }
            
            if (retrieveKey != null) {
                Object retrievedData = redisTemplate.opsForValue().get(retrieveKey);
                
                if (retrievedData != null) {
                    result.put("status", "success");
                    result.put("operation", "retrieve");
                    result.put("redisKey", retrieveKey);
                    result.put("found", true);
                    result.put("message", "Data retrieved successfully from Redis");
                    result.set("retrievedData", mapper.valueToTree(retrievedData));
                    
                    logger.info("Data retrieved from Redis with key: {} for request: {}", retrieveKey, requestId);
                } else {
                    result.put("status", "success");
                    result.put("operation", "retrieve");
                    result.put("redisKey", retrieveKey);
                    result.put("found", false);
                    result.put("message", "No data found in Redis for the specified key");
                }
            } else {
                result.put("status", "error");
                result.put("operation", "retrieve");
                result.put("errorMessage", "No valid key found for retrieval");
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving data from Redis: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("operation", "retrieve");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }

    /**
     * Handle update operation - update existing data in Redis
     */
    private ObjectNode handleUpdateOperation(JsonNode input, String requestId, String timestamp) {
        ObjectNode result = mapper.createObjectNode();
        
        try {
            String updateKey = null;
            
            if (input.has("storageId")) {
                updateKey = "data:" + input.get("storageId").asText();
            } else if (input.has("requestId")) {
                String lookupRequestId = input.get("requestId").asText();
                String indexKey = "request:" + lookupRequestId;
                Object storageId = redisTemplate.opsForValue().get(indexKey);
                if (storageId != null) {
                    updateKey = "data:" + storageId.toString();
                }
            }
            
            if (updateKey != null && redisTemplate.hasKey(updateKey)) {
                // Get existing data
                Object existingData = redisTemplate.opsForValue().get(updateKey);
                
                // Update with new data
                Map<String, Object> updatedData = new HashMap<>();
                if (existingData instanceof Map) {
                    updatedData.putAll((Map<String, Object>) existingData);
                }
                
                updatedData.put("lastUpdated", timestamp);
                updatedData.put("updateRequestId", requestId);
                
                if (input.has("data")) {
                    updatedData.put("payload", input.get("data").toString());
                }
                
                // Store updated data
                redisTemplate.opsForValue().set(updateKey, updatedData, 1, TimeUnit.HOURS);
                
                result.put("status", "success");
                result.put("operation", "update");
                result.put("redisKey", updateKey);
                result.put("message", "Data updated successfully in Redis");
                
                logger.info("Data updated in Redis with key: {} for request: {}", updateKey, requestId);
            } else {
                result.put("status", "error");
                result.put("operation", "update");
                result.put("errorMessage", "No existing data found for update");
            }
            
        } catch (Exception e) {
            logger.error("Error updating data in Redis: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("operation", "update");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }

    /**
     * Handle delete operation - remove data from Redis
     */
    private ObjectNode handleDeleteOperation(JsonNode input, String requestId, String timestamp) {
        ObjectNode result = mapper.createObjectNode();
        
        try {
            String deleteKey = null;
            
            if (input.has("storageId")) {
                deleteKey = "data:" + input.get("storageId").asText();
            } else if (input.has("requestId")) {
                String lookupRequestId = input.get("requestId").asText();
                String indexKey = "request:" + lookupRequestId;
                Object storageId = redisTemplate.opsForValue().get(indexKey);
                if (storageId != null) {
                    deleteKey = "data:" + storageId.toString();
                    // Also delete the index
                    redisTemplate.delete(indexKey);
                }
            }
            
            if (deleteKey != null) {
                Boolean deleted = redisTemplate.delete(deleteKey);
                
                result.put("status", "success");
                result.put("operation", "delete");
                result.put("redisKey", deleteKey);
                result.put("deleted", deleted != null && deleted);
                result.put("message", deleted != null && deleted ? 
                          "Data deleted successfully from Redis" : 
                          "No data found to delete");
                
                logger.info("Data deletion attempted for key: {} with result: {} for request: {}", 
                           deleteKey, deleted, requestId);
            } else {
                result.put("status", "error");
                result.put("operation", "delete");
                result.put("errorMessage", "No valid key found for deletion");
            }
            
        } catch (Exception e) {
            logger.error("Error deleting data from Redis: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("operation", "delete");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }

    /**
     * Handle default operation - store with enrichment
     */
    private ObjectNode handleDefaultOperation(JsonNode input, String requestId, String storageId, String timestamp) {
        ObjectNode result = mapper.createObjectNode();
        
        try {
            // Create enriched data object
            Map<String, Object> enrichedData = new HashMap<>();
            enrichedData.put("requestId", requestId);
            enrichedData.put("storageId", storageId);
            enrichedData.put("timestamp", timestamp);
            enrichedData.put("operation", "default-enriched");
            enrichedData.put("processed", true);
            enrichedData.put("enrichmentLevel", "standard");
            
            // Add original payload
            enrichedData.put("originalPayload", input.toString());
            
            // Add metadata from input
            if (input.has("clientId")) {
                enrichedData.put("clientId", input.get("clientId").asText());
            }
            if (input.has("requestSize")) {
                enrichedData.put("originalRequestSize", input.get("requestSize").asInt());
            }
            if (input.has("inputFields")) {
                enrichedData.put("originalInputFields", input.get("inputFields").asInt());
            }
            
            // Add processing metadata
            enrichedData.put("processedFields", enrichedData.size());
            enrichedData.put("dataEnrichment", "metadata-added");
            
            // Store to Redis
            String mainKey = "enriched:" + storageId;
            redisTemplate.opsForValue().set(mainKey, enrichedData, 2, TimeUnit.HOURS); // 2 hour TTL
            
            // Create index
            String requestIndexKey = "request:" + requestId;
            redisTemplate.opsForValue().set(requestIndexKey, storageId, 2, TimeUnit.HOURS);
            
            result.put("status", "success");
            result.put("operation", "default-enriched");
            result.put("storageId", storageId);
            result.put("redisKey", mainKey);
            result.put("message", "Data processed and stored with enrichment");
            result.put("enrichmentApplied", true);
            result.put("ttl", 7200); // 2 hours
            
            logger.info("Enriched data stored to Redis with key: {} for request: {}", mainKey, requestId);
            
        } catch (Exception e) {
            logger.error("Error in default processing: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("operation", "default-enriched");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }

    /**
     * Create error response
     */
    private ObjectNode createErrorResponse(JsonNode input, String errorMessage, long processingTime) {
        ObjectNode errorResponse = mapper.createObjectNode();
        errorResponse.put("status", "error");
        errorResponse.put("errorMessage", errorMessage);
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("processingTime", processingTime);
        errorResponse.put("processedBy", "message-processor");
        
        if (input != null && input.has("requestId")) {
            errorResponse.put("requestId", input.get("requestId").asText());
        }
        
        return errorResponse;
    }

    @Override
    public String getOperationName() {
        return "message-processor";
    }

    @Override
    public JsonNode getMetadata() {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("description", "Processes messages from RabbitMQ and interacts with Redis");
        metadata.put("version", "1.0");
        metadata.put("stage", "message-processing");
        metadata.put("inputSource", "RabbitMQ");
        metadata.put("outputTarget", "Redis");
        metadata.put("supportedOperations", "store,retrieve,update,delete,default");
        return metadata;
    }
}
