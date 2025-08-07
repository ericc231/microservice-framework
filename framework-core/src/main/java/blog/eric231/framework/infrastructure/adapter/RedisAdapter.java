package blog.eric231.framework.infrastructure.adapter;

import blog.eric231.framework.infrastructure.configuration.RedisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Adapter providing connection and operation capabilities for standalone Redis instances.
 * Supports common Redis operations including string operations, hash operations, set operations, and TTL management.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "framework.redis.mode", havingValue = "standalone", matchIfMissing = true)
public class RedisAdapter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisProperties redisProperties;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisAdapter(RedisTemplate<String, Object> redisTemplate, 
                       RedisProperties redisProperties, 
                       ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing Redis Adapter for standalone mode");
        log.info("Redis configuration - Host: {}, Port: {}, Database: {}", 
                redisProperties.getStandalone().getHost(),
                redisProperties.getStandalone().getPort(),
                redisProperties.getDatabase());
        
        try {
            // Test connection
            String pingResult = redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis connection test successful: {}", pingResult);
        } catch (Exception e) {
            log.warn("Redis connection test failed: {}", e.getMessage());
        }
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up Redis Adapter");
    }
    
    // String Operations
    
    /**
     * Set a key-value pair
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Set key: {}", key);
        } catch (Exception e) {
            log.error("Error setting key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to set key: " + key, e);
        }
    }
    
    /**
     * Set a key-value pair with expiration
     */
    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
            log.debug("Set key: {} with expiration: {}", key, timeout);
        } catch (Exception e) {
            log.error("Error setting key {} with expiration: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to set key with expiration: " + key, e);
        }
    }
    
    /**
     * Get value by key
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Get key: {} = {}", key, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            log.error("Error getting key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to get key: " + key, e);
        }
    }
    
    /**
     * Get value as specific type
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
            log.error("Error converting key {} to type {}: {}", key, type.getSimpleName(), e.getMessage(), e);
            throw new RedisAdapterException("Failed to convert key to type: " + key, e);
        }
    }
    
    /**
     * Delete a key
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Delete key: {} = {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error deleting key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to delete key: " + key, e);
        }
    }
    
    /**
     * Check if key exists
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking key existence {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to check key existence: " + key, e);
        }
    }
    
    /**
     * Set expiration for a key
     */
    public boolean expire(String key, Duration timeout) {
        try {
            Boolean result = redisTemplate.expire(key, timeout);
            log.debug("Set expiration for key: {} = {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error setting expiration for key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to set expiration for key: " + key, e);
        }
    }
    
    /**
     * Get remaining TTL for a key
     */
    public Duration getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                return null;
            }
            return Duration.ofSeconds(ttl);
        } catch (Exception e) {
            log.error("Error getting TTL for key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to get TTL for key: " + key, e);
        }
    }
    
    // Hash Operations
    
    /**
     * Set hash field
     */
    public void hset(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Hash set: {} field: {}", key, field);
        } catch (Exception e) {
            log.error("Error setting hash field {} for key {}: {}", field, key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to set hash field: " + key + "." + field, e);
        }
    }
    
    /**
     * Get hash field
     */
    public Object hget(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            log.debug("Hash get: {} field: {} = {}", key, field, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            log.error("Error getting hash field {} for key {}: {}", field, key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to get hash field: " + key + "." + field, e);
        }
    }
    
    /**
     * Get all hash fields and values
     */
    public Map<Object, Object> hgetAll(String key) {
        try {
            Map<Object, Object> result = redisTemplate.opsForHash().entries(key);
            log.debug("Hash get all: {} = {} fields", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error getting all hash fields for key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to get all hash fields: " + key, e);
        }
    }
    
    /**
     * Delete hash field
     */
    public boolean hdel(String key, String field) {
        try {
            Long result = redisTemplate.opsForHash().delete(key, field);
            log.debug("Hash delete: {} field: {} = {}", key, field, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Error deleting hash field {} for key {}: {}", field, key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to delete hash field: " + key + "." + field, e);
        }
    }
    
    // Set Operations
    
    /**
     * Add member to set
     */
    public boolean sadd(String key, Object... members) {
        try {
            Long result = redisTemplate.opsForSet().add(key, members);
            log.debug("Set add: {} members count: {}", key, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Error adding members to set {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to add members to set: " + key, e);
        }
    }
    
    /**
     * Get all members of set
     */
    public Set<Object> smembers(String key) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            log.debug("Set members: {} = {} members", key, members != null ? members.size() : 0);
            return members;
        } catch (Exception e) {
            log.error("Error getting set members for key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to get set members: " + key, e);
        }
    }
    
    /**
     * Check if member exists in set
     */
    public boolean sismember(String key, Object member) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, member);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking set membership for key {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to check set membership: " + key, e);
        }
    }
    
    /**
     * Remove member from set
     */
    public boolean srem(String key, Object... members) {
        try {
            Long result = redisTemplate.opsForSet().remove(key, members);
            log.debug("Set remove: {} members count: {}", key, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Error removing members from set {}: {}", key, e.getMessage(), e);
            throw new RedisAdapterException("Failed to remove members from set: " + key, e);
        }
    }
    
    // Utility Methods
    
    /**
     * Test Redis connection
     */
    public boolean testConnection() {
        try {
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equals(result);
        } catch (Exception e) {
            log.error("Redis connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get Redis info
     */
    public String getInfo() {
        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            return info != null ? info.toString() : "No info available";
        } catch (Exception e) {
            log.error("Error getting Redis info: {}", e.getMessage(), e);
            throw new RedisAdapterException("Failed to get Redis info", e);
        }
    }
    
    /**
     * Serialize object to JSON string
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage(), e);
            throw new RedisAdapterException("Failed to serialize object to JSON", e);
        }
    }
    
    /**
     * Custom exception for Redis operations
     */
    public static class RedisAdapterException extends RuntimeException {
        public RedisAdapterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}