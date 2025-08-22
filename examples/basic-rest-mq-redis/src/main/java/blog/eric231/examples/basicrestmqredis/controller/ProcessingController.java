package blog.eric231.examples.basicrestmqredis.controller;

import blog.eric231.examples.basicrestmqredis.logic.RequestProcessorLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * REST Controller for handling processing requests.
 * 
 * This controller exposes endpoints for different types of processing operations
 * that will be handled by the RequestProcessorLogic domain logic component.
 * 
 * All endpoints require Basic Authentication.
 */
@RestController
@RequestMapping("/api/v1/process")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final RequestProcessorLogic requestProcessorLogic;

    @Autowired
    public ProcessingController(RequestProcessorLogic requestProcessorLogic) {
        this.requestProcessorLogic = requestProcessorLogic;
    }

    /**
     * General processing endpoint that accepts any JSON payload
     * 
     * @param payload The JSON payload to process
     * @param principal The authenticated user
     * @return Processing result
     */
    @PostMapping("/general")
    public ResponseEntity<JsonNode> processGeneral(@RequestBody Map<String, Object> payload, 
                                                   Principal principal) {
        logger.info("Received general processing request from user: {}", principal.getName());
        
        try {
            // Create input node with user context
            ObjectNode input = mapper.valueToTree(payload);
            input.put("authenticatedUser", principal.getName());
            input.put("requestTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            input.put("endpoint", "/api/v1/process/general");
            
            // Process through domain logic
            JsonNode result = requestProcessorLogic.handle(input);
            
            logger.info("General processing completed successfully for user: {}", principal.getName());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in general processing for user {}: {}", principal.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("General processing failed", e.getMessage()));
        }
    }

    /**
     * Store operation endpoint
     * 
     * @param payload The data to store
     * @param principal The authenticated user
     * @return Processing result
     */
    @PostMapping("/store")
    public ResponseEntity<JsonNode> processStore(@RequestBody Map<String, Object> payload, 
                                                Principal principal) {
        logger.info("Received store request from user: {}", principal.getName());
        
        try {
            // Create input node with store operation
            ObjectNode input = mapper.valueToTree(payload);
            input.put("operation", "store");
            input.put("authenticatedUser", principal.getName());
            input.put("requestTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            input.put("endpoint", "/api/v1/process/store");
            
            // Process through domain logic
            JsonNode result = requestProcessorLogic.handle(input);
            
            logger.info("Store processing completed successfully for user: {}", principal.getName());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in store processing for user {}: {}", principal.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Store processing failed", e.getMessage()));
        }
    }

    /**
     * Retrieve operation endpoint
     * 
     * @param requestId Optional request ID to retrieve data for
     * @param storageId Optional storage ID to retrieve data for
     * @param principal The authenticated user
     * @return Processing result
     */
    @GetMapping("/retrieve")
    public ResponseEntity<JsonNode> processRetrieve(@RequestParam(required = false) String requestId,
                                                   @RequestParam(required = false) String storageId,
                                                   Principal principal) {
        logger.info("Received retrieve request from user: {} for requestId: {}, storageId: {}", 
                   principal.getName(), requestId, storageId);
        
        try {
            // Create input node with retrieve operation
            ObjectNode input = mapper.createObjectNode();
            input.put("operation", "retrieve");
            input.put("authenticatedUser", principal.getName());
            input.put("requestTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            input.put("endpoint", "/api/v1/process/retrieve");
            
            if (requestId != null) {
                input.put("requestId", requestId);
            }
            if (storageId != null) {
                input.put("storageId", storageId);
            }
            
            // Process through domain logic
            JsonNode result = requestProcessorLogic.handle(input);
            
            logger.info("Retrieve processing completed successfully for user: {}", principal.getName());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in retrieve processing for user {}: {}", principal.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Retrieve processing failed", e.getMessage()));
        }
    }

    /**
     * Update operation endpoint
     * 
     * @param payload The data to update
     * @param principal The authenticated user
     * @return Processing result
     */
    @PutMapping("/update")
    public ResponseEntity<JsonNode> processUpdate(@RequestBody Map<String, Object> payload, 
                                                 Principal principal) {
        logger.info("Received update request from user: {}", principal.getName());
        
        try {
            // Create input node with update operation
            ObjectNode input = mapper.valueToTree(payload);
            input.put("operation", "update");
            input.put("authenticatedUser", principal.getName());
            input.put("requestTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            input.put("endpoint", "/api/v1/process/update");
            
            // Process through domain logic
            JsonNode result = requestProcessorLogic.handle(input);
            
            logger.info("Update processing completed successfully for user: {}", principal.getName());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in update processing for user {}: {}", principal.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Update processing failed", e.getMessage()));
        }
    }

    /**
     * Delete operation endpoint
     * 
     * @param requestId Optional request ID to delete data for
     * @param storageId Optional storage ID to delete data for
     * @param principal The authenticated user
     * @return Processing result
     */
    @DeleteMapping("/delete")
    public ResponseEntity<JsonNode> processDelete(@RequestParam(required = false) String requestId,
                                                 @RequestParam(required = false) String storageId,
                                                 Principal principal) {
        logger.info("Received delete request from user: {} for requestId: {}, storageId: {}", 
                   principal.getName(), requestId, storageId);
        
        try {
            // Create input node with delete operation
            ObjectNode input = mapper.createObjectNode();
            input.put("operation", "delete");
            input.put("authenticatedUser", principal.getName());
            input.put("requestTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            input.put("endpoint", "/api/v1/process/delete");
            
            if (requestId != null) {
                input.put("requestId", requestId);
            }
            if (storageId != null) {
                input.put("storageId", storageId);
            }
            
            // Process through domain logic
            JsonNode result = requestProcessorLogic.handle(input);
            
            logger.info("Delete processing completed successfully for user: {}", principal.getName());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in delete processing for user {}: {}", principal.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Delete processing failed", e.getMessage()));
        }
    }

    /**
     * Health check endpoint for the processing service
     * 
     * @param authentication The authentication object
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<JsonNode> health(Authentication authentication) {
        logger.debug("Health check requested");
        
        try {
            ObjectNode health = mapper.createObjectNode();
            health.put("status", "UP");
            health.put("service", "basic-rest-mq-redis");
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            health.put("version", "1.0.0");
            
            if (authentication != null) {
                health.put("authenticated", true);
                health.put("user", authentication.getName());
            } else {
                health.put("authenticated", false);
            }
            
            // Test domain logic availability
            JsonNode metadata = requestProcessorLogic.getMetadata();
            health.set("domainLogicMetadata", metadata);
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            ObjectNode error = mapper.createObjectNode();
            error.put("status", "DOWN");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
    }

    /**
     * Create standardized error response
     */
    private ObjectNode createErrorResponse(String message, String details) {
        ObjectNode error = mapper.createObjectNode();
        error.put("status", "error");
        error.put("message", message);
        error.put("details", details);
        error.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        error.put("service", "basic-rest-mq-redis");
        return error;
    }
}
