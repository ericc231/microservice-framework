package blog.eric231.framework.infrastructure.adapter;

import blog.eric231.framework.infrastructure.configuration.RedisProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for Redis Adapter functionality without Spring context
 */
class RedisSimpleTest {
    
    @Test
    void testRedisPropertiesDefaults() {
        RedisProperties properties = new RedisProperties();
        
        assertEquals("standalone", properties.getMode());
        assertEquals("localhost", properties.getStandalone().getHost());
        assertEquals(6379, properties.getStandalone().getPort());
        assertEquals(0, properties.getDatabase());
        assertNull(properties.getPassword());
        
        // Test pool defaults
        assertEquals(8, properties.getPool().getMaxTotal());
        assertEquals(8, properties.getPool().getMaxIdle());
        assertEquals(0, properties.getPool().getMinIdle());
    }
    
    @Test
    void testRedisPropertiesStandaloneConfiguration() {
        RedisProperties properties = new RedisProperties();
        properties.setMode("standalone");
        properties.setDatabase(1);
        properties.setPassword("testpass");
        
        RedisProperties.Standalone standalone = properties.getStandalone();
        standalone.setHost("redis-server");
        standalone.setPort(6380);
        
        assertEquals("standalone", properties.getMode());
        assertEquals(1, properties.getDatabase());
        assertEquals("testpass", properties.getPassword());
        assertEquals("redis-server", standalone.getHost());
        assertEquals(6380, standalone.getPort());
    }
    
    @Test
    void testRedisPropertiesClusterConfiguration() {
        RedisProperties properties = new RedisProperties();
        properties.setMode("cluster");
        properties.setPassword("clusterpass");
        
        RedisProperties.Cluster cluster = properties.getCluster();
        cluster.setNodes(java.util.Arrays.asList("node1:7001", "node2:7002", "node3:7003"));
        cluster.setMaxRedirects(5);
        
        assertEquals("cluster", properties.getMode());
        assertEquals("clusterpass", properties.getPassword());
        assertEquals(3, cluster.getNodes().size());
        assertEquals("node1:7001", cluster.getNodes().get(0));
        assertEquals("node2:7002", cluster.getNodes().get(1));
        assertEquals("node3:7003", cluster.getNodes().get(2));
        assertEquals(5, cluster.getMaxRedirects());
    }
    
    @Test
    void testRedisAdapterExceptionMessage() {
        String message = "Test redis error";
        RuntimeException cause = new RuntimeException("Root cause");
        
        RedisAdapter.RedisAdapterException exception = 
            new RedisAdapter.RedisAdapterException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testRedisClusterAdapterExceptionMessage() {
        String message = "Test redis cluster error";
        RuntimeException cause = new RuntimeException("Root cause");
        
        RedisClusterAdapter.RedisClusterAdapterException exception = 
            new RedisClusterAdapter.RedisClusterAdapterException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testRedisPropertiesPoolConfiguration() {
        RedisProperties properties = new RedisProperties();
        RedisProperties.Pool pool = properties.getPool();
        
        pool.setMaxTotal(20);
        pool.setMaxIdle(15);
        pool.setMinIdle(5);
        
        assertEquals(20, pool.getMaxTotal());
        assertEquals(15, pool.getMaxIdle());
        assertEquals(5, pool.getMinIdle());
    }
    
    @Test
    void testRedisPropertiesNestedObjectsNotNull() {
        RedisProperties properties = new RedisProperties();
        
        assertNotNull(properties.getStandalone());
        assertNotNull(properties.getCluster());
        assertNotNull(properties.getPool());
        
        // Test that nested objects can be modified
        properties.getStandalone().setHost("newhost");
        assertEquals("newhost", properties.getStandalone().getHost());
        
        properties.getCluster().setMaxRedirects(99);
        assertEquals(99, properties.getCluster().getMaxRedirects());
        
        properties.getPool().setMaxTotal(100);
        assertEquals(100, properties.getPool().getMaxTotal());
    }
}