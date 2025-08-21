package blog.eric231.examples.basicrestredis.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedisDataTest {

    @Test
    void testDefaultConstructor() {
        // Act
        RedisData redisData = new RedisData();

        // Assert
        assertNotNull(redisData);
        assertNull(redisData.getId());
        assertNull(redisData.getName());
        assertNull(redisData.getDescription());
        assertNull(redisData.getValue());
        assertEquals("default", redisData.getCategory());
        assertEquals("active", redisData.getStatus());
        assertEquals(1L, redisData.getVersion());
        assertNotNull(redisData.getCreatedAt());
        assertNotNull(redisData.getUpdatedAt());
        assertNotNull(redisData.getMetadata());
        assertTrue(redisData.getMetadata().isEmpty());
    }

    @Test
    void testParameterizedConstructor() {
        // Act
        RedisData redisData = new RedisData("test-id", "Test Name", "Test Description", "Test Value");

        // Assert
        assertEquals("test-id", redisData.getId());
        assertEquals("Test Name", redisData.getName());
        assertEquals("Test Description", redisData.getDescription());
        assertEquals("Test Value", redisData.getValue());
        assertEquals("default", redisData.getCategory());
        assertEquals("active", redisData.getStatus());
        assertEquals(1L, redisData.getVersion());
        assertNotNull(redisData.getCreatedAt());
        assertNotNull(redisData.getUpdatedAt());
    }

    @Test
    void testBasicConstructorWithCategory() {
        // Act
        RedisData redisData = new RedisData("test-id", "Test Name", "Test Description", "Test Value");
        redisData.setCategory("test-category");

        // Assert
        assertEquals("test-id", redisData.getId());
        assertEquals("Test Name", redisData.getName());
        assertEquals("Test Description", redisData.getDescription());
        assertEquals("Test Value", redisData.getValue());
        assertEquals("test-category", redisData.getCategory());
        assertEquals("active", redisData.getStatus());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        RedisData redisData = new RedisData();
        LocalDateTime testTime = LocalDateTime.now().minusDays(1);

        // Act
        redisData.setId("new-id");
        redisData.setName("New Name");
        redisData.setDescription("New Description");
        redisData.setValue("New Value");
        redisData.setCategory("new-category");
        redisData.setStatus("inactive");
        redisData.setVersion(5L);
        redisData.setCreatedAt(testTime);
        redisData.setUpdatedAt(testTime);

        // Assert
        assertEquals("new-id", redisData.getId());
        assertEquals("New Name", redisData.getName());
        assertEquals("New Description", redisData.getDescription());
        assertEquals("New Value", redisData.getValue());
        assertEquals("new-category", redisData.getCategory());
        assertEquals("inactive", redisData.getStatus());
        assertEquals(5L, redisData.getVersion());
        assertEquals(testTime, redisData.getCreatedAt());
        assertEquals(testTime, redisData.getUpdatedAt());
    }

    @Test
    void testMetadataOperations() {
        // Arrange
        RedisData redisData = new RedisData();

        // Act & Assert - Add metadata
        redisData.addMetadata("key1", "value1");
        redisData.addMetadata("key2", 123);
        redisData.addMetadata("key3", true);

        assertEquals("value1", redisData.getMetadata().get("key1"));
        assertEquals(123, redisData.getMetadata().get("key2"));
        assertEquals(true, redisData.getMetadata().get("key3"));
        assertNull(redisData.getMetadata().get("nonexistent"));

        // Set metadata map
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("newKey", "newValue");
        redisData.setMetadata(newMetadata);

        assertEquals("newValue", redisData.getMetadata().get("newKey"));
        assertNull(redisData.getMetadata().get("key1")); // Should be replaced
    }

    @Test
    void testTouch() {
        // Arrange
        RedisData redisData = new RedisData();
        LocalDateTime originalTime = redisData.getUpdatedAt();
        Long originalVersion = redisData.getVersion();

        // Wait a bit to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        redisData.touch();

        // Assert
        assertTrue(redisData.getUpdatedAt().isAfter(originalTime));
        assertEquals(originalVersion + 1, redisData.getVersion());
    }

    @Test
    void testArchive() {
        // Arrange
        RedisData redisData = new RedisData();
        LocalDateTime beforeArchive = redisData.getUpdatedAt();
        String originalStatus = redisData.getStatus();

        // Wait a bit to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        redisData.archive();

        // Assert
        assertEquals("archived", redisData.getStatus());
        assertTrue(redisData.getUpdatedAt().isAfter(beforeArchive));
        assertNotEquals(originalStatus, redisData.getStatus());
    }

    @Test
    void testSetActive() {
        // Arrange
        RedisData redisData = new RedisData();
        redisData.setStatus("inactive");

        // Act
        redisData.setStatus("active");

        // Assert
        assertEquals("active", redisData.getStatus());
        assertTrue(redisData.isActive());
    }

    @Test
    void testSetInactive() {
        // Arrange
        RedisData redisData = new RedisData();
        assertEquals("active", redisData.getStatus()); // Default status

        // Act
        redisData.setStatus("inactive");

        // Assert
        assertEquals("inactive", redisData.getStatus());
        assertFalse(redisData.isActive());
    }

    @Test
    void testIsActive() {
        // Arrange
        RedisData redisData = new RedisData();

        // Assert - Default is active
        assertTrue(redisData.isActive());

        // Act & Assert - Set to inactive
        redisData.setStatus("inactive");
        assertFalse(redisData.isActive());

        // Act & Assert - Set to archived
        redisData.setStatus("archived");
        assertFalse(redisData.isActive());

        // Act & Assert - Set back to active
        redisData.setStatus("active");
        assertTrue(redisData.isActive());
    }

    @Test
    void testIsArchived() {
        // Arrange
        RedisData redisData = new RedisData();

        // Assert - Default is not archived
        assertNotEquals("archived", redisData.getStatus());

        // Act & Assert - Set to archived
        redisData.setStatus("archived");
        assertEquals("archived", redisData.getStatus());

        // Act & Assert - Set to active
        redisData.setStatus("active");
        assertNotEquals("archived", redisData.getStatus());

        // Act & Assert - Set to inactive
        redisData.setStatus("inactive");
        assertNotEquals("archived", redisData.getStatus());
    }

    @Test
    void testToString() {
        // Arrange
        RedisData redisData = new RedisData("test-id", "Test Name", "Test Description", "Test Value");
        redisData.setCategory("test-category");

        // Act
        String result = redisData.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("test-id"));
        assertTrue(result.contains("Test Name"));
        assertTrue(result.contains("test-category"));
        assertTrue(result.contains("active")); // Default status
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange - test equality based on object identity since timestamps differ
        RedisData data1 = new RedisData("test-id", "Test Name", "Description", "Value");
        RedisData data2 = data1; // Same reference
        RedisData data3 = new RedisData("different-id", "Test Name", "Description", "Value");

        // Assert equals
        assertEquals(data1, data2); // Same reference should be equal
        assertNotEquals(data1, data3); // Different objects should not be equal
        assertNotEquals(data1, null);
        assertNotEquals(data1, "string");

        // Assert hashCode consistency
        assertEquals(data1.hashCode(), data2.hashCode()); // Same reference should have same hash
        
        // Test reflexivity
        assertEquals(data1, data1);
        
        // Test ID-based comparison with manual setup
        RedisData data4 = new RedisData();
        data4.setId("test-id");
        data4.setName("Test Name");
        data4.setDescription("Description");
        data4.setValue("Value");
        data4.setCreatedAt(data1.getCreatedAt()); // Set same timestamps
        data4.setUpdatedAt(data1.getUpdatedAt());
        
        assertEquals(data1, data4); // Should be equal with same content and timestamps
    }

    @Test
    void testNullIdHandling() {
        // Arrange
        RedisData data1 = new RedisData();
        RedisData data2 = new RedisData();
        RedisData data3 = new RedisData();
        data3.setId("test-id");

        // Act & Assert
        assertEquals(data1, data2); // Both have null ID
        assertNotEquals(data1, data3); // One null, one not null
        assertEquals(data1.hashCode(), data2.hashCode()); // Same hash for null IDs
    }
}