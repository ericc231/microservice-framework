# Multi-Connector Pipeline Example

This comprehensive example demonstrates the integration of multiple connectors in a sophisticated file processing pipeline using the microservice framework. The pipeline combines **SSH, SFTP, FTP, S3, IBM MQ, Redis, and RabbitMQ** connectors to create a real-world enterprise integration scenario.

**🆕 Latest Update**: Added comprehensive IBM MQ integration with full enterprise messaging capabilities including transactional messaging, SSL/TLS support, queue monitoring, and seamless pipeline integration.

## Overview

The multi-connector pipeline showcases:

- **File Transfer Operations**: Download from FTP/SFTP, upload to S3
- **Remote Processing**: Execute commands via SSH
- **Message Queuing**: IBM MQ for enterprise messaging
- **Caching**: Redis for intermediate data storage
- **Event Routing**: RabbitMQ for notifications and events
- **File Monitoring**: Automatic pipeline triggering based on file events
- **REST API**: Complete API interface for pipeline operations

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   FTP/SFTP      │    │      SSH        │    │       S3        │
│   Download      │───▶│   Processing    │───▶│     Upload      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Redis       │◄──▶│    IBM MQ       │◄──▶│   RabbitMQ      │
│   Caching &     │    │  Enterprise     │    │   Event &       │
│  Job Tracking   │    │   Messaging     │    │ Notifications   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌─────────────────────┐
                    │   REST API Layer   │
                    │  Pipeline Control  │
                    └─────────────────────┘
```

## Features

### Core Pipeline Operations
- **Download**: Retrieve files from FTP/SFTP servers
- **Process**: Execute remote commands via SSH
- **Upload**: Store processed files in S3
- **Status Tracking**: Monitor job progress with Redis
- **Notifications**: Send completion alerts via RabbitMQ and IBM MQ

### IBM MQ Integration 🚀
- **Message Operations**: Send/receive messages with priority and expiry
- **Queue Management**: Browse, monitor, and manage queue depth
- **Enterprise Features**: SSL/TLS, transactional messaging, correlation IDs
- **Pipeline Integration**: Trigger and coordinate pipeline execution
- **Batch Processing**: Efficient bulk message processing
- **Connection Pooling**: Optimized connection management
- **Error Handling**: Comprehensive error recovery and logging
- **Monitoring**: Queue depth alerts and performance metrics

### File Event Handling
- **Auto-Triggering**: Automatic pipeline execution based on file patterns
- **Event Tracking**: Comprehensive audit trail in Redis
- **Smart Routing**: Route events based on file types and patterns

### REST API
- Complete REST interface with endpoints for all operations
- Comprehensive documentation with sample requests
- Health check and monitoring endpoints

## Getting Started

### Prerequisites

1. **Java 17+**
2. **Maven 3.6+**
3. **Redis Server** (optional, for caching)
4. **RabbitMQ Server** (optional, for messaging)
5. **IBM MQ Server** (optional, for IBM MQ features)
6. **FTP/SFTP Server** (for testing file operations)
7. **AWS Account** (for S3 operations) or LocalStack for local testing

### Configuration

The application supports multiple profiles (dev, prod, test) with environment-specific configurations.

#### Environment Variables

```bash
# SSH Configuration
export SSH_USERNAME=your-ssh-user
export SSH_PASSWORD=your-ssh-password

# FTP Configuration
export FTP_USERNAME=your-ftp-user
export FTP_PASSWORD=your-ftp-password

# SFTP Configuration
export SFTP_USERNAME=your-sftp-user
export SFTP_PASSWORD=your-sftp-password

# AWS S3 Configuration
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export S3_BUCKET_NAME=your-bucket-name

# IBM MQ Configuration
export IBMMQ_QUEUE_MANAGER=QM1
export IBMMQ_HOST=localhost
export IBMMQ_PORT=1414
export IBMMQ_CHANNEL=DEV.APP.SVRCONN
export IBMMQ_USER=your-mq-user
export IBMMQ_PASSWORD=your-mq-password

# IBM MQ SSL Configuration (Optional)
export IBMMQ_KEYSTORE=/path/to/keystore.jks
export IBMMQ_KEYSTORE_PASSWORD=keystore-password
export IBMMQ_TRUSTSTORE=/path/to/truststore.jks
export IBMMQ_TRUSTSTORE_PASSWORD=truststore-password
export IBMMQ_CIPHER_SUITE=TLS_RSA_WITH_AES_256_CBC_SHA256
```

### Running the Application

1. **Clone the repository and navigate to the example directory**:
   ```bash
   cd microservice-framework/examples/multi-connector-pipeline
   ```

2. **Build the application**:
   ```bash
   mvn clean compile
   ```

3. **Run with development profile**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. **Access the application**:
   - Base URL: `http://localhost:8080/multi-connector-pipeline`
   - Health Check: `http://localhost:8080/multi-connector-pipeline/api/pipeline/health`
   - API Info: `http://localhost:8080/multi-connector-pipeline/api/pipeline/info`

## API Usage

### Pipeline Operations

#### Execute Complete Pipeline
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/execute \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "process",
    "sourceType": "sftp",
    "remotePath": "/incoming/data.csv",
    "description": "Process CSV file"
  }'
```

#### Download File
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/download \
  -H "Content-Type: application/json" \
  -d '{
    "sourceType": "ftp",
    "remotePath": "/data/input.xml",
    "localPath": "./temp/input.xml"
  }'
```

#### Upload File to S3
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/upload \
  -H "Content-Type: application/json" \
  -d '{
    "localPath": "./temp/processed.json",
    "s3Key": "processed/2024/01/data.json"
  }'
```

### IBM MQ Operations

#### Send Message
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/mq/send \
  -H "Content-Type: application/json" \
  -d '{
    "queueName": "DEV.QUEUE.1",
    "message": "Hello from pipeline!",
    "priority": 5,
    "expiry": 3600000
  }'
```

#### Receive Message
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/mq/receive \
  -H "Content-Type: application/json" \
  -d '{
    "queueName": "DEV.QUEUE.1",
    "timeout": 5000
  }'
```

#### Browse Queue
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/mq/browse \
  -H "Content-Type: application/json" \
  -d '{
    "queueName": "DEV.QUEUE.1",
    "maxMessages": 10
  }'
```

#### Trigger Pipeline via MQ
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/mq/trigger-pipeline \
  -H "Content-Type: application/json" \
  -d '{
    "triggerType": "file_upload",
    "sourceType": "sftp",
    "remotePath": "/incoming/urgent.xml",
    "priority": "high"
  }'
```

### File Events

#### Handle File Event
```bash
curl -X POST http://localhost:8080/multi-connector-pipeline/api/pipeline/events/file \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "created",
    "filePath": "/watch/directory/new_file.csv",
    "timestamp": 1640995200000
  }'
```

### Monitoring

#### Check Job Status
```bash
curl http://localhost:8080/multi-connector-pipeline/api/pipeline/status/{jobId}
```

#### Health Check
```bash
curl http://localhost:8080/multi-connector-pipeline/api/pipeline/health
```

#### Get API Information
```bash
curl http://localhost:8080/multi-connector-pipeline/api/pipeline/info
```

## Domain Logic Components

### FilePipelineProcessor
The core orchestrator that manages the complete file processing pipeline:
- Download files from FTP/SFTP
- Process files via SSH
- Upload results to S3
- Track job status in Redis
- Send notifications via RabbitMQ

### IBMMQMessageHandler
Handles all IBM MQ operations:
- Send and receive messages
- Queue browsing and monitoring
- Pipeline integration
- Batch message processing
- Alert management

### FileEventHandler
Processes file system events:
- Auto-trigger pipelines based on file patterns
- Event audit trail
- File monitoring and status tracking
- Cache management

## Configuration Profiles

### Development (dev)
- Local services (localhost)
- Debug logging enabled
- LocalStack for S3 testing
- Embedded Redis and RabbitMQ for testing

### Production (prod)
- Production service endpoints
- Optimized logging
- SSL/TLS configurations
- Connection pooling

### Test (test)
- Connectors disabled for unit testing
- Embedded test services
- Debug logging for troubleshooting

## Connector Capabilities

| Connector | Operations | Features | Status |
|-----------|------------|----------|--------|
| **SSH** | Command execution | Remote processing, secure connections | ✅ Complete |
| **FTP** | Upload, download, list, delete | Passive mode, directory management | ✅ Complete |
| **SFTP** | Upload, download, list, delete | SSH key auth, secure transfer | ✅ Complete |
| **S3** | Upload, download, list, delete | Multi-part upload, metadata | ✅ Complete |
| **IBM MQ** | Send, receive, browse, monitor | Transactional messaging, SSL/TLS, pooling | 🆕 **NEW** |
| **Redis** | Caching, job tracking | TTL support, data structures | ✅ Complete |
| **RabbitMQ** | Event routing, notifications | Dead letter queues, routing | ✅ Complete |
| **TIBCO EMS** | Message queuing | Enterprise messaging | 🔄 Framework Ready |

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration-tests
```

### Testing with TestContainers
The project includes TestContainers configurations for:
- RabbitMQ
- Redis
- LocalStack (S3)
- FTP/SFTP servers

## Deployment

### Docker
```bash
# Build Docker image
docker build -t multi-connector-pipeline .

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e AWS_ACCESS_KEY_ID=your-key \
  -e AWS_SECRET_ACCESS_KEY=your-secret \
  multi-connector-pipeline
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: multi-connector-pipeline
spec:
  replicas: 3
  selector:
    matchLabels:
      app: multi-connector-pipeline
  template:
    metadata:
      labels:
        app: multi-connector-pipeline
    spec:
      containers:
      - name: pipeline
        image: multi-connector-pipeline:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        # Add other environment variables
```

## Troubleshooting

### Common Issues

1. **Connection Refused**:
   - Check service availability
   - Verify network connectivity
   - Review firewall settings

2. **Authentication Failures**:
   - Verify credentials
   - Check key files and permissions
   - Review SSL/TLS configurations

3. **Queue/Topic Not Found**:
   - Ensure queues are created
   - Check queue manager configuration
   - Verify queue permissions

4. **Memory Issues**:
   - Adjust JVM heap settings
   - Review connection pool sizes
   - Monitor resource usage

### Logging

Enable debug logging for specific components:
```yaml
logging:
  level:
    blog.eric231.examples.multiconnector: DEBUG
    blog.eric231.framework.infrastructure.connector: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions, issues, or contributions:
- Create an issue in the GitHub repository
- Check the documentation wiki
- Review the API examples and test cases
