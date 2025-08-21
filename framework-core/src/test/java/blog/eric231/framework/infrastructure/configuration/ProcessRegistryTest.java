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

    @Test
    void testRegisterProcessesWithNullBeanValue() {
        // Mock beans map with null value
        Map<String, Object> beans = new HashMap<>();
        beans.put("nullProcess", null);

        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(beans);

        processRegistry.registerProcesses();

        // Should not throw exception and should handle gracefully
        BusinessProcess retrievedProcess = processRegistry.getProcess("nullProcess");
        assertNull(retrievedProcess);
    }

    @Test
    void testRegisterProcessesWithNonBusinessProcessBean() {
        // Mock a bean that's not a BusinessProcess
        String nonBusinessProcessBean = "I'm not a BusinessProcess";
        Map<String, Object> beans = new HashMap<>();
        beans.put("stringBean", nonBusinessProcessBean);

        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(beans);
        when(applicationContext.findAnnotationOnBean("stringBean", BP.class)).thenReturn(new BP() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BP.class;
            }

            @Override
            public String value() {
                return "string-service";
            }
        });

        processRegistry.registerProcesses();

        // Should not register non-BusinessProcess beans
        BusinessProcess retrievedProcess = processRegistry.getProcess("string-service");
        assertNull(retrievedProcess);
    }

    @Test
    void testRegisterProcessesWithDuplicateProcessNames() {
        // Create two different BusinessProcess implementations with same name
        class FirstProcess implements BusinessProcess {
            @Override
            public com.fasterxml.jackson.databind.JsonNode handle(com.fasterxml.jackson.databind.JsonNode request) {
                return null;
            }
        }
        
        class SecondProcess implements BusinessProcess {
            @Override
            public com.fasterxml.jackson.databind.JsonNode handle(com.fasterxml.jackson.databind.JsonNode request) {
                return null;
            }
        }

        FirstProcess firstProcess = new FirstProcess();
        SecondProcess secondProcess = new SecondProcess();
        
        Map<String, Object> beans = new HashMap<>();
        beans.put("firstProcess", firstProcess);
        beans.put("secondProcess", secondProcess);

        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(beans);
        when(applicationContext.findAnnotationOnBean("firstProcess", BP.class)).thenReturn(new BP() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BP.class;
            }

            @Override
            public String value() {
                return "same-name";
            }
        });
        when(applicationContext.findAnnotationOnBean("secondProcess", BP.class)).thenReturn(new BP() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BP.class;
            }

            @Override
            public String value() {
                return "same-name";
            }
        });

        processRegistry.registerProcesses();

        // Should handle duplicate names (last one wins or first one wins depending on implementation)
        BusinessProcess retrievedProcess = processRegistry.getProcess("same-name");
        assertNotNull(retrievedProcess);
        assertTrue(retrievedProcess instanceof FirstProcess || retrievedProcess instanceof SecondProcess);
    }

    @Test
    void testGetProcessWithNullName() {
        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(Collections.emptyMap());
        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess(null);
        assertNull(retrievedProcess);
    }

    @Test
    void testGetProcessWithEmptyName() {
        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(Collections.emptyMap());
        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("");
        assertNull(retrievedProcess);
    }

    @Test
    void testGetProcessWithWhitespaceOnlyName() {
        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(Collections.emptyMap());
        processRegistry.registerProcesses();

        BusinessProcess retrievedProcess = processRegistry.getProcess("   ");
        assertNull(retrievedProcess);
    }

    @Test
    void testRegisterProcessesWithNullApplicationContext() {
        // Test with null application context - constructor accepts null but registerProcesses should fail
        ProcessRegistry registry = new ProcessRegistry(null);
        assertThrows(NullPointerException.class, () -> {
            registry.registerProcesses();
        });
    }

    @Test
    void testRegisterProcessesMultipleTimes() {
        // Mock a BusinessProcess bean
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

        // Register multiple times
        processRegistry.registerProcesses();
        processRegistry.registerProcesses();
        processRegistry.registerProcesses();

        // Should still work correctly
        BusinessProcess retrievedProcess = processRegistry.getProcess("echo-service");
        assertNotNull(retrievedProcess);
        assertEquals(echoService, retrievedProcess);
    }

    @Test
    void testRegisterProcessesWithLargeNumberOfProcesses() {
        // Test with many processes
        Map<String, Object> beans = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            final int index = i;
            BusinessProcess process = new BusinessProcess() {
                @Override
                public com.fasterxml.jackson.databind.JsonNode handle(com.fasterxml.jackson.databind.JsonNode request) {
                    return null;
                }
            };
            beans.put("process" + i, process);
        }

        when(applicationContext.getBeansWithAnnotation(BP.class)).thenReturn(beans);
        
        for (int i = 0; i < 100; i++) {
            final int index = i;
            when(applicationContext.findAnnotationOnBean("process" + i, BP.class)).thenReturn(new BP() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return BP.class;
                }

                @Override
                public String value() {
                    return "service-" + index;
                }
            });
        }

        processRegistry.registerProcesses();

        // Verify all processes are registered
        for (int i = 0; i < 100; i++) {
            BusinessProcess retrievedProcess = processRegistry.getProcess("service-" + i);
            assertNotNull(retrievedProcess, "Process service-" + i + " should be registered");
        }
    }
}
