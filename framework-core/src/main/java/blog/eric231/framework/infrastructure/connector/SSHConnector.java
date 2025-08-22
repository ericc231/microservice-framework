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
import java.io.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSH Connector that enables remote command execution over SSH protocol.
 * 
 * This connector establishes SSH connections and executes commands on remote servers,
 * routing results to appropriate domain logic based on routing configuration.
 */
@Component
@ConditionalOnProperty(name = "framework.connectors.ssh.enabled", havingValue = "true")
public class SSHConnector {

    private static final Logger logger = LoggerFactory.getLogger(SSHConnector.class);
    
    private final ProcessRegistry processRegistry;
    private final FrameworkProperties frameworkProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    private JSch jsch;
    private Session session;

    @Autowired
    public SSHConnector(ProcessRegistry processRegistry, 
                       FrameworkProperties frameworkProperties,
                       ObjectMapper objectMapper) {
        this.processRegistry = processRegistry;
        this.frameworkProperties = frameworkProperties;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(10);
        logger.info("SSH Connector initialized");
    }

    @PostConstruct
    public void initialize() {
        try {
            jsch = new JSch();
            FrameworkProperties.SSH sshConfig = frameworkProperties.getConnectors().getSsh();
            
            if (sshConfig == null) {
                logger.warn("SSH configuration not found, using defaults");
                return;
            }
            
            // Configure known hosts
            if (sshConfig.getKnownHostsPath() != null) {
                jsch.setKnownHosts(sshConfig.getKnownHostsPath());
            }
            
            // Configure private key if provided
            if (sshConfig.getPrivateKeyPath() != null) {
                jsch.addIdentity(sshConfig.getPrivateKeyPath());
            }
            
            // Establish initial connection
            establishConnection();
            
            logger.info("SSH Connector initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize SSH Connector: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            logger.info("SSH Connector cleaned up");
        } catch (Exception e) {
            logger.error("Error during SSH Connector cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute SSH command asynchronously
     */
    public CompletableFuture<JsonNode> executeCommand(String command, String processName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Executing SSH command: {} for process: {}", command, processName);
                
                if (!ensureConnection()) {
                    throw new SSHConnectorException("Failed to establish SSH connection");
                }
                
                ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.setCommand(command);
                
                // Capture output
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                channelExec.setOutputStream(outputStream);
                channelExec.setErrStream(errorStream);
                
                channelExec.connect();
                
                // Wait for command completion
                while (!channelExec.isClosed()) {
                    Thread.sleep(100);
                }
                
                int exitCode = channelExec.getExitStatus();
                String output = outputStream.toString();
                String error = errorStream.toString();
                
                channelExec.disconnect();
                
                // Create result JSON
                JsonNode result = createCommandResult(command, exitCode, output, error);
                
                // Route to domain logic if configured
                routeToDomainLogic(result, processName);
                
                logger.debug("SSH command executed successfully: {}", command);
                return result;
                
            } catch (Exception e) {
                logger.error("Error executing SSH command {}: {}", command, e.getMessage(), e);
                throw new SSHConnectorException("SSH command execution failed", e);
            }
        }, executorService);
    }

    /**
     * Execute command and route to domain logic based on routing configuration
     */
    public CompletableFuture<JsonNode> executeAndRoute(JsonNode commandRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = commandRequest.get("command").asText();
                String processName = commandRequest.has("processName") ? 
                    commandRequest.get("processName").asText() : null;
                
                // Find matching routing configuration
                Optional<FrameworkProperties.Routing> matchedRouting = findMatchedRouting();
                
                if (matchedRouting.isPresent()) {
                    processName = matchedRouting.get().getProcessName();
                }
                
                return executeCommand(command, processName).get();
                
            } catch (Exception e) {
                logger.error("Error in SSH executeAndRoute: {}", e.getMessage(), e);
                throw new SSHConnectorException("SSH routing execution failed", e);
            }
        }, executorService);
    }

    /**
     * Transfer file via SCP
     */
    public CompletableFuture<JsonNode> transferFile(String localPath, String remotePath, boolean upload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Transferring file via SCP: {} -> {}, upload: {}", localPath, remotePath, upload);
                
                if (!ensureConnection()) {
                    throw new SSHConnectorException("Failed to establish SSH connection");
                }
                
                ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                
                try {
                    if (upload) {
                        channelSftp.put(localPath, remotePath);
                    } else {
                        channelSftp.get(remotePath, localPath);
                    }
                    
                    JsonNode result = objectMapper.createObjectNode()
                        .put("operation", upload ? "upload" : "download")
                        .put("localPath", localPath)
                        .put("remotePath", remotePath)
                        .put("status", "success");
                    
                    logger.debug("File transfer completed successfully");
                    return result;
                    
                } finally {
                    channelSftp.disconnect();
                }
                
            } catch (Exception e) {
                logger.error("Error transferring file: {}", e.getMessage(), e);
                throw new SSHConnectorException("File transfer failed", e);
            }
        }, executorService);
    }

    private boolean ensureConnection() {
        try {
            if (session == null || !session.isConnected()) {
                establishConnection();
            }
            return session != null && session.isConnected();
        } catch (Exception e) {
            logger.error("Failed to ensure SSH connection: {}", e.getMessage(), e);
            return false;
        }
    }

    private void establishConnection() throws JSchException {
        FrameworkProperties.SSH sshConfig = frameworkProperties.getConnectors().getSsh();
        if (sshConfig == null) {
            throw new JSchException("SSH configuration not available");
        }
        
        session = jsch.getSession(sshConfig.getUsername(), sshConfig.getHost(), sshConfig.getPort());
        
        if (sshConfig.getPassword() != null) {
            session.setPassword(sshConfig.getPassword());
        }
        
        // Configure host key checking
        if (!sshConfig.isStrictHostKeyChecking()) {
            session.setConfig("StrictHostKeyChecking", "no");
        }
        
        session.setTimeout(sshConfig.getConnectionTimeout());
        session.connect();
        
        logger.info("SSH connection established to {}@{}:{}", 
                   sshConfig.getUsername(), sshConfig.getHost(), sshConfig.getPort());
    }

    private JsonNode createCommandResult(String command, int exitCode, String output, String error) {
        try {
            return objectMapper.createObjectNode()
                .put("command", command)
                .put("exitCode", exitCode)
                .put("output", output)
                .put("error", error)
                .put("success", exitCode == 0)
                .put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Error creating command result: {}", e.getMessage(), e);
            return objectMapper.createObjectNode().put("error", "Failed to create result");
        }
    }

    private void routeToDomainLogic(JsonNode result, String processName) {
        if (processName == null) return;
        
        try {
            DomainLogic domainLogic = processRegistry.getDomainLogic(processName);
            if (domainLogic != null) {
                domainLogic.handle(result);
            }
        } catch (Exception e) {
            logger.error("Error routing SSH result to domain logic: {}", e.getMessage(), e);
        }
    }

    private Optional<FrameworkProperties.Routing> findMatchedRouting() {
        if (frameworkProperties.getRouting() == null) {
            return Optional.empty();
        }
        
        return frameworkProperties.getRouting().stream()
                .filter(routing -> routing.getTriggers() != null)
                .filter(routing -> routing.getTriggers().stream()
                        .anyMatch(trigger -> "ssh".equalsIgnoreCase(trigger.getType())))
                .findFirst();
    }

    /**
     * Custom exception for SSH operations
     */
    public static class SSHConnectorException extends RuntimeException {
        public SSHConnectorException(String message) {
            super(message);
        }
        
        public SSHConnectorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
