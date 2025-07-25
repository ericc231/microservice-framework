package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "framework.connectors.rest.enabled", havingValue = "true")
public class RestConnector {

    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;

    @Autowired
    public RestConnector(ProcessRegistry processRegistry, FrameworkProperties frameworkProperties) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
    }

    @PostMapping("/{processName}")
    public JsonNode handlePostRequest(@PathVariable String processName, @RequestBody JsonNode request) {
        BusinessProcess process = findProcessForRequest(processName, "POST");
        if (process != null) {
            return process.handle(request);
        }
        // Handle error - process not found or not configured for REST POST
        return null; // Or return a proper error response
    }

    private BusinessProcess findProcessForRequest(String processName, String method) {
        for (FrameworkProperties.Routing routing : frameworkProperties.getRouting()) {
            if (routing.getProcessName().equals(processName)) {
                for (FrameworkProperties.Trigger trigger : routing.getTriggers()) {
                    if ("rest".equalsIgnoreCase(trigger.getType()) && method.equalsIgnoreCase(trigger.getMethod())) {
                        return processRegistry.getProcess(processName);
                    }
                }
            }
        }
        return null;
    }
}
