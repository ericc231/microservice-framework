package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.application.usecase.BP;
import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.adapter.DomainLogicAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProcessRegistry implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRegistry.class);

    private final ApplicationContext applicationContext;
    private final Map<String, BusinessProcess> processMap = new HashMap<>();
    private final Map<String, DomainLogic> domainLogicMap = new HashMap<>();

    public ProcessRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        registerProcesses();
        registerDomainLogics();
    }

    /**
     * Register legacy BusinessProcess components with @BP annotation
     * @deprecated Use @DL annotation with DomainLogic interface instead
     */
    @Deprecated
    public void registerProcesses() {
        Map<String, Object> beansWithBPAnnotation = applicationContext.getBeansWithAnnotation(BP.class);
        for (Map.Entry<String, Object> entry : beansWithBPAnnotation.entrySet()) {
            if (entry.getValue() instanceof BusinessProcess) {
                String processName = entry.getKey();
                BP bpAnnotation = applicationContext.findAnnotationOnBean(processName, BP.class);
                if (bpAnnotation != null && !bpAnnotation.value().isEmpty()) {
                    processName = bpAnnotation.value();
                }
                processMap.put(processName, (BusinessProcess) entry.getValue());
                logger.info("Registered legacy BusinessProcess: {} -> {}", processName, entry.getValue().getClass().getSimpleName());
            }
        }
    }

    /**
     * Register DomainLogic components with @DL annotation
     */
    public void registerDomainLogics() {
        Map<String, Object> beansWithDLAnnotation = applicationContext.getBeansWithAnnotation(DL.class);
        logger.info("Found {} beans with @DL annotation", beansWithDLAnnotation.size());
        
        for (Map.Entry<String, Object> entry : beansWithDLAnnotation.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            
            // Get the @DL annotation
            DL dlAnnotation = applicationContext.findAnnotationOnBean(beanName, DL.class);
            if (dlAnnotation != null) {
                String domainLogicName = dlAnnotation.value();
                
                // Create adapter if the bean doesn't implement DomainLogic interface
                DomainLogic domainLogic;
                if (bean instanceof DomainLogic) {
                    domainLogic = (DomainLogic) bean;
                } else {
                    // Create an adapter for beans that don't implement DomainLogic interface
                    domainLogic = new DomainLogicAdapter(bean, dlAnnotation);
                }
                
                domainLogicMap.put(domainLogicName, domainLogic);
                logger.info("Registered DomainLogic: {} -> {} (version: {})", 
                    domainLogicName, bean.getClass().getSimpleName(), dlAnnotation.version());
            }
        }
        
        logger.info("Total registered domain logics: {}", domainLogicMap.size());
    }

    /**
     * Get legacy BusinessProcess by name
     * @deprecated Use getDomainLogic instead
     */
    @Deprecated
    public BusinessProcess getProcess(String processName) {
        return processMap.get(processName);
    }

    /**
     * Get DomainLogic by name
     */
    public DomainLogic getDomainLogic(String domainLogicName) {
        return domainLogicMap.get(domainLogicName);
    }

    /**
     * Get all registered domain logic names
     */
    public Map<String, DomainLogic> getAllDomainLogics() {
        return new HashMap<>(domainLogicMap);
    }

    /**
     * Check if a domain logic is registered
     */
    public boolean hasDomainLogic(String domainLogicName) {
        return domainLogicMap.containsKey(domainLogicName);
    }
}
