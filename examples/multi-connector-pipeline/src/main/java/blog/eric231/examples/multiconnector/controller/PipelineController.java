package blog.eric231.examples.multiconnector.controller;

import blog.eric231.examples.multiconnector.logic.FilePipelineProcessor;
import blog.eric231.examples.multiconnector.logic.FileEventHandler;
import blog.eric231.examples.multiconnector.logic.IBMMQMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Pipeline Operations
 * 
 * Provides REST API endpoints for managing and executing file processing pipelines
 * and handling file events through the multi-connector framework.
 */
@RestController
@RequestMapping("/api/pipeline")
@CrossOrigin(origins = "*")
public class PipelineController {

    private static final Logger logger = LoggerFactory.getLogger(PipelineController.class);

    private final FilePipelineProcessor pipelineProcessor;
    private final FileEventHandler fileEventHandler;
    private final IBMMQMessageHandler ibmMQMessageHandler;
    private final ObjectMapper objectMapper;

    @Autowired
    public PipelineController(FilePipelineProcessor pipelineProcessor,
                             FileEventHandler fileEventHandler,
                             IBMMQMessageHandler ibmMQMessageHandler,
                             ObjectMapper objectMapper) {
        this.pipelineProcessor = pipelineProcessor;
        this.fileEventHandler = fileEventHandler;
        this.ibmMQMessageHandler = ibmMQMessageHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a complete file processing pipeline
     */
    @PostMapping("/execute")
    public ResponseEntity<JsonNode> executePipeline(@RequestBody Map<String, Object> request) {
        try {
            logger.info("Executing pipeline with request: {}", request);
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = pipelineProcessor.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error executing pipeline: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Pipeline execution failed", e.getMessage()));
        }
    }

    /**
     * Download files from FTP/SFTP sources
     */
    @PostMapping("/download")
    public ResponseEntity<JsonNode> downloadFile(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "download");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = pipelineProcessor.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Download failed", e.getMessage()));
        }
    }

    /**
     * Upload files to S3
     */
    @PostMapping("/upload")
    public ResponseEntity<JsonNode> uploadFile(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "upload");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = pipelineProcessor.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Upload failed", e.getMessage()));
        }
    }

    /**
     * Check the status of a pipeline job
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<JsonNode> getJobStatus(@PathVariable String jobId) {
        try {
            Map<String, Object> statusRequest = new HashMap<>();
            statusRequest.put("operation", "status");
            statusRequest.put("jobId", jobId);
            
            JsonNode requestNode = objectMapper.valueToTree(statusRequest);
            JsonNode result = pipelineProcessor.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting job status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Status check failed", e.getMessage()));
        }
    }

    /**
     * Handle file system events
     */
    @PostMapping("/events/file")
    public ResponseEntity<JsonNode> handleFileEvent(@RequestBody Map<String, Object> event) {
        try {
            logger.info("Processing file event: {}", event);
            
            JsonNode eventNode = objectMapper.valueToTree(event);
            JsonNode result = fileEventHandler.handle(eventNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error handling file event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Event processing failed", e.getMessage()));
        }
    }

    /**
     * Send message to IBM MQ queue
     */
    @PostMapping("/mq/send")
    public ResponseEntity<JsonNode> sendMQMessage(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "send");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = ibmMQMessageHandler.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error sending MQ message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MQ send failed", e.getMessage()));
        }
    }

    /**
     * Receive message from IBM MQ queue
     */
    @PostMapping("/mq/receive")
    public ResponseEntity<JsonNode> receiveMQMessage(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "receive");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = ibmMQMessageHandler.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error receiving MQ message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MQ receive failed", e.getMessage()));
        }
    }

    /**
     * Browse messages in IBM MQ queue
     */
    @PostMapping("/mq/browse")
    public ResponseEntity<JsonNode> browseMQQueue(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "browse");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = ibmMQMessageHandler.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error browsing MQ queue: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MQ browse failed", e.getMessage()));
        }
    }

    /**
     * Get IBM MQ queue depth
     */
    @PostMapping("/mq/queue-depth")
    public ResponseEntity<JsonNode> getMQQueueDepth(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "queue_depth");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = ibmMQMessageHandler.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting MQ queue depth: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MQ queue depth failed", e.getMessage()));
        }
    }

    /**
     * Send pipeline trigger via IBM MQ
     */
    @PostMapping("/mq/trigger-pipeline")
    public ResponseEntity<JsonNode> triggerPipelineViaMQ(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "send_pipeline_trigger");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = ibmMQMessageHandler.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error triggering pipeline via MQ: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MQ pipeline trigger failed", e.getMessage()));
        }
    }

    /**
     * Process batch of messages from IBM MQ queue
     */
    @PostMapping("/mq/batch-process")
    public ResponseEntity<JsonNode> batchProcessMQMessages(@RequestBody Map<String, Object> request) {
        try {
            request.put("operation", "batch_process");
            
            JsonNode requestNode = objectMapper.valueToTree(request);
            JsonNode result = ibmMQMessageHandler.handle(requestNode);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in MQ batch processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MQ batch processing failed", e.getMessage()));
        }
    }

    /**
     * Get pipeline metadata and capabilities
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getPipelineInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            // Get metadata from domain logic components
            info.put("pipelineProcessor", pipelineProcessor.getMetadata());
            info.put("fileEventHandler", fileEventHandler.getMetadata());
            info.put("ibmMQMessageHandler", ibmMQMessageHandler.getMetadata());
            
            // Add API capabilities
            info.put("apiVersion", "1.0.0");
            Map<String, String> endpoints = new HashMap<>();
            endpoints.put("execute", "POST /api/pipeline/execute - Execute complete pipeline");
            endpoints.put("download", "POST /api/pipeline/download - Download files from FTP/SFTP");
            endpoints.put("upload", "POST /api/pipeline/upload - Upload files to S3");
            endpoints.put("status", "GET /api/pipeline/status/{jobId} - Check job status");
            endpoints.put("fileEvent", "POST /api/pipeline/events/file - Handle file events");
            endpoints.put("mqSend", "POST /api/pipeline/mq/send - Send message to IBM MQ queue");
            endpoints.put("mqReceive", "POST /api/pipeline/mq/receive - Receive message from IBM MQ queue");
            endpoints.put("mqBrowse", "POST /api/pipeline/mq/browse - Browse IBM MQ queue messages");
            endpoints.put("mqQueueDepth", "POST /api/pipeline/mq/queue-depth - Get IBM MQ queue depth");
            endpoints.put("mqTriggerPipeline", "POST /api/pipeline/mq/trigger-pipeline - Trigger pipeline via IBM MQ");
            endpoints.put("mqBatchProcess", "POST /api/pipeline/mq/batch-process - Process batch of IBM MQ messages");
            endpoints.put("info", "GET /api/pipeline/info - Get pipeline information");
            info.put("endpoints", endpoints);
            
            // Add sample request formats
            info.put("sampleRequests", createSampleRequests());
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Error getting pipeline info: {}", e.getMessage(), e);
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", "Failed to retrieve pipeline info");
            errorInfo.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorInfo);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        Map<String, String> components = new HashMap<>();
        components.put("pipelineProcessor", "UP");
        components.put("fileEventHandler", "UP");
        components.put("ibmMQMessageHandler", "UP");
        health.put("components", components);
        return ResponseEntity.ok(health);
    }

    /**
     * Create sample request formats for documentation
     */
    private Map<String, Object> createSampleRequests() {
        Map<String, Object> samples = new HashMap<>();
        
        // Pipeline execution sample
        samples.put("executeRequest", Map.of(
            "operation", "process",
            "sourceType", "ftp",  // or "sftp"
            "remotePath", "/remote/path/to/file.csv",
            "description", "Process incoming CSV file"
        ));
        
        // Download sample
        samples.put("downloadRequest", Map.of(
            "sourceType", "sftp",
            "remotePath", "/remote/data/input.xml",
            "localPath", "./temp/downloaded/input.xml"
        ));
        
        // Upload sample
        samples.put("uploadRequest", Map.of(
            "localPath", "./temp/processed/output.json",
            "s3Key", "processed/2024/01/output.json"
        ));
        
        // File event sample
        samples.put("fileEventRequest", Map.of(
            "eventType", "created",  // or "modified", "deleted", "moved"
            "filePath", "/watch/directory/new_file.csv",
            "timestamp", System.currentTimeMillis()
        ));
        
        // IBM MQ samples
        samples.put("mqSendRequest", Map.of(
            "queueName", "DEV.QUEUE.1",
            "message", "Hello from multi-connector pipeline!",
            "priority", 5,
            "expiry", 3600000
        ));
        
        samples.put("mqReceiveRequest", Map.of(
            "queueName", "DEV.QUEUE.1",
            "timeout", 5000
        ));
        
        samples.put("mqBrowseRequest", Map.of(
            "queueName", "DEV.QUEUE.1",
            "maxMessages", 10
        ));
        
        samples.put("mqTriggerPipelineRequest", Map.of(
            "triggerType", "file_upload",
            "sourceType", "sftp",
            "remotePath", "/incoming/data.xml",
            "priority", "high"
        ));
        
        samples.put("mqBatchProcessRequest", Map.of(
            "queueName", "BATCH.PROCESSING.QUEUE",
            "batchSize", 20,
            "timeout", 10000
        ));
        
        return samples;
    }

    /**
     * Create standardized error response
     */
    private JsonNode createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return objectMapper.valueToTree(response);
    }
}
