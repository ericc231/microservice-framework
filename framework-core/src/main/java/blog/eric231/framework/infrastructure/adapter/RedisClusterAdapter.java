package blog.eric231.framework.infrastructure.adapter;

import blog.eric231.framework.infrastructure.configuration.FrameworkRedisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cluster Adapter providing connection and operation capabilities for Redis Cluster deployments.
 * Supports Redis Cluster specific operations and handles cluster topology changes gracefully.
 */
@Component
@ConditionalOnExpression("'${framework.redis.enabled:false}'=='true' and '${framework.redis.mode:standalone}'=='cluster'")
@Slf4j
public class RedisClusterAdapter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final FrameworkRedisProperties redisProperties;
    private final ObjectMapper objectMapper;
    private final RedisConnectionFactory connectionFactory;
    
    @Autowired
    public RedisClusterAdapter(RedisTemplate<String, Object> redisTemplate,
                              FrameworkRedisProperties redisProperties,
                              ObjectMapper objectMapper,
                              RedisConnectionFactory connectionFactory) {
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
        this.objectMapper = objectMapper;
        this.connectionFactory = connectionFactory;
    }
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing Redis Cluster Adapter");
        log.info("Redis Cluster configuration - Nodes: {}, Max Redirects: {}",
                redisProperties.getCluster().getNodes(),
                redisProperties.getCluster().getMaxRedirects());
        
        try {
            // Test cluster connection
            testClusterConnection();
            log.info("Redis Cluster connection test successful");
        } catch (Exception e) {
            log.warn("Redis Cluster connection test failed: {}", e.getMessage());
        }
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up Redis Cluster Adapter");
    }
    
    // String Operations
    
    /**
     * Set a key-value pair in cluster
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Cluster set key: {}", key);
        } catch (Exception e) {
            log.error("Error setting key {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to set key in cluster: " + key, e);
        }
    }
    
    /**
     * Set a key-value pair with expiration in cluster
     */
    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
            log.debug("Cluster set key: {} with expiration: {}", key, timeout);
        } catch (Exception e) {
            log.error("Error setting key {} with expiration in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to set key with expiration in cluster: " + key, e);
        }
    }
    
    /**
     * Get value by key from cluster
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Cluster get key: {} = {}", key, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            log.error("Error getting key {} from cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get key from cluster: " + key, e);
        }
    }
    
    /**
     * Get value as specific type from cluster
     */
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = get(key);
            if (value == null) {
                return null;
            }
            
            if (type.isAssignableFrom(value.getClass())) {
                return type.cast(value);
            }
            
            // Try to convert using ObjectMapper
            if (value instanceof String) {
                return objectMapper.readValue((String) value, type);
            }
            
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.error("Error converting key {} to type {} in cluster: {}", key, type.getSimpleName(), e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to convert key to type in cluster: " + key, e);
        }
    }
    
    /**
     * Delete a key from cluster
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Cluster delete key: {} = {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error deleting key {} from cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to delete key from cluster: " + key, e);
        }
    }
    
    /**
     * Check if key exists in cluster
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking key existence {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to check key existence in cluster: " + key, e);
        }
    }
    
    /**
     * Set expiration for a key in cluster
     */
    public boolean expire(String key, Duration timeout) {
        try {
            Boolean result = redisTemplate.expire(key, timeout);
            log.debug("Cluster set expiration for key: {} = {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error setting expiration for key {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to set expiration for key in cluster: " + key, e);
        }
    }
    
    /**
     * Get remaining TTL for a key in cluster
     */
    public Duration getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                return null;
            }
            return Duration.ofSeconds(ttl);
        } catch (Exception e) {
            log.error("Error getting TTL for key {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get TTL for key in cluster: " + key, e);
        }
    }
    
    // Hash Operations
    
    /**
     * Set hash field in cluster
     */
    public void hset(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Cluster hash set: {} field: {}", key, field);
        } catch (Exception e) {
            log.error("Error setting hash field {} for key {} in cluster: {}", field, key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to set hash field in cluster: " + key + "." + field, e);
        }
    }
    
    /**
     * Get hash field from cluster
     */
    public Object hget(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            log.debug("Cluster hash get: {} field: {} = {}", key, field, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            log.error("Error getting hash field {} for key {} from cluster: {}", field, key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get hash field from cluster: " + key + "." + field, e);
        }
    }
    
    /**
     * Get all hash fields and values from cluster
     */
    public Map<Object, Object> hgetAll(String key) {
        try {
            Map<Object, Object> result = redisTemplate.opsForHash().entries(key);
            log.debug("Cluster hash get all: {} = {} fields", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error getting all hash fields for key {} from cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get all hash fields from cluster: " + key, e);
        }
    }
    
    /**
     * Delete hash field from cluster
     */
    public boolean hdel(String key, String field) {
        try {
            Long result = redisTemplate.opsForHash().delete(key, field);
            log.debug("Cluster hash delete: {} field: {} = {}", key, field, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Error deleting hash field {} for key {} from cluster: {}", field, key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to delete hash field from cluster: " + key + "." + field, e);
        }
    }
    
    // Set Operations
    
    /**
     * Add member to set in cluster
     */
    public boolean sadd(String key, Object... members) {
        try {
            Long result = redisTemplate.opsForSet().add(key, members);
            log.debug("Cluster set add: {} members count: {}", key, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Error adding members to set {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to add members to set in cluster: " + key, e);
        }
    }
    
    /**
     * Get all members of set from cluster
     */
    public Set<Object> smembers(String key) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            log.debug("Cluster set members: {} = {} members", key, members != null ? members.size() : 0);
            return members;
        } catch (Exception e) {
            log.error("Error getting set members for key {} from cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get set members from cluster: " + key, e);
        }
    }
    
    /**
     * Check if member exists in set in cluster
     */
    public boolean sismember(String key, Object member) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, member);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking set membership for key {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to check set membership in cluster: " + key, e);
        }
    }
    
    /**
     * Remove member from set in cluster
     */
    public boolean srem(String key, Object... members) {
        try {
            Long result = redisTemplate.opsForSet().remove(key, members);
            log.debug("Cluster set remove: {} members count: {}", key, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Error removing members from set {} in cluster: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to remove members from set in cluster: " + key, e);
        }
    }
    
    // Cluster-specific Operations
    
    /**
     * Get cluster info
     */
    public String getClusterInfo() {
        try {
            RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
            if (clusterConnection != null) {
                return clusterConnection.clusterGetClusterInfo().toString();
            }
            throw new RedisClusterAdapterException("No cluster connection available", null);
        } catch (Exception e) {
            log.error("Error getting cluster info: {}", e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get cluster info", e);
        }
    }
    
    /**
     * Get cluster nodes info
     */
    public Set<String> getClusterNodes() {
        try {
            RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
            if (clusterConnection != null) {
                Set<String> nodeStrings = new HashSet<>();
                Iterable<?> nodes = clusterConnection.clusterGetNodes();
                for (Object node : nodes) {
                    // Use toString to get node information instead of direct property access
                    nodeStrings.add(node.toString());
                }
                return nodeStrings;
            }
            throw new RedisClusterAdapterException("No cluster connection available", null);
        } catch (Exception e) {
            log.error("Error getting cluster nodes: {}", e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get cluster nodes", e);
        }
    }
    
    /**
     * Execute command on specific cluster slot
     */
    public <T> T executeOnSlot(int slot, RedisClusterCallback<T> callback) {
        try {
            RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
            if (clusterConnection != null) {
                return callback.execute(clusterConnection, slot);
            }
            throw new RedisClusterAdapterException("No cluster connection available", null);
        } catch (Exception e) {
            log.error("Error executing command on slot {}: {}", slot, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to execute command on slot: " + slot, e);
        }
    }
    
    /**
     * Get cluster slot for key
     */
    public int getSlotForKey(String key) {
        try {
            RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
            if (clusterConnection != null) {
                return clusterConnection.clusterGetSlotForKey(key.getBytes());
            }
            throw new RedisClusterAdapterException("No cluster connection available", null);
        } catch (Exception e) {
            log.error("Error getting slot for key {}: {}", key, e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to get slot for key: " + key, e);
        }
    }
    
    // Utility Methods
    
    /**
     * Test Redis cluster connection
     */
    public boolean testClusterConnection() {
        try {
            RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
            if (clusterConnection != null) {
                String info = clusterConnection.clusterGetClusterInfo().toString();
                return info.contains("cluster_state:ok");
            }
            return false;
        } catch (Exception e) {
            log.error("Redis cluster connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get cluster statistics
     */
    public Map<String, Object> getClusterStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
            if (clusterConnection != null) {
                String clusterInfo = clusterConnection.clusterGetClusterInfo().toString();
                stats.put("clusterInfo", clusterInfo);
                stats.put("nodeCount", getClusterNodes().size());
                stats.put("connectionStatus", "connected");
            } else {
                stats.put("connectionStatus", "disconnected");
            }
        } catch (Exception e) {
            log.error("Error getting cluster stats: {}", e.getMessage(), e);
            stats.put("connectionStatus", "error");
            stats.put("error", e.getMessage());
        }
        return stats;
    }
    
    /**
     * Serialize object to JSON string
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage(), e);
            throw new RedisClusterAdapterException("Failed to serialize object to JSON", e);
        }
    }
    
    /**
     * Callback interface for cluster-specific operations
     */
    @FunctionalInterface
    public interface RedisClusterCallback<T> {
        T execute(RedisClusterConnection connection, int slot) throws DataAccessException;
    }
    
    /**
     * Custom exception for Redis cluster operations
     */
    public static class RedisClusterAdapterException extends RuntimeException {
        public RedisClusterAdapterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}