package blog.eric231.examples.basicrestmqredis.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RequestProcessorLogic
 */
@ExtendWith(MockitoExtension.class)
class RequestProcessorLogicTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RequestProcessorLogic requestProcessorLogic;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        requestProcessorLogic = new RequestProcessorLogic(rabbitTemplate);
        mapper = new ObjectMapper();
    }

    @Test
    void testHandle_SuccessfulProcessing() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("operation", "store");
        input.put("clientId", "test-client");
        input.set("data", mapper.createObjectNode().put("key", "value"));

        ObjectNode mqResponse = mapper.createObjectNode();
        mqResponse.put("status", "success");
        mqResponse.put("storageId", "test-storage-id");
        mqResponse.put("redisKey", "data:test-storage-id");
        mqResponse.put("message", "Data stored successfully");
        mqResponse.put("processingTime", 100);

        when(rabbitTemplate.sendAndReceive(eq("processing-exchange"), eq("process.request"), 
                                           any(Message.class)))
            .thenReturn(new Message(mqResponse.toString().getBytes(), new MessageProperties()));

        // When
        JsonNode result = requestProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.has("requestId")).isTrue();
        assertThat(result.get("processingStatus").asText()).isEqualTo("success");
        assertThat(result.get("storageId").asText()).isEqualTo("test-storage-id");
        assertThat(result.get("redisKey").asText()).isEqualTo("data:test-storage-id");
        assertThat(result.has("totalRoundTripTime")).isTrue();
        assertThat(result.has("mqResponse")).isTrue();

        // Verify the MQ interaction
        verify(rabbitTemplate).sendAndReceive(eq("processing-exchange"), eq("process.request"), 
                                             any(Message.class));
    }

    @Test
    void testHandle_DefaultOperation() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("clientId", "test-client");
        input.set("data", mapper.createObjectNode().put("message", "test message"));

        ObjectNode mqResponse = mapper.createObjectNode();
        mqResponse.put("status", "success");
        mqResponse.put("operation", "default-enriched");
        mqResponse.put("storageId", "default-storage-id");
        mqResponse.put("enrichmentApplied", true);

        when(rabbitTemplate.sendAndReceive(eq("processing-exchange"), eq("process.request"), 
                                           any(Message.class)))
            .thenReturn(new Message(mqResponse.toString().getBytes(), new MessageProperties()));

        // When
        JsonNode result = requestProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("success");
        assertThat(result.has("requestId")).isTrue();
        assertThat(result.get("processingStatus").asText()).isEqualTo("success");

        // Verify the MQ interaction
        verify(rabbitTemplate).sendAndReceive(eq("processing-exchange"), eq("process.request"), 
                                             any(Message.class));
    }

    @Test
    void testHandle_NullResponse() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("operation", "store");

        when(rabbitTemplate.sendAndReceive(anyString(), anyString(), any(Message.class)))
            .thenReturn(null);

        // When
        JsonNode result = requestProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("error");
        assertThat(result.get("errorCode").asText()).isEqualTo("TIMEOUT");
        assertThat(result.get("errorMessage").asText()).isEqualTo("No response received from message processor");
        assertThat(result.has("requestId")).isTrue();
        assertThat(result.has("timestamp")).isTrue();
    }

    @Test
    void testHandle_RabbitTemplateThrowsException() throws Exception {
        // Given
        ObjectNode input = mapper.createObjectNode();
        input.put("operation", "delete");

        when(rabbitTemplate.sendAndReceive(anyString(), anyString(), any(Message.class)))
            .thenThrow(new RuntimeException("RabbitMQ service unavailable"));

        // When
        JsonNode result = requestProcessorLogic.handle(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("status").asText()).isEqualTo("error");
        assertThat(result.get("errorCode").asText()).isEqualTo("MQ_ERROR");
        assertThat(result.get("errorMessage").asText()).contains("MQ communication failed");
    }

    @Test
    void testGetOperationName() {
        // When & Then
        assertThat(requestProcessorLogic.getOperationName()).isEqualTo("request-processor");
    }

    @Test
    void testGetMetadata() {
        // When
        JsonNode metadata = requestProcessorLogic.getMetadata();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.get("description").asText())
            .isEqualTo("Handles REST requests and coordinates with MQ for processing");
        assertThat(metadata.get("version").asText()).isEqualTo("1.0");
        assertThat(metadata.get("stage").asText()).isEqualTo("request-processing");
        assertThat(metadata.get("inputSource").asText()).isEqualTo("REST API");
        assertThat(metadata.get("outputTarget").asText()).isEqualTo("RabbitMQ");
        assertThat(metadata.get("responsePattern").asText()).isEqualTo("synchronous-over-async");
    }
}
