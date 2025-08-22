package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FTP Connector that provides file transfer capabilities over FTP protocol.
 * 
 * This connector handles file uploads, downloads, directory operations, and file monitoring,
 * routing file events to appropriate domain logic based on routing configuration.
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.ftp.enabled", havingValue = "true")
public class FTPConnector {

    private static final Logger logger = LoggerFactory.getLogger(FTPConnector.class);
    
    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    private FTPClient ftpClient;

    @Autowired
    public FTPConnector(ProcessRegistry processRegistry, 
                       FrameworkProperties frameworkProperties,
                       ObjectMapper objectMapper) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(5);
        logger.info("FTP Connector initialized");
    }

    @PostConstruct
    public void initialize() {
        try {
            FrameworkProperties.FTP ftpConfig = frameworkProperties.getConnectors().getFtp();
            
            if (ftpConfig == null) {
                logger.warn("FTP configuration not found, using defaults");
                return;
            }
            
            // Create local directory if needed
            if (ftpConfig.isCreateLocalDir()) {
                createLocalDirectoryIfNeeded(ftpConfig.getLocalDirectory());
            }
            
            // Establish initial connection
            establishConnection();
            
            logger.info("FTP Connector initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize FTP Connector: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            logger.info("FTP Connector cleaned up");
        } catch (Exception e) {
            logger.error("Error during FTP Connector cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Upload file to remote server
     */
    public CompletableFuture<JsonNode> uploadFile(String localPath, String remotePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Uploading file via FTP: {} -> {}", localPath, remotePath);
                
                if (!ensureConnection()) {
                    throw new FTPConnectorException("Failed to establish FTP connection");
                }
                
                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    throw new FTPConnectorException("Local file not found: " + localPath);
                }
                
                try (FileInputStream inputStream = new FileInputStream(localFile)) {
                    boolean success = ftpClient.storeFile(remotePath, inputStream);
                    
                    if (!success) {
                        throw new FTPConnectorException("Failed to upload file: " + ftpClient.getReplyString());
                    }
                    
                    JsonNode result = createFileOperationResult("upload", localPath, remotePath, localFile.length());
                    
                    // Route to domain logic
                    routeToDomainLogic(result, "upload");
                    
                    logger.debug("File uploaded successfully: {}", remotePath);
                    return result;
                }
                
            } catch (Exception e) {
                logger.error("Error uploading file {}: {}", localPath, e.getMessage(), e);
                throw new FTPConnectorException("File upload failed", e);
            }
        }, executorService);
    }

    /**
     * Download file from remote server
     */
    public CompletableFuture<JsonNode> downloadFile(String remotePath, String localPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Downloading file via FTP: {} -> {}", remotePath, localPath);
                
                if (!ensureConnection()) {
                    throw new FTPConnectorException("Failed to establish FTP connection");
                }
                
                // Ensure local directory exists
                ensureLocalDirectory(localPath);
                
                try (FileOutputStream outputStream = new FileOutputStream(localPath)) {
                    boolean success = ftpClient.retrieveFile(remotePath, outputStream);
                    
                    if (!success) {
                        throw new FTPConnectorException("Failed to download file: " + ftpClient.getReplyString());
                    }
                    
                    File downloadedFile = new File(localPath);
                    JsonNode result = createFileOperationResult("download", localPath, remotePath, 
                        downloadedFile.exists() ? downloadedFile.length() : 0);
                    
                    // Route to domain logic
                    routeToDomainLogic(result, "download");
                    
                    logger.debug("File downloaded successfully: {}", localPath);
                    return result;
                }
                
            } catch (Exception e) {
                logger.error("Error downloading file {}: {}", remotePath, e.getMessage(), e);
                throw new FTPConnectorException("File download failed", e);
            }
        }, executorService);
    }

    /**
     * List files in remote directory
     */
    public CompletableFuture<JsonNode> listFiles(String remotePath, String filePattern) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Listing files in remote directory: {} with pattern: {}", remotePath, filePattern);
                
                if (!ensureConnection()) {
                    throw new FTPConnectorException("Failed to establish FTP connection");
                }
                
                FTPFile[] files = ftpClient.listFiles(remotePath);
                List<Map<String, Object>> fileList = new ArrayList<>();
                
                for (FTPFile file : files) {
                    String filename = file.getName();
                    
                    // Skip . and .. entries
                    if (".".equals(filename) || "..".equals(filename)) {
                        continue;
                    }
                    
                    // Apply file pattern filter if provided
                    if (filePattern != null && !filename.matches(filePattern)) {
                        continue;
                    }
                    
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", filename);
                    fileInfo.put("size", file.getSize());
                    fileInfo.put("modified", file.getTimestamp().getTimeInMillis());
                    fileInfo.put("isDirectory", file.isDirectory());
                    fileInfo.put("isFile", file.isFile());
                    
                    fileList.add(fileInfo);
                }
                
                JsonNode result = objectMapper.createObjectNode()
                    .put("operation", "list")
                    .put("remotePath", remotePath)
                    .put("pattern", filePattern)
                    .put("fileCount", fileList.size())
                    .put("timestamp", System.currentTimeMillis())
                    .set("files", objectMapper.valueToTree(fileList));
                
                // Route to domain logic
                routeToDomainLogic(result, "list");
                
                logger.debug("Listed {} files in remote directory: {}", fileList.size(), remotePath);
                return result;
                
            } catch (Exception e) {
                logger.error("Error listing files in {}: {}", remotePath, e.getMessage(), e);
                throw new FTPConnectorException("File listing failed", e);
            }
        }, executorService);
    }

    /**
     * Delete remote file
     */
    public CompletableFuture<JsonNode> deleteFile(String remotePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Deleting remote file: {}", remotePath);
                
                if (!ensureConnection()) {
                    throw new FTPConnectorException("Failed to establish FTP connection");
                }
                
                boolean success = ftpClient.deleteFile(remotePath);
                
                if (!success) {
                    throw new FTPConnectorException("Failed to delete file: " + ftpClient.getReplyString());
                }
                
                JsonNode result = objectMapper.createObjectNode()
                    .put("operation", "delete")
                    .put("remotePath", remotePath)
                    .put("status", "success")
                    .put("timestamp", System.currentTimeMillis());
                
                // Route to domain logic
                routeToDomainLogic(result, "delete");
                
                logger.debug("File deleted successfully: {}", remotePath);
                return result;
                
            } catch (Exception e) {
                logger.error("Error deleting file {}: {}", remotePath, e.getMessage(), e);
                throw new FTPConnectorException("File deletion failed", e);
            }
        }, executorService);
    }

    /**
     * Monitor directory for file changes
     */
    public void startDirectoryMonitoring(String remotePath, String filePattern) {
        executorService.submit(() -> {
            logger.info("Starting FTP directory monitoring for: {} with pattern: {}", remotePath, filePattern);
            
            Set<String> previousFiles = new HashSet<>();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    JsonNode fileList = listFiles(remotePath, filePattern).get();
                    Set<String> currentFiles = new HashSet<>();
                    
                    if (fileList.has("files")) {
                        fileList.get("files").forEach(file -> {
                            currentFiles.add(file.get("name").asText());
                        });
                    }
                    
                    // Check for new files
                    for (String filename : currentFiles) {
                        if (!previousFiles.contains(filename)) {
                            logger.debug("New file detected: {}", filename);
                            JsonNode newFileEvent = createFileEvent("new", remotePath + "/" + filename);
                            routeToDomainLogic(newFileEvent, "monitor");
                        }
                    }
                    
                    // Check for deleted files
                    for (String filename : previousFiles) {
                        if (!currentFiles.contains(filename)) {
                            logger.debug("File removed: {}", filename);
                            JsonNode removedFileEvent = createFileEvent("removed", remotePath + "/" + filename);
                            routeToDomainLogic(removedFileEvent, "monitor");
                        }
                    }
                    
                    previousFiles = currentFiles;
                    
                    Thread.sleep(10000); // Check every 10 seconds
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error during FTP directory monitoring: {}", e.getMessage(), e);
                    try {
                        Thread.sleep(30000); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            logger.info("FTP directory monitoring stopped for: {}", remotePath);
        });
    }

    private boolean ensureConnection() {
        try {
            if (ftpClient == null || !ftpClient.isConnected()) {
                establishConnection();
            }
            return ftpClient != null && ftpClient.isConnected();
        } catch (Exception e) {
            logger.error("Failed to ensure FTP connection: {}", e.getMessage(), e);
            return false;
        }
    }

    private void establishConnection() throws IOException {
        FrameworkProperties.FTP ftpConfig = frameworkProperties.getConnectors().getFtp();
        if (ftpConfig == null) {
            throw new IOException("FTP configuration not available");
        }
        
        ftpClient = new FTPClient();
        
        // Configure timeouts
        ftpClient.setConnectTimeout(ftpConfig.getConnectionTimeout());
        ftpClient.setDataTimeout(ftpConfig.getDataTimeout());
        
        // Connect to server
        ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
        
        // Login
        if (!ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword())) {
            throw new IOException("FTP login failed: " + ftpClient.getReplyString());
        }
        
        // Set transfer mode
        if ("BINARY".equalsIgnoreCase(ftpConfig.getTransferMode())) {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        } else {
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
        }
        
        // Set passive/active mode
        if (ftpConfig.isPassiveMode()) {
            ftpClient.enterLocalPassiveMode();
        } else {
            ftpClient.enterLocalActiveMode();
        }
        
        // Change to remote directory if specified
        if (ftpConfig.getRemoteDirectory() != null && !"/".equals(ftpConfig.getRemoteDirectory())) {
            if (!ftpClient.changeWorkingDirectory(ftpConfig.getRemoteDirectory())) {
                logger.warn("Failed to change to remote directory {}: {}", 
                           ftpConfig.getRemoteDirectory(), ftpClient.getReplyString());
            }
        }
        
        logger.info("FTP connection established to {}@{}:{}", 
                   ftpConfig.getUsername(), ftpConfig.getHost(), ftpConfig.getPort());
    }

    private JsonNode createFileOperationResult(String operation, String localPath, String remotePath, long fileSize) {
        return objectMapper.createObjectNode()
            .put("operation", operation)
            .put("localPath", localPath)
            .put("remotePath", remotePath)
            .put("fileSize", fileSize)
            .put("status", "success")
            .put("timestamp", System.currentTimeMillis());
    }

    private JsonNode createFileEvent(String eventType, String filePath) {
        return objectMapper.createObjectNode()
            .put("eventType", eventType)
            .put("filePath", filePath)
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

    private void createLocalDirectoryIfNeeded(String localDirectory) {
        try {
            Path path = Paths.get(localDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.debug("Created local directory: {}", localDirectory);
            }
        } catch (Exception e) {
            logger.warn("Failed to create local directory {}: {}", localDirectory, e.getMessage());
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
            logger.error("Error routing FTP result to domain logic: {}", e.getMessage(), e);
        }
    }

    private Optional<FrameworkProperties.Routing> findMatchedRouting(String operationType) {
        if (frameworkProperties.getRouting() == null) {
            return Optional.empty();
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "ftp".equalsIgnoreCase(trigger.getType()) &&
                                          (operationType == null || operationType.equals(trigger.getMethod()))))
                .findFirst();
    }

    /**
     * Custom exception for FTP operations
     */
    public static class FTPConnectorException extends RuntimeException {
        public FTPConnectorException(String message) {
            super(message);
        }
        
        public FTPConnectorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
