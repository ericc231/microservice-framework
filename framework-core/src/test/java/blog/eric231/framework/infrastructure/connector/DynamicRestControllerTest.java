package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicRestControllerTest {

    private DynamicRestController controller;
    private ObjectMapper objectMapper;

    @Mock
    private ProcessRegistry processRegistry;

    @Mock
    private FrameworkProperties frameworkProperties;

    @Mock
    private BusinessProcess businessProcess;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        controller = new DynamicRestController(processRegistry, frameworkProperties);
    }

    @Test
    void constructor_ShouldSetFieldsCorrectly() {
        DynamicRestController newController = new DynamicRestController(processRegistry, frameworkProperties);
        assertNotNull(newController);
    }

    @Test
    void handleDynamicRequest_WithMatchingRoute_ShouldReturnProcessResponse() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("data", "test");

        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("result", "processed");

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("POST");

        FrameworkProperties.Routing routing = createMockRouting();
        List<FrameworkProperties.Routing> routings = Collections.singletonList(routing);
        when(frameworkProperties.getRouting()).thenReturn(routings);

        when(processRegistry.getProcess("test-process")).thenReturn(businessProcess);
        when(businessProcess.handle(requestBody)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("processed", response.getBody().get("result").asText());
        
        verify(processRegistry).getProcess("test-process");
        verify(businessProcess).handle(requestBody);
    }

    @Test
    void handleDynamicRequest_WithNullRequestBody_ShouldHandleGracefully() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("POST"); // Changed to POST to match the routing

        FrameworkProperties.Routing routing = createMockRouting();
        List<FrameworkProperties.Routing> routings = Collections.singletonList(routing);
        when(frameworkProperties.getRouting()).thenReturn(routings);

        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("result", "ok");

        when(processRegistry.getProcess("test-process")).thenReturn(businessProcess);
        when(businessProcess.handle(null)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(null, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ok", response.getBody().get("result").asText());
        
        verify(businessProcess).handle(null);
    }

    @Test
    void handleDynamicRequest_WithNoMatchingRoute_ShouldReturnNotFound() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        
        when(request.getRequestURI()).thenReturn("/api/nonexistent");
        when(request.getMethod()).thenReturn("POST");
        when(frameworkProperties.getRouting()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void handleDynamicRequest_WithMatchingRouteButNoProcess_ShouldReturnNotFound() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("POST");

        FrameworkProperties.Routing routing = createMockRouting();
        List<FrameworkProperties.Routing> routings = Collections.singletonList(routing);
        when(frameworkProperties.getRouting()).thenReturn(routings);
        
        when(processRegistry.getProcess("test-process")).thenReturn(null);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(processRegistry).getProcess("test-process");
    }

    @Test
    void handleDynamicRequest_WithDifferentHttpMethods_ShouldMatchCorrectly() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("method", "GET");

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");

        FrameworkProperties.Routing routing = createMockRoutingWithMethod("GET");
        List<FrameworkProperties.Routing> routings = Collections.singletonList(routing);
        when(frameworkProperties.getRouting()).thenReturn(routings);

        when(processRegistry.getProcess("test-process")).thenReturn(businessProcess);
        when(businessProcess.handle(any())).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("GET", response.getBody().get("method").asText());
    }

    @Test
    void handleDynamicRequest_WithMultipleRoutings_ShouldFindFirstMatch() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("process", "first");

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("POST");

        FrameworkProperties.Routing firstRouting = createMockRouting();
        FrameworkProperties.Routing secondRouting = createMockRoutingWithProcess("second-process");
        
        List<FrameworkProperties.Routing> routings = Arrays.asList(firstRouting, secondRouting);
        when(frameworkProperties.getRouting()).thenReturn(routings);

        when(processRegistry.getProcess("test-process")).thenReturn(businessProcess);
        when(businessProcess.handle(any())).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processRegistry).getProcess("test-process");
        verify(processRegistry, never()).getProcess("second-process");
    }

    @Test
    void handleDynamicRequest_WithComplexPathPattern_ShouldMatchCorrectly() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();
        expectedResponse.put("matched", true);

        when(request.getRequestURI()).thenReturn("/api/users/123");
        when(request.getMethod()).thenReturn("GET");

        FrameworkProperties.Routing routing = createMockRoutingWithPath("/api/users/.*");
        List<FrameworkProperties.Routing> routings = Collections.singletonList(routing);
        when(frameworkProperties.getRouting()).thenReturn(routings);

        when(processRegistry.getProcess("user-process")).thenReturn(businessProcess);
        when(businessProcess.handle(any())).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().get("matched").asBoolean());
    }

    @Test
    void handleDynamicRequest_WithCaseInsensitiveMethod_ShouldMatch() {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode expectedResponse = objectMapper.createObjectNode();

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("post"); // lowercase

        FrameworkProperties.Routing routing = createMockRoutingWithMethod("POST"); // uppercase
        List<FrameworkProperties.Routing> routings = Collections.singletonList(routing);
        when(frameworkProperties.getRouting()).thenReturn(routings);

        when(processRegistry.getProcess("test-process")).thenReturn(businessProcess);
        when(businessProcess.handle(any())).thenReturn(expectedResponse);

        // Act
        ResponseEntity<JsonNode> response = controller.handleDynamicRequest(requestBody, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Helper methods to create mock objects
    private FrameworkProperties.Routing createMockRouting() {
        return createMockRoutingWithProcess("test-process");
    }

    private FrameworkProperties.Routing createMockRoutingWithProcess(String processName) {
        FrameworkProperties.Routing routing = mock(FrameworkProperties.Routing.class);
        FrameworkProperties.Trigger trigger = mock(FrameworkProperties.Trigger.class);
        
        when(trigger.getType()).thenReturn("rest");
        when(trigger.getPath()).thenReturn("/api/test");
        when(trigger.getMethod()).thenReturn("POST");
        
        when(routing.getProcessName()).thenReturn(processName);
        when(routing.getTriggers()).thenReturn(Collections.singletonList(trigger));
        
        return routing;
    }

    private FrameworkProperties.Routing createMockRoutingWithMethod(String method) {
        FrameworkProperties.Routing routing = mock(FrameworkProperties.Routing.class);
        FrameworkProperties.Trigger trigger = mock(FrameworkProperties.Trigger.class);
        
        when(trigger.getType()).thenReturn("rest");
        when(trigger.getPath()).thenReturn("/api/test");
        when(trigger.getMethod()).thenReturn(method);
        
        when(routing.getProcessName()).thenReturn("test-process");
        when(routing.getTriggers()).thenReturn(Collections.singletonList(trigger));
        
        return routing;
    }

    private FrameworkProperties.Routing createMockRoutingWithPath(String path) {
        FrameworkProperties.Routing routing = mock(FrameworkProperties.Routing.class);
        FrameworkProperties.Trigger trigger = mock(FrameworkProperties.Trigger.class);
        
        when(trigger.getType()).thenReturn("rest");
        when(trigger.getPath()).thenReturn(path);
        when(trigger.getMethod()).thenReturn("GET");
        
        when(routing.getProcessName()).thenReturn("user-process");
        when(routing.getTriggers()).thenReturn(Collections.singletonList(trigger));
        
        return routing;
    }
}