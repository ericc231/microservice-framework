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
 * Domain Logic for updating Redis data using @DL pattern.
 * Handles modification of existing data in Redis with version control.
 */
@Slf4j
@Component
@DL(value = "redis-update", description = "Update existing data in Redis", version = "1.0")
public class RedisUpdateLogic {
    
    private final RedisAdapter redisAdapter;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisUpdateLogic(RedisAdapter redisAdapter, ObjectMapper objectMapper) {
        this.redisAdapter = redisAdapter;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle Redis update operation
     * 
     * Expected input format:
     * {
     *   "id": "unique-id",
     *   "name": "new name",          // optional
     *   "description": "new desc",   // optional
     *   "value": "new value",        // optional
     *   "category": "new category",  // optional
     *   "status": "active|inactive|archived", // optional
     *   "metadata": {                // optional - will merge with existing
     *     "key1": "value1",
     *     "key2": "value2"
     *   },
     *   "version": 1,                // optional - for optimistic locking
     *   "ttlMinutes": 60,           // optional - update TTL
     *   "operation": "update|patch" // update = replace, patch = merge (default: patch)
     * }
     */
    public JsonNode handle(JsonNode input) {
        ObjectNode result = objectMapper.createObjectNode();
        
        try {
            if (!validateInput(input)) {
                result.put("success", false);
                result.put("error", "Invalid input: missing required fields");
                return result;
            }
            
            String id = input.get("id").asText();
            String redisKey = "data:" + id;
            
            // Check if data exists
            if (!redisAdapter.exists(redisKey)) {
                result.put("success", false);
                result.put("error", "Data with id '" + id + "' not found");
                result.put("id", id);
                return result;
            }
            
            // Get existing data
            RedisData existingData = redisAdapter.get(redisKey, RedisData.class);
            if (existingData == null) {
                result.put("success", false);
                result.put("error", "Failed to retrieve existing data");
                result.put("id", id);
                return result;
            }
            
            // Check version for optimistic locking
            if (input.has("version")) {
                Long inputVersion = input.get("version").asLong();
                if (!existingData.getVersion().equals(inputVersion)) {
                    result.put("success", false);
                    result.put("error", "Version mismatch. Expected: " + existingData.getVersion() + ", got: " + inputVersion);
                    result.put("currentVersion", existingData.getVersion());
                    return result;
                }
            }
            
            // Determine operation type
            String operation = input.has("operation") ? input.get("operation").asText() : "patch";
            RedisData updatedData;
            
            if ("update".equals(operation)) {
                // Full update - replace with new data
                updatedData = createUpdatedDataFromInput(input, existingData.getCreatedAt());
                updatedData.setVersion(existingData.getVersion() + 1);
            } else {
                // Patch update - merge changes
                updatedData = patchExistingData(existingData, input);
            }
            
            // Handle category change
            String oldCategory = existingData.getCategory();
            String newCategory = updatedData.getCategory();
            if (!oldCategory.equals(newCategory)) {
                // Remove from old category
                String oldCategoryKey = "category:" + oldCategory;
                redisAdapter.srem(oldCategoryKey, id);
                
                // Add to new category
                String newCategoryKey = "category:" + newCategory;
                redisAdapter.sadd(newCategoryKey, id);
                
                log.info("Moved data {} from category '{}' to '{}'", id, oldCategory, newCategory);
            }
            
            // Store updated data
            if (input.has("ttlMinutes")) {
                int ttlMinutes = input.get("ttlMinutes").asInt();
                Duration ttl = Duration.ofMinutes(ttlMinutes);
                redisAdapter.set(redisKey, updatedData, ttl);
                result.put("ttlMinutes", ttlMinutes);
                log.info("Updated Redis data with id: {} and new TTL: {} minutes", id, ttlMinutes);
            } else {
                // Keep existing TTL if any
                Duration existingTtl = redisAdapter.getTtl(redisKey);
                if (existingTtl != null && existingTtl.getSeconds() > 0) {
                    redisAdapter.set(redisKey, updatedData, existingTtl);
                } else {
                    redisAdapter.set(redisKey, updatedData);
                }
                log.info("Updated Redis data with id: {} (keeping existing TTL)", id);
            }
            
            result.put("success", true);
            result.put("message", "Data updated successfully");
            result.put("id", id);
            result.put("operation", operation);
            result.put("previousVersion", existingData.getVersion());
            result.put("newVersion", updatedData.getVersion());
            result.put("updatedAt", updatedData.getUpdatedAt().toString());
            result.set("updatedData", objectMapper.valueToTree(updatedData));
            
        } catch (Exception e) {
            log.error("Error updating Redis data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Failed to update data: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Create fully updated data from input (full replacement)
     */
    private RedisData createUpdatedDataFromInput(JsonNode input, LocalDateTime originalCreatedAt) {
        RedisData redisData = new RedisData();
        
        redisData.setId(input.get("id").asText());
        redisData.setName(input.has("name") ? input.get("name").asText() : "");
        redisData.setDescription(input.has("description") ? input.get("description").asText() : "");
        redisData.setValue(input.has("value") ? input.get("value").asText() : "");
        redisData.setCategory(input.has("category") ? input.get("category").asText() : "default");
        redisData.setStatus(input.has("status") ? input.get("status").asText() : "active");
        redisData.setCreatedAt(originalCreatedAt);
        redisData.setUpdatedAt(LocalDateTime.now());
        redisData.setVersion(1L); // Will be updated by caller
        
        // Handle metadata - replace completely
        if (input.has("metadata") && input.get("metadata").isObject()) {
            JsonNode metadata = input.get("metadata");
            metadata.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = metadata.get(fieldName);
                redisData.addMetadata(fieldName, convertJsonNodeToObject(fieldValue));
            });
        }
        
        return redisData;
    }
    
    /**
     * Patch existing data with changes from input
     */
    private RedisData patchExistingData(RedisData existingData, JsonNode input) {
        // Clone existing data
        RedisData updatedData = new RedisData();
        updatedData.setId(existingData.getId());
        updatedData.setName(existingData.getName());
        updatedData.setDescription(existingData.getDescription());
        updatedData.setValue(existingData.getValue());
        updatedData.setCategory(existingData.getCategory());
        updatedData.setStatus(existingData.getStatus());
        updatedData.setCreatedAt(existingData.getCreatedAt());
        updatedData.setVersion(existingData.getVersion());
        
        // Copy existing metadata
        if (existingData.getMetadata() != null) {
            updatedData.setMetadata(new java.util.HashMap<>(existingData.getMetadata()));
        }
        
        // Apply patches
        if (input.has("name")) updatedData.setName(input.get("name").asText());
        if (input.has("description")) updatedData.setDescription(input.get("description").asText());
        if (input.has("value")) updatedData.setValue(input.get("value").asText());
        if (input.has("category")) updatedData.setCategory(input.get("category").asText());
        if (input.has("status")) updatedData.setStatus(input.get("status").asText());
        
        // Merge metadata
        if (input.has("metadata") && input.get("metadata").isObject()) {
            JsonNode metadata = input.get("metadata");
            metadata.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = metadata.get(fieldName);
                updatedData.addMetadata(fieldName, convertJsonNodeToObject(fieldValue));
            });
        }
        
        // Update modification info
        updatedData.setUpdatedAt(LocalDateTime.now());
        updatedData.setVersion(existingData.getVersion() + 1);
        
        return updatedData;
    }
    
    /**
     * Convert JsonNode to appropriate Java object
     */
    private Object convertJsonNodeToObject(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.asDouble();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else {
            return node.toString();
        }
    }
    
    /**
     * Validate input data
     */
    public boolean validateInput(JsonNode input) {
        if (input == null || !input.isObject()) {
            return false;
        }
        
        // ID is required
        if (!input.has("id") || input.get("id").asText().trim().isEmpty()) {
            log.warn("Missing or empty required field: id");
            return false;
        }
        
        // At least one field to update should be present
        String[] updateFields = {"name", "description", "value", "category", "status", "metadata", "ttlMinutes"};
        for (String field : updateFields) {
            if (input.has(field)) {
                return true;
            }
        }
        
        log.warn("No update fields provided");
        return false;
    }
    
    /**
     * Get operation metadata
     */
    public JsonNode getMetadata() {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("operation", "redis-update");
        metadata.put("description", "Update existing data in Redis with version control");
        metadata.put("version", "1.0");
        
        ObjectNode inputSchema = metadata.putObject("inputSchema");
        inputSchema.put("id", "string (required) - Data ID to update");
        inputSchema.put("name", "string (optional) - New name");
        inputSchema.put("description", "string (optional) - New description");
        inputSchema.put("value", "string (optional) - New value");
        inputSchema.put("category", "string (optional) - New category");
        inputSchema.put("status", "string (optional) - New status (active|inactive|archived)");
        inputSchema.putObject("metadata").put("description", "object (optional) - Metadata to merge");
        inputSchema.put("version", "number (optional) - Version for optimistic locking");
        inputSchema.put("ttlMinutes", "number (optional) - New TTL in minutes");
        inputSchema.put("operation", "string (optional) - update|patch (default: patch)");
        
        ObjectNode outputSchema = metadata.putObject("outputSchema");
        outputSchema.put("success", "boolean - Operation result");
        outputSchema.put("message", "string - Success message");
        outputSchema.put("error", "string - Error message (if failed)");
        outputSchema.put("id", "string - Updated data ID");
        outputSchema.put("operation", "string - Operation performed");
        outputSchema.put("previousVersion", "number - Previous version number");
        outputSchema.put("newVersion", "number - New version number");
        outputSchema.put("updatedAt", "string - Update timestamp");
        outputSchema.putObject("updatedData").put("description", "object - Updated data");
        
        return metadata;
    }
}