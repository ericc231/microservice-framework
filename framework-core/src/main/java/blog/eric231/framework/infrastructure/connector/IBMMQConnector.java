package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.infrastructure.config.IBMMQProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IBM MQ Connector
 * 
 * Provides connectivity to IBM MQ Queue Managers with support for:
 * - Sending and receiving messages
 * - Queue browsing and management
 * - Connection pooling
 * - SSL/TLS connections
 * - Transactional messaging
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.ibmmq.enabled", havingValue = "true", matchIfMissing = true)
public class IBMMQConnector {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQConnector.class);

    private final IBMMQProperties properties;
    private final ObjectMapper objectMapper;

    // Connection and queue management
    private MQQueueManager queueManager;
    private final Map<String, MQQueue> queueCache = new ConcurrentHashMap<>();
    private volatile boolean isConnected = false;

    @Autowired
    public IBMMQConnector(IBMMQProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        if (properties.isEnabled()) {
            try {
                connect();
                logger.info("IBM MQ Connector initialized successfully for Queue Manager: {}", 
                           properties.getQueueManager());
            } catch (Exception e) {
                logger.error("Failed to initialize IBM MQ Connector: {}", e.getMessage(), e);
            }
        } else {
            logger.info("IBM MQ Connector is disabled");
        }
    }

    @PreDestroy
    public void destroy() {
        disconnect();
    }

    /**
     * Establish connection to IBM MQ Queue Manager
     */
    public CompletableFuture<JsonNode> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isConnected && queueManager != null && queueManager.isConnected()) {
                    return createSuccessResponse("Already connected to Queue Manager", 
                                               properties.getQueueManager());
                }

                // Set up connection properties
                Hashtable<String, Object> connectionProperties = new Hashtable<>();
                connectionProperties.put(MQConstants.HOST_NAME_PROPERTY, properties.getHost());
                connectionProperties.put(MQConstants.PORT_PROPERTY, properties.getPort());
                connectionProperties.put(MQConstants.CHANNEL_PROPERTY, properties.getChannel());
                
                if (properties.getUser() != null) {
                    connectionProperties.put(MQConstants.USER_ID_PROPERTY, properties.getUser());
                }
                if (properties.getPassword() != null) {
                    connectionProperties.put(MQConstants.PASSWORD_PROPERTY, properties.getPassword());
                }

                // SSL Configuration
                if (properties.isUseSsl()) {
                    connectionProperties.put(MQConstants.USE_MQCSP_AUTHENTICATION_PROPERTY, true);
                    if (properties.getSsl().getCipherSuite() != null) {
                        connectionProperties.put(MQConstants.SSL_CIPHER_SUITE_PROPERTY, 
                                               properties.getSsl().getCipherSuite());
                    }
                    if (properties.getSsl().isFipsRequired()) {
                        connectionProperties.put(MQConstants.SSL_FIPS_REQUIRED_PROPERTY, true);
                    }
                }

                // Connection timeout
                connectionProperties.put(MQConstants.CONNECTION_TIMEOUT_PROPERTY, 
                                       properties.getConnectionTimeout());

                // Create Queue Manager connection
                queueManager = new MQQueueManager(properties.getQueueManager(), connectionProperties);
                isConnected = true;

                logger.info("Successfully connected to IBM MQ Queue Manager: {} on {}:{}", 
                           properties.getQueueManager(), properties.getHost(), properties.getPort());

                return createSuccessResponse("Connected to IBM MQ Queue Manager", 
                                           properties.getQueueManager());

            } catch (MQException e) {
                logger.error("Failed to connect to IBM MQ: {} (Reason Code: {})", 
                           e.getMessage(), e.reasonCode, e);
                isConnected = false;
                return createErrorResponse("IBM MQ Connection failed", 
                                         "Reason Code: " + e.reasonCode + " - " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error connecting to IBM MQ: {}", e.getMessage(), e);
                isConnected = false;
                return createErrorResponse("Unexpected connection error", e.getMessage());
            }
        });
    }

    /**
     * Disconnect from IBM MQ Queue Manager
     */
    public CompletableFuture<JsonNode> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Close all cached queues
                closeAllQueues();

                // Disconnect from Queue Manager
                if (queueManager != null && queueManager.isConnected()) {
                    queueManager.disconnect();
                    logger.info("Disconnected from IBM MQ Queue Manager: {}", properties.getQueueManager());
                }

                isConnected = false;
                return createSuccessResponse("Disconnected from IBM MQ", properties.getQueueManager());

            } catch (MQException e) {
                logger.error("Error disconnecting from IBM MQ: {} (Reason Code: {})", 
                           e.getMessage(), e.reasonCode, e);
                return createErrorResponse("Disconnect failed", 
                                         "Reason Code: " + e.reasonCode + " - " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error during disconnect: {}", e.getMessage(), e);
                return createErrorResponse("Unexpected disconnect error", e.getMessage());
            }
        });
    }

    /**
     * Send message to a queue
     */
    public CompletableFuture<JsonNode> sendMessage(String queueName, String message) {
        return sendMessage(queueName, message, new HashMap<>());
    }

    /**
     * Send message to a queue with options
     */
    public CompletableFuture<JsonNode> sendMessage(String queueName, String message, 
                                                  Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnected();

                MQQueue queue = getQueue(queueName, MQConstants.MQOO_OUTPUT);
                
                // Create message
                MQMessage mqMessage = new MQMessage();
                mqMessage.writeString(message);

                // Set message properties from options
                setMessageProperties(mqMessage, options);

                // Create put message options
                MQPutMessageOptions putOptions = new MQPutMessageOptions();
                
                // Set transactional mode if specified
                boolean transactional = (Boolean) options.getOrDefault("transactional", 
                                                 properties.getDefaultQueue().isTransactional());
                if (transactional) {
                    putOptions.options = MQConstants.MQPMO_SYNCPOINT;
                } else {
                    putOptions.options = MQConstants.MQPMO_NO_SYNCPOINT;
                }

                // Send message
                queue.put(mqMessage, putOptions);

                // Commit transaction if needed
                if (transactional) {
                    queueManager.commit();
                }

                String messageId = bytesToHex(mqMessage.messageId);
                
                logger.debug("Message sent to queue {}: Message ID = {}", queueName, messageId);

                Map<String, Object> result = new HashMap<>();
                result.put("messageId", messageId);
                result.put("correlationId", bytesToHex(mqMessage.correlationId));
                result.put("queueName", queueName);
                result.put("messageLength", message.length());

                return createSuccessResponse("Message sent successfully", result);

            } catch (Exception e) {
                logger.error("Error sending message to queue {}: {}", queueName, e.getMessage(), e);
                return createErrorResponse("Failed to send message", e.getMessage());
            }
        });
    }

    /**
     * Receive message from a queue (non-blocking)
     */
    public CompletableFuture<JsonNode> receiveMessage(String queueName) {
        return receiveMessage(queueName, 0); // No wait
    }

    /**
     * Receive message from a queue with timeout
     */
    public CompletableFuture<JsonNode> receiveMessage(String queueName, int timeoutMillis) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnected();

                MQQueue queue = getQueue(queueName, MQConstants.MQOO_INPUT_AS_Q_DEF);
                
                // Create get message options
                MQGetMessageOptions getOptions = new MQGetMessageOptions();
                getOptions.options = MQConstants.MQGMO_NO_WAIT;
                
                if (timeoutMillis > 0) {
                    getOptions.options = MQConstants.MQGMO_WAIT;
                    getOptions.waitInterval = timeoutMillis;
                }

                // Create message to receive into
                MQMessage message = new MQMessage();

                try {
                    // Get message
                    queue.get(message, getOptions);

                    // Extract message content
                    String messageText = message.readStringOfByteLength(message.getDataLength());
                    String messageId = bytesToHex(message.messageId);
                    String correlationId = bytesToHex(message.correlationId);

                    logger.debug("Message received from queue {}: Message ID = {}", queueName, messageId);

                    Map<String, Object> result = new HashMap<>();
                    result.put("messageId", messageId);
                    result.put("correlationId", correlationId);
                    result.put("queueName", queueName);
                    result.put("message", messageText);
                    result.put("priority", message.priority);
                    result.put("expiry", message.expiry);
                    result.put("format", message.format.trim());
                    result.put("putDateTime", message.putDateTime.toString());

                    return createSuccessResponse("Message received successfully", result);

                } catch (MQException e) {
                    if (e.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
                        return createSuccessResponse("No messages available", 
                                                   Map.of("queueName", queueName, "available", false));
                    }
                    throw e;
                }

            } catch (Exception e) {
                logger.error("Error receiving message from queue {}: {}", queueName, e.getMessage(), e);
                return createErrorResponse("Failed to receive message", e.getMessage());
            }
        });
    }

    /**
     * Browse messages in a queue without removing them
     */
    public CompletableFuture<JsonNode> browseQueue(String queueName, int maxMessages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnected();

                MQQueue queue = getQueue(queueName, MQConstants.MQOO_BROWSE);
                
                List<Map<String, Object>> messages = new ArrayList<>();
                
                // Browse messages
                MQGetMessageOptions getOptions = new MQGetMessageOptions();
                getOptions.options = MQConstants.MQGMO_BROWSE_FIRST | MQConstants.MQGMO_NO_WAIT;

                for (int i = 0; i < maxMessages; i++) {
                    try {
                        MQMessage message = new MQMessage();
                        queue.get(message, getOptions);

                        String messageText = message.readStringOfByteLength(message.getDataLength());
                        
                        Map<String, Object> msgInfo = new HashMap<>();
                        msgInfo.put("messageId", bytesToHex(message.messageId));
                        msgInfo.put("correlationId", bytesToHex(message.correlationId));
                        msgInfo.put("message", messageText);
                        msgInfo.put("priority", message.priority);
                        msgInfo.put("expiry", message.expiry);
                        msgInfo.put("format", message.format.trim());
                        msgInfo.put("putDateTime", message.putDateTime.toString());

                        messages.add(msgInfo);

                        // Set options for next message
                        getOptions.options = MQConstants.MQGMO_BROWSE_NEXT | MQConstants.MQGMO_NO_WAIT;

                    } catch (MQException e) {
                        if (e.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
                            break; // No more messages
                        }
                        throw e;
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("queueName", queueName);
                result.put("messageCount", messages.size());
                result.put("messages", messages);

                return createSuccessResponse("Queue browsed successfully", result);

            } catch (Exception e) {
                logger.error("Error browsing queue {}: {}", queueName, e.getMessage(), e);
                return createErrorResponse("Failed to browse queue", e.getMessage());
            }
        });
    }

    /**
     * Get queue depth (number of messages)
     */
    public CompletableFuture<JsonNode> getQueueDepth(String queueName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnected();

                MQQueue queue = getQueue(queueName, MQConstants.MQOO_INQUIRE);
                
                int depth = queue.getCurrentDepth();
                int maxDepth = queue.getMaximumDepth();

                Map<String, Object> result = new HashMap<>();
                result.put("queueName", queueName);
                result.put("currentDepth", depth);
                result.put("maximumDepth", maxDepth);

                return createSuccessResponse("Queue depth retrieved", result);

            } catch (Exception e) {
                logger.error("Error getting queue depth for {}: {}", queueName, e.getMessage(), e);
                return createErrorResponse("Failed to get queue depth", e.getMessage());
            }
        });
    }

    // Helper methods

    private void ensureConnected() throws MQException {
        if (!isConnected || queueManager == null || !queueManager.isConnected()) {
            throw new RuntimeException("Not connected to IBM MQ Queue Manager");
        }
    }

    private MQQueue getQueue(String queueName, int openOptions) throws MQException {
        String cacheKey = queueName + "_" + openOptions;
        
        MQQueue queue = queueCache.get(cacheKey);
        if (queue != null && queue.isOpen()) {
            return queue;
        }

        // Open new queue
        queue = queueManager.accessQueue(queueName, openOptions);
        queueCache.put(cacheKey, queue);
        
        logger.debug("Opened queue: {} with options: {}", queueName, openOptions);
        return queue;
    }

    private void closeAllQueues() {
        queueCache.forEach((key, queue) -> {
            try {
                if (queue != null && queue.isOpen()) {
                    queue.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing queue {}: {}", key, e.getMessage());
            }
        });
        queueCache.clear();
    }

    private void setMessageProperties(MQMessage message, Map<String, Object> options) {
        // Set priority
        Integer priority = (Integer) options.get("priority");
        if (priority != null) {
            message.priority = priority;
        } else {
            message.priority = properties.getMessage().getDefaultPriority();
        }

        // Set expiry
        Integer expiry = (Integer) options.get("expiry");
        if (expiry != null) {
            message.expiry = expiry;
        } else {
            message.expiry = properties.getMessage().getDefaultExpiry();
        }

        // Set correlation ID
        String correlationId = (String) options.get("correlationId");
        if (correlationId != null) {
            message.correlationId = hexToBytes(correlationId);
        }

        // Set message format
        String format = (String) options.get("format");
        if (format != null) {
            message.format = format;
        } else {
            message.format = properties.getMessage().getDefaultFormat();
        }

        // Set character set
        message.characterSet = 1208; // UTF-8
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[0];
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private JsonNode createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        response.put("queueManager", properties.getQueueManager());

        return objectMapper.valueToTree(response);
    }

    private JsonNode createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        response.put("queueManager", properties.getQueueManager());

        return objectMapper.valueToTree(response);
    }

    // Getter methods for testing and monitoring
    public boolean isConnected() {
        return isConnected && queueManager != null && queueManager.isConnected();
    }

    public String getQueueManagerName() {
        return properties.getQueueManager();
    }

    public int getActiveQueueCount() {
        return queueCache.size();
    }
}
