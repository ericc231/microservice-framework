package blog.eric231.examples.basicrestredis.api;

import blog.eric231.examples.basicrestredis.logic.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedisTestController.class)
class RedisTestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisCreateLogic redisCreateLogic;

    @MockBean
    private RedisReadLogic redisReadLogic;

    @MockBean
    private RedisUpdateLogic redisUpdateLogic;

    @MockBean
    private RedisDeleteLogic redisDeleteLogic;

    @Autowired
    private ObjectMapper objectMapper;

    private ObjectNode successResponse;
    private ObjectNode errorResponse;

    @BeforeEach
    void setUp() {
        successResponse = objectMapper.createObjectNode();
        successResponse.put("success", true);
        successResponse.put("message", "Operation completed successfully");
        successResponse.put("id", "test-id");

        errorResponse = objectMapper.createObjectNode();
        errorResponse.put("success", false);
        errorResponse.put("error", "Operation failed");
    }

    @Test
    void testCreateDataSuccess() throws Exception {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("id", "test-id");
        requestBody.put("name", "Test Name");

        when(redisCreateLogic.handle(any(JsonNode.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/test/redis/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void testCreateDataFailure() throws Exception {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("id", "test-id");
        requestBody.put("name", "Test Name");

        when(redisCreateLogic.handle(any(JsonNode.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/test/redis/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Operation failed"));
    }

    @Test
    void testReadDataSuccess() throws Exception {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("id", "test-id");
        requestBody.put("operation", "single");

        when(redisReadLogic.handle(any(JsonNode.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/test/redis/read")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetByIdSuccess() throws Exception {
        // Arrange
        when(redisReadLogic.handle(any(JsonNode.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        // Arrange
        when(redisReadLogic.handle(any(JsonNode.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/nonexistent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetByCategorySuccess() throws Exception {
        // Arrange
        ObjectNode categoryResponse = objectMapper.createObjectNode();
        categoryResponse.put("success", true);
        categoryResponse.put("category", "test-category");
        categoryResponse.put("count", 2);

        when(redisReadLogic.handle(any(JsonNode.class))).thenReturn(categoryResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/category/test-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.category").value("test-category"))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void testGetAllDataSuccess() throws Exception {
        // Arrange
        ObjectNode allResponse = objectMapper.createObjectNode();
        allResponse.put("success", true);
        allResponse.put("count", 5);

        when(redisReadLogic.handle(any(JsonNode.class))).thenReturn(allResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void testGetAllDataWithMetadata() throws Exception {
        // Arrange
        ObjectNode allResponse = objectMapper.createObjectNode();
        allResponse.put("success", true);
        allResponse.put("count", 3);

        when(redisReadLogic.handle(any(JsonNode.class))).thenReturn(allResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/all?includeMetadata=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void testUpdateDataSuccess() throws Exception {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("id", "test-id");
        requestBody.put("name", "Updated Name");

        ObjectNode updateResponse = objectMapper.createObjectNode();
        updateResponse.put("success", true);
        updateResponse.put("id", "test-id");
        updateResponse.put("operation", "patch");

        when(redisUpdateLogic.handle(any(JsonNode.class))).thenReturn(updateResponse);

        // Act & Assert
        mockMvc.perform(put("/test/redis/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.operation").value("patch"));
    }

    @Test
    void testDeleteDataSuccess() throws Exception {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("id", "test-id");
        requestBody.put("operation", "single");

        ObjectNode deleteResponse = objectMapper.createObjectNode();
        deleteResponse.put("success", true);
        deleteResponse.put("message", "Data deleted successfully");

        when(redisDeleteLogic.handle(any(JsonNode.class))).thenReturn(deleteResponse);

        // Act & Assert
        mockMvc.perform(delete("/test/redis/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Data deleted successfully"));
    }

    @Test
    void testDeleteByIdSuccess() throws Exception {
        // Arrange
        ObjectNode deleteResponse = objectMapper.createObjectNode();
        deleteResponse.put("success", true);
        deleteResponse.put("id", "test-id");

        when(redisDeleteLogic.handle(any(JsonNode.class))).thenReturn(deleteResponse);

        // Act & Assert
        mockMvc.perform(delete("/test/redis/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void testArchiveDataSuccess() throws Exception {
        // Arrange
        ObjectNode archiveResponse = objectMapper.createObjectNode();
        archiveResponse.put("success", true);
        archiveResponse.put("id", "test-id");
        archiveResponse.put("newStatus", "archived");

        when(redisDeleteLogic.handle(any(JsonNode.class))).thenReturn(archiveResponse);

        // Act & Assert
        mockMvc.perform(post("/test/redis/archive/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newStatus").value("archived"));
    }

    @Test
    void testGetOperationMetadataCreate() throws Exception {
        // Arrange
        ObjectNode metadataResponse = objectMapper.createObjectNode();
        metadataResponse.put("operation", "redis-create");
        metadataResponse.put("description", "Create new data in Redis");
        metadataResponse.put("version", "1.0");

        when(redisCreateLogic.getMetadata()).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/metadata/create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("redis-create"))
                .andExpect(jsonPath("$.version").value("1.0"));
    }

    @Test
    void testGetOperationMetadataRead() throws Exception {
        // Arrange
        ObjectNode metadataResponse = objectMapper.createObjectNode();
        metadataResponse.put("operation", "redis-read");
        metadataResponse.put("description", "Read data from Redis");

        when(redisReadLogic.getMetadata()).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/metadata/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("redis-read"));
    }

    @Test
    void testGetOperationMetadataUpdate() throws Exception {
        // Arrange
        ObjectNode metadataResponse = objectMapper.createObjectNode();
        metadataResponse.put("operation", "redis-update");

        when(redisUpdateLogic.getMetadata()).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/metadata/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("redis-update"));
    }

    @Test
    void testGetOperationMetadataDelete() throws Exception {
        // Arrange
        ObjectNode metadataResponse = objectMapper.createObjectNode();
        metadataResponse.put("operation", "redis-delete");

        when(redisDeleteLogic.getMetadata()).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/test/redis/metadata/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("redis-delete"));
    }

    @Test
    void testGetOperationMetadataInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/test/redis/metadata/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown operation: invalid"))
                .andExpect(jsonPath("$.availableOperations").value("create, read, update, delete"));
    }

    @Test
    void testHealthCheck() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/test/redis/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("basic-rest-redis"))
                .andExpect(jsonPath("$.operations.create").value("redis-create"))
                .andExpect(jsonPath("$.operations.read").value("redis-read"))
                .andExpect(jsonPath("$.operations.update").value("redis-update"))
                .andExpect(jsonPath("$.operations.delete").value("redis-delete"));
    }

    @Test
    void testCreateDataInternalServerError() throws Exception {
        // Arrange
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("id", "test-id");

        when(redisCreateLogic.handle(any(JsonNode.class))).thenThrow(new RuntimeException("Internal error"));

        // Act & Assert
        mockMvc.perform(post("/test/redis/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }
}