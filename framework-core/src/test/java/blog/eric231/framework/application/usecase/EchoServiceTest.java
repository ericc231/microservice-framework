package blog.eric231.framework.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EchoServiceTest {

    private EchoService echoService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        echoService = new EchoService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testHandleWithSimpleJson() {
        String jsonString = "{\"message\":\"hello\"}";
        JsonNode requestNode = null;
        try {
            requestNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Failed to parse JSON string: " + e.getMessage());
        }

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
    }

    @Test
    void testHandleWithComplexJson() {
        String jsonString = "{\"user\":{\"name\":\"test\",\"age\":30},\"items\":[1,2,3]}";
        JsonNode requestNode = null;
        try {
            requestNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Failed to parse JSON string: " + e.getMessage());
        }

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
    }

    @Test
    void testHandleWithEmptyJson() {
        String jsonString = "{}";
        JsonNode requestNode = null;
        try {
            requestNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Failed to parse JSON string: " + e.getMessage());
        }

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
    }

    @Test
    void testHandleWithNullInput() {
        JsonNode responseNode = echoService.handle(null);
        // EchoService simply returns the input, so null input should result in null output
        assertNull(responseNode);
    }

    @Test
    void testHandleWithArrayInput() {
        String jsonString = "[1, 2, 3, \"test\", true]";
        JsonNode requestNode = null;
        try {
            requestNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Failed to parse JSON array: " + e.getMessage());
        }

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
        assertTrue(responseNode.isArray());
        assertEquals(5, responseNode.size());
    }

    @Test
    void testHandleWithStringInput() {
        String jsonString = "\"simple string\"";
        JsonNode requestNode = null;
        try {
            requestNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            fail("Failed to parse JSON string: " + e.getMessage());
        }

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
        assertEquals("simple string", responseNode.asText());
    }

    @Test
    void testHandleWithNumberInput() {
        JsonNode requestNode = objectMapper.valueToTree(42.5);

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
        assertEquals(42.5, responseNode.asDouble(), 0.0001);
    }

    @Test
    void testHandleWithBooleanInput() {
        JsonNode requestNode = objectMapper.valueToTree(true);

        JsonNode responseNode = echoService.handle(requestNode);

        assertNotNull(responseNode);
        assertEquals(requestNode, responseNode);
        assertTrue(responseNode.asBoolean());
    }

    @Test
    void testHandleReturnsSameReference() {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("test", "reference");

        JsonNode responseNode = echoService.handle(requestNode);

        assertSame(requestNode, responseNode);
    }

    @Test
    void testHandleWithDeeplyNestedJson() {
        ObjectNode level3 = objectMapper.createObjectNode();
        level3.put("deep", "value");
        
        ObjectNode level2 = objectMapper.createObjectNode();
        level2.set("level3", level3);
        level2.put("count", 100);
        
        ObjectNode level1 = objectMapper.createObjectNode();
        level1.set("level2", level2);
        level1.put("root", "property");

        JsonNode responseNode = echoService.handle(level1);

        assertNotNull(responseNode);
        assertEquals(level1, responseNode);
        assertEquals("value", responseNode.get("level2").get("level3").get("deep").asText());
        assertEquals(100, responseNode.get("level2").get("count").asInt());
        assertEquals("property", responseNode.get("root").asText());
    }

    @Test
    void testEchoServiceImplementsBusinessProcess() {
        assertTrue(echoService instanceof BusinessProcess);
    }

    @Test
    void testEchoServiceHasBPAnnotation() {
        BP annotation = EchoService.class.getAnnotation(BP.class);
        assertNotNull(annotation);
        assertEquals("echo-service", annotation.value());
    }

    @Test
    void testHandleWithLargeJson() {
        ObjectNode largeNode = objectMapper.createObjectNode();
        for (int i = 0; i < 1000; i++) {
            largeNode.put("field" + i, "value" + i);
        }

        JsonNode responseNode = echoService.handle(largeNode);

        assertNotNull(responseNode);
        assertEquals(largeNode, responseNode);
        assertEquals(1000, responseNode.size());
        assertEquals("value999", responseNode.get("field999").asText());
    }

    @Test
    void testHandleMultipleTimes() {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("message", "consistent");

        JsonNode response1 = echoService.handle(requestNode);
        JsonNode response2 = echoService.handle(requestNode);

        assertEquals(response1, response2);
        assertSame(requestNode, response1);
        assertSame(requestNode, response2);
    }
}
