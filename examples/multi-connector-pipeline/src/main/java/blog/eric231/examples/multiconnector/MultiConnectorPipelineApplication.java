package blog.eric231.examples.multiconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Multi-Connector Pipeline Application
 * 
 * This application demonstrates a comprehensive file processing pipeline that integrates
 * multiple connectors to showcase the framework's connector ecosystem:
 * 
 * Pipeline Flow:
 * 1. REST API receives file processing requests
 * 2. Files are downloaded from FTP/SFTP servers
 * 3. Files are processed via SSH commands on remote servers
 * 4. Results are stored in Redis for caching
 * 5. Processed files are uploaded to S3 for long-term storage
 * 6. Status updates are sent via RabbitMQ
 * 
 * Connectors demonstrated:
 * - RestConnector: API endpoints for pipeline control
 * - FTPConnector: Download input files from FTP servers
 * - SFTPConnector: Secure file transfers
 * - SSHConnector: Remote command execution for file processing
 * - RabbitMQConnector: Asynchronous status notifications
 * - S3Connector: Cloud storage for processed files
 * - RedisAdapter: Caching and temporary data storage
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "blog.eric231.examples.multiconnector",
    "blog.eric231.framework"
})
public class MultiConnectorPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiConnectorPipelineApplication.class, args);
    }
}
