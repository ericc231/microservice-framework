package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ Connector that handles message routing to domain logic components.
 * 
 * This connector listens to configured RabbitMQ queues and routes messages to 
 * appropriate domain logic based on routing configuration. It also supports
 * request-reply patterns for synchronous-like operations over async messaging.
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConnector {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConnector.class);
    
    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    // For handling request-reply patterns
    private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    @Autowired
    public RabbitMQConnector(ProcessRegistry processRegistry, 
                           FrameworkProperties frameworkProperties,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        logger.info("RabbitMQ Connector initialized");
    }

    /**
     * Generic message listener that routes messages based on queue name
     */
    @RabbitListener(queues = "#{@rabbitMQConnector.getListenQueues()}")
    public void handleMessage(String message, Message amqpMessage) {
        String queueName = amqpMessage.getMessageProperties().getConsumerQueue();
        logger.debug("Received message from queue: {}", queueName);
        
        try {
            // Find matching routing configuration
            Optional<FrameworkProperties.Routing> matchedRouting = findMatchedRouting(queueName);
            
            if (matchedRouting.isPresent()) {
                String processName = matchedRouting.get().getProcessName();
                
                // Get domain logic from process registry
                DomainLogic domainLogic = processRegistry.getDomainLogic(processName);
                
                if (domainLogic != null) {
                    // Parse message to JsonNode
                    JsonNode messageNode = parseMessage(message);
                    
                    // Handle the message
                    JsonNode response = domainLogic.handle(messageNode);
                    
                    // Send response if configured
                    sendResponseIfConfigured(matchedRouting.get(), response, amqpMessage);
                    
                    logger.debug("Successfully processed message for domain logic: {}", processName);
                } else {
                    logger.warn("No domain logic found for process name: {}", processName);
                }
            } else {
                logger.warn("No routing configuration found for queue: {}", queueName);
            }
            
        } catch (Exception e) {
            logger.error("Error processing RabbitMQ message from queue {}: {}", queueName, e.getMessage(), e);
        }
    }

    /**
     * Send a message to RabbitMQ and optionally wait for reply
     */
    public CompletableFuture<JsonNode> sendMessage(String exchangeName, String routingKey, JsonNode message, String replyQueue) {
        try {
            String messageStr = objectMapper.writeValueAsString(message);
            
            if (replyQueue != null && !replyQueue.trim().isEmpty()) {
                // Request-reply pattern
                String correlationId = java.util.UUID.randomUUID().toString();
                CompletableFuture<JsonNode> future = new CompletableFuture<>();
                pendingRequests.put(correlationId, future);
                
                // Set timeout for the request
                future.orTimeout(30, TimeUnit.SECONDS)
                      .whenComplete((result, throwable) -> {
                          pendingRequests.remove(correlationId);
                          if (throwable != null) {
                              logger.warn("Request-reply timeout or error for correlation ID: {}", correlationId, throwable);
                          }
                      });
                
                // Send message with reply-to and correlation ID
                MessageProperties properties = new MessageProperties();
                properties.setReplyTo(replyQueue);
                properties.setCorrelationId(correlationId);
                Message amqpMessage = new Message(messageStr.getBytes(), properties);
                
                rabbitTemplate.send(exchangeName, routingKey, amqpMessage);
                logger.debug("Sent request message to exchange: {}, routingKey: {}, correlationId: {}", 
                           exchangeName, routingKey, correlationId);
                
                return future;
            } else {
                // Fire-and-forget
                rabbitTemplate.convertAndSend(exchangeName, routingKey, messageStr);
                logger.debug("Sent fire-and-forget message to exchange: {}, routingKey: {}", exchangeName, routingKey);
                
                return CompletableFuture.completedFuture(null);
            }
            
        } catch (Exception e) {
            logger.error("Error sending message to RabbitMQ: {}", e.getMessage(), e);
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Handle reply messages for request-reply pattern
     */
    @RabbitListener(queues = "#{@rabbitMQConnector.getReplyQueues()}")
    public void handleReply(String message, Message amqpMessage) {
        String correlationId = amqpMessage.getMessageProperties().getCorrelationId();
        logger.debug("Received reply message with correlation ID: {}", correlationId);
        
        if (correlationId != null) {
            CompletableFuture<JsonNode> future = pendingRequests.remove(correlationId);
            if (future != null) {
                try {
                    JsonNode responseNode = parseMessage(message);
                    future.complete(responseNode);
                    logger.debug("Completed future for correlation ID: {}", correlationId);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    logger.error("Error parsing reply message: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("No pending request found for correlation ID: {}", correlationId);
            }
        } else {
            logger.warn("Received reply message without correlation ID");
        }
    }

    /**
     * Get list of queues to listen to from routing configuration
     */
    public String[] getListenQueues() {
        if (frameworkProperties.getRouting() == null) {
            return new String[0];
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .flatMap(routing -> routing.getTriggers().stream())
                .filter(trigger -> "rabbitmq".equalsIgnoreCase(trigger.getType()))
                .filter(trigger -> trigger.getQueueName() != null && !trigger.getQueueName().trim().isEmpty())
                .map(FrameworkProperties.Trigger::getQueueName)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * Get list of reply queues from routing configuration
     */
    public String[] getReplyQueues() {
        if (frameworkProperties.getRouting() == null) {
            return new String[0];
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .flatMap(routing -> routing.getTriggers().stream())
                .filter(trigger -> "rabbitmq".equalsIgnoreCase(trigger.getType()))
                .filter(trigger -> trigger.getReplyQueueName() != null && !trigger.getReplyQueueName().trim().isEmpty())
                .map(FrameworkProperties.Trigger::getReplyQueueName)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * Find routing configuration that matches the given queue name
     */
    private Optional<FrameworkProperties.Routing> findMatchedRouting(String queueName) {
        if (frameworkProperties.getRouting() == null) {
            return Optional.empty();
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "rabbitmq".equalsIgnoreCase(trigger.getType()) && 
                                           queueName.equals(trigger.getQueueName())))
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
     * Send response to configured reply queue if available
     */
    private void sendResponseIfConfigured(FrameworkProperties.Routing routing, JsonNode response, Message originalMessage) {
        if (routing.getTriggers() != null) {
            routing.getTriggers().stream()
                    .filter(trigger -> "rabbitmq".equalsIgnoreCase(trigger.getType()))
                    .forEach(trigger -> {
                        // Check for reply-to header first (RPC pattern)
                        String replyTo = originalMessage.getMessageProperties().getReplyTo();
                        String correlationId = originalMessage.getMessageProperties().getCorrelationId();
                        
                        if (replyTo != null && !replyTo.trim().isEmpty()) {
                            try {
                                String responseMessage = objectMapper.writeValueAsString(response);
                                MessageProperties properties = new MessageProperties();
                                if (correlationId != null) {
                                    properties.setCorrelationId(correlationId);
                                }
                                Message replyMessage = new Message(responseMessage.getBytes(), properties);
                                
                                rabbitTemplate.send("", replyTo, replyMessage);
                                logger.debug("Sent reply to: {} with correlation ID: {}", replyTo, correlationId);
                            } catch (Exception e) {
                                logger.error("Failed to send reply message: {}", e.getMessage(), e);
                            }
                        } else if (trigger.getReplyQueueName() != null && !trigger.getReplyQueueName().trim().isEmpty()) {
                            // Use configured reply queue
                            try {
                                String responseMessage = objectMapper.writeValueAsString(response);
                                rabbitTemplate.convertAndSend(trigger.getReplyQueueName(), responseMessage);
                                logger.debug("Sent response to configured reply queue: {}", trigger.getReplyQueueName());
                            } catch (Exception e) {
                                logger.error("Failed to send response to reply queue {}: {}", trigger.getReplyQueueName(), e.getMessage(), e);
                            }
                        }
                    });
        }
    }
}
