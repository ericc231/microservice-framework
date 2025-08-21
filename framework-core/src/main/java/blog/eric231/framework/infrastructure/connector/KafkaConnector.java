package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kafka Connector that routes messages from Kafka topics to domain logic components.
 * 
 * This connector listens to configured Kafka topics and routes messages to 
 * appropriate domain logic based on routing configuration.
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.kafka.enabled", havingValue = "true")
public class KafkaConnector {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConnector.class);
    
    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaConnector(ProcessRegistry processRegistry, 
                         FrameworkProperties frameworkProperties,
                         KafkaTemplate<String, Object> kafkaTemplate,
                         ObjectMapper objectMapper) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        logger.info("Kafka Connector initialized");
    }

    /**
     * Dynamic Kafka listener that routes messages based on topic configuration
     */
    @KafkaListener(topics = "#{@kafkaConnector.getTopics()}", groupId = "#{T(blog.eric231.framework.infrastructure.connector.KafkaConnector).getGroupId()}")
    public void handleKafkaMessage(@Payload String message,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 ConsumerRecord<String, String> record,
                                 Acknowledgment acknowledgment) {
        
        logger.debug("Received Kafka message from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        
        try {
            // Find matching routing configuration
            Optional<FrameworkProperties.Routing> matchedRouting = findMatchedRouting(topic);
            
            if (matchedRouting.isPresent()) {
                String processName = matchedRouting.get().getProcessName();
                
                // Get domain logic from process registry
                DomainLogic domainLogic = processRegistry.getDomainLogic(processName);
                
                if (domainLogic != null) {
                    // Convert message to JsonNode
                    JsonNode messageNode = parseMessage(message);
                    
                    // Handle the message
                    JsonNode response = domainLogic.handle(messageNode);
                    
                    // Send response if response topic is configured
                    sendResponseIfConfigured(matchedRouting.get(), response, record);
                    
                    logger.debug("Successfully processed message for domain logic: {}", processName);
                } else {
                    logger.warn("No domain logic found for process name: {}", processName);
                }
            } else {
                logger.warn("No routing configuration found for topic: {}", topic);
            }
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing Kafka message from topic {}: {}", topic, e.getMessage(), e);
            // In production, you might want to send to dead letter queue
        }
    }

    /**
     * Find routing configuration that matches the given topic
     */
    private Optional<FrameworkProperties.Routing> findMatchedRouting(String topic) {
        if (frameworkProperties.getRouting() == null) {
            return Optional.empty();
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "kafka".equalsIgnoreCase(trigger.getType()) && 
                                           topic.equals(trigger.getListenTopic())))
                .findFirst();
    }

    /**
     * Parse message string to JsonNode
     */
    private JsonNode parseMessage(String message) {
        try {
            return objectMapper.readTree(message);
        } catch (Exception e) {
            logger.warn("Failed to parse message as JSON, treating as plain text: {}", e.getMessage());
            return objectMapper.createObjectNode().put("message", message);
        }
    }

    /**
     * Send response to configured response topic if available
     */
    private void sendResponseIfConfigured(FrameworkProperties.Routing routing, JsonNode response, ConsumerRecord<String, String> originalRecord) {
        if (routing.getTriggers() != null) {
            routing.getTriggers().stream()
                    .filter(trigger -> "kafka".equalsIgnoreCase(trigger.getType()))
                    .filter(trigger -> trigger.getResponseTopic() != null && !trigger.getResponseTopic().trim().isEmpty())
                    .findFirst()
                    .ifPresent(trigger -> {
                        try {
                            String responseMessage = objectMapper.writeValueAsString(response);
                            kafkaTemplate.send(trigger.getResponseTopic(), originalRecord.key(), responseMessage);
                            logger.debug("Sent response to topic: {}", trigger.getResponseTopic());
                        } catch (Exception e) {
                            logger.error("Failed to send response to topic {}: {}", trigger.getResponseTopic(), e.getMessage(), e);
                        }
                    });
        }
    }

    /**
     * Get topics to listen to from routing configuration
     */
    public String[] getTopics() {
        if (frameworkProperties.getRouting() == null) {
            return new String[0];
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .flatMap(routing -> routing.getTriggers().stream())
                .filter(trigger -> "kafka".equalsIgnoreCase(trigger.getType()))
                .filter(trigger -> trigger.getListenTopic() != null && !trigger.getListenTopic().trim().isEmpty())
                .map(FrameworkProperties.Trigger::getListenTopic)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * Get group ID from framework properties
     */
    public static String getGroupId() {
        // This is a static method to work with SpEL in @KafkaListener
        // In real implementation, you might want to inject this differently
        return "framework-group";
    }
}
