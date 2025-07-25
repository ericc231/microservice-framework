package blog.eric231.framework.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(null, responseNode);
    }
}
