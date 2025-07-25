package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import blog.eric231.framework.infrastructure.security.VaultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@ConditionalOnProperty(name = "framework.connectors.rest.enabled", havingValue = "true")
public class DynamicRestController {

    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final VaultService vaultService;

    @Autowired
    public DynamicRestController(ProcessRegistry processRegistry, FrameworkProperties frameworkProperties, VaultService vaultService) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.vaultService = vaultService;
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

    @GetMapping("/secret")
    public String getSecret() {
        return vaultService.getMySecret();
    }

    private Optional<FrameworkProperties.Routing> findMatchedRouting(String path, String method) {
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "rest".equalsIgnoreCase(trigger.getType()) &&
                                path.equals(trigger.getPath()) &&
                                method.equalsIgnoreCase(trigger.getMethod())))
                .findFirst();
    }
}
