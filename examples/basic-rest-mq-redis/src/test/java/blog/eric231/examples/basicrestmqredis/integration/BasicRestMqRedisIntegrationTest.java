package blog.eric231.examples.basicrestmqredis.integration;

import blog.eric231.examples.basicrestmqredis.BasicRestMqRedisApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Basic REST MQ Redis example.
 * 
 * This test class uses TestContainers to spin up real RabbitMQ and Redis instances
 * and tests the complete flow from REST API through message processing to Redis storage.
 */
@SpringBootTest(
    classes = BasicRestMqRedisApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "logging.level.blog.eric231.examples.basicrestmqredis=DEBUG",
        "logging.level.blog.eric231.framework=DEBUG"
    }
)
@ActiveProfiles("test")
@Testcontainers
class BasicRestMqRedisIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ObjectMapper objectMapper;
    private String baseUrl;

    // TestContainers for RabbitMQ and Redis
    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"))
            .withExposedPorts(5672, 15672);

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure RabbitMQ properties
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        
        // Configure framework RabbitMQ properties
        registry.add("framework.rabbitmq.connection.host", rabbitmq::getHost);
        registry.add("framework.rabbitmq.connection.port", rabbitmq::getAmqpPort);
        registry.add("framework.rabbitmq.connection.username", rabbitmq::getAdminUsername);
        registry.add("framework.rabbitmq.connection.password", rabbitmq::getAdminPassword);

        // Configure Redis properties
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        
        // Configure framework Redis properties
        registry.add("framework.redis.connection.host", redis::getHost);
        registry.add("framework.redis.connection.port", () -> redis.getMappedPort(6379).toString());
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        baseUrl = "http://localhost:" + port;
        
        // Clear Redis before each test
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        } catch (Exception e) {
            // Ignore if Redis is not yet ready
        }
    }

    @Test
    void testCompleteStoreFlow_Success() throws Exception {
        // Given
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("operation", "store");
        requestPayload.put("clientId", "integration-test-client");
        
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "PROD-001");
        data.put("productName", "Integration Test Product");
        data.put("price", 99.99);
        data.put("category", "electronics");
        requestPayload.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("admin", "admin123");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/process/store", 
            request, 
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertThat(responseJson.get("status").asText()).isEqualTo("success");
        assertThat(responseJson.has("requestId")).isTrue();
        assertThat(responseJson.get("processingStatus").asText()).isEqualTo("success");
        assertThat(responseJson.has("storageId")).isTrue();
        assertThat(responseJson.has("redisKey")).isTrue();

        String storageId = responseJson.get("storageId").asText();
        String redisKey = responseJson.get("redisKey").asText();

        // Verify data is stored in Redis
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Object storedData = redisTemplate.opsForValue().get(redisKey);
            assertThat(storedData).isNotNull();
            
            if (storedData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) storedData;
                assertThat(dataMap.get("operation")).isEqualTo("store");
                assertThat(dataMap.get("storageId")).isEqualTo(storageId);
                assertThat(dataMap.get("clientId")).isEqualTo("integration-test-client");
                assertThat(dataMap).containsKey("payload");
            }
        });
    }

    @Test
    void testCompleteRetrieveFlow_Success() throws Exception {
        // First, store some data
        testCompleteStoreFlow_Success();

        // Wait a bit to ensure data is stored
        Thread.sleep(2000);

        // Get stored data keys to retrieve
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // Look for any data keys in Redis
            Set<String> keys = redisTemplate.keys("data:*");
            assertThat(keys).isNotEmpty();
            
            String dataKey = keys.iterator().next();
            String storageId = dataKey.substring("data:".length());

            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("admin", "admin123");
            HttpEntity<String> request = new HttpEntity<>(headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/process/retrieve?storageId=" + storageId,
                HttpMethod.GET,
                request,
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            assertThat(responseJson.get("status").asText()).isEqualTo("success");
            assertThat(responseJson.get("processingStatus").asText()).isEqualTo("success");
            assertThat(responseJson.has("requestId")).isTrue();
        });
    }

    @Test
    void testCompleteDefaultFlow_Success() throws Exception {
        // Given - General processing request (will use default operation)
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("clientId", "default-test-client");
        requestPayload.put("action", "process");
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "default-processing");
        data.put("value", "test-value");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "integration-test");
        metadata.put("version", 1.0);
        data.put("metadata", metadata);
        requestPayload.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("user", "user123");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/process/general", 
            request, 
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertThat(responseJson.get("status").asText()).isEqualTo("success");
        assertThat(responseJson.has("requestId")).isTrue();
        assertThat(responseJson.get("processingStatus").asText()).isEqualTo("success");

        // For default operation, it should create enriched data
        if (responseJson.has("storageId") && responseJson.has("redisKey")) {
            String redisKey = responseJson.get("redisKey").asText();
            
            // Verify enriched data is stored in Redis
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Object storedData = redisTemplate.opsForValue().get(redisKey);
                assertThat(storedData).isNotNull();
                
                if (storedData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) storedData;
                    assertThat(dataMap.get("operation")).isEqualTo("default-enriched");
                    assertThat(dataMap.get("clientId")).isEqualTo("default-test-client");
                    assertThat(dataMap.get("processed")).isEqualTo(true);
                    assertThat(dataMap.get("enrichmentLevel")).isEqualTo("standard");
                    assertThat(dataMap).containsKey("originalPayload");
                }
            });
        }
    }

    @Test
    void testUpdateFlow_Success() throws Exception {
        // First store some data
        testCompleteStoreFlow_Success();
        
        // Wait for storage to complete
        Thread.sleep(2000);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // Get a stored data key to update
            Set<String> keys = redisTemplate.keys("data:*");
            assertThat(keys).isNotEmpty();
            
            String dataKey = keys.iterator().next();
            String storageId = dataKey.substring("data:".length());

            // Given - Update request
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("operation", "update");
            updatePayload.put("storageId", storageId);
            
            Map<String, Object> newData = new HashMap<>();
            newData.put("productName", "Updated Product Name");
            newData.put("price", 149.99);
            newData.put("lastModified", System.currentTimeMillis());
            updatePayload.put("data", newData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth("admin", "admin123");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(updatePayload, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/process/update",
                HttpMethod.PUT,
                request,
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            assertThat(responseJson.get("status").asText()).isEqualTo("success");
            assertThat(responseJson.get("processingStatus").asText()).isEqualTo("success");
        });
    }

    @Test
    void testDeleteFlow_Success() throws Exception {
        // First store some data
        testCompleteStoreFlow_Success();
        
        // Wait for storage to complete
        Thread.sleep(2000);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // Get a stored data key to delete
            Set<String> keys = redisTemplate.keys("data:*");
            assertThat(keys).isNotEmpty();
            
            String dataKey = keys.iterator().next();
            String storageId = dataKey.substring("data:".length());

            // Given - Delete request
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("admin", "admin123");
            HttpEntity<String> request = new HttpEntity<>(headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/process/delete?storageId=" + storageId,
                HttpMethod.DELETE,
                request,
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            assertThat(responseJson.get("status").asText()).isEqualTo("success");
            assertThat(responseJson.get("processingStatus").asText()).isEqualTo("success");
        });
    }

    @Test
    void testAuthenticationRequired() throws Exception {
        // Given - Request without authentication
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("operation", "store");
        Map<String, Object> testData = new HashMap<>();
        testData.put("test", "data");
        requestPayload.put("data", testData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // No authentication headers
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/process/store", 
            request, 
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testInvalidCredentials() throws Exception {
        // Given - Request with invalid credentials
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("operation", "store");
        Map<String, Object> testData2 = new HashMap<>();
        testData2.put("test", "data");
        requestPayload.put("data", testData2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("wrong", "credentials");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/process/store", 
            request, 
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("user", "user123");
        HttpEntity<String> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/v1/process/health",
            HttpMethod.GET,
            request,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertThat(responseJson.get("status").asText()).isEqualTo("UP");
        assertThat(responseJson.get("service").asText()).isEqualTo("basic-rest-mq-redis");
        assertThat(responseJson.get("authenticated").asBoolean()).isTrue();
        assertThat(responseJson.get("user").asText()).isEqualTo("user");
        assertThat(responseJson.has("domainLogicMetadata")).isTrue();
    }

    @Test
    void testConcurrentRequests() throws Exception {
        // Given - Multiple concurrent requests
        int numRequests = 5;
        Thread[] threads = new Thread[numRequests];
        String[] responses = new String[numRequests];
        Exception[] exceptions = new Exception[numRequests];

        // When - Execute concurrent requests
        for (int i = 0; i < numRequests; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    Map<String, Object> requestPayload = new HashMap<>();
                    requestPayload.put("operation", "store");
                    requestPayload.put("clientId", "concurrent-client-" + index);
                    Map<String, Object> concurrentData = new HashMap<>();
                    concurrentData.put("threadId", index);
                    concurrentData.put("timestamp", System.currentTimeMillis());
                    requestPayload.put("data", concurrentData);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBasicAuth("admin", "admin123");
                    
                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(
                        baseUrl + "/api/v1/process/store", 
                        request, 
                        String.class
                    );

                    responses[index] = response.getBody();
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(30000); // 30 second timeout
        }

        // Then - Verify all requests succeeded
        for (int i = 0; i < numRequests; i++) {
            assertThat(exceptions[i]).isNull();
            assertThat(responses[i]).isNotNull();
            
            JsonNode responseJson = objectMapper.readTree(responses[i]);
            assertThat(responseJson.get("status").asText()).isEqualTo("success");
            assertThat(responseJson.has("requestId")).isTrue();
        }

        // Verify Redis contains data from all requests
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Set<String> keys = redisTemplate.keys("data:*");
            // Should have at least the number of concurrent requests we made
            assertThat(keys.size()).isGreaterThanOrEqualTo(numRequests);
        });
    }
}
