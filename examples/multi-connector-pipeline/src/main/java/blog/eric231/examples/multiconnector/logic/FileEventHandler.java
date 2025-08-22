package blog.eric231.examples.multiconnector.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File Event Handler - Domain Logic for handling file system events
 * 
 * This domain logic processes file events triggered by file monitoring systems
 * and routes them to appropriate handlers based on event type and file patterns.
 */
@Component
@DL(name = "FileEventHandler", description = "Handles file system events and routing")
public class FileEventHandler implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(FileEventHandler.class);

    private final RedisAdapter redisAdapter;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final FilePipelineProcessor pipelineProcessor;

    @Autowired
    public FileEventHandler(RedisAdapter redisAdapter,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper,
                           FilePipelineProcessor pipelineProcessor) {
        this.redisAdapter = redisAdapter;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.pipelineProcessor = pipelineProcessor;
    }

    @Override
    public JsonNode handle(JsonNode input) {
        try {
            String eventType = input.get("eventType").asText();
            String filePath = input.get("filePath").asText();
            
            logger.info("Processing file event: {} for file: {}", eventType, filePath);
            
            // Store event in Redis for audit trail
            storeEvent(input);
            
            switch (eventType.toLowerCase()) {
                case "created":
                    return handleFileCreated(input);
                case "modified":
                    return handleFileModified(input);
                case "deleted":
                    return handleFileDeleted(input);
                case "moved":
                    return handleFileMoved(input);
                default:
                    return handleUnknownEvent(input);
            }
            
        } catch (Exception e) {
            logger.error("Error processing file event: {}", e.getMessage(), e);
            return createErrorResponse("Event processing failed", e.getMessage());
        }
    }

    /**
     * Handle file created events
     */
    private JsonNode handleFileCreated(JsonNode input) {
        String filePath = input.get("filePath").asText();
        String fileName = getFileName(filePath);
        
        logger.info("File created: {}", filePath);
        
        // Check if this file matches any processing patterns
        if (shouldTriggerPipeline(fileName)) {
            // Trigger automatic pipeline processing
            Map<String, Object> pipelineInput = new HashMap<>();
            pipelineInput.put("operation", "process");
            pipelineInput.put("sourceType", "local");
            pipelineInput.put("remotePath", filePath);
            pipelineInput.put("trigger", "file_created");
            
            JsonNode pipelineInputNode = objectMapper.valueToTree(pipelineInput);
            JsonNode pipelineResult = pipelineProcessor.handle(pipelineInputNode);
            
            // Send notification about auto-triggered pipeline
            sendEventNotification("auto_pipeline_triggered", input, pipelineResult);
            
            return createSuccessResponse("file_created", "Pipeline auto-triggered", pipelineResult);
        }
        
        // Just log and notify about file creation
        sendEventNotification("file_created", input, null);
        
        return createSuccessResponse("file_created", "File creation processed", null);
    }

    /**
     * Handle file modified events
     */
    private JsonNode handleFileModified(JsonNode input) {
        String filePath = input.get("filePath").asText();
        
        logger.info("File modified: {}", filePath);
        
        // Check if this is a monitored file that requires reprocessing
        if (isMonitoredFile(filePath)) {
            // Update monitoring status
            updateMonitoringStatus(filePath, "modified");
            
            // Optionally trigger reprocessing
            if (shouldReprocess(filePath)) {
                return triggerReprocessing(input);
            }
        }
        
        sendEventNotification("file_modified", input, null);
        
        return createSuccessResponse("file_modified", "File modification processed", null);
    }

    /**
     * Handle file deleted events
     */
    private JsonNode handleFileDeleted(JsonNode input) {
        String filePath = input.get("filePath").asText();
        
        logger.info("File deleted: {}", filePath);
        
        // Clean up any related cache entries
        cleanupFileReferences(filePath);
        
        // Update monitoring status
        updateMonitoringStatus(filePath, "deleted");
        
        sendEventNotification("file_deleted", input, null);
        
        return createSuccessResponse("file_deleted", "File deletion processed", null);
    }

    /**
     * Handle file moved events
     */
    private JsonNode handleFileMoved(JsonNode input) {
        String oldPath = input.get("filePath").asText();
        String newPath = input.has("newPath") ? input.get("newPath").asText() : "";
        
        logger.info("File moved from {} to {}", oldPath, newPath);
        
        // Update references
        updateFileReferences(oldPath, newPath);
        
        sendEventNotification("file_moved", input, null);
        
        return createSuccessResponse("file_moved", "File move processed", null);
    }

    /**
     * Handle unknown event types
     */
    private JsonNode handleUnknownEvent(JsonNode input) {
        String eventType = input.get("eventType").asText();
        
        logger.warn("Unknown event type: {}", eventType);
        
        sendEventNotification("unknown_event", input, null);
        
        return createSuccessResponse("unknown_event", "Unknown event logged", null);
    }

    /**
     * Check if file should trigger automatic pipeline processing
     */
    private boolean shouldTriggerPipeline(String fileName) {
        // Define patterns that should trigger automatic processing
        String[] triggerPatterns = {
            ".*\\.csv$",      // CSV files
            ".*\\.xml$",      // XML files
            ".*\\.json$",     // JSON files
            ".*_incoming\\..*", // Files with "_incoming" in name
            "batch_.*"        // Files starting with "batch_"
        };
        
        for (String pattern : triggerPatterns) {
            if (fileName.matches(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if file is being monitored
     */
    private boolean isMonitoredFile(String filePath) {
        try {
            String key = "monitored_file:" + filePath;
            return redisAdapter.get(key, String.class) != null;
        } catch (Exception e) {
            logger.error("Error checking if file is monitored: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if file should be reprocessed on modification
     */
    private boolean shouldReprocess(String filePath) {
        try {
            String key = "reprocess_config:" + filePath;
            String config = redisAdapter.get(key, String.class);
            return "true".equals(config);
        } catch (Exception e) {
            logger.error("Error checking reprocess config: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Trigger reprocessing of a modified file
     */
    private JsonNode triggerReprocessing(JsonNode input) {
        try {
            Map<String, Object> pipelineInput = new HashMap<>();
            pipelineInput.put("operation", "process");
            pipelineInput.put("sourceType", "local");
            pipelineInput.put("remotePath", input.get("filePath").asText());
            pipelineInput.put("trigger", "file_modified_reprocess");
            
            JsonNode pipelineInputNode = objectMapper.valueToTree(pipelineInput);
            return pipelineProcessor.handle(pipelineInputNode);
            
        } catch (Exception e) {
            logger.error("Error triggering reprocessing: {}", e.getMessage(), e);
            return createErrorResponse("Reprocessing failed", e.getMessage());
        }
    }

    /**
     * Store event for audit trail
     */
    private void storeEvent(JsonNode event) {
        try {
            String eventId = System.currentTimeMillis() + "_" + event.hashCode();
            String key = "file_event:" + eventId;
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventId", eventId);
            eventData.put("timestamp", System.currentTimeMillis());
            eventData.put("event", event);
            
            redisAdapter.set(key, eventData, Duration.ofDays(30));
            
            // Also add to event log list
            redisAdapter.listPush("file_events_log", eventId);
            
        } catch (Exception e) {
            logger.error("Error storing event: {}", e.getMessage(), e);
        }
    }

    /**
     * Update monitoring status for a file
     */
    private void updateMonitoringStatus(String filePath, String status) {
        try {
            String key = "file_status:" + filePath;
            
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("filePath", filePath);
            statusData.put("status", status);
            statusData.put("lastUpdated", System.currentTimeMillis());
            
            redisAdapter.set(key, statusData, Duration.ofDays(7));
            
        } catch (Exception e) {
            logger.error("Error updating monitoring status: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up cache references for deleted file
     */
    private void cleanupFileReferences(String filePath) {
        try {
            // List of key patterns to clean up
            String[] keyPatterns = {
                "file_status:" + filePath,
                "monitored_file:" + filePath,
                "reprocess_config:" + filePath
            };
            
            for (String key : keyPatterns) {
                redisAdapter.delete(key);
            }
            
        } catch (Exception e) {
            logger.error("Error cleaning up file references: {}", e.getMessage(), e);
        }
    }

    /**
     * Update file references when file is moved
     */
    private void updateFileReferences(String oldPath, String newPath) {
        try {
            // Move monitoring status
            String oldStatusKey = "file_status:" + oldPath;
            String newStatusKey = "file_status:" + newPath;
            
            Map<String, Object> statusData = redisAdapter.get(oldStatusKey, Map.class);
            if (statusData != null) {
                statusData.put("filePath", newPath);
                statusData.put("lastUpdated", System.currentTimeMillis());
                redisAdapter.set(newStatusKey, statusData, Duration.ofDays(7));
                redisAdapter.delete(oldStatusKey);
            }
            
            // Move other references if needed
            // ... additional reference updates can be added here
            
        } catch (Exception e) {
            logger.error("Error updating file references: {}", e.getMessage(), e);
        }
    }

    /**
     * Send event notification
     */
    private void sendEventNotification(String notificationType, JsonNode originalEvent, JsonNode result) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationType", notificationType);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("originalEvent", originalEvent);
            if (result != null) {
                notification.put("result", result);
            }
            
            rabbitTemplate.convertAndSend("file.events", notification);
            
        } catch (Exception e) {
            logger.error("Error sending event notification: {}", e.getMessage(), e);
        }
    }

    private String getFileName(String filePath) {
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }

    private JsonNode createSuccessResponse(String eventType, String message, JsonNode result) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("eventType", eventType);
        response.put("message", message);
        if (result != null) {
            response.put("result", result);
        }
        response.put("timestamp", System.currentTimeMillis());
        
        return objectMapper.valueToTree(response);
    }

    private JsonNode createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return objectMapper.valueToTree(response);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "FileEventHandler");
        metadata.put("version", "1.0.0");
        metadata.put("description", "Handles file system events and routing");
        metadata.put("supportedEvents", new String[]{
            "created", "modified", "deleted", "moved"
        });
        metadata.put("features", new String[]{
            "auto_pipeline_trigger", "file_monitoring", "event_audit_trail",
            "reprocessing", "cache_cleanup"
        });
        return metadata;
    }
}
