package blog.eric231.framework.infrastructure.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FrameworkProperties Tests")
class FrameworkPropertiesTest {

    private FrameworkProperties frameworkProperties;

    @BeforeEach
    void setUp() {
        frameworkProperties = new FrameworkProperties();
    }

    @Test
    void testConnectors_ShouldSetAndGetCorrectly() {
        FrameworkProperties.Connectors connectors = new FrameworkProperties.Connectors();
        frameworkProperties.setConnectors(connectors);

        assertEquals(connectors, frameworkProperties.getConnectors());
    }

    @Test
    void testRouting_ShouldSetAndGetCorrectly() {
        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName("test-process");
        
        frameworkProperties.setRouting(Collections.singletonList(routing));

        assertNotNull(frameworkProperties.getRouting());
        assertEquals(1, frameworkProperties.getRouting().size());
        assertEquals("test-process", frameworkProperties.getRouting().get(0).getProcessName());
    }

    @Test
    void testConnectors_RestConfiguration() {
        FrameworkProperties.Connectors connectors = new FrameworkProperties.Connectors();
        FrameworkProperties.Rest rest = new FrameworkProperties.Rest();
        rest.setEnabled(true);
        rest.setAuthMode("basic");
        connectors.setRest(rest);

        assertTrue(rest.isEnabled());
        assertEquals("basic", rest.getAuthMode());
    }

    @Test
    void testConnectors_RestDefaultAuthMode() {
        FrameworkProperties.Rest rest = new FrameworkProperties.Rest();
        
        assertEquals("bypass", rest.getAuthMode()); // Default value
    }

    @Test
    void testConnectors_KafkaConfiguration() {
        FrameworkProperties.Connectors connectors = new FrameworkProperties.Connectors();
        FrameworkProperties.Kafka kafka = new FrameworkProperties.Kafka();
        kafka.setEnabled(true);
        kafka.setBootstrapServers("localhost:9092");
        connectors.setKafka(kafka);

        assertTrue(kafka.isEnabled());
        assertEquals("localhost:9092", kafka.getBootstrapServers());
    }

    @Test
    void testRouting_MultipleRoutings() {
        FrameworkProperties.Routing routing1 = new FrameworkProperties.Routing();
        routing1.setProcessName("process1");
        
        FrameworkProperties.Routing routing2 = new FrameworkProperties.Routing();
        routing2.setProcessName("process2");
        
        frameworkProperties.setRouting(Arrays.asList(routing1, routing2));

        assertEquals(2, frameworkProperties.getRouting().size());
        assertEquals("process1", frameworkProperties.getRouting().get(0).getProcessName());
        assertEquals("process2", frameworkProperties.getRouting().get(1).getProcessName());
    }

    @Test
    void testTrigger_RestTrigger() {
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath("/api/test");
        trigger.setMethod("POST");

        assertEquals("rest", trigger.getType());
        assertEquals("/api/test", trigger.getPath());
        assertEquals("POST", trigger.getMethod());
    }

    @Test
    void testTrigger_KafkaTrigger() {
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("kafka");
        trigger.setListenTopic("input-topic");
        trigger.setResponseTopic("output-topic");

        assertEquals("kafka", trigger.getType());
        assertEquals("input-topic", trigger.getListenTopic());
        assertEquals("output-topic", trigger.getResponseTopic());
    }

    @Test
    void testRouting_WithTriggers() {
        FrameworkProperties.Trigger restTrigger = new FrameworkProperties.Trigger();
        restTrigger.setType("rest");
        restTrigger.setPath("/api/test");
        restTrigger.setMethod("GET");

        FrameworkProperties.Trigger kafkaTrigger = new FrameworkProperties.Trigger();
        kafkaTrigger.setType("kafka");
        kafkaTrigger.setListenTopic("test-topic");

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName("multi-trigger-process");
        routing.setTriggers(Arrays.asList(restTrigger, kafkaTrigger));

        assertNotNull(routing.getTriggers());
        assertEquals(2, routing.getTriggers().size());
        assertEquals("rest", routing.getTriggers().get(0).getType());
        assertEquals("kafka", routing.getTriggers().get(1).getType());
    }

    @Test
    void testCompleteConfiguration() {
        // Setup connectors
        FrameworkProperties.Rest rest = new FrameworkProperties.Rest();
        rest.setEnabled(true);
        rest.setAuthMode("ldap");

        FrameworkProperties.Kafka kafka = new FrameworkProperties.Kafka();
        kafka.setEnabled(false);
        kafka.setBootstrapServers("localhost:9092");

        FrameworkProperties.Connectors connectors = new FrameworkProperties.Connectors();
        connectors.setRest(rest);
        connectors.setKafka(kafka);

        // Setup routing
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        trigger.setType("rest");
        trigger.setPath("/api/user");
        trigger.setMethod("POST");

        FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
        routing.setProcessName("user-management");
        routing.setTriggers(Collections.singletonList(trigger));

        // Complete configuration
        frameworkProperties.setConnectors(connectors);
        frameworkProperties.setRouting(Collections.singletonList(routing));

        // Verify
        assertNotNull(frameworkProperties.getConnectors());
        assertTrue(frameworkProperties.getConnectors().getRest().isEnabled());
        assertEquals("ldap", frameworkProperties.getConnectors().getRest().getAuthMode());
        assertFalse(frameworkProperties.getConnectors().getKafka().isEnabled());

        assertNotNull(frameworkProperties.getRouting());
        assertEquals(1, frameworkProperties.getRouting().size());
        assertEquals("user-management", frameworkProperties.getRouting().get(0).getProcessName());
        assertEquals("rest", frameworkProperties.getRouting().get(0).getTriggers().get(0).getType());
    }

    @Test
    void testEmptyConfiguration() {
        FrameworkProperties emptyProps = new FrameworkProperties();
        
        assertNull(emptyProps.getConnectors());
        assertNull(emptyProps.getRouting());
    }

    @Test
    void testNullSafetyInTriggers() {
        FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
        
        assertNull(trigger.getType());
        assertNull(trigger.getPath());
        assertNull(trigger.getMethod());
        assertNull(trigger.getListenTopic());
        assertNull(trigger.getResponseTopic());
    }

    @Nested
    @DisplayName("Property Binding Tests")
    class PropertyBindingTests {

        @Test
        @DisplayName("Should bind properties from configuration source")
        void testPropertyBinding() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("framework.connectors.rest.enabled", "true");
            properties.put("framework.connectors.rest.authMode", "oauth2");
            properties.put("framework.connectors.kafka.enabled", "false");
            properties.put("framework.connectors.kafka.bootstrapServers", "localhost:9093");

            ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
            Binder binder = new Binder(source);

            FrameworkProperties boundProperties = binder.bind("framework", FrameworkProperties.class).get();

            assertNotNull(boundProperties.getConnectors());
            assertTrue(boundProperties.getConnectors().getRest().isEnabled());
            assertEquals("oauth2", boundProperties.getConnectors().getRest().getAuthMode());
            assertFalse(boundProperties.getConnectors().getKafka().isEnabled());
            assertEquals("localhost:9093", boundProperties.getConnectors().getKafka().getBootstrapServers());
        }

        @Test
        @DisplayName("Should bind complex routing configuration")
        void testComplexRoutingBinding() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("framework.routing[0].processName", "api-gateway");
            properties.put("framework.routing[0].triggers[0].type", "rest");
            properties.put("framework.routing[0].triggers[0].path", "/api/v1/users");
            properties.put("framework.routing[0].triggers[0].method", "GET");
            properties.put("framework.routing[1].processName", "message-processor");
            properties.put("framework.routing[1].triggers[0].type", "kafka");
            properties.put("framework.routing[1].triggers[0].listenTopic", "user-events");
            properties.put("framework.routing[1].triggers[0].responseTopic", "user-responses");

            ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
            Binder binder = new Binder(source);

            FrameworkProperties boundProperties = binder.bind("framework", FrameworkProperties.class).get();

            assertNotNull(boundProperties.getRouting());
            assertEquals(2, boundProperties.getRouting().size());

            // Verify first routing
            FrameworkProperties.Routing firstRouting = boundProperties.getRouting().get(0);
            assertEquals("api-gateway", firstRouting.getProcessName());
            assertEquals(1, firstRouting.getTriggers().size());
            assertEquals("rest", firstRouting.getTriggers().get(0).getType());
            assertEquals("/api/v1/users", firstRouting.getTriggers().get(0).getPath());
            assertEquals("GET", firstRouting.getTriggers().get(0).getMethod());

            // Verify second routing
            FrameworkProperties.Routing secondRouting = boundProperties.getRouting().get(1);
            assertEquals("message-processor", secondRouting.getProcessName());
            assertEquals(1, secondRouting.getTriggers().size());
            assertEquals("kafka", secondRouting.getTriggers().get(0).getType());
            assertEquals("user-events", secondRouting.getTriggers().get(0).getListenTopic());
            assertEquals("user-responses", secondRouting.getTriggers().get(0).getResponseTopic());
        }

        @Test
        @DisplayName("Should handle partial property binding")
        void testPartialPropertyBinding() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("framework.connectors.rest.enabled", "true");
            // Note: authMode is not set, should use default

            ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
            Binder binder = new Binder(source);

            FrameworkProperties boundProperties = binder.bind("framework", FrameworkProperties.class).get();

            assertNotNull(boundProperties.getConnectors());
            assertNotNull(boundProperties.getConnectors().getRest());
            assertTrue(boundProperties.getConnectors().getRest().isEnabled());
            // authMode should be the default value "bypass" since not specified in properties
            assertEquals("bypass", boundProperties.getConnectors().getRest().getAuthMode());
        }

        @Test
        @DisplayName("Should handle empty routing configuration")
        void testEmptyRoutingBinding() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("framework.routing", Collections.emptyList());

            ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
            Binder binder = new Binder(source);

            FrameworkProperties boundProperties = binder.bind("framework", FrameworkProperties.class).get();

            assertNotNull(boundProperties.getRouting());
            assertTrue(boundProperties.getRouting().isEmpty());
        }
    }

    @Nested
    @DisplayName("Connectors Configuration Tests")
    class ConnectorsTests {

        @Test
        @DisplayName("Should configure REST connector with all properties")
        void testRestConnectorFullConfiguration() {
            FrameworkProperties.Rest rest = new FrameworkProperties.Rest();
            rest.setEnabled(true);
            rest.setAuthMode("mtls");

            assertEquals(true, rest.isEnabled());
            assertEquals("mtls", rest.getAuthMode());
        }

        @Test
        @DisplayName("Should configure Kafka connector with all properties")
        void testKafkaConnectorFullConfiguration() {
            FrameworkProperties.Kafka kafka = new FrameworkProperties.Kafka();
            kafka.setEnabled(true);
            kafka.setBootstrapServers("broker1:9092,broker2:9092,broker3:9092");

            assertTrue(kafka.isEnabled());
            assertEquals("broker1:9092,broker2:9092,broker3:9092", kafka.getBootstrapServers());
        }

        @Test
        @DisplayName("Should handle disabled connectors")
        void testDisabledConnectors() {
            FrameworkProperties.Connectors connectors = new FrameworkProperties.Connectors();
            
            FrameworkProperties.Rest rest = new FrameworkProperties.Rest();
            rest.setEnabled(false);
            
            FrameworkProperties.Kafka kafka = new FrameworkProperties.Kafka();
            kafka.setEnabled(false);

            connectors.setRest(rest);
            connectors.setKafka(kafka);

            assertFalse(connectors.getRest().isEnabled());
            assertFalse(connectors.getKafka().isEnabled());
        }
    }

    @Nested
    @DisplayName("Trigger Configuration Tests")
    class TriggerTests {

        @Test
        @DisplayName("Should validate REST trigger configuration")
        void testRestTriggerValidation() {
            FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
            trigger.setType("rest");
            trigger.setPath("/api/test");
            trigger.setMethod("POST");

            assertEquals("rest", trigger.getType());
            assertEquals("/api/test", trigger.getPath());
            assertEquals("POST", trigger.getMethod());
            
            // Kafka-specific properties should be null for REST trigger
            assertNull(trigger.getListenTopic());
            assertNull(trigger.getResponseTopic());
        }

        @Test
        @DisplayName("Should validate Kafka trigger configuration")
        void testKafkaTriggerValidation() {
            FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
            trigger.setType("kafka");
            trigger.setListenTopic("input-topic");
            trigger.setResponseTopic("output-topic");

            assertEquals("kafka", trigger.getType());
            assertEquals("input-topic", trigger.getListenTopic());
            assertEquals("output-topic", trigger.getResponseTopic());
            
            // REST-specific properties should be null for Kafka trigger
            assertNull(trigger.getPath());
            assertNull(trigger.getMethod());
        }

        @Test
        @DisplayName("Should handle invalid trigger types gracefully")
        void testInvalidTriggerType() {
            FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
            trigger.setType("invalid-type");
            trigger.setPath("/some/path");
            trigger.setListenTopic("some-topic");

            // Should accept any type (validation happens at runtime)
            assertEquals("invalid-type", trigger.getType());
            assertEquals("/some/path", trigger.getPath());
            assertEquals("some-topic", trigger.getListenTopic());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null connectors gracefully")
        void testNullConnectors() {
            FrameworkProperties properties = new FrameworkProperties();
            properties.setConnectors(null);

            assertNull(properties.getConnectors());
        }

        @Test
        @DisplayName("Should handle null routing gracefully")
        void testNullRouting() {
            FrameworkProperties properties = new FrameworkProperties();
            properties.setRouting(null);

            assertNull(properties.getRouting());
        }

        @Test
        @DisplayName("Should handle empty trigger list")
        void testEmptyTriggerList() {
            FrameworkProperties.Routing routing = new FrameworkProperties.Routing();
            routing.setProcessName("test-process");
            routing.setTriggers(Collections.emptyList());

            assertEquals("test-process", routing.getProcessName());
            assertNotNull(routing.getTriggers());
            assertTrue(routing.getTriggers().isEmpty());
        }

        @Test
        @DisplayName("Should handle multiple processes with same name")
        void testDuplicateProcessNames() {
            FrameworkProperties.Routing routing1 = new FrameworkProperties.Routing();
            routing1.setProcessName("duplicate-name");

            FrameworkProperties.Routing routing2 = new FrameworkProperties.Routing();
            routing2.setProcessName("duplicate-name");

            List<FrameworkProperties.Routing> routingList = Arrays.asList(routing1, routing2);

            // Should allow duplicates (validation happens at runtime)
            assertEquals(2, routingList.size());
            assertEquals("duplicate-name", routingList.get(0).getProcessName());
            assertEquals("duplicate-name", routingList.get(1).getProcessName());
        }

        @Test
        @DisplayName("Should handle very long configuration values")
        void testLongConfigurationValues() {
            String longString = "a".repeat(1000);
            
            FrameworkProperties.Rest rest = new FrameworkProperties.Rest();
            rest.setAuthMode(longString);

            assertEquals(longString, rest.getAuthMode());
        }

        @Test
        @DisplayName("Should handle special characters in configuration")
        void testSpecialCharacters() {
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`";
            
            FrameworkProperties.Trigger trigger = new FrameworkProperties.Trigger();
            trigger.setPath("/api/" + specialChars);
            trigger.setListenTopic("topic-" + specialChars);

            assertTrue(trigger.getPath().contains(specialChars));
            assertTrue(trigger.getListenTopic().contains(specialChars));
        }
    }
}