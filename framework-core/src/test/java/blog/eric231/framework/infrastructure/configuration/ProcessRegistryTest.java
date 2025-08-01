package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.application.usecase.BP;
import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.EchoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ProcessRegistryTest {

    private ApplicationContext applicationContext;
    private ProcessRegistry processRegistry;

    @BeforeEach
    void setUp() {
        applicationContext = Mockito.mock(ApplicationContext.class);
        processRegistry = new ProcessRegistry(applicationContext);
    }

    @Test
    void testRegisterProcessesWithBPAnnotationWithValue() {
        // Mock a BusinessProcess bean with @BP annotation that has a value
        EchoService echoService = new EchoService();
        Map<String, Object> beans = new HashMap<>();
        beans.put("echoService", echoService);

        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(beans);
        when(applicationContext.findAnnotationOnBean("echoService", BP.class)).thenReturn(new BP() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BP.class;
            }

            @Override
            public String value() {
                return "echo-service";
            }
        });

        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("echo-service");
        assertNotNull(retrievedProcess);
        assertEquals(echoService, retrievedProcess);
    }

    @Test
    void testRegisterProcessesWithBPAnnotationWithoutValue() {
        // Mock a BusinessProcess bean with @BP annotation that does not have a value
        class AnotherBusinessProcess implements BusinessProcess {
            @Override
            public com.fasterxml.jackson.databind.JsonNode handle(com.fasterxml.jackson.databind.JsonNode request) {
                return null;
            }
        }
        AnotherBusinessProcess anotherProcess = new AnotherBusinessProcess();
        Map<String, Object> beans = new HashMap<>();
        beans.put("anotherBusinessProcess", anotherProcess);

        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(beans);
        when(applicationContext.findAnnotationOnBean("anotherBusinessProcess", BP.class)).thenReturn(new BP() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BP.class;
            }

            @Override
            public String value() {
                return "";
            }
        });

        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("anotherBusinessProcess");
        assertNotNull(retrievedProcess);
        assertEquals(anotherProcess, retrievedProcess);
    }

    @Test
    void testGetProcessNotFound() {
        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(Collections.emptyMap());
        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("non-existent-service");
        assertNull(retrievedProcess);
    }

    @Test
    void testGetProcessBeforeRegistration() {
        // Do not call registerProcesses()
        BusinessProcess retrievedProcess = processRegistry.getProcess("echo-service");
        assertNull(retrievedProcess);
    }
}
