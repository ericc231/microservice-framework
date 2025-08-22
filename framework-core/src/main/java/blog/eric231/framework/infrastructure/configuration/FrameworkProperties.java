package blog.eric231.framework.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "framework")
public class FrameworkProperties {

    private Connectors connectors;
    private List<Routing> routing;

    @Data
    public static class Connectors {
        private Rest rest;
        private Kafka kafka;
        private RabbitMQ rabbitmq;
        private SSH ssh;
        private SFTP sftp;
        private FTP ftp;
        private FTPS ftps;
        private TibcoEms tibcoEms;
        private S3 s3;
    }

    @Data
    public static class Rest {
        private boolean enabled;
        private String authMode = "bypass"; // New property for authentication mode
    }

    @Data
    public static class Kafka {
        private boolean enabled;
        private String bootstrapServers = "localhost:9092";
        private String groupId = "framework-group";
        private String autoOffsetReset = "earliest";
        private int maxPollRecords = 500;
        private boolean enableAutoCommit = true;
        private int autoCommitIntervalMs = 1000;
    }

    @Data
    public static class RabbitMQ {
        private boolean enabled;
        private String host = "localhost";
        private int port = 5672;
        private String username = "guest";
        private String password = "guest";
        private String virtualHost = "/";
        private int connectionTimeout = 60000;
        private int requestedHeartbeat = 60;
    }

    @Data
    public static class SSH {
        private boolean enabled;
        private String host = "localhost";
        private int port = 22;
        private String username;
        private String password;
        private String privateKeyPath;
        private String knownHostsPath;
        private boolean strictHostKeyChecking = true;
        private int connectionTimeout = 30000;
        private int sessionTimeout = 60000;
    }

    @Data
    public static class SFTP {
        private boolean enabled;
        private String host = "localhost";
        private int port = 22;
        private String username;
        private String password;
        private String privateKeyPath;
        private String knownHostsPath;
        private boolean strictHostKeyChecking = true;
        private int connectionTimeout = 30000;
        private int sessionTimeout = 60000;
        private String remoteDirectory = "/";
        private String localDirectory = "./temp";
        private boolean createLocalDir = true;
    }

    @Data
    public static class FTP {
        private boolean enabled;
        private String host = "localhost";
        private int port = 21;
        private String username;
        private String password;
        private boolean passiveMode = true;
        private String transferMode = "BINARY";
        private int connectionTimeout = 30000;
        private int dataTimeout = 30000;
        private String remoteDirectory = "/";
        private String localDirectory = "./temp";
        private boolean createLocalDir = true;
    }

    @Data
    public static class FTPS {
        private boolean enabled;
        private String host = "localhost";
        private int port = 21;
        private String username;
        private String password;
        private boolean passiveMode = true;
        private String transferMode = "BINARY";
        private int connectionTimeout = 30000;
        private int dataTimeout = 30000;
        private String remoteDirectory = "/";
        private String localDirectory = "./temp";
        private boolean createLocalDir = true;
        private boolean implicitSecurity = false;
        private String securityMode = "TLS";
        private String trustStore;
        private String trustStorePassword;
    }

    @Data
    public static class TibcoEms {
        private boolean enabled;
        private String serverUrl = "tcp://localhost:7222";
        private String username;
        private String password;
        private int connectionTimeout = 30000;
        private int reconnectAttempts = 5;
        private int reconnectDelay = 5000;
        private boolean useSSL = false;
        private String trustStore;
        private String trustStorePassword;
    }

    @Data
    public static class S3 {
        private boolean enabled;
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String endpoint;
        private boolean pathStyleAccess = false;
        private int connectionTimeout = 30000;
        private int readTimeout = 30000;
        private String defaultKeyPrefix = "";
        private boolean createBucket = false;
    }

    @Data
    public static class Routing {
        private String processName;
        private List<Trigger> triggers;
    }

    @Data
    public static class Trigger {
        private String type;
        private String path;
        private String method;
        private String listenTopic;
        private String responseTopic;
        // RabbitMQ specific properties
        private String queueName;
        private String exchangeName;
        private String routingKey;
        private String replyQueueName;
        // SSH/SFTP specific properties
        private String hostKeyChecking;
        private String remoteDirectory;
        private String localDirectory;
        private String filePattern;
        // FTP specific properties
        private String transferMode;
        private boolean passiveMode;
        // S3 specific properties
        private String bucketName;
        private String keyPrefix;
        // TIBCO EMS specific properties
        private String destinationName;
        private String selector;
    }
}
