package blog.eric231.framework.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RedisConfig configuration class
 */
class RedisConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RedisConfig.class);

    @Test
    @DisplayName("Should create RedisProperties bean with default values")
    void testFrameworkRedisPropertiesBean() {
        // Given & When
        FrameworkRedisProperties properties = new FrameworkRedisProperties();

        // Then
        assertEquals("standalone", properties.getMode());
        assertEquals(Duration.ofMillis(2000), properties.getTimeout());
        assertEquals(0, properties.getDatabase());
        assertNull(properties.getPassword());

        // Test standalone defaults
        FrameworkRedisProperties.Standalone standalone = properties.getStandalone();
        assertNotNull(standalone);
        assertEquals("localhost", standalone.getHost());
        assertEquals(6379, standalone.getPort());

        // Test cluster defaults
        FrameworkRedisProperties.Cluster cluster = properties.getCluster();
        assertNotNull(cluster);
        assertEquals(3, cluster.getMaxRedirects());

        // Test pool defaults
        FrameworkRedisProperties.Pool pool = properties.getPool();
        assertNotNull(pool);
        assertEquals(8, pool.getMaxTotal());
        assertEquals(8, pool.getMaxIdle());
        assertEquals(0, pool.getMinIdle());
        assertEquals(Duration.ofMillis(-1), pool.getMaxWait());
    }

    @Test
    @DisplayName("Should not load RedisConfig when redis is disabled")
    void testRedisConfigNotLoadedWhenDisabled() {
        contextRunner
                .withPropertyValues("framework.redis.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisConfig.class);
                    assertThat(context).doesNotHaveBean("frameworkRedisProperties");
                });
    }

    @Test
    @DisplayName("Should load RedisConfig when redis is enabled")
    void testRedisConfigLoadedWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "framework.redis.enabled=true",
                        "framework.redis.mode=standalone",
                        "framework.redis.standalone.host=test-host",
                        "framework.redis.standalone.port=6380"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisConfig.class);
                    assertThat(context).hasBean("frameworkRedisProperties");
                    
                    FrameworkRedisProperties properties = context.getBean("frameworkRedisProperties", FrameworkRedisProperties.class);
                    assertEquals("test-host", properties.getStandalone().getHost());
                    assertEquals(6380, properties.getStandalone().getPort());
                });
    }

    @Test
    @DisplayName("Should create JedisPoolConfig with custom properties")
    void testJedisPoolConfigCreation() {
        // Given
        FrameworkRedisProperties properties = new FrameworkRedisProperties();
        properties.getPool().setMaxTotal(16);
        properties.getPool().setMaxIdle(10);
        properties.getPool().setMinIdle(2);
        properties.getPool().setMaxWait(Duration.ofMillis(5000));

        RedisConfig redisConfig = new RedisConfig();

        // When
        JedisPoolConfig poolConfig = redisConfig.jedisPoolConfig(properties);

        // Then
        assertNotNull(poolConfig);
        assertEquals(16, poolConfig.getMaxTotal());
        assertEquals(10, poolConfig.getMaxIdle());
        assertEquals(2, poolConfig.getMinIdle());
        assertEquals(5000, poolConfig.getMaxWaitMillis());
        
        // Test additional pool settings
        assertTrue(poolConfig.getTestOnBorrow());
        assertTrue(poolConfig.getTestOnReturn());
        assertTrue(poolConfig.getTestWhileIdle());
        assertTrue(poolConfig.getBlockWhenExhausted());
    }

    @Test
    @DisplayName("Should create standalone RedisConnectionFactory")
    void testStandaloneRedisConnectionFactory() {
        contextRunner
                .withPropertyValues(
                        "framework.redis.enabled=true",
                        "framework.redis.mode=standalone",
                        "framework.redis.standalone.host=standalone-host",
                        "framework.redis.standalone.port=6381",
                        "framework.redis.database=1",
                        "framework.redis.password=test-password"
                )
                .run(context -> {
                    assertThat(context).hasBean("standaloneRedisConnectionFactory");
                    
                    RedisConnectionFactory factory = context.getBean("standaloneRedisConnectionFactory", RedisConnectionFactory.class);
                    assertNotNull(factory);
                    assertThat(factory).isInstanceOf(JedisConnectionFactory.class);
                });
    }

    @Test
    @DisplayName("Should create cluster RedisConnectionFactory when mode is cluster")
    void testClusterRedisConnectionFactory() {
        contextRunner
                .withPropertyValues(
                        "framework.redis.enabled=true",
                        "framework.redis.mode=cluster",
                        "framework.redis.cluster.nodes=localhost:7001,localhost:7002,localhost:7003",
                        "framework.redis.cluster.maxRedirects=5"
                )
                .run(context -> {
                    assertThat(context).hasBean("clusterRedisConnectionFactory");
                    
                    RedisConnectionFactory factory = context.getBean("clusterRedisConnectionFactory", RedisConnectionFactory.class);
                    assertNotNull(factory);
                    assertThat(factory).isInstanceOf(JedisConnectionFactory.class);
                });
    }

    @Test
    @DisplayName("Should create RedisTemplate with proper serializers")
    void testRedisTemplateCreation() {
        contextRunner
                .withPropertyValues("framework.redis.enabled=true")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    assertThat(context).hasBean("redisTemplate");
                    
                    RedisTemplate<String, Object> template = context.getBean("redisTemplate", RedisTemplate.class);
                    assertNotNull(template);
                    assertNotNull(template.getKeySerializer());
                    assertNotNull(template.getValueSerializer());
                    assertNotNull(template.getHashKeySerializer());
                    assertNotNull(template.getHashValueSerializer());
                });
    }

    @Test
    @DisplayName("Should handle Redis properties validation")
    void testRedisPropertiesValidation() {
        // Test invalid mode
        FrameworkRedisProperties properties = new FrameworkRedisProperties();
        properties.setMode("invalid-mode");
        
        // Should still create object but with default behavior
        assertNotNull(properties);
        assertEquals("invalid-mode", properties.getMode());
        
        // Test negative values handled gracefully
        properties.getPool().setMaxTotal(-1);
        properties.getPool().setMaxIdle(-1);
        
        // Values should be set (validation happens in actual Redis config)
        assertEquals(-1, properties.getPool().getMaxTotal());
        assertEquals(-1, properties.getPool().getMaxIdle());
    }

    @Test
    @DisplayName("Should handle timeout configuration")
    void testTimeoutConfiguration() {
        FrameworkRedisProperties properties = new FrameworkRedisProperties();
        
        // Test default timeout
        assertEquals(Duration.ofMillis(2000), properties.getTimeout());
        
        // Test custom timeout
        properties.setTimeout(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), properties.getTimeout());
        
        // Test zero timeout
        properties.setTimeout(Duration.ZERO);
        assertEquals(Duration.ZERO, properties.getTimeout());
    }

    @Test
    @DisplayName("Should handle cluster nodes configuration")
    void testClusterNodesConfiguration() {
        FrameworkRedisProperties properties = new FrameworkRedisProperties();
        FrameworkRedisProperties.Cluster cluster = properties.getCluster();
        
        // Test default empty nodes
        assertNotNull(cluster.getNodes());
        assertTrue(cluster.getNodes().isEmpty());
        
        // Test setting nodes
        cluster.getNodes().add("localhost:7001");
        cluster.getNodes().add("localhost:7002");
        cluster.getNodes().add("localhost:7003");
        
        assertEquals(3, cluster.getNodes().size());
        assertTrue(cluster.getNodes().contains("localhost:7001"));
        assertTrue(cluster.getNodes().contains("localhost:7002"));
        assertTrue(cluster.getNodes().contains("localhost:7003"));
    }

    @Test
    @DisplayName("Should handle password configuration")
    void testPasswordConfiguration() {
        FrameworkRedisProperties properties = new FrameworkRedisProperties();
        
        // Test default null password
        assertNull(properties.getPassword());
        
        // Test setting password
        properties.setPassword("secret-password");
        assertEquals("secret-password", properties.getPassword());
        
        // Test empty password
        properties.setPassword("");
        assertEquals("", properties.getPassword());
    }

    @Test
    @DisplayName("Should create configuration with all components when enabled")
    void testFullConfigurationWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "framework.redis.enabled=true",
                        "framework.redis.mode=standalone"
                )
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    // Verify all beans are created
                    assertThat(context).hasBean("frameworkRedisProperties");
                    assertThat(context).hasBean("jedisPoolConfig");
                    assertThat(context).hasBean("standaloneRedisConnectionFactory");
                    assertThat(context).hasBean("redisTemplate");
                    
                    // Verify beans are properly wired
                    FrameworkRedisProperties properties = context.getBean(FrameworkRedisProperties.class);
                    assertNotNull(properties);
                    
                    JedisPoolConfig poolConfig = context.getBean(JedisPoolConfig.class);
                    assertNotNull(poolConfig);
                    
                    RedisConnectionFactory connectionFactory = context.getBean(RedisConnectionFactory.class);
                    assertNotNull(connectionFactory);
                    
                    RedisTemplate<String, Object> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
                    assertNotNull(redisTemplate);
                    assertEquals(connectionFactory, redisTemplate.getConnectionFactory());
                });
    }
}