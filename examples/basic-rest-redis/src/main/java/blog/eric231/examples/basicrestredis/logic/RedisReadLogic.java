package blog.eric231.examples.basicrestredis.logic;

import blog.eric231.examples.basicrestredis.model.RedisData;
import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.infrastructure.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Domain Logic for reading Redis data using @DL pattern.
 * Handles retrieval of data from Redis with various query options.
 */
@Slf4j
@Component
@DL(value = "redis-read", description = "Read data from Redis", version = "1.0")
public class RedisReadLogic {
    
    private final RedisAdapter redisAdapter;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisReadLogic(RedisAdapter redisAdapter, ObjectMapper objectMapper) {
        this.redisAdapter = redisAdapter;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle Redis read operation
     * 
     * Expected input format:
     * {
     *   "id": "unique-id",           // for single item read
     *   "category": "category-name", // for category-based read
     *   "operation": "single|category|all|keys", // operation type
     *   "includeMetadata": true      // include Redis metadata (TTL, etc.)
     * }
     */
    public JsonNode handle(JsonNode input) {
        ObjectNode result = objectMapper.createObjectNode();
        
        try {
            String operation = input.has("operation") ? input.get("operation").asText() : "single";
            boolean includeMetadata = input.has("includeMetadata") && input.get("includeMetadata").asBoolean();
            
            // First check if operation is supported
            switch (operation.toLowerCase()) {
                case "single":
                case "category": 
                case "all":
                case "keys":
                    break; // Valid operations
                default:
                    result.put("success", false);
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }
            
            // Then validate input based on operation
            if (!validateInput(input)) {
                result.put("success", false);
                result.put("error", "Invalid input");
                return result;
            }
            
            switch (operation.toLowerCase()) {
                case "single":
                    return handleSingleRead(input, includeMetadata);
                case "category":
                    return handleCategoryRead(input, includeMetadata);
                case "all":
                    return handleAllRead(includeMetadata);
                case "keys":
                    return handleKeysRead();
                default:
                    // Should never reach here due to earlier validation
                    result.put("success", false);
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }
            
        } catch (Exception e) {
            log.error("Error reading Redis data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Failed to read data: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Handle single item read
     */
    private JsonNode handleSingleRead(JsonNode input, boolean includeMetadata) {
        ObjectNode result = objectMapper.createObjectNode();
        
        String id = input.get("id").asText();
        String redisKey = "data:" + id;
        
        if (!redisAdapter.exists(redisKey)) {
            result.put("success", false);
            result.put("error", "Data not found");
            result.put("id", id);
            return result;
        }
        
        RedisData redisData = redisAdapter.get(redisKey, RedisData.class);
        if (redisData == null) {
            result.put("success", false);
            result.put("error", "Failed to deserialize data");
            result.put("id", id);
            return result;
        }
        
        result.put("success", true);
        result.put("message", "Data retrieved successfully");
        result.set("data", objectMapper.valueToTree(redisData));
        
        if (includeMetadata) {
            ObjectNode metadata = result.putObject("metadata");
            Duration ttl = redisAdapter.getTtl(redisKey);
            if (ttl != null) {
                metadata.put("ttlSeconds", ttl.getSeconds());
                metadata.put("ttlMinutes", ttl.toMinutes());
            } else {
                metadata.put("ttl", "no expiration");
            }
            metadata.put("key", redisKey);
        }
        
        log.info("Successfully read Redis data with id: {}", id);
        return result;
    }
    
    /**
     * Handle category-based read
     */
    private JsonNode handleCategoryRead(JsonNode input, boolean includeMetadata) {
        ObjectNode result = objectMapper.createObjectNode();
        
        String category = input.get("category").asText();
        String categoryKey = "category:" + category;
        
        Set<Object> dataIds = redisAdapter.smembers(categoryKey);
        if (dataIds == null || dataIds.isEmpty()) {
            result.put("success", true);
            result.put("message", "No data found in category: " + category);
            result.putArray("data");
            result.put("count", 0);
            return result;
        }
        
        ArrayNode dataArray = result.putArray("data");
        int successCount = 0;
        
        for (Object idObj : dataIds) {
            String id = idObj.toString();
            String redisKey = "data:" + id;
            
            try {
                RedisData redisData = redisAdapter.get(redisKey, RedisData.class);
                if (redisData != null && redisData.isActive()) {
                    ObjectNode dataNode = objectMapper.valueToTree(redisData);
                    
                    if (includeMetadata) {
                        ObjectNode metadata = dataNode.putObject("_metadata");
                        Duration ttl = redisAdapter.getTtl(redisKey);
                        if (ttl != null) {
                            metadata.put("ttlSeconds", ttl.getSeconds());
                        }
                        metadata.put("key", redisKey);
                    }
                    
                    dataArray.add(dataNode);
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to read data with id: {}, error: {}", id, e.getMessage());
            }
        }
        
        result.put("success", true);
        result.put("message", "Retrieved data from category: " + category);
        result.put("category", category);
        result.put("count", successCount);
        result.put("totalIds", dataIds.size());
        
        log.info("Successfully read {} items from category: {}", successCount, category);
        return result;
    }
    
    /**
     * Handle read all data
     */
    private JsonNode handleAllRead(boolean includeMetadata) {
        ObjectNode result = objectMapper.createObjectNode();
        
        // This is a simplified approach - in production, you'd want pagination
        // Get all keys matching pattern "data:*"
        ArrayNode dataArray = result.putArray("data");
        int count = 0;
        
        // Note: In a real implementation, you'd use SCAN for better performance
        // Here we'll use a different approach through category index
        try {
            Set<Object> createdTimeEntries = redisAdapter.smembers("created_time");
            
            for (Object entry : createdTimeEntries) {
                String entryStr = entry.toString();
                String[] parts = entryStr.split(":", 2);
                if (parts.length >= 1) {
                    String id = parts[0];
                    String redisKey = "data:" + id;
                    
                    try {
                        RedisData redisData = redisAdapter.get(redisKey, RedisData.class);
                        if (redisData != null && redisData.isActive()) {
                            ObjectNode dataNode = objectMapper.valueToTree(redisData);
                            
                            if (includeMetadata) {
                                ObjectNode metadata = dataNode.putObject("_metadata");
                                Duration ttl = redisAdapter.getTtl(redisKey);
                                if (ttl != null) {
                                    metadata.put("ttlSeconds", ttl.getSeconds());
                                }
                                metadata.put("key", redisKey);
                            }
                            
                            dataArray.add(dataNode);
                            count++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to read data with id: {}", id);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to read from time index, falling back to empty result");
        }
        
        result.put("success", true);
        result.put("message", "Retrieved all active data");
        result.put("count", count);
        
        log.info("Successfully read all data, count: {}", count);
        return result;
    }
    
    /**
     * Handle keys read (list all data keys)
     */
    private JsonNode handleKeysRead() {
        ObjectNode result = objectMapper.createObjectNode();
        
        try {
            Set<Object> createdTimeEntries = redisAdapter.smembers("created_time");
            ArrayNode keysArray = result.putArray("keys");
            
            for (Object entry : createdTimeEntries) {
                String entryStr = entry.toString();
                String[] parts = entryStr.split(":", 2);
                if (parts.length >= 1) {
                    String id = parts[0];
                    String redisKey = "data:" + id;
                    if (redisAdapter.exists(redisKey)) {
                        keysArray.add(redisKey);
                    }
                }
            }
            
            result.put("success", true);
            result.put("message", "Retrieved all data keys");
            result.put("count", keysArray.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to retrieve keys: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate input data
     */
    public boolean validateInput(JsonNode input) {
        if (input == null || !input.isObject()) {
            return false;
        }
        
        String operation = input.has("operation") ? input.get("operation").asText() : "single";
        
        switch (operation.toLowerCase()) {
            case "single":
                return input.has("id") && !input.get("id").asText().trim().isEmpty();
            case "category":
                return input.has("category") && !input.get("category").asText().trim().isEmpty();
            case "all":
            case "keys":
                return true; // No additional validation needed
            default:
                return false;
        }
    }
    
    /**
     * Get operation metadata
     */
    public JsonNode getMetadata() {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("operation", "redis-read");
        metadata.put("description", "Read data from Redis with various query options");
        metadata.put("version", "1.0");
        
        ObjectNode inputSchema = metadata.putObject("inputSchema");
        inputSchema.put("id", "string (required for single) - Data ID to read");
        inputSchema.put("category", "string (required for category) - Category to query");
        inputSchema.put("operation", "string (optional) - Operation type: single|category|all|keys");
        inputSchema.put("includeMetadata", "boolean (optional) - Include Redis metadata");
        
        ObjectNode outputSchema = metadata.putObject("outputSchema");
        outputSchema.put("success", "boolean - Operation result");
        outputSchema.put("message", "string - Result message");
        outputSchema.put("error", "string - Error message (if failed)");
        outputSchema.putObject("data").put("description", "object|array - Retrieved data");
        outputSchema.put("count", "number - Number of items retrieved");
        outputSchema.putObject("metadata").put("description", "object - Redis metadata (if requested)");
        
        return metadata;
    }
}