package blog.eric231.examples.helloworld;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for HelloWorld business process without web layer
 */
@SpringBootTest(classes = HelloworldServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration", 
    "framework.connectors.rest.enabled=false", 
    "jasypt.encryptor.enabled=false",
    "framework.redis.enabled=false"
})
public class HelloWorldBusinessProcessIntegrationTest {

    @Autowired
    private HelloWorldBusinessProcess helloWorldBusinessProcess;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testBusinessProcessDirectly() throws Exception {
        // Create a simple request node
        JsonNode request = objectMapper.createObjectNode();
        
        // Test the business logic directly
        JsonNode result = helloWorldBusinessProcess.handle(request);
        
        assertNotNull(result);
        assertEquals("Hello from Business Process!", result.asText());
    }

    @Test
    public void testBusinessProcessInitialization() {
        // Verify that the business process is properly initialized
        assertNotNull(helloWorldBusinessProcess);
    }
}