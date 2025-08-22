package blog.eric231.framework.infrastructure.connector;

import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.configuration.ProcessRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SFTP Connector that provides secure file transfer capabilities over SSH.
 * 
 * This connector handles file uploads, downloads, directory operations, and file monitoring,
 * routing file events to appropriate domain logic based on routing configuration.
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.sftp.enabled", havingValue = "true")
public class SFTPConnector {

    private static final Logger logger = LoggerFactory.getLogger(SFTPConnector.class);
    
    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    private JSch jsch;
    private Session session;
    private ChannelSftp sftpChannel;

    @Autowired
    public SFTPConnector(ProcessRegistry processRegistry, 
                        FrameworkProperties frameworkProperties,
                        ObjectMapper objectMapper) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(5);
        logger.info("SFTP Connector initialized");
    }

    @PostConstruct
    public void initialize() {
        try {
            jsch = new JSch();
            FrameworkProperties.SFTP sftpConfig = frameworkProperties.getConnectors().getSftp();
            
            if (sftpConfig == null) {
                logger.warn("SFTP configuration not found, using defaults");
                return;
            }
            
            // Configure known hosts
            if (sftpConfig.getKnownHostsPath() != null) {
                jsch.setKnownHosts(sftpConfig.getKnownHostsPath());
            }
            
            // Configure private key if provided
            if (sftpConfig.getPrivateKeyPath() != null) {
                jsch.addIdentity(sftpConfig.getPrivateKeyPath());
            }
            
            // Create local directory if needed
            if (sftpConfig.isCreateLocalDir()) {
                createLocalDirectoryIfNeeded(sftpConfig.getLocalDirectory());
            }
            
            // Establish initial connection
            establishConnection();
            
            logger.info("SFTP Connector initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize SFTP Connector: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            logger.info("SFTP Connector cleaned up");
        } catch (Exception e) {
            logger.error("Error during SFTP Connector cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Upload file to remote server
     */
    public CompletableFuture<JsonNode> uploadFile(String localPath, String remotePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Uploading file: {} -> {}", localPath, remotePath);
                
                if (!ensureConnection()) {
                    throw new SFTPConnectorException("Failed to establish SFTP connection");
                }
                
                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    throw new SFTPConnectorException("Local file not found: " + localPath);
                }
                
                // Ensure remote directory exists
                ensureRemoteDirectory(remotePath);
                
                sftpChannel.put(localPath, remotePath);
                
                JsonNode result = createFileOperationResult("upload", localPath, remotePath, localFile.length());
                
                // Route to domain logic
                routeToDomainLogic(result, "upload");
                
                logger.debug("File uploaded successfully: {}", remotePath);
                return result;
                
            } catch (Exception e) {
                logger.error("Error uploading file {}: {}", localPath, e.getMessage(), e);
                throw new SFTPConnectorException("File upload failed", e);
            }
        }, executorService);
    }

    /**
     * Download file from remote server
     */
    public CompletableFuture<JsonNode> downloadFile(String remotePath, String localPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Downloading file: {} -> {}", remotePath, localPath);
                
                if (!ensureConnection()) {
                    throw new SFTPConnectorException("Failed to establish SFTP connection");
                }
                
                // Ensure local directory exists
                ensureLocalDirectory(localPath);
                
                sftpChannel.get(remotePath, localPath);
                
                File downloadedFile = new File(localPath);
                JsonNode result = createFileOperationResult("download", localPath, remotePath, 
                    downloadedFile.exists() ? downloadedFile.length() : 0);
                
                // Route to domain logic
                routeToDomainLogic(result, "download");
                
                logger.debug("File downloaded successfully: {}", localPath);
                return result;
                
            } catch (Exception e) {
                logger.error("Error downloading file {}: {}", remotePath, e.getMessage(), e);
                throw new SFTPConnectorException("File download failed", e);
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
                    throw new SFTPConnectorException("Failed to establish SFTP connection");
                }
                
                @SuppressWarnings("unchecked")
                Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(remotePath);
                
                List<Map<String, Object>> files = new ArrayList<>();
                
                for (ChannelSftp.LsEntry entry : entries) {
                    String filename = entry.getFilename();
                    
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
                    fileInfo.put("size", entry.getAttrs().getSize());
                    fileInfo.put("modified", entry.getAttrs().getMTime() * 1000L);
                    fileInfo.put("isDirectory", entry.getAttrs().isDir());
                    fileInfo.put("permissions", entry.getAttrs().getPermissionsString());
                    
                    files.add(fileInfo);
                }
                
                JsonNode result = objectMapper.createObjectNode()
                    .put("operation", "list")
                    .put("remotePath", remotePath)
                    .put("pattern", filePattern)
                    .put("fileCount", files.size())
                    .put("timestamp", System.currentTimeMillis())
                    .set("files", objectMapper.valueToTree(files));
                
                // Route to domain logic
                routeToDomainLogic(result, "list");
                
                logger.debug("Listed {} files in remote directory: {}", files.size(), remotePath);
                return result;
                
            } catch (Exception e) {
                logger.error("Error listing files in {}: {}", remotePath, e.getMessage(), e);
                throw new SFTPConnectorException("File listing failed", e);
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
                    throw new SFTPConnectorException("Failed to establish SFTP connection");
                }
                
                sftpChannel.rm(remotePath);
                
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
                throw new SFTPConnectorException("File deletion failed", e);
            }
        }, executorService);
    }

    /**
     * Monitor directory for file changes
     */
    public void startDirectoryMonitoring(String remotePath, String filePattern) {
        executorService.submit(() -> {
            logger.info("Starting directory monitoring for: {} with pattern: {}", remotePath, filePattern);
            
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
                    logger.error("Error during directory monitoring: {}", e.getMessage(), e);
                    try {
                        Thread.sleep(30000); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            logger.info("Directory monitoring stopped for: {}", remotePath);
        });
    }

    private boolean ensureConnection() {
        try {
            if (session == null || !session.isConnected() || sftpChannel == null || !sftpChannel.isConnected()) {
                establishConnection();
            }
            return session != null && session.isConnected() && sftpChannel != null && sftpChannel.isConnected();
        } catch (Exception e) {
            logger.error("Failed to ensure SFTP connection: {}", e.getMessage(), e);
            return false;
        }
    }

    private void establishConnection() throws JSchException {
        FrameworkProperties.SFTP sftpConfig = frameworkProperties.getConnectors().getSftp();
        if (sftpConfig == null) {
            throw new JSchException("SFTP configuration not available");
        }
        
        session = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
        
        if (sftpConfig.getPassword() != null) {
            session.setPassword(sftpConfig.getPassword());
        }
        
        // Configure host key checking
        if (!sftpConfig.isStrictHostKeyChecking()) {
            session.setConfig("StrictHostKeyChecking", "no");
        }
        
        session.setTimeout(sftpConfig.getConnectionTimeout());
        session.connect();
        
        sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        
        // Change to remote directory if specified
        if (sftpConfig.getRemoteDirectory() != null && !"/".equals(sftpConfig.getRemoteDirectory())) {
            try {
                sftpChannel.cd(sftpConfig.getRemoteDirectory());
            } catch (SftpException e) {
                logger.warn("Failed to change to remote directory {}: {}", 
                           sftpConfig.getRemoteDirectory(), e.getMessage());
            }
        }
        
        logger.info("SFTP connection established to {}@{}:{}", 
                   sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
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

    private void ensureRemoteDirectory(String remotePath) {
        try {
            String directory = remotePath.substring(0, remotePath.lastIndexOf('/'));
            if (directory.length() > 0) {
                String[] dirs = directory.split("/");
                String path = "";
                for (String dir : dirs) {
                    if (dir.length() > 0) {
                        path += "/" + dir;
                        try {
                            sftpChannel.mkdir(path);
                        } catch (SftpException e) {
                            // Directory might already exist
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not ensure remote directory: {}", e.getMessage());
        }
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
            logger.error("Error routing SFTP result to domain logic: {}", e.getMessage(), e);
        }
    }

    private Optional<FrameworkProperties.Routing> findMatchedRouting(String operationType) {
        if (frameworkProperties.getRouting() == null) {
            return Optional.empty();
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "sftp".equalsIgnoreCase(trigger.getType()) &&
                                          (operationType == null || operationType.equals(trigger.getMethod()))))
                .findFirst();
    }

    /**
     * Custom exception for SFTP operations
     */
    public static class SFTPConnectorException extends RuntimeException {
        public SFTPConnectorException(String message) {
            super(message);
        }
        
        public SFTPConnectorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
