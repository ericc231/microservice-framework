package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.application.usecase.BusinessProcess;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProcessRegistry implements ApplicationListener<ContextRefreshedEvent> {

    private final ApplicationContext applicationContext;
    private final Map<String, BusinessProcess> processMap = new HashMap<>();

    public ProcessRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        registerProcesses();
    }

    public void registerProcesses() {
        Map<String, BusinessProcess> beansOfType = applicationContext.getBeansOfType(BusinessProcess.class);
        for (Map.Entry<String, BusinessProcess> entry : beansOfType.entrySet()) {
            processMap.put(entry.getKey(), entry.getValue());
        }
    }

    public BusinessProcess getProcess(String processName) {
        return processMap.get(processName);
    }
}
