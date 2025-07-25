package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.application.usecase.BusinessProcess;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProcessRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, BusinessProcess> processMap = new HashMap<>();

    public ProcessRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void registerProcesses() {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(Component.class);
        for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
            if (entry.getValue() instanceof BusinessProcess) {
                processMap.put(entry.getKey(), (BusinessProcess) entry.getValue());
            }
        }
    }

    public BusinessProcess getProcess(String processName) {
        return processMap.get(processName);
    }
}
