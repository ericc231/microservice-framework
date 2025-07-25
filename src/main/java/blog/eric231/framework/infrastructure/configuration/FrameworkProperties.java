package blog.eric231.framework.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

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
    }

    @Data
    public static class Kafka {
        private boolean enabled;
        private String bootstrapServers;
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
