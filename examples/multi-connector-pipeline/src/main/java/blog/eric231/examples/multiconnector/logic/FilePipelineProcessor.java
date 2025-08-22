package blog.eric231.examples.multiconnector.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.connector.FTPConnector;
import blog.eric231.framework.infrastructure.connector.S3Connector;
import blog.eric231.framework.infrastructure.connector.SFTPConnector;
import blog.eric231.framework.infrastructure.connector.SSHConnector;
import blog.eric231.framework.infrastructure.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * File Pipeline Processor - Core Domain Logic
 * 
 * This domain logic orchestrates a complex file processing pipeline that demonstrates
 * the integration of multiple connectors in a real-world scenario.
 * 
 * Pipeline Steps:
 * 1. Download source files from FTP/SFTP
 * 2. Process files using remote SSH commands
 * 3. Cache intermediate results in Redis
 * 4. Upload processed files to S3
 * 5. Send completion notifications via RabbitMQ
 */
@Component
@DL(name = "FilePipelineProcessor", description = "Multi-connector file processing pipeline")
public class FilePipelineProcessor implements DomainLogic {

    private static final Logger logger = LoggerFactory.getLogger(FilePipelineProcessor.class);

    private final FTPConnector ftpConnector;
    private final SFTPConnector sftpConnector;
    private final SSHConnector sshConnector;
    private final S3Connector s3Connector;
    private final RedisAdapter redisAdapter;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public FilePipelineProcessor(FTPConnector ftpConnector,
                                SFTPConnector sftpConnector,
                                SSHConnector sshConnector,
                                S3Connector s3Connector,
                                RedisAdapter redisAdapter,
                                RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper) {
        this.ftpConnector = ftpConnector;
        this.sftpConnector = sftpConnector;
        this.sshConnector = sshConnector;
        this.s3Connector = s3Connector;
        this.redisAdapter = redisAdapter;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode handle(JsonNode input) {
        try {
            String operation = input.has("operation") ? input.get("operation").asText() : "process";
            String jobId = UUID.randomUUID().toString();
            
            logger.info("Starting file pipeline processing with job ID: {} for operation: {}", jobId, operation);
            
            // Store job metadata in Redis
            storeJobMetadata(jobId, input);
            
            switch (operation.toLowerCase()) {
                case "download":
                    return handleDownload(jobId, input);
                case "process":
                    return handleFullPipeline(jobId, input);
                case "upload":
                    return handleUpload(jobId, input);
                case "status":
                    return handleStatusCheck(input);
                default:
                    return handleFullPipeline(jobId, input);
            }
            
        } catch (Exception e) {
            logger.error("Error in file pipeline processing: {}", e.getMessage(), e);
            return createErrorResponse("Pipeline processing failed", e.getMessage());
        }
    }

    /**
     * Handle file download from FTP/SFTP sources
     */
    private JsonNode handleDownload(String jobId, JsonNode input) {
        try {
            updateJobStatus(jobId, "downloading", "Starting file download");
            
            String sourceType = input.get("sourceType").asText(); // "ftp" or "sftp"
            String remotePath = input.get("remotePath").asText();
            String localPath = "./temp/" + jobId + "/" + new File(remotePath).getName();
            
            CompletableFuture<JsonNode> downloadFuture;
            
            if ("sftp".equalsIgnoreCase(sourceType)) {
                downloadFuture = sftpConnector.downloadFile(remotePath, localPath);
            } else {
                downloadFuture = ftpConnector.downloadFile(remotePath, localPath);
            }
            
            JsonNode downloadResult = downloadFuture.join();
            
            // Cache download result
            redisAdapter.set("download:" + jobId, downloadResult, Duration.ofHours(24));
            
            updateJobStatus(jobId, "downloaded", "File downloaded successfully");
            
            // Send notification
            sendNotification(jobId, "download_complete", downloadResult);
            
            return createSuccessResponse(jobId, "download", downloadResult);
            
        } catch (Exception e) {
            updateJobStatus(jobId, "download_failed", e.getMessage());
            throw new RuntimeException("Download failed", e);
        }
    }

    /**
     * Handle full pipeline processing
     */
    private JsonNode handleFullPipeline(String jobId, JsonNode input) {
        try {
            logger.info("Executing full pipeline for job: {}", jobId);
            
            // Step 1: Download file
            updateJobStatus(jobId, "downloading", "Downloading source file");
            JsonNode downloadResult = executeDownloadStep(jobId, input);
            
            // Step 2: Process file via SSH
            updateJobStatus(jobId, "processing", "Processing file via SSH");
            JsonNode processResult = executeProcessingStep(jobId, downloadResult);
            
            // Step 3: Upload to S3
            updateJobStatus(jobId, "uploading", "Uploading to S3");
            JsonNode uploadResult = executeUploadStep(jobId, processResult);
            
            // Step 4: Final cleanup
            updateJobStatus(jobId, "completed", "Pipeline completed successfully");
            
            // Send final notification
            sendNotification(jobId, "pipeline_complete", uploadResult);
            
            Map<String, Object> pipelineResult = new HashMap<>();
            pipelineResult.put("jobId", jobId);
            pipelineResult.put("status", "completed");
            pipelineResult.put("steps", Map.of(
                "download", downloadResult,
                "process", processResult,
                "upload", uploadResult
            ));
            
            return objectMapper.valueToTree(pipelineResult);
            
        } catch (Exception e) {
            updateJobStatus(jobId, "failed", e.getMessage());
            sendNotification(jobId, "pipeline_failed", createErrorResponse("Pipeline failed", e.getMessage()));
            throw new RuntimeException("Pipeline execution failed", e);
        }
    }

    /**
     * Handle file upload to S3
     */
    private JsonNode handleUpload(String jobId, JsonNode input) {
        try {
            updateJobStatus(jobId, "uploading", "Starting S3 upload");
            
            String localPath = input.get("localPath").asText();
            String s3Key = input.has("s3Key") ? input.get("s3Key").asText() : 
                          "processed/" + jobId + "/" + new File(localPath).getName();
            
            CompletableFuture<JsonNode> uploadFuture = s3Connector.uploadFile(localPath, s3Key);
            JsonNode uploadResult = uploadFuture.join();
            
            // Cache upload result
            redisAdapter.set("upload:" + jobId, uploadResult, Duration.ofHours(24));
            
            updateJobStatus(jobId, "uploaded", "File uploaded to S3 successfully");
            
            // Send notification
            sendNotification(jobId, "upload_complete", uploadResult);
            
            return createSuccessResponse(jobId, "upload", uploadResult);
            
        } catch (Exception e) {
            updateJobStatus(jobId, "upload_failed", e.getMessage());
            throw new RuntimeException("Upload failed", e);
        }
    }

    /**
     * Handle status check for job
     */
    private JsonNode handleStatusCheck(JsonNode input) {
        try {
            String jobId = input.get("jobId").asText();
            Map<String, Object> jobStatus = redisAdapter.get("job:" + jobId, Map.class);
            
            if (jobStatus == null) {
                return createErrorResponse("Job not found", "No job found with ID: " + jobId);
            }
            
            return objectMapper.valueToTree(jobStatus);
            
        } catch (Exception e) {
            return createErrorResponse("Status check failed", e.getMessage());
        }
    }

    private JsonNode executeDownloadStep(String jobId, JsonNode input) {
        String sourceType = input.has("sourceType") ? input.get("sourceType").asText() : "ftp";
        String remotePath = input.get("remotePath").asText();
        String localPath = "./temp/" + jobId + "/source/" + new File(remotePath).getName();
        
        if ("sftp".equalsIgnoreCase(sourceType)) {
            return sftpConnector.downloadFile(remotePath, localPath).join();
        } else {
            return ftpConnector.downloadFile(remotePath, localPath).join();
        }
    }

    private JsonNode executeProcessingStep(String jobId, JsonNode downloadResult) {
        String localPath = downloadResult.get("localPath").asText();
        String command = String.format("wc -l %s && head -10 %s", localPath, localPath); // Example processing
        
        return sshConnector.executeCommand(command, "FilePipelineProcessor").join();
    }

    private JsonNode executeUploadStep(String jobId, JsonNode processResult) {
        // Create a processed file path - in real scenario this would be the actual processed file
        String processedFile = "./temp/" + jobId + "/processed/result.txt";
        String s3Key = "processed/" + jobId + "/result.txt";
        
        return s3Connector.uploadFile(processedFile, s3Key).join();
    }

    private void storeJobMetadata(String jobId, JsonNode input) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("jobId", jobId);
            metadata.put("status", "initialized");
            metadata.put("createdAt", System.currentTimeMillis());
            metadata.put("input", input);
            
            redisAdapter.set("job:" + jobId, metadata, Duration.ofDays(7));
        } catch (Exception e) {
            logger.error("Failed to store job metadata: {}", e.getMessage(), e);
        }
    }

    private void updateJobStatus(String jobId, String status, String message) {
        try {
            Map<String, Object> jobData = redisAdapter.get("job:" + jobId, Map.class);
            if (jobData != null) {
                jobData.put("status", status);
                jobData.put("message", message);
                jobData.put("updatedAt", System.currentTimeMillis());
                
                redisAdapter.set("job:" + jobId, jobData, Duration.ofDays(7));
                
                logger.info("Job {} status updated to: {} - {}", jobId, status, message);
            }
        } catch (Exception e) {
            logger.error("Failed to update job status: {}", e.getMessage(), e);
        }
    }

    private void sendNotification(String jobId, String eventType, JsonNode data) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("jobId", jobId);
            notification.put("eventType", eventType);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("data", data);
            
            rabbitTemplate.convertAndSend("pipeline.notifications", notification);
            
            logger.debug("Notification sent for job {}: {}", jobId, eventType);
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }

    private JsonNode createSuccessResponse(String jobId, String operation, JsonNode result) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("jobId", jobId);
        response.put("operation", operation);
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        
        return objectMapper.valueToTree(response);
    }

    private JsonNode createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return objectMapper.valueToTree(response);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "FilePipelineProcessor");
        metadata.put("version", "1.0.0");
        metadata.put("description", "Multi-connector file processing pipeline");
        metadata.put("connectors", new String[]{
            "FTPConnector", "SFTPConnector", "SSHConnector", 
            "S3Connector", "RabbitMQConnector", "RedisAdapter"
        });
        metadata.put("operations", new String[]{
            "download", "process", "upload", "status"
        });
        return metadata;
    }
}
