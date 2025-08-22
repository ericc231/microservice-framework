package blog.eric231.framework.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * IBM MQ Configuration Properties
 * 
 * Configuration properties for IBM MQ connections including basic connection settings,
 * SSL configuration, queue settings, and connection pooling options.
 */
@Component
@ConfigurationProperties(prefix = "framework.connectors.ibmmq")
public class IBMMQProperties {

    /**
     * Whether IBM MQ connector is enabled
     */
    private boolean enabled = true;

    /**
     * Queue Manager name
     */
    private String queueManager = "QM1";

    /**
     * MQ Server hostname or IP address
     */
    private String host = "localhost";

    /**
     * MQ Server port (default 1414 for MQ)
     */
    private int port = 1414;

    /**
     * MQ Channel name
     */
    private String channel = "DEV.APP.SVRCONN";

    /**
     * User ID for MQ connection (optional)
     */
    private String user;

    /**
     * Password for MQ connection (optional)
     */
    private String password;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;

    /**
     * Whether to use SSL/TLS connection
     */
    private boolean useSsl = false;

    /**
     * SSL Configuration
     */
    private SslConfig ssl = new SslConfig();

    /**
     * Connection pooling settings
     */
    private PoolConfig pool = new PoolConfig();

    /**
     * Default queue settings
     */
    private QueueConfig defaultQueue = new QueueConfig();

    /**
     * Named queue configurations
     */
    private Map<String, QueueConfig> queues = new HashMap<>();

    /**
     * Message settings
     */
    private MessageConfig message = new MessageConfig();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getQueueManager() {
        return queueManager;
    }

    public void setQueueManager(String queueManager) {
        this.queueManager = queueManager;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public void setSsl(SslConfig ssl) {
        this.ssl = ssl;
    }

    public PoolConfig getPool() {
        return pool;
    }

    public void setPool(PoolConfig pool) {
        this.pool = pool;
    }

    public QueueConfig getDefaultQueue() {
        return defaultQueue;
    }

    public void setDefaultQueue(QueueConfig defaultQueue) {
        this.defaultQueue = defaultQueue;
    }

    public Map<String, QueueConfig> getQueues() {
        return queues;
    }

    public void setQueues(Map<String, QueueConfig> queues) {
        this.queues = queues;
    }

    public MessageConfig getMessage() {
        return message;
    }

    public void setMessage(MessageConfig message) {
        this.message = message;
    }

    /**
     * SSL Configuration for IBM MQ
     */
    public static class SslConfig {
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
        private String cipherSuite;
        private boolean fipsRequired = false;

        // Getters and Setters
        public String getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getTrustStore() {
            return trustStore;
        }

        public void setTrustStore(String trustStore) {
            this.trustStore = trustStore;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public String getCipherSuite() {
            return cipherSuite;
        }

        public void setCipherSuite(String cipherSuite) {
            this.cipherSuite = cipherSuite;
        }

        public boolean isFipsRequired() {
            return fipsRequired;
        }

        public void setFipsRequired(boolean fipsRequired) {
            this.fipsRequired = fipsRequired;
        }
    }

    /**
     * Connection Pool Configuration
     */
    public static class PoolConfig {
        private int maxConnections = 10;
        private int minConnections = 1;
        private int maxIdleTime = 300000; // 5 minutes
        private int connectionRetryInterval = 5000;
        private int maxRetries = 3;

        // Getters and Setters
        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getMinConnections() {
            return minConnections;
        }

        public void setMinConnections(int minConnections) {
            this.minConnections = minConnections;
        }

        public int getMaxIdleTime() {
            return maxIdleTime;
        }

        public void setMaxIdleTime(int maxIdleTime) {
            this.maxIdleTime = maxIdleTime;
        }

        public int getConnectionRetryInterval() {
            return connectionRetryInterval;
        }

        public void setConnectionRetryInterval(int connectionRetryInterval) {
            this.connectionRetryInterval = connectionRetryInterval;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    /**
     * Queue Configuration
     */
    public static class QueueConfig {
        private String name;
        private int putMessageOptions = 0; // MQPMO_NONE
        private int getMessageOptions = 0; // MQGMO_NONE
        private int openOptions = 2032; // MQOO_INPUT_AS_Q_DEF + MQOO_OUTPUT
        private boolean transactional = false;
        private int messageExpiry = 0; // No expiry
        private int priority = 0; // Default priority

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPutMessageOptions() {
            return putMessageOptions;
        }

        public void setPutMessageOptions(int putMessageOptions) {
            this.putMessageOptions = putMessageOptions;
        }

        public int getGetMessageOptions() {
            return getMessageOptions;
        }

        public void setGetMessageOptions(int getMessageOptions) {
            this.getMessageOptions = getMessageOptions;
        }

        public int getOpenOptions() {
            return openOptions;
        }

        public void setOpenOptions(int openOptions) {
            this.openOptions = openOptions;
        }

        public boolean isTransactional() {
            return transactional;
        }

        public void setTransactional(boolean transactional) {
            this.transactional = transactional;
        }

        public int getMessageExpiry() {
            return messageExpiry;
        }

        public void setMessageExpiry(int messageExpiry) {
            this.messageExpiry = messageExpiry;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    /**
     * Message Configuration
     */
    public static class MessageConfig {
        private String defaultFormat = "MQSTR"; // String format
        private String defaultCharSet = "UTF-8";
        private int defaultExpiry = 0; // No expiry
        private int defaultPriority = 0;
        private boolean enableCorrelationId = true;
        private boolean enableMessageId = true;
        private int maxMessageSize = 4194304; // 4MB default

        // Getters and Setters
        public String getDefaultFormat() {
            return defaultFormat;
        }

        public void setDefaultFormat(String defaultFormat) {
            this.defaultFormat = defaultFormat;
        }

        public String getDefaultCharSet() {
            return defaultCharSet;
        }

        public void setDefaultCharSet(String defaultCharSet) {
            this.defaultCharSet = defaultCharSet;
        }

        public int getDefaultExpiry() {
            return defaultExpiry;
        }

        public void setDefaultExpiry(int defaultExpiry) {
            this.defaultExpiry = defaultExpiry;
        }

        public int getDefaultPriority() {
            return defaultPriority;
        }

        public void setDefaultPriority(int defaultPriority) {
            this.defaultPriority = defaultPriority;
        }

        public boolean isEnableCorrelationId() {
            return enableCorrelationId;
        }

        public void setEnableCorrelationId(boolean enableCorrelationId) {
            this.enableCorrelationId = enableCorrelationId;
        }

        public boolean isEnableMessageId() {
            return enableMessageId;
        }

        public void setEnableMessageId(boolean enableMessageId) {
            this.enableMessageId = enableMessageId;
        }

        public int getMaxMessageSize() {
            return maxMessageSize;
        }

        public void setMaxMessageSize(int maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
        }
    }
}
