package blog.eric231.examples.multiconnector.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.connector.IBMMQConnector;
import blog.eric231.framework.infrastructure.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * IBM MQ Message Handler - Domain Logic for IBM MQ Operations
 * 
 * This domain logic handles IBM MQ message operations including:
 * - Sending and receiving messages
 * - Queue monitoring and management
 * - Message routing and transformation
 * - Integration with pipeline processing
 */
@Component
@DL(name = "IBMMQMessageHandler", description = "Handles IBM MQ message operations and integration")
public class IBMMQMessageHandler implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQMessageHandler.class);

    private final IBMMQConnector mqConnector;
    private final RedisAdapter redisAdapter;
    private final ObjectMapper objectMapper;
    private final FilePipelineProcessor pipelineProcessor;

    @Autowired
    public IBMMQMessageHandler(IBMMQConnector mqConnector,
                              RedisAdapter redisAdapter,
                              ObjectMapper objectMapper,
                              FilePipelineProcessor pipelineProcessor) {
        this.mqConnector = mqConnector;
        this.redisAdapter = redisAdapter;
        this.objectMapper = objectMapper;
        this.pipelineProcessor = pipelineProcessor;
    }

    @Override
    public JsonNode handle(JsonNode input) {
        try {
            String operation = input.has("operation") ? input.get("operation").asText() : "send";
            
            logger.info("Processing IBM MQ operation: {}", operation);
            
            switch (operation.toLowerCase()) {
                case "send":
                    return handleSendMessage(input);
                case "receive":
                    return handleReceiveMessage(input);
                case "browse":
                    return handleBrowseQueue(input);
                case "monitor":
                    return handleMonitorQueue(input);
                case "process_pipeline_notification":
                    return handlePipelineNotification(input);
                case "send_pipeline_trigger":
                    return handleSendPipelineTrigger(input);
                case "queue_depth":
                    return handleQueueDepth(input);
                case "batch_process":
                    return handleBatchProcess(input);
                default:
                    return createErrorResponse("Unsupported operation", "Operation: " + operation);
            }
            
        } catch (Exception e) {
            logger.error("Error in IBM MQ message handler: {}", e.getMessage(), e);
            return createErrorResponse("Message handler error", e.getMessage());
        }
    }

    /**
     * Handle sending messages to IBM MQ queues
     */
    private JsonNode handleSendMessage(JsonNode input) {
        try {
            String queueName = input.get("queueName").asText();
            String message = input.get("message").asText();
            
            // Build message options
            Map<String, Object> options = new HashMap<>();
            if (input.has("priority")) {
                options.put("priority", input.get("priority").asInt());
            }
            if (input.has("expiry")) {
                options.put("expiry", input.get("expiry").asInt());
            }
            if (input.has("correlationId")) {
                options.put("correlationId", input.get("correlationId").asText());
            }
            if (input.has("transactional")) {
                options.put("transactional", input.get("transactional").asBoolean());
            }

            CompletableFuture<JsonNode> sendResult = mqConnector.sendMessage(queueName, message, options);
            JsonNode result = sendResult.join();

            // Store message metadata for tracking
            if (result.has("data")) {
                storeMessageMetadata(result.get("data"), "sent");
            }

            logger.debug("Message sent successfully to queue: {}", queueName);
            return result;

        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
            return createErrorResponse("Send message failed", e.getMessage());
        }
    }

    /**
     * Handle receiving messages from IBM MQ queues
     */
    private JsonNode handleReceiveMessage(JsonNode input) {
        try {
            String queueName = input.get("queueName").asText();
            int timeout = input.has("timeout") ? input.get("timeout").asInt() : 0;

            CompletableFuture<JsonNode> receiveResult = mqConnector.receiveMessage(queueName, timeout);
            JsonNode result = receiveResult.join();

            // Process received message if available
            if (result.has("data") && result.get("data").has("message")) {
                JsonNode messageData = result.get("data");
                
                // Store message metadata
                storeMessageMetadata(messageData, "received");
                
                // Check if this should trigger pipeline processing
                if (shouldTriggerPipelineFromMessage(messageData)) {
                    triggerPipelineFromMessage(messageData);
                }
            }

            logger.debug("Message received from queue: {}", queueName);
            return result;

        } catch (Exception e) {
            logger.error("Error receiving message: {}", e.getMessage(), e);
            return createErrorResponse("Receive message failed", e.getMessage());
        }
    }

    /**
     * Handle browsing queue messages
     */
    private JsonNode handleBrowseQueue(JsonNode input) {
        try {
            String queueName = input.get("queueName").asText();
            int maxMessages = input.has("maxMessages") ? input.get("maxMessages").asInt() : 10;

            CompletableFuture<JsonNode> browseResult = mqConnector.browseQueue(queueName, maxMessages);
            JsonNode result = browseResult.join();

            // Cache browse results for monitoring
            if (result.has("data")) {
                String cacheKey = "queue_browse:" + queueName;
                redisAdapter.set(cacheKey, result.get("data"), Duration.ofMinutes(5));
            }

            logger.debug("Queue browsed: {} - {} messages found", queueName, 
                        result.has("data") ? result.get("data").get("messageCount").asInt() : 0);
            return result;

        } catch (Exception e) {
            logger.error("Error browsing queue: {}", e.getMessage(), e);
            return createErrorResponse("Browse queue failed", e.getMessage());
        }
    }

    /**
     * Handle queue monitoring
     */
    private JsonNode handleMonitorQueue(JsonNode input) {
        try {
            String queueName = input.get("queueName").asText();

            CompletableFuture<JsonNode> depthResult = mqConnector.getQueueDepth(queueName);
            JsonNode result = depthResult.join();

            if (result.has("data")) {
                JsonNode depthData = result.get("data");
                
                // Store queue monitoring data
                storeQueueMonitoringData(depthData);
                
                // Check for alerts
                checkQueueAlerts(depthData);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error monitoring queue: {}", e.getMessage(), e);
            return createErrorResponse("Queue monitoring failed", e.getMessage());
        }
    }

    /**
     * Handle pipeline notifications via IBM MQ
     */
    private JsonNode handlePipelineNotification(JsonNode input) {
        try {
            String notificationType = input.get("notificationType").asText();
            String jobId = input.has("jobId") ? input.get("jobId").asText() : UUID.randomUUID().toString();
            
            // Build notification message
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationType", notificationType);
            notification.put("jobId", jobId);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("data", input.get("data"));

            String notificationJson = objectMapper.writeValueAsString(notification);
            
            // Send to notification queue
            String notificationQueue = "PIPELINE.NOTIFICATIONS";
            Map<String, Object> options = Map.of("priority", 5, "expiry", 3600000); // 1 hour expiry

            CompletableFuture<JsonNode> sendResult = mqConnector.sendMessage(
                notificationQueue, notificationJson, options);
            
            JsonNode result = sendResult.join();

            logger.info("Pipeline notification sent: {} for job: {}", notificationType, jobId);
            return result;

        } catch (Exception e) {
            logger.error("Error sending pipeline notification: {}", e.getMessage(), e);
            return createErrorResponse("Pipeline notification failed", e.getMessage());
        }
    }

    /**
     * Handle sending pipeline trigger messages
     */
    private JsonNode handleSendPipelineTrigger(JsonNode input) {
        try {
            String triggerType = input.get("triggerType").asText();
            
            // Build pipeline trigger message
            Map<String, Object> trigger = new HashMap<>();
            trigger.put("operation", "process");
            trigger.put("triggerType", triggerType);
            trigger.put("triggerSource", "ibm_mq");
            trigger.put("timestamp", System.currentTimeMillis());
            
            // Copy relevant data from input
            if (input.has("sourceType")) trigger.put("sourceType", input.get("sourceType").asText());
            if (input.has("remotePath")) trigger.put("remotePath", input.get("remotePath").asText());
            if (input.has("priority")) trigger.put("priority", input.get("priority").asText());

            String triggerJson = objectMapper.writeValueAsString(trigger);
            
            // Send to pipeline trigger queue
            String triggerQueue = "PIPELINE.TRIGGERS";
            Map<String, Object> options = Map.of("priority", 8); // High priority

            CompletableFuture<JsonNode> sendResult = mqConnector.sendMessage(
                triggerQueue, triggerJson, options);
            
            JsonNode result = sendResult.join();

            logger.info("Pipeline trigger sent: {}", triggerType);
            return result;

        } catch (Exception e) {
            logger.error("Error sending pipeline trigger: {}", e.getMessage(), e);
            return createErrorResponse("Pipeline trigger failed", e.getMessage());
        }
    }

    /**
     * Handle queue depth checking
     */
    private JsonNode handleQueueDepth(JsonNode input) {
        try {
            String queueName = input.get("queueName").asText();
            return mqConnector.getQueueDepth(queueName).join();

        } catch (Exception e) {
            logger.error("Error getting queue depth: {}", e.getMessage(), e);
            return createErrorResponse("Queue depth check failed", e.getMessage());
        }
    }

    /**
     * Handle batch message processing
     */
    private JsonNode handleBatchProcess(JsonNode input) {
        try {
            String queueName = input.get("queueName").asText();
            int batchSize = input.has("batchSize") ? input.get("batchSize").asInt() : 10;
            int timeout = input.has("timeout") ? input.get("timeout").asInt() : 5000;

            List<Map<String, Object>> processedMessages = new ArrayList<>();
            int processedCount = 0;
            int errorCount = 0;

            for (int i = 0; i < batchSize; i++) {
                try {
                    CompletableFuture<JsonNode> receiveResult = mqConnector.receiveMessage(queueName, timeout);
                    JsonNode result = receiveResult.join();

                    if (result.get("status").asText().equals("success") && 
                        result.has("data") && result.get("data").has("message")) {
                        
                        JsonNode messageData = result.get("data");
                        
                        // Process the message (could trigger pipeline or other logic)
                        processMessage(messageData);
                        
                        Map<String, Object> processedMsg = new HashMap<>();
                        processedMsg.put("messageId", messageData.get("messageId").asText());
                        processedMsg.put("processed", true);
                        processedMsg.put("processedAt", System.currentTimeMillis());
                        processedMessages.add(processedMsg);
                        
                        processedCount++;
                    } else {
                        break; // No more messages or error
                    }

                } catch (Exception e) {
                    logger.error("Error processing message in batch: {}", e.getMessage(), e);
                    errorCount++;
                }
            }

            Map<String, Object> batchResult = new HashMap<>();
            batchResult.put("queueName", queueName);
            batchResult.put("processedCount", processedCount);
            batchResult.put("errorCount", errorCount);
            batchResult.put("messages", processedMessages);

            return createSuccessResponse("Batch processing completed", batchResult);

        } catch (Exception e) {
            logger.error("Error in batch processing: {}", e.getMessage(), e);
            return createErrorResponse("Batch processing failed", e.getMessage());
        }
    }

    // Helper methods

    private void storeMessageMetadata(JsonNode messageData, String operation) {
        try {
            String messageId = messageData.get("messageId").asText();
            String key = "mq_message:" + messageId;
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("messageId", messageId);
            metadata.put("operation", operation);
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("queueName", messageData.get("queueName").asText());
            
            if (messageData.has("correlationId")) {
                metadata.put("correlationId", messageData.get("correlationId").asText());
            }

            redisAdapter.set(key, metadata, Duration.ofHours(24));

        } catch (Exception e) {
            logger.warn("Failed to store message metadata: {}", e.getMessage());
        }
    }

    private void storeQueueMonitoringData(JsonNode queueData) {
        try {
            String queueName = queueData.get("queueName").asText();
            String key = "queue_monitoring:" + queueName;
            
            Map<String, Object> monitoringData = new HashMap<>();
            monitoringData.put("queueName", queueName);
            monitoringData.put("currentDepth", queueData.get("currentDepth").asInt());
            monitoringData.put("maximumDepth", queueData.get("maximumDepth").asInt());
            monitoringData.put("timestamp", System.currentTimeMillis());

            redisAdapter.set(key, monitoringData, Duration.ofMinutes(10));

        } catch (Exception e) {
            logger.warn("Failed to store queue monitoring data: {}", e.getMessage());
        }
    }

    private void checkQueueAlerts(JsonNode queueData) {
        try {
            String queueName = queueData.get("queueName").asText();
            int currentDepth = queueData.get("currentDepth").asInt();
            int maximumDepth = queueData.get("maximumDepth").asInt();

            // Check for high queue depth (80% of maximum)
            double utilizationPercent = (double) currentDepth / maximumDepth * 100;
            
            if (utilizationPercent > 80) {
                logger.warn("Queue {} depth alert: {}% utilization ({}/{})", 
                           queueName, String.format("%.1f", utilizationPercent), 
                           currentDepth, maximumDepth);
                
                // Could send alert notification here
                sendQueueAlert(queueName, "HIGH_DEPTH", utilizationPercent);
            }

        } catch (Exception e) {
            logger.warn("Failed to check queue alerts: {}", e.getMessage());
        }
    }

    private void sendQueueAlert(String queueName, String alertType, double utilizationPercent) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("alertType", alertType);
            alert.put("queueName", queueName);
            alert.put("utilizationPercent", utilizationPercent);
            alert.put("timestamp", System.currentTimeMillis());

            String alertJson = objectMapper.writeValueAsString(alert);
            
            // Send alert to monitoring queue
            mqConnector.sendMessage("QUEUE.ALERTS", alertJson, Map.of("priority", 9));

        } catch (Exception e) {
            logger.error("Failed to send queue alert: {}", e.getMessage());
        }
    }

    private boolean shouldTriggerPipelineFromMessage(JsonNode messageData) {
        try {
            String message = messageData.get("message").asText();
            
            // Check if message contains pipeline trigger keywords
            return message.contains("PIPELINE_TRIGGER") || 
                   message.contains("PROCESS_FILE") ||
                   message.contains("BATCH_PROCESS");
                   
        } catch (Exception e) {
            return false;
        }
    }

    private void triggerPipelineFromMessage(JsonNode messageData) {
        try {
            String message = messageData.get("message").asText();
            
            // Parse trigger data from message (could be JSON)
            Map<String, Object> pipelineInput = objectMapper.readValue(message, Map.class);
            pipelineInput.put("triggerSource", "ibm_mq_message");
            pipelineInput.put("messageId", messageData.get("messageId").asText());

            JsonNode pipelineInputNode = objectMapper.valueToTree(pipelineInput);
            
            // Trigger pipeline asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    pipelineProcessor.handle(pipelineInputNode);
                } catch (Exception e) {
                    logger.error("Error triggering pipeline from MQ message: {}", e.getMessage(), e);
                }
            });

            logger.info("Pipeline triggered from IBM MQ message: {}", 
                       messageData.get("messageId").asText());

        } catch (Exception e) {
            logger.error("Failed to trigger pipeline from message: {}", e.getMessage(), e);
        }
    }

    private void processMessage(JsonNode messageData) {
        try {
            // This could include various processing logic:
            // - Message transformation
            // - Data validation
            // - Routing decisions
            // - Pipeline triggering
            
            String message = messageData.get("message").asText();
            logger.debug("Processing message: {}", messageData.get("messageId").asText());
            
            // Example: Check for specific message patterns
            if (message.contains("FILE_PROCESSED")) {
                // Handle file processing completion
                handleFileProcessedNotification(messageData);
            } else if (message.contains("ERROR")) {
                // Handle error messages
                handleErrorMessage(messageData);
            }

        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    private void handleFileProcessedNotification(JsonNode messageData) {
        logger.info("File processing notification received: {}", 
                   messageData.get("messageId").asText());
        // Could update status, send notifications, etc.
    }

    private void handleErrorMessage(JsonNode messageData) {
        logger.warn("Error message received: {}", messageData.get("messageId").asText());
        // Could send alerts, retry processing, etc.
    }

    private JsonNode createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("data", data);
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
        metadata.put("name", "IBMMQMessageHandler");
        metadata.put("version", "1.0.0");
        metadata.put("description", "Handles IBM MQ message operations and integration");
        metadata.put("operations", new String[]{
            "send", "receive", "browse", "monitor", "process_pipeline_notification",
            "send_pipeline_trigger", "queue_depth", "batch_process"
        });
        metadata.put("queues", new String[]{
            "PIPELINE.NOTIFICATIONS", "PIPELINE.TRIGGERS", "QUEUE.ALERTS"
        });
        metadata.put("features", new String[]{
            "message_tracking", "queue_monitoring", "pipeline_integration", 
            "batch_processing", "alert_management"
        });
        return metadata;
    }
}
