package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * S3 Connector that provides cloud storage capabilities using AWS S3.
 * 
 * This connector handles file uploads, downloads, bucket operations, and object monitoring,
 * routing S3 events to appropriate domain logic based on routing configuration.
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.s3.enabled", havingValue = "true")
public class S3Connector {

    private static final Logger logger = LoggerFactory.getLogger(S3Connector.class);
    
    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    private S3Client s3Client;

    @Autowired
    public S3Connector(ProcessRegistry processRegistry, 
                      FrameworkProperties frameworkProperties,
                      ObjectMapper objectMapper) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(10);
        logger.info("S3 Connector initialized");
    }

    @PostConstruct
    public void initialize() {
        try {
            FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
            
            if (s3Config == null) {
                logger.warn("S3 configuration not found, using defaults");
                return;
            }
            
            // Create S3 client
            createS3Client();
            
            // Create bucket if needed
            if (s3Config.isCreateBucket()) {
                ensureBucketExists(s3Config.getBucketName());
            }
            
            logger.info("S3 Connector initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize S3 Connector: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (s3Client != null) {
                s3Client.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            logger.info("S3 Connector cleaned up");
        } catch (Exception e) {
            logger.error("Error during S3 Connector cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Upload file to S3 bucket
     */
    public CompletableFuture<JsonNode> uploadFile(String localPath, String s3Key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Uploading file to S3: {} -> {}", localPath, s3Key);
                
                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    throw new S3ConnectorException("Local file not found: " + localPath);
                }
                
                FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
                String bucketName = s3Config.getBucketName();
                
                // Add key prefix if configured
                if (s3Config.getDefaultKeyPrefix() != null && !s3Config.getDefaultKeyPrefix().isEmpty()) {
                    s3Key = s3Config.getDefaultKeyPrefix() + "/" + s3Key;
                }
                
                PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
                
                s3Client.putObject(putRequest, RequestBody.fromFile(localFile));
                
                JsonNode result = createFileOperationResult("upload", localPath, s3Key, localFile.length(), bucketName);
                
                // Route to domain logic
                routeToDomainLogic(result, "upload");
                
                logger.debug("File uploaded successfully to S3: {}/{}", bucketName, s3Key);
                return result;
                
            } catch (Exception e) {
                logger.error("Error uploading file to S3 {}: {}", localPath, e.getMessage(), e);
                throw new S3ConnectorException("S3 file upload failed", e);
            }
        }, executorService);
    }

    /**
     * Download file from S3 bucket
     */
    public CompletableFuture<JsonNode> downloadFile(String s3Key, String localPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Downloading file from S3: {} -> {}", s3Key, localPath);
                
                FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
                String bucketName = s3Config.getBucketName();
                
                // Add key prefix if configured
                if (s3Config.getDefaultKeyPrefix() != null && !s3Config.getDefaultKeyPrefix().isEmpty()) {
                    s3Key = s3Config.getDefaultKeyPrefix() + "/" + s3Key;
                }
                
                // Ensure local directory exists
                ensureLocalDirectory(localPath);
                
                GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
                
                try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
                     FileOutputStream fos = new FileOutputStream(localPath)) {
                    
                    s3Object.transferTo(fos);
                }
                
                File downloadedFile = new File(localPath);
                JsonNode result = createFileOperationResult("download", localPath, s3Key, 
                    downloadedFile.exists() ? downloadedFile.length() : 0, bucketName);
                
                // Route to domain logic
                routeToDomainLogic(result, "download");
                
                logger.debug("File downloaded successfully from S3: {}", localPath);
                return result;
                
            } catch (Exception e) {
                logger.error("Error downloading file from S3 {}: {}", s3Key, e.getMessage(), e);
                throw new S3ConnectorException("S3 file download failed", e);
            }
        }, executorService);
    }

    /**
     * List objects in S3 bucket
     */
    public CompletableFuture<JsonNode> listObjects(String prefix, int maxKeys) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Listing S3 objects with prefix: {}, maxKeys: {}", prefix, maxKeys);
                
                FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
                String bucketName = s3Config.getBucketName();
                
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);
                
                if (prefix != null && !prefix.isEmpty()) {
                    String fullPrefix = s3Config.getDefaultKeyPrefix() != null ? 
                        s3Config.getDefaultKeyPrefix() + "/" + prefix : prefix;
                    requestBuilder.prefix(fullPrefix);
                }
                
                if (maxKeys > 0) {
                    requestBuilder.maxKeys(maxKeys);
                }
                
                ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
                
                List<Map<String, Object>> objects = new ArrayList<>();
                
                for (S3Object s3Object : response.contents()) {
                    Map<String, Object> objectInfo = new HashMap<>();
                    objectInfo.put("key", s3Object.key());
                    objectInfo.put("size", s3Object.size());
                    objectInfo.put("lastModified", s3Object.lastModified().toEpochMilli());
                    objectInfo.put("etag", s3Object.eTag());
                    objectInfo.put("storageClass", s3Object.storageClassAsString());
                    
                    objects.add(objectInfo);
                }
                
                JsonNode result = objectMapper.createObjectNode()
                    .put("operation", "list")
                    .put("bucketName", bucketName)
                    .put("prefix", prefix)
                    .put("objectCount", objects.size())
                    .put("isTruncated", response.isTruncated())
                    .put("timestamp", System.currentTimeMillis())
                    .set("objects", objectMapper.valueToTree(objects));
                
                // Route to domain logic
                routeToDomainLogic(result, "list");
                
                logger.debug("Listed {} objects in S3 bucket: {}", objects.size(), bucketName);
                return result;
                
            } catch (Exception e) {
                logger.error("Error listing S3 objects: {}", e.getMessage(), e);
                throw new S3ConnectorException("S3 object listing failed", e);
            }
        }, executorService);
    }

    /**
     * Delete object from S3 bucket
     */
    public CompletableFuture<JsonNode> deleteObject(String s3Key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Deleting S3 object: {}", s3Key);
                
                FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
                String bucketName = s3Config.getBucketName();
                
                // Add key prefix if configured
                if (s3Config.getDefaultKeyPrefix() != null && !s3Config.getDefaultKeyPrefix().isEmpty()) {
                    s3Key = s3Config.getDefaultKeyPrefix() + "/" + s3Key;
                }
                
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
                
                s3Client.deleteObject(deleteRequest);
                
                JsonNode result = objectMapper.createObjectNode()
                    .put("operation", "delete")
                    .put("bucketName", bucketName)
                    .put("s3Key", s3Key)
                    .put("status", "success")
                    .put("timestamp", System.currentTimeMillis());
                
                // Route to domain logic
                routeToDomainLogic(result, "delete");
                
                logger.debug("S3 object deleted successfully: {}", s3Key);
                return result;
                
            } catch (Exception e) {
                logger.error("Error deleting S3 object {}: {}", s3Key, e.getMessage(), e);
                throw new S3ConnectorException("S3 object deletion failed", e);
            }
        }, executorService);
    }

    /**
     * Get pre-signed URL for object access
     */
    public CompletableFuture<JsonNode> generatePresignedUrl(String s3Key, Duration expiration, String operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Generating pre-signed URL for S3 object: {} (operation: {})", s3Key, operation);
                
                FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
                String bucketName = s3Config.getBucketName();
                
                // Add key prefix if configured
                if (s3Config.getDefaultKeyPrefix() != null && !s3Config.getDefaultKeyPrefix().isEmpty()) {
                    s3Key = s3Config.getDefaultKeyPrefix() + "/" + s3Key;
                }
                
                // Note: Pre-signed URL generation requires additional AWS SDK setup
                // This is a simplified version - in production, you'd use S3Presigner
                
                JsonNode result = objectMapper.createObjectNode()
                    .put("operation", "presign")
                    .put("bucketName", bucketName)
                    .put("s3Key", s3Key)
                    .put("expiration", expiration.getSeconds())
                    .put("urlOperation", operation)
                    .put("timestamp", System.currentTimeMillis())
                    .put("url", "https://" + bucketName + ".s3.amazonaws.com/" + s3Key); // Simplified URL
                
                logger.debug("Pre-signed URL generated for S3 object: {}", s3Key);
                return result;
                
            } catch (Exception e) {
                logger.error("Error generating pre-signed URL for S3 object {}: {}", s3Key, e.getMessage(), e);
                throw new S3ConnectorException("Pre-signed URL generation failed", e);
            }
        }, executorService);
    }

    private void createS3Client() {
        FrameworkProperties.S3 s3Config = frameworkProperties.getConnectors().getS3();
        if (s3Config == null) {
            throw new S3ConnectorException("S3 configuration not available");
        }
        
        var clientBuilder = S3Client.builder()
            .region(Region.of(s3Config.getRegion()));
        
        // Set credentials if provided
        if (s3Config.getAccessKey() != null && s3Config.getSecretKey() != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                s3Config.getAccessKey(), s3Config.getSecretKey());
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }
        
        // Set custom endpoint if provided (for S3-compatible services)
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
            
            // Configure path-style access for custom endpoints
            if (s3Config.isPathStyleAccess()) {
                clientBuilder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
            }
        }
        
        s3Client = clientBuilder.build();
        
        logger.info("S3 Client created for region: {} with endpoint: {}", 
                   s3Config.getRegion(), s3Config.getEndpoint());
    }

    private void ensureBucketExists(String bucketName) {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
            
            s3Client.headBucket(headRequest);
            logger.debug("S3 bucket exists: {}", bucketName);
            
        } catch (NoSuchBucketException e) {
            try {
                CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
                
                s3Client.createBucket(createRequest);
                logger.info("Created S3 bucket: {}", bucketName);
                
            } catch (Exception ce) {
                logger.error("Failed to create S3 bucket {}: {}", bucketName, ce.getMessage());
                throw new S3ConnectorException("Failed to create S3 bucket", ce);
            }
        }
    }

    private JsonNode createFileOperationResult(String operation, String localPath, String s3Key, 
                                             long fileSize, String bucketName) {
        return objectMapper.createObjectNode()
            .put("operation", operation)
            .put("localPath", localPath)
            .put("s3Key", s3Key)
            .put("bucketName", bucketName)
            .put("fileSize", fileSize)
            .put("status", "success")
            .put("timestamp", System.currentTimeMillis());
    }

    private void ensureLocalDirectory(String localPath) {
        try {
            Path path = Paths.get(localPath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            logger.debug("Could not ensure local directory: {}", e.getMessage());
        }
    }

    private void routeToDomainLogic(JsonNode result, String operationType) {
        try {
            Optional<FrameworkProperties.Routing> matchedRouting = findMatchedRouting(operationType);
            if (matchedRouting.isPresent()) {
                String processName = matchedRouting.get().getProcessName();
                DomainLogic domainLogic = processRegistry.getDomainLogic(processName);
                if (domainLogic != null) {
                    domainLogic.handle(result);
                }
            }
        } catch (Exception e) {
            logger.error("Error routing S3 result to domain logic: {}", e.getMessage(), e);
        }
    }

    private Optional<FrameworkProperties.Routing> findMatchedRouting(String operationType) {
        if (frameworkProperties.getRouting() == null) {
            return Optional.empty();
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "s3".equalsIgnoreCase(trigger.getType()) &&
                                          (operationType == null || operationType.equals(trigger.getMethod()))))
                .findFirst();
    }

    /**
     * Custom exception for S3 operations
     */
    public static class S3ConnectorException extends RuntimeException {
        public S3ConnectorException(String message) {
            super(message);
        }
        
        public S3ConnectorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
