package blog.eric231.framework.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis configuration for both standalone and cluster modes.
 * Configures connection factories, templates, and serializers based on the mode setting.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "framework.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {
    
    // Note: FrameworkRedisProperties is already configured with @ConfigurationProperties
    // and registered as a bean via @Component annotation
    
    /**
     * Jedis connection pool configuration
     */
    @Bean
    public JedisPoolConfig jedisPoolConfig(FrameworkRedisProperties frameworkRedisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        FrameworkRedisProperties.Pool pool = frameworkRedisProperties.getPool();
        
        poolConfig.setMaxTotal(pool.getMaxTotal());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());
        
        // Additional pool settings for better performance
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(true);
        
        log.info("Configured Jedis pool: maxTotal={}, maxIdle={}, minIdle={}, maxWait={}ms",
                pool.getMaxTotal(), pool.getMaxIdle(), pool.getMinIdle(), pool.getMaxWait().toMillis());
        
        return poolConfig;
    }
    
    /**
     * Redis connection factory for standalone mode
     */
    @Bean
    @ConditionalOnProperty(name = "framework.redis.mode", havingValue = "standalone", matchIfMissing = true)
    public RedisConnectionFactory standaloneRedisConnectionFactory(JedisPoolConfig jedisPoolConfig, FrameworkRedisProperties frameworkRedisProperties) {
        log.info("Configuring Redis for standalone mode");
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(frameworkRedisProperties.getStandalone().getHost());
        config.setPort(frameworkRedisProperties.getStandalone().getPort());
        config.setDatabase(frameworkRedisProperties.getDatabase());
        
        if (frameworkRedisProperties.getPassword() != null) {
            config.setPassword(frameworkRedisProperties.getPassword());
        }
        
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(frameworkRedisProperties.getTimeout())
                .readTimeout(frameworkRedisProperties.getTimeout())
                .usePooling()
                .poolConfig(jedisPoolConfig)
                .build();
        
        JedisConnectionFactory factory = new JedisConnectionFactory(config, clientConfig);
        
        log.info("Standalone Redis connection factory configured: {}:{}/{}",
                frameworkRedisProperties.getStandalone().getHost(),
                frameworkRedisProperties.getStandalone().getPort(),
                frameworkRedisProperties.getDatabase());
        
        return factory;
    }
    
    /**
     * Redis connection factory for cluster mode
     */
    @Bean
    @ConditionalOnProperty(name = "framework.redis.mode", havingValue = "cluster")
    public RedisConnectionFactory clusterRedisConnectionFactory(JedisPoolConfig jedisPoolConfig, FrameworkRedisProperties frameworkRedisProperties) {
        log.info("Configuring Redis for cluster mode");
        
        RedisClusterConfiguration config = new RedisClusterConfiguration(frameworkRedisProperties.getCluster().getNodes());
        config.setMaxRedirects(frameworkRedisProperties.getCluster().getMaxRedirects());
        
        if (frameworkRedisProperties.getPassword() != null) {
            config.setPassword(frameworkRedisProperties.getPassword());
        }
        
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(frameworkRedisProperties.getTimeout())
                .readTimeout(frameworkRedisProperties.getTimeout())
                .usePooling()
                .poolConfig(jedisPoolConfig)
                .build();
        
        JedisConnectionFactory factory = new JedisConnectionFactory(config, clientConfig);
        
        log.info("Cluster Redis connection factory configured: nodes={}, maxRedirects={}",
                frameworkRedisProperties.getCluster().getNodes(),
                frameworkRedisProperties.getCluster().getMaxRedirects());
        
        return factory;
    }
    
    /**
     * Redis template with JSON serialization
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        
        template.setDefaultSerializer(jsonRedisSerializer);
        template.afterPropertiesSet();
        
        log.info("Redis template configured with JSON serialization");
        
        return template;
    }
    
    /**
     * String Redis template for simple string operations
     */
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);
        
        template.setDefaultSerializer(stringRedisSerializer);
        template.afterPropertiesSet();
        
        log.info("String Redis template configured");
        
        return template;
    }
    
    /**
     * ObjectMapper bean for JSON serialization (if not already configured)
     */
    @Bean
    @ConditionalOnProperty(name = "framework.redis.object-mapper.auto-configure", havingValue = "true", matchIfMissing = true)
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure ObjectMapper for better Redis serialization
        mapper.findAndRegisterModules();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        
        log.info("Redis ObjectMapper configured with default typing");
        
        return mapper;
    }
}