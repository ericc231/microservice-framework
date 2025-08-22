package blog.eric231.examples.basicrestmqredis.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * First stage domain logic for handling REST requests.
 * 
 * This component receives REST requests, processes them, sends messages to RabbitMQ,
 * waits for replies, and returns the final response to the REST client.
 * 
 * Message flow: REST -> RequestProcessorLogic -> RabbitMQ -> MessageProcessorLogic -> Redis
 *                                             <- RabbitMQ <- MessageProcessorLogic <- Redis
 *               REST <- RequestProcessorLogic <-
 */
@DL("request-processor")
@Component
public class RequestProcessorLogic implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(RequestProcessorLogic.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public RequestProcessorLogic(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        // Set reply timeout for sendAndReceive operations
        this.rabbitTemplate.setReplyTimeout(30000); // 30 seconds
    }

    @Override
    public JsonNode handle(JsonNode input) {
        logger.info("Processing REST request: {}", input);
        
        try {
            // Generate request metadata
            String requestId = UUID.randomUUID().toString();
            String requestTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Create enriched request message for MQ
            ObjectNode requestMessage = mapper.createObjectNode();
            requestMessage.put("requestId", requestId);
            requestMessage.put("timestamp", requestTimestamp);
            requestMessage.put("stage", "request-processing");
            
            // Extract and validate request data
            if (input.has("operation")) {
                requestMessage.put("operation", input.get("operation").asText());
            } else {
                requestMessage.put("operation", "default");
            }
            
            if (input.has("data")) {
                requestMessage.set("data", input.get("data"));
            } else {
                requestMessage.set("data", input);
            }
            
            // Add client information if available
            if (input.has("clientId")) {
                requestMessage.put("clientId", input.get("clientId").asText());
            }
            
            // Add request metadata
            requestMessage.put("requestSize", input.toString().length());
            requestMessage.put("inputFields", input.size());
            
            logger.info("Sending request to MQ with ID: {}", requestId);
            
            // Send message to RabbitMQ and wait for reply using RabbitTemplate's sendAndReceive
            JsonNode mqResponse = null;
            try {
                Message responseMessage = rabbitTemplate.sendAndReceive(
                    "processing-exchange",
                    "process.request",
                    new Message(requestMessage.toString().getBytes(), new MessageProperties())
                );
                
                if (responseMessage != null && responseMessage.getBody() != null) {
                    String responseBody = new String(responseMessage.getBody());
                    mqResponse = mapper.readTree(responseBody);
                }
            } catch (Exception e) {
                logger.error("Error communicating with MQ for request ID: {}", requestId, e);
                return createErrorResponse(requestId, "MQ communication failed: " + e.getMessage(), "MQ_ERROR");
            }
            
            if (mqResponse != null) {
                logger.info("Received response from MQ for request ID: {}", requestId);
                
                // Create final REST response
                ObjectNode restResponse = mapper.createObjectNode();
                restResponse.put("status", "success");
                restResponse.put("requestId", requestId);
                restResponse.put("processedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                // Include response data from MQ
                if (mqResponse.has("status")) {
                    restResponse.put("processingStatus", mqResponse.get("status").asText());
                }
                if (mqResponse.has("storageId")) {
                    restResponse.put("storageId", mqResponse.get("storageId").asText());
                }
                if (mqResponse.has("redisKey")) {
                    restResponse.put("redisKey", mqResponse.get("redisKey").asText());
                }
                if (mqResponse.has("message")) {
                    restResponse.put("message", mqResponse.get("message").asText());
                }
                if (mqResponse.has("processingTime")) {
                    restResponse.put("processingTime", mqResponse.get("processingTime").asLong());
                }
                
                // Include metadata
                restResponse.put("totalRoundTripTime", calculateProcessingTime(requestTimestamp));
                restResponse.set("mqResponse", mqResponse);
                
                return restResponse;
                
            } else {
                logger.warn("No response received from MQ for request ID: {}", requestId);
                return createErrorResponse(requestId, "No response received from message processor", "TIMEOUT");
            }
            
        } catch (Exception e) {
            logger.error("Error processing REST request: {}", e.getMessage(), e);
            return createErrorResponse(null, e.getMessage(), "PROCESSING_ERROR");
        }
    }

    /**
     * Create error response for REST client
     */
    private ObjectNode createErrorResponse(String requestId, String errorMessage, String errorCode) {
        ObjectNode errorResponse = mapper.createObjectNode();
        errorResponse.put("status", "error");
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("errorMessage", errorMessage);
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        if (requestId != null) {
            errorResponse.put("requestId", requestId);
        }
        
        return errorResponse;
    }

    /**
     * Calculate total processing time
     */
    private long calculateProcessingTime(String startTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime end = LocalDateTime.now();
            return java.time.Duration.between(start, end).toMillis();
        } catch (Exception e) {
            logger.warn("Could not calculate processing time: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public String getOperationName() {
        return "request-processor";
    }

    @Override
    public JsonNode getMetadata() {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("description", "Handles REST requests and coordinates with MQ for processing");
        metadata.put("version", "1.0");
        metadata.put("stage", "request-processing");
        metadata.put("inputSource", "REST API");
        metadata.put("outputTarget", "RabbitMQ");
        metadata.put("responsePattern", "synchronous-over-async");
        return metadata;
    }
}
