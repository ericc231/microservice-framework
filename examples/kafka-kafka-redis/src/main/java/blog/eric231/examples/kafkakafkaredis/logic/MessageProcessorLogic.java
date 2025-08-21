package blog.eric231.examples.kafkakafkaredis.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * First stage domain logic for processing initial Kafka messages.
 * 
 * This component receives raw messages from the input Kafka topic,
 * processes and enriches them, then returns them for forwarding
 * to the intermediate Kafka topic.
 * 
 * Message flow: input-topic -> MessageProcessorLogic -> intermediate-topic
 */
@DL("kafka-message-processor")
@Component
public class MessageProcessorLogic implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessorLogic.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public JsonNode handle(JsonNode input) {
        logger.info("Processing initial Kafka message: {}", input);
        
        try {
            // Generate processing metadata
            String processingId = UUID.randomUUID().toString();
            String processingTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Create enriched message
            ObjectNode processedMessage = mapper.createObjectNode();
            processedMessage.put("processingId", processingId);
            processedMessage.put("processedAt", processingTimestamp);
            processedMessage.put("stage", "processor");
            processedMessage.put("status", "processed");
            
            // Extract and enrich original data
            if (input.has("userId")) {
                processedMessage.put("userId", input.get("userId").asText());
                processedMessage.put("userCategory", categorizeUser(input.get("userId").asText()));
            }
            
            if (input.has("eventType")) {
                String eventType = input.get("eventType").asText();
                processedMessage.put("eventType", eventType);
                processedMessage.put("priority", calculatePriority(eventType));
            }
            
            if (input.has("data")) {
                processedMessage.set("data", input.get("data"));
                processedMessage.put("dataSize", input.get("data").toString().length());
            } else {
                // If no data field, wrap the entire input as data
                processedMessage.set("data", input);
                processedMessage.put("dataSize", input.toString().length());
            }
            
            // Add processing metrics
            processedMessage.put("inputFields", input.size());
            processedMessage.put("processingDuration", calculateProcessingTime());
            
            // Store original message for audit
            processedMessage.set("originalMessage", input);
            
            logger.info("Successfully processed message with ID: {}", processingId);
            
            return processedMessage;
            
        } catch (Exception e) {
            logger.error("Error processing initial Kafka message: {}", e.getMessage(), e);
            
            // Return error message for downstream handling
            ObjectNode errorMessage = mapper.createObjectNode();
            errorMessage.put("processingId", UUID.randomUUID().toString());
            errorMessage.put("processedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            errorMessage.put("stage", "processor");
            errorMessage.put("status", "error");
            errorMessage.put("error", e.getMessage());
            errorMessage.set("originalMessage", input);
            
            return errorMessage;
        }
    }

    /**
     * Categorize user based on user ID pattern
     */
    private String categorizeUser(String userId) {
        if (userId.startsWith("admin_")) {
            return "admin";
        } else if (userId.startsWith("vip_")) {
            return "vip";
        } else if (userId.matches("\\d+")) {
            return "regular";
        } else {
            return "guest";
        }
    }

    /**
     * Calculate priority based on event type
     */
    private String calculatePriority(String eventType) {
        switch (eventType.toLowerCase()) {
            case "login":
            case "logout":
                return "high";
            case "purchase":
            case "payment":
                return "critical";
            case "view":
            case "click":
                return "low";
            case "error":
            case "security":
                return "urgent";
            default:
                return "medium";
        }
    }

    /**
     * Simulate processing time calculation
     */
    private long calculateProcessingTime() {
        // Simulate some processing work
        try {
            Thread.sleep(1); // 1ms processing simulation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 1; // Return fixed 1ms for consistency in tests
    }

    @Override
    public String getOperationName() {
        return "kafka-message-processor";
    }

    @Override
    public JsonNode getMetadata() {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("description", "Processes initial Kafka messages and enriches them for downstream processing");
        metadata.put("version", "1.0");
        metadata.put("stage", "processor");
        metadata.put("inputSource", "input-topic");
        metadata.put("outputTarget", "intermediate-topic");
        return metadata;
    }
}
