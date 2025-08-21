package blog.eric231.framework.infrastructure.adapter;

import blog.eric231.framework.infrastructure.configuration.FrameworkRedisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisClusterAdapter
 * Uses mocks since cluster setup is complex
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisClusterAdapterTest {

    @Mock
    private RedisTemplate<String, Object> mockRedisTemplate;

    @Mock
    private FrameworkRedisProperties mockRedisProperties;

    @Mock
    private ObjectMapper mockObjectMapper;

    @Mock
    private RedisConnectionFactory mockConnectionFactory;

    @Mock
    private ValueOperations<String, Object> mockValueOperations;

    @Mock
    private HashOperations<String, Object, Object> mockHashOperations;

    @Mock
    private SetOperations<String, Object> mockSetOperations;

    private RedisClusterAdapter redisClusterAdapter;
    private FrameworkRedisProperties.Cluster clusterProperties;

    @BeforeEach
    void setUp() {
        // Setup cluster properties
        clusterProperties = new FrameworkRedisProperties.Cluster();
        clusterProperties.setNodes(Arrays.asList(
            "localhost:7001", "localhost:7002", "localhost:7003"
        ));
        clusterProperties.setMaxRedirects(5);

        when(mockRedisProperties.getCluster()).thenReturn(clusterProperties);

        // Setup template operations
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);
        when(mockRedisTemplate.opsForHash()).thenReturn(mockHashOperations);
        when(mockRedisTemplate.opsForSet()).thenReturn(mockSetOperations);

        redisClusterAdapter = new RedisClusterAdapter(
            mockRedisTemplate, 
            mockRedisProperties, 
            mockObjectMapper,
            mockConnectionFactory
        );
    }

    @Test
    @DisplayName("Should initialize cluster adapter with correct configuration")
    void testInitialization() {
        assertNotNull(redisClusterAdapter);
        verify(mockRedisProperties, atLeastOnce()).getCluster();
    }

    @Test
    @DisplayName("Should set and get string values")
    void testSetAndGetString() {
        // Given
        String key = "test:key";
        String value = "test-value";

        when(mockValueOperations.get(key)).thenReturn(value);

        // When
        redisClusterAdapter.set(key, value);
        Object result = redisClusterAdapter.get(key);

        // Then
        verify(mockValueOperations).set(key, value);
        verify(mockValueOperations).get(key);
        assertEquals(value, result);
    }

    @Test
    @DisplayName("Should set string values with TTL")
    void testSetWithTtl() {
        // Given
        String key = "test:ttl";
        String value = "ttl-value";
        Duration timeout = Duration.ofSeconds(300);

        // When
        redisClusterAdapter.set(key, value, timeout);

        // Then
        verify(mockValueOperations).set(key, value, timeout);
    }

    @Test
    @DisplayName("Should handle hash operations")
    void testHashOperations() {
        // Given
        String key = "test:hash";
        String field = "field1";
        String value = "value1";
        Map<Object, Object> hashValues = Map.of("field1", "value1", "field2", "value2");

        when(mockHashOperations.get(key, field)).thenReturn(value);
        when(mockHashOperations.entries(key)).thenReturn(hashValues);

        // When & Then - hset
        redisClusterAdapter.hset(key, field, value);
        verify(mockHashOperations).put(key, field, value);

        // When & Then - hget
        Object result = redisClusterAdapter.hget(key, field);
        verify(mockHashOperations).get(key, field);
        assertEquals(value, result);

        // When & Then - hgetAll
        Map<Object, Object> allResults = redisClusterAdapter.hgetAll(key);
        verify(mockHashOperations).entries(key);
        assertEquals(hashValues, allResults);
    }

    @Test
    @DisplayName("Should handle set operations")
    void testSetOperations() {
        // Given
        String key = "test:set";
        String member1 = "member1";
        String member2 = "member2";
        Set<Object> members = Set.of(member1, member2);

        when(mockSetOperations.isMember(key, member1)).thenReturn(true);
        when(mockSetOperations.isMember(key, member2)).thenReturn(true);
        when(mockSetOperations.members(key)).thenReturn(members);
        when(mockSetOperations.add(key, member1, member2)).thenReturn(2L);

        // When & Then - sadd
        boolean addResult = redisClusterAdapter.sadd(key, member1, member2);
        verify(mockSetOperations).add(key, member1, member2);
        assertTrue(addResult);

        // When & Then - sismember
        assertTrue(redisClusterAdapter.sismember(key, member1));
        assertTrue(redisClusterAdapter.sismember(key, member2));
        verify(mockSetOperations).isMember(key, member1);
        verify(mockSetOperations).isMember(key, member2);

        // When & Then - smembers
        Set<Object> result = redisClusterAdapter.smembers(key);
        verify(mockSetOperations).members(key);
        assertEquals(members, result);
    }

    @Test
    @DisplayName("Should check key existence")
    void testKeyExists() {
        // Given
        String key = "test:exists";

        when(mockRedisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean exists = redisClusterAdapter.exists(key);

        // Then
        verify(mockRedisTemplate).hasKey(key);
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should delete keys")
    void testKeyDeletion() {
        // Given
        String key = "test:delete";

        when(mockRedisTemplate.delete(key)).thenReturn(true);

        // When
        boolean deleted = redisClusterAdapter.delete(key);

        // Then
        verify(mockRedisTemplate).delete(key);
        assertTrue(deleted);
    }

    @Test
    @DisplayName("Should get TTL")
    void testGetTtl() {
        // Given
        String key = "test:ttl";
        long expectedTtlSeconds = 300L;

        when(mockRedisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(expectedTtlSeconds);

        // When
        Duration ttl = redisClusterAdapter.getTtl(key);

        // Then
        verify(mockRedisTemplate).getExpire(key, TimeUnit.SECONDS);
        assertEquals(Duration.ofSeconds(expectedTtlSeconds), ttl);
    }

    @Test
    @DisplayName("Should handle typed object operations")
    void testTypedObjectOperations() throws Exception {
        // Given
        String key = "test:object";
        TestObject testObject = new TestObject("test", 123);
        String jsonString = "{\"name\":\"test\",\"value\":123}";

        when(mockObjectMapper.readValue(jsonString, TestObject.class)).thenReturn(testObject);
        when(mockValueOperations.get(key)).thenReturn(jsonString);

        // When - get with type
        TestObject result = redisClusterAdapter.get(key, TestObject.class);

        // Then - get with type
        verify(mockValueOperations).get(key);
        verify(mockObjectMapper).readValue(jsonString, TestObject.class);
        assertEquals(testObject.getName(), result.getName());
        assertEquals(testObject.getValue(), result.getValue());
    }

    @Test
    @DisplayName("Should handle cluster node failures gracefully")
    void testClusterFailureHandling() {
        // Given
        String key = "test:failure";
        RuntimeException clusterException = new RuntimeException("Cluster node unavailable");

        when(mockValueOperations.get(key)).thenThrow(clusterException);

        // When & Then
        assertThrows(RedisClusterAdapter.RedisClusterAdapterException.class, () -> {
            redisClusterAdapter.get(key);
        });
    }

    @Test
    @DisplayName("Should handle JSON serialization")
    void testJsonSerialization() throws Exception {
        // Given
        TestObject testObject = new TestObject("test", 123);
        String expectedJson = "{\"name\":\"test\",\"value\":123}";
        
        when(mockObjectMapper.writeValueAsString(testObject)).thenReturn(expectedJson);

        // When
        String result = redisClusterAdapter.toJson(testObject);

        // Then
        verify(mockObjectMapper).writeValueAsString(testObject);
        assertEquals(expectedJson, result);
    }

    @Test
    @DisplayName("Should handle deserialization errors")
    void testDeserializationErrorHandling() throws Exception {
        // Given
        String key = "test:deserialization";
        String invalidJson = "invalid-json";

        when(mockValueOperations.get(key)).thenReturn(invalidJson);
        when(mockObjectMapper.readValue(invalidJson, TestObject.class))
            .thenThrow(new RuntimeException("Deserialization failed"));

        // When & Then
        assertThrows(RedisClusterAdapter.RedisClusterAdapterException.class, () -> {
            redisClusterAdapter.get(key, TestObject.class);
        });
    }

    @Test
    @DisplayName("Should test cluster connection")
    void testClusterConnection() {
        // Given
        when(mockConnectionFactory.getClusterConnection()).thenReturn(null);

        // When
        boolean isConnected = redisClusterAdapter.testClusterConnection();

        // Then
        verify(mockConnectionFactory).getClusterConnection();
        assertFalse(isConnected);
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullValueHandling() {
        // Given
        String key = "test:null";

        when(mockValueOperations.get(key)).thenReturn(null);

        // When
        Object result = redisClusterAdapter.get(key);

        // Then
        verify(mockValueOperations).get(key);
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle empty cluster configuration")
    void testEmptyClusterConfiguration() {
        // Given
        FrameworkRedisProperties.Cluster emptyCluster = new FrameworkRedisProperties.Cluster();
        when(mockRedisProperties.getCluster()).thenReturn(emptyCluster);

        // When & Then - should not throw exception during initialization
        assertDoesNotThrow(() -> {
            new RedisClusterAdapter(mockRedisTemplate, mockRedisProperties, mockObjectMapper, mockConnectionFactory);
        });
    }

    // Test object for serialization tests
    public static class TestObject {
        private String name;
        private Integer value;

        public TestObject() {}

        public TestObject(String name, Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }
}