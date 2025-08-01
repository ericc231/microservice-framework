package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProcessRegistry processRegistry;

    @Mock
    private FrameworkProperties frameworkProperties;

    @InjectMocks
    private DynamicRestController dynamicRestController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(dynamicRestController).build();
        // Reset mocks before each test
        when(processRegistry.getProcess(any(String.class))).thenReturn(null);
        when(frameworkProperties.getRouting()).thenReturn(Collections.emptyList());
    }

    @Test
    void testHandleDynamicRequest_matchingRoute_success() throws Exception {
        String processName = "test-process";
        String path = "/api/test";
        String method = "POST";
        JsonNode requestBody = objectMapper.createObjectNode().put("input", "data");
        JsonNode responseBody = objectMapper.createObjectNode().put("output", "result");

        // Mock FrameworkProperties routing
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath(path);
        trigger.setMethod(method);

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName(processName);
        routing.setTriggers(Collections.singletonList(trigger));

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));

        // Mock BusinessProcess
        BusinessProcess mockProcess = new BusinessProcess() {
            @Override
            public JsonNode handle(JsonNode request) {
                return responseBody;
            }
        };
        when(processRegistry.getProcess(processName)).thenReturn(mockProcess);

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody.toString()));
    }

    @Test
    void testHandleDynamicRequest_noMatchingRoute() throws Exception {
        JsonNode requestBody = objectMapper.createObjectNode().put("input", "data");

        mockMvc.perform(post("/api/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testHandleDynamicRequest_processNotFoundInRegistry() throws Exception {
        String processName = "unregistered-process";
        String path = "/api/unregistered";
        String method = "POST";
        JsonNode requestBody = objectMapper.createObjectNode().put("input", "data");

        // Mock FrameworkProperties routing to a process that is not in the registry
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath(path);
        trigger.setMethod(method);

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName(processName);
        routing.setTriggers(Collections.singletonList(trigger));

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));
        when(processRegistry.getProcess(processName)).thenReturn(null); // Process not found

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testHandleDynamicRequest_differentHttpMethod() throws Exception {
        String processName = "test-process";
        String path = "/api/test";
        String method = "GET"; // Configured for GET, but we'll send POST
        JsonNode requestBody = objectMapper.createObjectNode().put("input", "data");

        // Mock FrameworkProperties routing
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath(path);
        trigger.setMethod(method);

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName(processName);
        routing.setTriggers(Collections.singletonList(trigger));

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isNotFound()); // Should be 404 because method doesn't match
    }

    @Test
    void testHandleDynamicRequest_emptyRequestBody() throws Exception {
        String processName = "test-process";
        String path = "/api/empty-body";
        String method = "POST";
        JsonNode responseBody = objectMapper.createObjectNode().put("output", "empty");

        // Mock FrameworkProperties routing
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath(path);
        trigger.setMethod(method);

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName(processName);
        routing.setTriggers(Collections.singletonList(trigger));

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));

        // Mock BusinessProcess to handle null/empty input
        BusinessProcess mockProcess = new BusinessProcess() {
            @Override
            public JsonNode handle(JsonNode request) {
                return request == null ? responseBody : request;
            }
        };
        when(processRegistry.getProcess(processName)).thenReturn(mockProcess);

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody.toString()));
    }

    @Test
    void testHandleDynamicRequest_nullResponseBodyFromProcess() throws Exception {
        String processName = "null-response-process";
        String path = "/api/null-response";
        String method = "POST";
        JsonNode requestBody = objectMapper.createObjectNode().put("input", "data");

        // Mock FrameworkProperties routing
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath(path);
        trigger.setMethod(method);

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName(processName);
        routing.setTriggers(Collections.singletonList(trigger));

        when(frameworkProperties.getRouting()).thenReturn(Collections.singletonList(routing));

        // Mock BusinessProcess to return null
        BusinessProcess mockProcess = new BusinessProcess() {
            @Override
            public JsonNode handle(JsonNode request) {
                return null;
            }
        };
        when(processRegistry.getProcess(processName)).thenReturn(mockProcess);

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("")); // Expect empty body for null JsonNode
    }
}
