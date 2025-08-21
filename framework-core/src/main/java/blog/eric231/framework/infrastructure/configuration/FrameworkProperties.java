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
    }
}
