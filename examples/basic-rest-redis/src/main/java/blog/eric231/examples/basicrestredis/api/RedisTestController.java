package blog.eric231.examples.basicrestredis.api;

import blog.eric231.examples.basicrestredis.logic.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for testing Redis CRUD operations using @DL domain logic.
 * This controller provides convenient endpoints for testing the Redis functionality.
 */
@Slf4j
@RestController
@RequestMapping("/test/redis")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RedisTestController {
    
    private final RedisCreateLogic redisCreateLogic;
    private final RedisReadLogic redisReadLogic;
    private final RedisUpdateLogic redisUpdateLogic;
    private final RedisDeleteLogic redisDeleteLogic;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisTestController(
            RedisCreateLogic redisCreateLogic,
            RedisReadLogic redisReadLogic,
            RedisUpdateLogic redisUpdateLogic,
            RedisDeleteLogic redisDeleteLogic,
            ObjectMapper objectMapper) {
        this.redisCreateLogic = redisCreateLogic;
        this.redisReadLogic = redisReadLogic;
        this.redisUpdateLogic = redisUpdateLogic;
        this.redisDeleteLogic = redisDeleteLogic;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create data in Redis
     * POST /test/redis/create
     */
    @PostMapping("/create")
    public ResponseEntity<JsonNode> createData(@RequestBody JsonNode request) {
        try {
            log.info("Creating Redis data: {}", request);
            JsonNode result = redisCreateLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error in create operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Read data from Redis
     * POST /test/redis/read
     */
    @PostMapping("/read")
    public ResponseEntity<JsonNode> readData(@RequestBody JsonNode request) {
        try {
            log.info("Reading Redis data: {}", request);
            JsonNode result = redisReadLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error in read operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get single item by ID
     * GET /test/redis/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> getById(@PathVariable String id) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("id", id);
            request.put("operation", "single");
            request.put("includeMetadata", true);
            
            log.info("Reading Redis data by ID: {}", id);
            JsonNode result = redisReadLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error in get by ID operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get items by category
     * GET /test/redis/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<JsonNode> getByCategory(@PathVariable String category) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("category", category);
            request.put("operation", "category");
            request.put("includeMetadata", false);
            
            log.info("Reading Redis data by category: {}", category);
            JsonNode result = redisReadLogic.handle(request);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in get by category operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get all data
     * GET /test/redis/all
     */
    @GetMapping("/all")
    public ResponseEntity<JsonNode> getAllData(@RequestParam(defaultValue = "false") boolean includeMetadata) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("operation", "all");
            request.put("includeMetadata", includeMetadata);
            
            log.info("Reading all Redis data");
            JsonNode result = redisReadLogic.handle(request);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in get all operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Update data in Redis
     * PUT /test/redis/update
     */
    @PutMapping("/update")
    public ResponseEntity<JsonNode> updateData(@RequestBody JsonNode request) {
        try {
            log.info("Updating Redis data: {}", request);
            JsonNode result = redisUpdateLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error in update operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Delete data from Redis
     * DELETE /test/redis/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<JsonNode> deleteData(@RequestBody JsonNode request) {
        try {
            log.info("Deleting Redis data: {}", request);
            JsonNode result = redisDeleteLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error in delete operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Delete single item by ID
     * DELETE /test/redis/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<JsonNode> deleteById(@PathVariable String id) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("id", id);
            request.put("operation", "single");
            
            log.info("Deleting Redis data by ID: {}", id);
            JsonNode result = redisDeleteLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error in delete by ID operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Archive (soft delete) data
     * POST /test/redis/archive/{id}
     */
    @PostMapping("/archive/{id}")
    public ResponseEntity<JsonNode> archiveData(@PathVariable String id) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("id", id);
            request.put("operation", "archive");
            
            log.info("Archiving Redis data with ID: {}", id);
            JsonNode result = redisDeleteLogic.handle(request);
            
            if (result.has("success") && result.get("success").asBoolean()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error in archive operation", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("success", false);
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get operation metadata
     * GET /test/redis/metadata/{operation}
     */
    @GetMapping("/metadata/{operation}")
    public ResponseEntity<JsonNode> getOperationMetadata(@PathVariable String operation) {
        try {
            JsonNode metadata;
            
            switch (operation.toLowerCase()) {
                case "create":
                    metadata = redisCreateLogic.getMetadata();
                    break;
                case "read":
                    metadata = redisReadLogic.getMetadata();
                    break;
                case "update":
                    metadata = redisUpdateLogic.getMetadata();
                    break;
                case "delete":
                    metadata = redisDeleteLogic.getMetadata();
                    break;
                default:
                    ObjectNode error = objectMapper.createObjectNode();
                    error.put("error", "Unknown operation: " + operation);
                    error.put("availableOperations", "create, read, update, delete");
                    return ResponseEntity.badRequest().body(error);
            }
            
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("Error getting metadata for operation: {}", operation, e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Failed to get metadata: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Health check endpoint
     * GET /test/redis/health
     */
    @GetMapping("/health")
    public ResponseEntity<JsonNode> healthCheck() {
        ObjectNode health = objectMapper.createObjectNode();
        health.put("status", "UP");
        health.put("service", "basic-rest-redis");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        
        ObjectNode operations = health.putObject("operations");
        operations.put("create", "redis-create");
        operations.put("read", "redis-read");
        operations.put("update", "redis-update");
        operations.put("delete", "redis-delete");
        
        return ResponseEntity.ok(health);
    }
}