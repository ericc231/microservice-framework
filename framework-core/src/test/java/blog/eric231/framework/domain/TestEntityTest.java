package blog.eric231.framework.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestEntityTest {

    private TestEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TestEntity();
    }

    @Test
    void testDefaultConstructor_ShouldCreateInstance() {
        assertNotNull(testEntity);
        assertNull(testEntity.getId());
        assertNull(testEntity.getName());
    }

    @Test
    void testSetAndGetId() {
        Long expectedId = 123L;
        testEntity.setId(expectedId);
        
        assertEquals(expectedId, testEntity.getId());
    }

    @Test
    void testSetAndGetName() {
        String expectedName = "Test Name";
        testEntity.setName(expectedName);
        
        assertEquals(expectedName, testEntity.getName());
    }

    @Test
    void testSetIdToNull() {
        testEntity.setId(1L);
        testEntity.setId(null);
        
        assertNull(testEntity.getId());
    }

    @Test
    void testSetNameToNull() {
        testEntity.setName("Initial Name");
        testEntity.setName(null);
        
        assertNull(testEntity.getName());
    }

    @Test
    void testSetNameToEmptyString() {
        testEntity.setName("");
        
        assertEquals("", testEntity.getName());
    }

    @Test
    void testSetNameWithSpecialCharacters() {
        String specialName = "Test!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
        testEntity.setName(specialName);
        
        assertEquals(specialName, testEntity.getName());
    }

    @Test
    void testSetNameWithUnicodeCharacters() {
        String unicodeName = "Test Unicode Characters";
        testEntity.setName(unicodeName);
        
        assertEquals(unicodeName, testEntity.getName());
    }

    @Test
    void testEntityEqualsAndHashCode_WithLombokGeneratedMethods() {
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);
        entity1.setName("Test");

        TestEntity entity2 = new TestEntity();
        entity2.setId(1L);
        entity2.setName("Test");

        TestEntity entity3 = new TestEntity();
        entity3.setId(2L);
        entity3.setName("Test");

        // Test equals
        assertEquals(entity1, entity2);
        assertNotEquals(entity1, entity3);
        assertNotEquals(entity1, null);
        assertNotEquals(entity1, "Not an entity");

        // Test hashCode consistency
        assertEquals(entity1.hashCode(), entity2.hashCode());
        assertNotEquals(entity1.hashCode(), entity3.hashCode());
    }

    @Test
    void testToString_ShouldContainFieldValues() {
        testEntity.setId(42L);
        testEntity.setName("Test Entity");
        
        String toString = testEntity.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("42"));
        assertTrue(toString.contains("Test Entity"));
        assertTrue(toString.contains("TestEntity"));
    }

    @Test
    void testSetCompleteEntity() {
        Long id = 999L;
        String name = "Complete Test Entity";
        
        testEntity.setId(id);
        testEntity.setName(name);
        
        assertEquals(id, testEntity.getId());
        assertEquals(name, testEntity.getName());
    }

    @Test
    void testEntityWithLongName() {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longName.append("a");
        }
        
        testEntity.setName(longName.toString());
        
        assertEquals(longName.toString(), testEntity.getName());
        assertEquals(1000, testEntity.getName().length());
    }

    @Test
    void testEntityWithNegativeId() {
        Long negativeId = -1L;
        testEntity.setId(negativeId);
        
        assertEquals(negativeId, testEntity.getId());
    }

    @Test
    void testEntityWithZeroId() {
        Long zeroId = 0L;
        testEntity.setId(zeroId);
        
        assertEquals(zeroId, testEntity.getId());
    }

    @Test
    void testEntityWithMaxLongId() {
        Long maxId = Long.MAX_VALUE;
        testEntity.setId(maxId);
        
        assertEquals(maxId, testEntity.getId());
    }

    @Test
    void testEntityWithMinLongId() {
        Long minId = Long.MIN_VALUE;
        testEntity.setId(minId);
        
        assertEquals(minId, testEntity.getId());
    }
}