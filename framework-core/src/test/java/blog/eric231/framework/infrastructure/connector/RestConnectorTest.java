package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestConnectorTest {

    private RestConnector restConnector;
    private ObjectMapper objectMapper;

    @Mock
    private ProcessRegistry processRegistry;

    @Mock
    private FrameworkProperties frameworkProperties;

    @Mock
    private BusinessProcess businessProcess;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        restConnector = new RestConnector(processRegistry, frameworkProperties);
    }

    @Test
    void constructor_ShouldSetFieldsCorrectly() {
        RestConnector connector = new RestConnector(processRegistry, frameworkProperties);
        assertNotNull(connector);
    }

    @Test
    void handlePostRequest_WithValidProcess_ShouldReturnProcessResult() {
        // Arrange
        String processName = "test-process";
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("data", "test");

        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("result", "processed");

        FrameworkProperties.Trigger trigger = createMockTrigger("rest", "POST");
        FrameworkProperties.Routing routing = createMockRouting(processName, trigger);

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));
        when(processRegistry.getProcess(processName)).thenReturn(businessProcess);
        when(businessProcess.handle(requestBody)).thenReturn(expectedResponse);

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, requestBody);

        // Assert
        assertNotNull(result);
        assertEquals("processed", result.get("result").asText());
        verify(processRegistry).getProcess(processName);
        verify(businessProcess).handle(requestBody);
    }

    @Test
    void handlePostRequest_WithNonExistentProcess_ShouldReturnNull() {
        // Arrange
        String processName = "non-existent-process";
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("data", "test");

        when(frameworkProperties.getRouting()).thenReturn(Collections.emptyList());

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, requestBody);

        // Assert
        assertNull(result);
        verify(processRegistry, never()).getProcess(anyString());
    }

    @Test
    void handlePostRequest_WithProcessNotConfiguredForRest_ShouldReturnNull() {
        // Arrange
        String processName = "kafka-only-process";
        ObjectNode requestBody = objectMapper.createObjectNode();

        FrameworkProperties.Trigger kafkaTrigger = createMockTrigger("kafka", null);
        FrameworkProperties.Routing routing = createMockRouting(processName, kafkaTrigger);

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, requestBody);

        // Assert
        assertNull(result);
        verify(processRegistry, never()).getProcess(anyString());
    }

    @Test
    void handlePostRequest_WithProcessConfiguredForGetOnly_ShouldReturnNull() {
        // Arrange
        String processName = "get-only-process";
        ObjectNode requestBody = objectMapper.createObjectNode();

        FrameworkProperties.Trigger getTrigger = createMockTrigger("rest", "GET");
        FrameworkProperties.Routing routing = createMockRouting(processName, getTrigger);

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, requestBody);

        // Assert
        assertNull(result);
        verify(processRegistry, never()).getProcess(anyString());
    }

    @Test
    void handlePostRequest_WithNullRequestBody_ShouldStillWork() {
        // Arrange
        String processName = "test-process";
        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("result", "ok");

        FrameworkProperties.Trigger trigger = createMockTrigger("rest", "POST");
        FrameworkProperties.Routing routing = createMockRouting(processName, trigger);

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));
        when(processRegistry.getProcess(processName)).thenReturn(businessProcess);
        when(businessProcess.handle(null)).thenReturn(expectedResponse);

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, null);

        // Assert
        assertNotNull(result);
        assertEquals("ok", result.get("result").asText());
        verify(businessProcess).handle(null);
    }

    @Test
    void handlePostRequest_WithMultipleRoutings_ShouldFindCorrectOne() {
        // Arrange
        String targetProcess = "target-process";
        String otherProcess = "other-process";
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("source", "target");

        FrameworkProperties.Trigger otherTrigger = createMockTrigger("kafka", null);
        FrameworkProperties.Routing otherRouting = createMockRouting(otherProcess, otherTrigger);

        FrameworkProperties.Trigger targetTrigger = createMockTrigger("rest", "POST");
        FrameworkProperties.Routing targetRouting = createMockRouting(targetProcess, targetTrigger);

        when(frameworkProperties.getRouting()).thenReturn(Arrays.asList(otherRouting, targetRouting));
        when(processRegistry.getProcess(targetProcess)).thenReturn(businessProcess);
        when(businessProcess.handle(requestBody)).thenReturn(expectedResponse);

        // Act
        JsonNode result = restConnector.handlePostRequest(targetProcess, requestBody);

        // Assert
        assertNotNull(result);
        assertEquals("target", result.get("source").asText());
        verify(processRegistry).getProcess(targetProcess);
        verify(processRegistry, never()).getProcess(otherProcess);
    }

    @Test
    void handlePostRequest_WithMultipleTriggers_ShouldFindRestTrigger() {
        // Arrange
        String processName = "multi-trigger-process";
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("trigger", "rest");

        FrameworkProperties.Trigger kafkaTrigger = createMockTrigger("kafka", null);
        FrameworkProperties.Trigger restTrigger = createMockTrigger("rest", "POST");

        FrameworkProperties.Routing routing = mock(FrameworkProperties.Routing.class);
        when(routing.getProcessName()).thenReturn(processName);
        when(routing.getTriggers()).thenReturn(Arrays.asList(kafkaTrigger, restTrigger));

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));
        when(processRegistry.getProcess(processName)).thenReturn(businessProcess);
        when(businessProcess.handle(requestBody)).thenReturn(expectedResponse);

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, requestBody);

        // Assert
        assertNotNull(result);
        assertEquals("rest", result.get("trigger").asText());
        verify(processRegistry).getProcess(processName);
    }

    @Test
    void handlePostRequest_WithCaseSensitiveMethodMatch_ShouldWork() {
        // Arrange
        String processName = "case-test-process";
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();

        // Trigger configured for "post" (lowercase)
        FrameworkProperties.Trigger trigger = createMockTrigger("rest", "post");
        FrameworkProperties.Routing routing = createMockRouting(processName, trigger);

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));
        when(processRegistry.getProcess(processName)).thenReturn(businessProcess);
        when(businessProcess.handle(requestBody)).thenReturn(expectedResponse);

        // Act - handlePostRequest uses "POST" (uppercase)
        JsonNode result = restConnector.handlePostRequest(processName, requestBody);

        // Assert
        assertNotNull(result);
        verify(processRegistry).getProcess(processName);
    }

    @Test
    void findProcessForRequest_WithNullRouting_ShouldReturnNull() {
        // Arrange
        when(frameworkProperties.getRouting()).thenReturn(null);

        // Act
        JsonNode result = restConnector.handlePostRequest("any-process", objectMapper.createObjectNode());

        // Assert
        assertNull(result);
        verify(processRegistry, never()).getProcess(anyString());
    }

    @Test
    void findProcessForRequest_WithProcessRegistryReturningNull_ShouldReturnNull() {
        // Arrange
        String processName = "registered-but-null-process";
        FrameworkProperties.Trigger trigger = createMockTrigger("rest", "POST");
        FrameworkProperties.Routing routing = createMockRouting(processName, trigger);

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));
        when(processRegistry.getProcess(processName)).thenReturn(null);

        // Act
        JsonNode result = restConnector.handlePostRequest(processName, objectMapper.createObjectNode());

        // Assert
        assertNull(result);
        verify(processRegistry).getProcess(processName);
    }

    // Helper methods
    private FrameworkProperties.Trigger createMockTrigger(String type, String method) {
        FrameworkProperties.Trigger trigger = mock(FrameworkProperties.Trigger.class);
        when(trigger.getType()).thenReturn(type);
        when(trigger.getMethod()).thenReturn(method);
        return trigger;
    }

    private FrameworkProperties.Routing createMockRouting(String processName, FrameworkProperties.Trigger trigger) {
        FrameworkProperties.Routing routing = mock(FrameworkProperties.Routing.class);
        when(routing.getProcessName()).thenReturn(processName);
        when(routing.getTriggers()).thenReturn(Collections.singletonList(trigger));
        return routing;
    }
}