package blog.eric231.framework.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for Redis and Redis Cluster connections.
 * Supports both single Redis instance and Redis Cluster configurations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "framework.redis")
public class FrameworkRedisProperties {
    
    /**
     * Redis connection mode: "standalone" or "cluster"
     */
    private String mode = "standalone";
    
    /**
     * Single Redis instance configuration
     */
    private Standalone standalone = new Standalone();
    
    /**
     * Redis Cluster configuration
     */
    private Cluster cluster = new Cluster();
    
    /**
     * Connection pool configuration
     */
    private Pool pool = new Pool();
    
    /**
     * Connection timeout
     */
    private Duration timeout = Duration.ofMillis(2000);
    
    /**
     * Database index (only for standalone mode)
     */
    private int database = 0;
    
    /**
     * Password for authentication
     */
    private String password;
    
    @Data
    public static class Standalone {
        /**
         * Redis server host
         */
        private String host = "localhost";
        
        /**
         * Redis server port
         */
        private int port = 6379;
    }
    
    @Data
    public static class Cluster {
        /**
         * Redis cluster nodes in format host:port
         */
        private List<String> nodes;
        
        /**
         * Maximum number of redirections to follow
         */
        private int maxRedirects = 3;
    }
    
    @Data
    public static class Pool {
        /**
         * Maximum number of connections in the pool
         */
        private int maxTotal = 8;
        
        /**
         * Maximum number of idle connections in the pool
         */
        private int maxIdle = 8;
        
        /**
         * Minimum number of idle connections in the pool
         */
        private int minIdle = 0;
        
        /**
         * Maximum amount of time to wait for a connection
         */
        private Duration maxWait = Duration.ofMillis(-1);
    }
}