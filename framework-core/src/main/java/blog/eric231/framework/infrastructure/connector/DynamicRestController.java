package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@ConditionalOnProperty(name = "framework.connectors.rest.enabled", havingValue = "true")
public class DynamicRestController {

    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;

    public DynamicRestController(ProcessRegistry processRegistry, FrameworkProperties frameworkProperties) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
    }

    @RequestMapping("/**")
    public ResponseEntity<JsonNode> handleDynamicRequest(@RequestBody(required = false) JsonNode requestBody, HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        Optional<FrameworkProperties.Routing> matchedRouting = findMatchedRouting(path, method);

        if (matchedRouting.isPresent()) {
            String processName = matchedRouting.get().getProcessName();
            BusinessProcess process = processRegistry.getProcess(processName);
            if (process != null) {
                JsonNode response = process.handle(requestBody);
                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.notFound().build();
    }

    private Optional<FrameworkProperties.Routing> findMatchedRouting(String path, String method) {
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "rest".equalsIgnoreCase(trigger.getType()) &&
                                Pattern.matches(trigger.getPath(), path) &&
                                method.equalsIgnoreCase(trigger.getMethod())))
                .findFirst();
    }
}
