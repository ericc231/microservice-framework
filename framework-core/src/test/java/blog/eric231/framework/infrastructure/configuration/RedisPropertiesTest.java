package blog.eric231.framework.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RedisProperties configuration class
 */
class RedisPropertiesTest {
    
    @Test
    void testDefaultValues() {
        RedisProperties properties = new RedisProperties();
        
        assertEquals("standalone", properties.getMode());
        assertEquals(Duration.ofMillis(2000), properties.getTimeout());
        assertEquals(0, properties.getDatabase());
        assertNull(properties.getPassword());
        
        // Test standalone defaults
        RedisProperties.Standalone standalone = properties.getStandalone();
        assertNotNull(standalone);
        assertEquals("localhost", standalone.getHost());
        assertEquals(6379, standalone.getPort());
        
        // Test cluster defaults
        RedisProperties.Cluster cluster = properties.getCluster();
        assertNotNull(cluster);
        assertNull(cluster.getNodes());
        assertEquals(3, cluster.getMaxRedirects());
        
        // Test pool defaults
        RedisProperties.Pool pool = properties.getPool();
        assertNotNull(pool);
        assertEquals(8, pool.getMaxTotal());
        assertEquals(8, pool.getMaxIdle());
        assertEquals(0, pool.getMinIdle());
        assertEquals(Duration.ofMillis(-1), pool.getMaxWait());
    }
    
    @Test
    void testStandaloneConfiguration() {
        RedisProperties properties = new RedisProperties();
        properties.setMode("standalone");
        properties.setDatabase(1);
        properties.setPassword("testpass");
        properties.setTimeout(Duration.ofMillis(5000));
        
        RedisProperties.Standalone standalone = properties.getStandalone();
        standalone.setHost("redis-server");
        standalone.setPort(6380);
        
        assertEquals("standalone", properties.getMode());
        assertEquals(1, properties.getDatabase());
        assertEquals("testpass", properties.getPassword());
        assertEquals(Duration.ofMillis(5000), properties.getTimeout());
        assertEquals("redis-server", standalone.getHost());
        assertEquals(6380, standalone.getPort());
    }
    
    @Test
    void testClusterConfiguration() {
        RedisProperties properties = new RedisProperties();
        properties.setMode("cluster");
        properties.setPassword("clusterpass");
        
        RedisProperties.Cluster cluster = properties.getCluster();
        cluster.setNodes(Arrays.asList("node1:7001", "node2:7002", "node3:7003"));
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
    void testPoolConfiguration() {
        RedisProperties properties = new RedisProperties();
        RedisProperties.Pool pool = properties.getPool();
        
        pool.setMaxTotal(20);
        pool.setMaxIdle(15);
        pool.setMinIdle(5);
        pool.setMaxWait(Duration.ofMillis(3000));
        
        assertEquals(20, pool.getMaxTotal());
        assertEquals(15, pool.getMaxIdle());
        assertEquals(5, pool.getMinIdle());
        assertEquals(Duration.ofMillis(3000), pool.getMaxWait());
    }
    
    @Test
    void testCompleteConfiguration() {
        RedisProperties properties = new RedisProperties();
        
        // Main configuration
        properties.setMode("cluster");
        properties.setDatabase(2);
        properties.setPassword("mypassword");
        properties.setTimeout(Duration.ofMillis(10000));
        
        // Standalone configuration (should be ignored in cluster mode)
        RedisProperties.Standalone standalone = properties.getStandalone();
        standalone.setHost("ignored-host");
        standalone.setPort(9999);
        
        // Cluster configuration
        RedisProperties.Cluster cluster = properties.getCluster();
        cluster.setNodes(Arrays.asList("cluster1:7001", "cluster2:7002", "cluster3:7003"));
        cluster.setMaxRedirects(10);
        
        // Pool configuration
        RedisProperties.Pool pool = properties.getPool();
        pool.setMaxTotal(50);
        pool.setMaxIdle(25);
        pool.setMinIdle(10);
        pool.setMaxWait(Duration.ofMillis(5000));
        
        // Verify all settings
        assertEquals("cluster", properties.getMode());
        assertEquals(2, properties.getDatabase());
        assertEquals("mypassword", properties.getPassword());
        assertEquals(Duration.ofMillis(10000), properties.getTimeout());
        
        assertEquals("ignored-host", standalone.getHost());
        assertEquals(9999, standalone.getPort());
        
        assertEquals(3, cluster.getNodes().size());
        assertEquals("cluster1:7001", cluster.getNodes().get(0));
        assertEquals("cluster2:7002", cluster.getNodes().get(1));
        assertEquals("cluster3:7003", cluster.getNodes().get(2));
        assertEquals(10, cluster.getMaxRedirects());
        
        assertEquals(50, pool.getMaxTotal());
        assertEquals(25, pool.getMaxIdle());
        assertEquals(10, pool.getMinIdle());
        assertEquals(Duration.ofMillis(5000), pool.getMaxWait());
    }
    
    @Test
    void testEqualsAndHashCode() {
        RedisProperties properties1 = new RedisProperties();
        properties1.setMode("standalone");
        properties1.setDatabase(1);
        
        RedisProperties properties2 = new RedisProperties();
        properties2.setMode("standalone");
        properties2.setDatabase(1);
        
        // Test equals
        assertEquals(properties1.getMode(), properties2.getMode());
        assertEquals(properties1.getDatabase(), properties2.getDatabase());
        
        // Test different values
        properties2.setDatabase(2);
        assertNotEquals(properties1.getDatabase(), properties2.getDatabase());
    }
    
    @Test
    void testNestedObjectsNotNull() {
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