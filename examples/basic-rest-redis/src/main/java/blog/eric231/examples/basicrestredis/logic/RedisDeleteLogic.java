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

import java.util.Set;

/**
 * Domain Logic for deleting Redis data using @DL pattern.
 * Handles removal of data from Redis with cleanup of indices.
 */
@Slf4j
@Component
@DL(value = "redis-delete", description = "Delete data from Redis", version = "1.0")
public class RedisDeleteLogic {
    
    private final RedisAdapter redisAdapter;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisDeleteLogic(RedisAdapter redisAdapter, ObjectMapper objectMapper) {
        this.redisAdapter = redisAdapter;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle Redis delete operation
     * 
     * Expected input format:
     * {
     *   "id": "unique-id",           // for single delete
     *   "category": "category-name", // for category delete
     *   "operation": "single|category|archive", // operation type
     *   "force": true,               // force delete even if data is archived
     *   "cleanup": true              // cleanup related indices (default: true)
     * }
     */
    public JsonNode handle(JsonNode input) {
        ObjectNode result = objectMapper.createObjectNode();
        
        try {
            if (!validateInput(input)) {
                result.put("success", false);
                result.put("error", "Invalid input");
                return result;
            }
            
            String operation = input.has("operation") ? input.get("operation").asText() : "single";
            boolean force = input.has("force") && input.get("force").asBoolean();
            boolean cleanup = !input.has("cleanup") || input.get("cleanup").asBoolean();
            
            switch (operation.toLowerCase()) {
                case "single":
                    return handleSingleDelete(input, force, cleanup);
                case "category":
                    return handleCategoryDelete(input, force, cleanup);
                case "archive":
                    return handleArchiveOperation(input);
                default:
                    result.put("success", false);
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }
            
        } catch (Exception e) {
            log.error("Error deleting Redis data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Failed to delete data: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Handle single item delete
     */
    private JsonNode handleSingleDelete(JsonNode input, boolean force, boolean cleanup) {
        ObjectNode result = objectMapper.createObjectNode();
        
        String id = input.get("id").asText();
        String redisKey = "data:" + id;
        
        if (!redisAdapter.exists(redisKey)) {
            result.put("success", false);
            result.put("error", "Data not found");
            result.put("id", id);
            return result;
        }
        
        // Get existing data for cleanup purposes
        RedisData existingData = null;
        if (cleanup) {
            existingData = redisAdapter.get(redisKey, RedisData.class);
        }
        
        // Check if data is archived and force is not set
        if (existingData != null && "archived".equals(existingData.getStatus()) && !force) {
            result.put("success", false);
            result.put("error", "Data is archived. Use force=true to delete archived data");
            result.put("id", id);
            result.put("status", existingData.getStatus());
            return result;
        }
        
        // Delete the main data
        boolean deleted = redisAdapter.delete(redisKey);
        if (!deleted) {
            result.put("success", false);
            result.put("error", "Failed to delete data from Redis");
            result.put("id", id);
            return result;
        }
        
        // Cleanup indices if requested and we have the data
        if (cleanup && existingData != null) {
            cleanupIndices(id, existingData);
        }
        
        result.put("success", true);
        result.put("message", "Data deleted successfully");
        result.put("id", id);
        result.put("key", redisKey);
        result.put("cleanupPerformed", cleanup);
        
        log.info("Successfully deleted Redis data with id: {}", id);
        return result;
    }
    
    /**
     * Handle category-based delete
     */
    private JsonNode handleCategoryDelete(JsonNode input, boolean force, boolean cleanup) {
        ObjectNode result = objectMapper.createObjectNode();
        
        String category = input.get("category").asText();
        String categoryKey = "category:" + category;
        
        Set<Object> dataIds = redisAdapter.smembers(categoryKey);
        if (dataIds == null || dataIds.isEmpty()) {
            result.put("success", true);
            result.put("message", "No data found in category: " + category);
            result.put("category", category);
            result.put("deletedCount", 0);
            return result;
        }
        
        ArrayNode deletedItems = result.putArray("deletedItems");
        ArrayNode failedItems = result.putArray("failedItems");
        int successCount = 0;
        
        for (Object idObj : dataIds) {
            String id = idObj.toString();
            String redisKey = "data:" + id;
            
            try {
                RedisData existingData = null;
                if (cleanup) {
                    existingData = redisAdapter.get(redisKey, RedisData.class);
                }
                
                // Check archived status
                if (existingData != null && "archived".equals(existingData.getStatus()) && !force) {
                    ObjectNode failedItem = objectMapper.createObjectNode();
                    failedItem.put("id", id);
                    failedItem.put("reason", "archived (use force=true to delete)");
                    failedItems.add(failedItem);
                    continue;
                }
                
                // Delete the item
                if (redisAdapter.delete(redisKey)) {
                    if (cleanup && existingData != null) {
                        cleanupIndices(id, existingData);
                    }
                    
                    deletedItems.add(id);
                    successCount++;
                } else {
                    ObjectNode failedItem = objectMapper.createObjectNode();
                    failedItem.put("id", id);
                    failedItem.put("reason", "delete operation failed");
                    failedItems.add(failedItem);
                }
                
            } catch (Exception e) {
                ObjectNode failedItem = objectMapper.createObjectNode();
                failedItem.put("id", id);
                failedItem.put("reason", "exception: " + e.getMessage());
                failedItems.add(failedItem);
                log.warn("Failed to delete data with id: {}, error: {}", id, e.getMessage());
            }
        }
        
        // Clean up the category index if all items were processed
        if (successCount > 0 || failedItems.size() > 0) {
            redisAdapter.delete(categoryKey);
        }
        
        result.put("success", true);
        result.put("message", "Category delete operation completed");
        result.put("category", category);
        result.put("totalItems", dataIds.size());
        result.put("deletedCount", successCount);
        result.put("failedCount", failedItems.size());
        result.put("cleanupPerformed", cleanup);
        
        log.info("Deleted {} items from category: {}, failed: {}", successCount, category, failedItems.size());
        return result;
    }
    
    /**
     * Handle archive operation (soft delete)
     */
    private JsonNode handleArchiveOperation(JsonNode input) {
        ObjectNode result = objectMapper.createObjectNode();
        
        String id = input.get("id").asText();
        String redisKey = "data:" + id;
        
        if (!redisAdapter.exists(redisKey)) {
            result.put("success", false);
            result.put("error", "Data not found");
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
        
        // Check if already archived
        if ("archived".equals(existingData.getStatus())) {
            result.put("success", true);
            result.put("message", "Data is already archived");
            result.put("id", id);
            result.put("status", "archived");
            return result;
        }
        
        // Archive the data
        existingData.archive(); // This sets status to "archived" and updates timestamp
        
        // Save back to Redis
        redisAdapter.set(redisKey, existingData);
        
        result.put("success", true);
        result.put("message", "Data archived successfully");
        result.put("id", id);
        result.put("previousStatus", existingData.getStatus());
        result.put("newStatus", "archived");
        result.put("archivedAt", existingData.getUpdatedAt().toString());
        
        log.info("Successfully archived Redis data with id: {}", id);
        return result;
    }
    
    /**
     * Clean up related indices
     */
    private void cleanupIndices(String id, RedisData data) {
        try {
            // Remove from category index
            if (data.getCategory() != null) {
                String categoryKey = "category:" + data.getCategory();
                redisAdapter.srem(categoryKey, id);
            }
            
            // Remove from time index
            if (data.getCreatedAt() != null) {
                String timeIndexKey = "created_time";
                String timeEntry = id + ":" + data.getCreatedAt();
                redisAdapter.srem(timeIndexKey, timeEntry);
            }
            
            log.debug("Cleaned up indices for data id: {}", id);
        } catch (Exception e) {
            log.warn("Failed to cleanup indices for id: {}, error: {}", id, e.getMessage());
        }
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
            case "archive":
                return input.has("id") && !input.get("id").asText().trim().isEmpty();
            case "category":
                return input.has("category") && !input.get("category").asText().trim().isEmpty();
            default:
                return false;
        }
    }
    
    /**
     * Get operation metadata
     */
    public JsonNode getMetadata() {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("operation", "redis-delete");
        metadata.put("description", "Delete data from Redis with cleanup options");
        metadata.put("version", "1.0");
        
        ObjectNode inputSchema = metadata.putObject("inputSchema");
        inputSchema.put("id", "string (required for single/archive) - Data ID to delete");
        inputSchema.put("category", "string (required for category) - Category to delete");
        inputSchema.put("operation", "string (optional) - Operation type: single|category|archive");
        inputSchema.put("force", "boolean (optional) - Force delete archived data");
        inputSchema.put("cleanup", "boolean (optional) - Cleanup related indices (default: true)");
        
        ObjectNode outputSchema = metadata.putObject("outputSchema");
        outputSchema.put("success", "boolean - Operation result");
        outputSchema.put("message", "string - Result message");
        outputSchema.put("error", "string - Error message (if failed)");
        outputSchema.put("id", "string - Deleted/archived data ID");
        outputSchema.put("deletedCount", "number - Number of items deleted (for category operation)");
        outputSchema.put("failedCount", "number - Number of items that failed to delete");
        outputSchema.putArray("deletedItems").add("array - List of successfully deleted IDs");
        outputSchema.putArray("failedItems").add("array - List of items that failed to delete");
        
        return metadata;
    }
}