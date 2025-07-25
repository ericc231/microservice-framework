package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.EchoService;
import com.fasterxml.jackson.databind.JsonNode;
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
    void testRegisterProcesses() {
        // Mock a BusinessProcess bean
        EchoService echoService = new EchoService();
        Map<String, Object> beans = new HashMap<>();
        beans.put("echo-service", echoService);

        when(applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Component.class)).thenReturn(beans);

        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("echo-service");
        assertNotNull(retrievedProcess);
        assertEquals(echoService, retrievedProcess);
    }

    @Test
    void testGetProcessNotFound() {
        when(applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Component.class)).thenReturn(Collections.emptyMap());
        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("non-existent-service");
        assertNull(retrievedProcess);
    }

    @Test
    void testRegisterProcessesWithNonBusinessProcessBeans() {
        Object nonProcessBean = new Object();
        Map<String, Object> beans = new HashMap<>();
        beans.put("someOtherBean", nonProcessBean);

        when(applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Component.class)).thenReturn(beans);

        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("someOtherBean");
        assertNull(retrievedProcess);
    }

    @Test
    void testGetProcessBeforeRegistration() {
        // Do not call registerProcesses()
        BusinessProcess retrievedProcess = processRegistry.getProcess("echo-service");
        assertNull(retrievedProcess);
    }
}
