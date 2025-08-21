package blog.eric231.examples.helloworld;

import blog.eric231.framework.application.usecase.BP;
import blog.eric231.framework.application.usecase.BusinessProcess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelloWorldBusinessProcessTest {

    private HelloWorldBusinessProcess businessProcess;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        businessProcess = new HelloWorldBusinessProcess();
        objectMapper = new ObjectMapper();
    }

    @Test
    void handle_WithAnyInput_ShouldReturnHelloMessage() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("name", "Test User");

        // Act
        JsonNode result = businessProcess.handle(input);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTextual());
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithNullInput_ShouldReturnHelloMessage() {
        // Act
        JsonNode result = businessProcess.handle(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTextual());
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithEmptyObject_ShouldReturnHelloMessage() {
        // Arrange
        ObjectNode emptyInput = objectMapper.createObjectNode();

        // Act
        JsonNode result = businessProcess.handle(emptyInput);

        // Assert
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithComplexInput_ShouldReturnHelloMessage() {
        // Arrange
        ObjectNode complexInput = objectMapper.createObjectNode();
        ObjectNode user = objectMapper.createObjectNode();
        user.put("name", "John Doe");
        user.put("age", 30);
        complexInput.set("user", user);
        complexInput.set("items", objectMapper.createArrayNode().add("item1").add("item2"));

        // Act
        JsonNode result = businessProcess.handle(complexInput);

        // Assert
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithStringInput_ShouldReturnHelloMessage() {
        // Arrange
        JsonNode stringInput = objectMapper.valueToTree("test string");

        // Act
        JsonNode result = businessProcess.handle(stringInput);

        // Assert
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithNumberInput_ShouldReturnHelloMessage() {
        // Arrange
        JsonNode numberInput = objectMapper.valueToTree(42);

        // Act
        JsonNode result = businessProcess.handle(numberInput);

        // Assert
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithBooleanInput_ShouldReturnHelloMessage() {
        // Arrange
        JsonNode booleanInput = objectMapper.valueToTree(true);

        // Act
        JsonNode result = businessProcess.handle(booleanInput);

        // Assert
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_WithArrayInput_ShouldReturnHelloMessage() {
        // Arrange
        JsonNode arrayInput = objectMapper.createArrayNode()
                .add("element1")
                .add("element2")
                .add(123);

        // Act
        JsonNode result = businessProcess.handle(arrayInput);

        // Assert
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    void handle_ConsistentOutput_ShouldAlwaysReturnSameMessage() {
        // Arrange
        ObjectNode input1 = objectMapper.createObjectNode().put("test", "1");
        ObjectNode input2 = objectMapper.createObjectNode().put("test", "2");

        // Act
        JsonNode result1 = businessProcess.handle(input1);
        JsonNode result2 = businessProcess.handle(input2);

        // Assert
        assertEquals(result1.asText(), result2.asText());
        assertEquals("Hello from Business Process!", result1.asText());
        assertEquals("Hello from Business Process!", result2.asText());
    }

    @Test
    void implementsBusinessProcess_ShouldImplementInterface() {
        // Assert
        assertTrue(businessProcess instanceof BusinessProcess);
    }

    @Test
    void hasCorrectAnnotation_ShouldHaveBPAnnotation() {
        // Assert
        BP annotation = HelloWorldBusinessProcess.class.getAnnotation(BP.class);
        assertNotNull(annotation);
        assertEquals("helloworld-process", annotation.value());
    }

    @Test
    void handle_MultipleCalls_ShouldReturnConsistentResults() {
        // Arrange
        ObjectNode input = objectMapper.createObjectNode();
        input.put("message", "test");

        // Act
        JsonNode result1 = businessProcess.handle(input);
        JsonNode result2 = businessProcess.handle(input);
        JsonNode result3 = businessProcess.handle(input);

        // Assert
        assertEquals(result1.asText(), result2.asText());
        assertEquals(result2.asText(), result3.asText());
        assertEquals("Hello from Business Process!", result1.asText());
    }

    @Test
    void handle_ReturnsTextNode_ShouldBeCorrectType() {
        // Arrange
        JsonNode input = objectMapper.valueToTree("any input");

        // Act
        JsonNode result = businessProcess.handle(input);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTextual());
        assertFalse(result.isObject());
        assertFalse(result.isArray());
        assertFalse(result.isNumber());
        assertFalse(result.isBoolean());
    }
}