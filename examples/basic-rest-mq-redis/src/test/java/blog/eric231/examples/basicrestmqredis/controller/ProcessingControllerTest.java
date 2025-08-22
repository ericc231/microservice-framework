package blog.eric231.examples.basicrestmqredis.controller;

import blog.eric231.examples.basicrestmqredis.logic.RequestProcessorLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProcessingController
 */
@WebMvcTest(ProcessingController.class)
class ProcessingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestProcessorLogic requestProcessorLogic;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessGeneral_Success() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "test message");
        payload.put("data", Map.of("key", "value"));

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("requestId", "test-request-id");
        mockResponse.put("processingStatus", "success");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.requestId").value("test-request-id"));
    }

    @Test
    void testProcessGeneral_Unauthorized() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "test message");

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProcessGeneral_WithBasicAuth() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "test message");

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("requestId", "test-request-id");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessGeneral_ProcessingError() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "test message");

        when(requestProcessorLogic.handle(any(JsonNode.class)))
            .thenThrow(new RuntimeException("Processing failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("General processing failed"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessStore_Success() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", Map.of("key", "value"));

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "store");
        mockResponse.put("storageId", "test-storage-id");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.operation").value("store"))
                .andExpect(jsonPath("$.storageId").value("test-storage-id"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessRetrieve_WithRequestId_Success() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "retrieve");
        mockResponse.put("found", true);

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/process/retrieve")
                .param("requestId", "test-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.operation").value("retrieve"))
                .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessRetrieve_WithStorageId_Success() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "retrieve");
        mockResponse.put("found", true);

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/process/retrieve")
                .param("storageId", "test-storage-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessRetrieve_WithoutParameters_Success() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "error");
        mockResponse.put("operation", "retrieve");
        mockResponse.put("errorMessage", "No valid key found for retrieval");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/process/retrieve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessUpdate_Success() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("storageId", "test-storage-id");
        payload.put("data", Map.of("updatedKey", "updatedValue"));

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "update");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/process/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.operation").value("update"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessDelete_WithRequestId_Success() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "delete");
        mockResponse.put("deleted", true);

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/process/delete")
                .param("requestId", "test-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.operation").value("delete"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessDelete_WithStorageId_Success() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "delete");
        mockResponse.put("deleted", true);

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/process/delete")
                .param("storageId", "test-storage-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testHealth_WithAuthentication() throws Exception {
        // Given
        ObjectNode mockMetadata = mapper.createObjectNode();
        mockMetadata.put("description", "Test description");
        mockMetadata.put("version", "1.0");

        when(requestProcessorLogic.getMetadata()).thenReturn(mockMetadata);

        // When & Then
        mockMvc.perform(get("/api/v1/process/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("basic-rest-mq-redis"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user").value("testuser"))
                .andExpect(jsonPath("$.domainLogicMetadata").exists());
    }

    @Test
    void testHealth_WithoutAuthentication() throws Exception {
        // Given
        ObjectNode mockMetadata = mapper.createObjectNode();
        mockMetadata.put("description", "Test description");

        when(requestProcessorLogic.getMetadata()).thenReturn(mockMetadata);

        // When & Then
        mockMvc.perform(get("/api/v1/process/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testHealth_DomainLogicError() throws Exception {
        // Given
        when(requestProcessorLogic.getMetadata())
            .thenThrow(new RuntimeException("Domain logic unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/process/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.error").value("Domain logic unavailable"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessGeneral_MalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessGeneral_EmptyPayload() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("requestId", "test-request-id");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessStore_EmptyPayload() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "store");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("store"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessUpdate_EmptyPayload() throws Exception {
        // Given
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("operation", "update");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/process/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("update"));
    }

    @Test
    void testProcessStore_WithBasicAuthAndWrongCredentials() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", Map.of("key", "value"));

        // When & Then
        mockMvc.perform(post("/api/v1/process/store")
                .with(httpBasic("wrong", "credentials"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void testProcessGeneral_WithAdminUser() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("adminOperation", true);

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("requestId", "admin-request-id");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = "service", roles = {"SERVICE"})
    void testProcessGeneral_WithServiceUser() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceOperation", true);

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("requestId", "service-request-id");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testProcessGeneral_ComplexPayload() throws Exception {
        // Given
        Map<String, Object> complexPayload = new HashMap<>();
        complexPayload.put("operation", "complexOperation");
        complexPayload.put("clientId", "complex-client");
        
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("items", java.util.Arrays.asList("item1", "item2", "item3"));
        nestedData.put("metadata", Map.of("source", "test", "version", 2.0));
        complexPayload.put("data", nestedData);

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("status", "success");
        mockResponse.put("requestId", "complex-request-id");
        mockResponse.put("complexity", "high");

        when(requestProcessorLogic.handle(any(JsonNode.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/process/general")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(complexPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.requestId").value("complex-request-id"));
    }
}
