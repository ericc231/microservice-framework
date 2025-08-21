package blog.eric231.examples.basicrestredis.logic;

import blog.eric231.examples.basicrestredis.model.RedisData;
import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.infrastructure.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Domain Logic for creating Redis data using @DL pattern.
 * Handles the creation of new data entries in Redis with optional TTL.
 */
@Slf4j
@Component
@DL(value = "redis-create", description = "Create new data in Redis", version = "1.0")
public class RedisCreateLogic {
    
    private final RedisAdapter redisAdapter;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisCreateLogic(RedisAdapter redisAdapter, ObjectMapper objectMapper) {
        this.redisAdapter = redisAdapter;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle Redis create operation
     * 
     * Expected input format:
     * {
     *   "id": "unique-id",
     *   "name": "data name",
     *   "description": "data description",
     *   "value": "data value",
     *   "category": "data category",
     *   "metadata": {
     *     "key1": "value1",
     *     "key2": "value2"
     *   },
     *   "ttlMinutes": 60  // optional, TTL in minutes
     * }
     */
    public JsonNode handle(JsonNode input) {
        ObjectNode result = objectMapper.createObjectNode();
        
        try {
            // Validate input
            if (!validateInput(input)) {
                result.put("success", false);
                result.put("error", "Invalid input: missing required fields");
                return result;
            }
            
            String id = input.get("id").asText();
            
            // Check if data already exists
            String redisKey = "data:" + id;
            if (redisAdapter.exists(redisKey)) {
                result.put("success", false);
                result.put("error", "Data with id '" + id + "' already exists");
                result.put("id", id);
                return result;
            }
            
            // Create Redis data object
            RedisData redisData = createRedisDataFromInput(input);
            
            // Store in Redis
            
            // Check if TTL is specified
            if (input.has("ttlMinutes")) {
                int ttlMinutes = input.get("ttlMinutes").asInt();
                Duration ttl = Duration.ofMinutes(ttlMinutes);
                redisAdapter.set(redisKey, redisData, ttl);
                result.put("ttlMinutes", ttlMinutes);
                log.info("Created Redis data with id: {} and TTL: {} minutes", id, ttlMinutes);
            } else {
                redisAdapter.set(redisKey, redisData);
                log.info("Created Redis data with id: {} (no TTL)", id);
            }
            
            // Store in category index for querying
            String categoryKey = "category:" + redisData.getCategory();
            redisAdapter.sadd(categoryKey, id);
            
            // Store creation timestamp in sorted set for time-based queries
            String timeIndexKey = "created_time";
            redisAdapter.sadd(timeIndexKey, id + ":" + redisData.getCreatedAt());
            
            result.put("success", true);
            result.put("message", "Data created successfully");
            result.put("id", id);
            result.put("name", redisData.getName());
            result.put("key", redisKey);
            result.put("category", redisData.getCategory());
            result.put("createdAt", redisData.getCreatedAt().toString());
            
        } catch (Exception e) {
            log.error("Error creating Redis data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Failed to create data: " + e.getMessage());
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
        
        // Required fields
        String[] requiredFields = {"id", "name"};
        for (String field : requiredFields) {
            if (!input.has(field) || input.get(field).asText().trim().isEmpty()) {
                log.warn("Missing or empty required field: {}", field);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create RedisData object from JSON input
     */
    private RedisData createRedisDataFromInput(JsonNode input) {
        RedisData redisData = new RedisData();
        
        redisData.setId(input.get("id").asText());
        redisData.setName(input.get("name").asText());
        redisData.setDescription(input.has("description") ? input.get("description").asText() : "");
        redisData.setValue(input.has("value") ? input.get("value").asText() : "");
        redisData.setCategory(input.has("category") ? input.get("category").asText() : "default");
        redisData.setCreatedAt(LocalDateTime.now());
        redisData.setUpdatedAt(LocalDateTime.now());
        redisData.setStatus("active");
        redisData.setVersion(1L);
        
        // Handle metadata
        if (input.has("metadata") && input.get("metadata").isObject()) {
            JsonNode metadata = input.get("metadata");
            metadata.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = metadata.get(fieldName);
                if (fieldValue.isTextual()) {
                    redisData.addMetadata(fieldName, fieldValue.asText());
                } else if (fieldValue.isNumber()) {
                    redisData.addMetadata(fieldName, fieldValue.asDouble());
                } else if (fieldValue.isBoolean()) {
                    redisData.addMetadata(fieldName, fieldValue.asBoolean());
                } else {
                    redisData.addMetadata(fieldName, fieldValue.toString());
                }
            });
        }
        
        return redisData;
    }
    
    /**
     * Get operation metadata
     */
    public JsonNode getMetadata() {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("operation", "redis-create");
        metadata.put("description", "Create new data in Redis with indexing and TTL support");
        metadata.put("version", "1.0");
        
        ObjectNode inputSchema = metadata.putObject("inputSchema");
        inputSchema.put("id", "string (required) - Unique identifier");
        inputSchema.put("name", "string (required) - Name of the data");
        inputSchema.put("description", "string (optional) - Description of the data");
        inputSchema.put("value", "string (optional) - Value of the data");
        inputSchema.put("category", "string (optional) - Category for grouping");
        inputSchema.putObject("metadata").put("description", "object (optional) - Additional metadata");
        inputSchema.put("ttlMinutes", "number (optional) - TTL in minutes");
        
        ObjectNode outputSchema = metadata.putObject("outputSchema");
        outputSchema.put("success", "boolean - Operation result");
        outputSchema.put("message", "string - Success message");
        outputSchema.put("error", "string - Error message (if failed)");
        outputSchema.put("id", "string - Created data ID");
        outputSchema.put("key", "string - Redis key used");
        outputSchema.put("category", "string - Data category");
        outputSchema.put("createdAt", "string - Creation timestamp");
        outputSchema.put("ttlMinutes", "number - TTL if specified");
        
        return metadata;
    }
}