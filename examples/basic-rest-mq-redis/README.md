# Basic REST MQ Redis Example

This example demonstrates a complete microservice processing pipeline using the framework's @DL (Domain Logic) pattern. The application implements a complex request-response flow that processes REST requests through RabbitMQ message queues and stores data in Redis.

## Architecture Overview

The application implements a multi-stage processing pipeline:

```
REST API (Basic Auth) → @DL1 (RequestProcessor) → RabbitMQ → @DL2 (MessageProcessor) → Redis
                                                  ↑                                    ↓
                                                  ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
```

### Components

1. **REST Layer**: Spring Boot REST controllers with Basic Authentication
2. **RequestProcessorLogic (@DL1)**: Handles REST requests, sends to RabbitMQ, waits for replies
3. **RabbitMQ**: Message queue middleware for asynchronous communication
4. **MessageProcessorLogic (@DL2)**: Processes messages from RabbitMQ, interacts with Redis
5. **Redis**: Data storage with TTL support and indexing

### Message Flow

1. Client sends HTTP request with Basic Auth
2. `RequestProcessorLogic` receives request, enriches with metadata
3. Message sent to RabbitMQ exchange with routing key
4. `MessageProcessorLogic` consumes message from queue
5. Business logic executed, data stored/retrieved from Redis
6. Response sent back through RabbitMQ reply queue
7. `RequestProcessorLogic` receives reply and responds to client

## Features

- **Multiple Operations**: Store, retrieve, update, delete, and default processing
- **Authentication**: Basic Auth with multiple user roles (admin, user, service)
- **Async over Sync**: Synchronous REST API over asynchronous message processing
- **Data Persistence**: Redis storage with automatic TTL and indexing
- **Error Handling**: Comprehensive error handling and timeout management
- **Monitoring**: Health endpoints and metrics
- **Testing**: Complete unit and integration tests with TestContainers

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (for running RabbitMQ, Redis, and integration tests)

## Setup and Running

### 1. Start Infrastructure Services

Start RabbitMQ and Redis using Docker:

```bash
# Start RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management

# Start Redis
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

### 2. Build the Application

```bash
# From the project root directory
mvn clean compile

# Or build the specific module
cd examples/basic-rest-mq-redis
mvn clean compile
```

### 3. Run the Application

```bash
# From the examples/basic-rest-mq-redis directory
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on port 8082 by default.

## API Documentation

### Authentication

All API endpoints require Basic Authentication. Default users:

- **admin/admin123** - Full access (ADMIN, USER roles)
- **user/user123** - Standard access (USER role)
- **service/service123** - Service access (SERVICE role)

### Endpoints

#### General Processing
```http
POST /api/v1/process/general
Authorization: Basic <credentials>
Content-Type: application/json

{
  "clientId": "my-client",
  "action": "process",
  "data": {
    "key": "value"
  }
}
```

#### Store Operation
```http
POST /api/v1/process/store
Authorization: Basic <credentials>
Content-Type: application/json

{
  "clientId": "my-client",
  "data": {
    "productId": "PROD-001",
    "productName": "Example Product",
    "price": 99.99
  }
}
```

#### Retrieve Operation
```http
GET /api/v1/process/retrieve?storageId=<storage-id>
# OR
GET /api/v1/process/retrieve?requestId=<request-id>
Authorization: Basic <credentials>
```

#### Update Operation
```http
PUT /api/v1/process/update
Authorization: Basic <credentials>
Content-Type: application/json

{
  "storageId": "<storage-id>",
  "data": {
    "productName": "Updated Product Name",
    "price": 149.99
  }
}
```

#### Delete Operation
```http
DELETE /api/v1/process/delete?storageId=<storage-id>
# OR
DELETE /api/v1/process/delete?requestId=<request-id>
Authorization: Basic <credentials>
```

#### Health Check
```http
GET /api/v1/process/health
Authorization: Basic <credentials>
```

### Response Format

Successful responses:
```json
{
  "status": "success",
  "requestId": "uuid-generated-request-id",
  "processedAt": "2024-01-01T12:00:00",
  "processingStatus": "success",
  "storageId": "uuid-generated-storage-id",
  "redisKey": "data:storage-id",
  "totalRoundTripTime": 150,
  "mqResponse": { ... }
}
```

Error responses:
```json
{
  "status": "error",
  "errorCode": "PROCESSING_ERROR",
  "errorMessage": "Detailed error description",
  "timestamp": "2024-01-01T12:00:00"
}
```

## Configuration

### Application Properties

Key configuration properties in `application.yml`:

```yaml
# Server configuration
server:
  port: 8082

# RabbitMQ configuration
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Redis configuration
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

# Framework configuration
framework:
  rabbitmq:
    enabled: true
    exchanges:
      - name: "processing-exchange"
        type: "topic"
    queues:
      - name: "processing-queue"
        bindings:
          - exchange: "processing-exchange"
            routingKey: "process.request"
```

### Environment-Specific Configuration

- **Development**: Use `application-dev.yml` or `dev` profile
- **Production**: Use `application-prod.yml` or `prod` profile
- **Testing**: Use `application-test.yml` or `test` profile

## Testing

### Unit Tests

Run unit tests for individual components:

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=RequestProcessorLogicTest
mvn test -Dtest=MessageProcessorLogicTest
mvn test -Dtest=ProcessingControllerTest
```

### Integration Tests

Integration tests use TestContainers to spin up real RabbitMQ and Redis instances:

```bash
# Run integration tests (requires Docker)
mvn test -Dtest=BasicRestMqRedisIntegrationTest

# Run all tests including integration
mvn verify
```

### Manual Testing

Use curl to test the API:

```bash
# Store data
curl -X POST http://localhost:8082/api/v1/process/store \
  -H "Content-Type: application/json" \
  -u "admin:admin123" \
  -d '{
    "clientId": "test-client",
    "data": {
      "productId": "PROD-001",
      "productName": "Test Product",
      "price": 99.99
    }
  }'

# Health check
curl http://localhost:8082/api/v1/process/health \
  -u "user:user123"
```

## Monitoring and Management

### Actuator Endpoints

- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

### RabbitMQ Management

Access RabbitMQ management interface at http://localhost:15672
- Username: guest
- Password: guest

### Redis Monitoring

Connect to Redis CLI:
```bash
docker exec -it redis redis-cli

# Check stored keys
KEYS *

# Get specific data
GET data:storage-id

# Check TTL
TTL data:storage-id
```

## Troubleshooting

### Common Issues

1. **Connection Refused Errors**
   - Ensure RabbitMQ and Redis are running
   - Check port availability (5672, 6379)
   - Verify Docker containers are healthy

2. **Authentication Failures**
   - Verify Basic Auth credentials
   - Check user roles and permissions
   - Ensure correct Authorization header format

3. **Message Processing Timeouts**
   - Check RabbitMQ connection stability
   - Verify queue configuration
   - Check Redis connectivity

4. **Integration Test Failures**
   - Ensure Docker is running and accessible
   - Check available disk space for containers
   - Verify TestContainers can pull images

### Debug Configuration

Enable debug logging:

```yaml
logging:
  level:
    blog.eric231.examples.basicrestmqredis: DEBUG
    blog.eric231.framework: DEBUG
    org.springframework.amqp: DEBUG
    org.springframework.data.redis: DEBUG
```

### Performance Tuning

For production environments:

1. **RabbitMQ**: Adjust connection pool, prefetch, and concurrency
2. **Redis**: Configure connection pool and timeout values
3. **Application**: Tune thread pools and timeout settings

## Framework Integration

This example demonstrates advanced framework features:

- **@DL Annotations**: Domain Logic component registration
- **RabbitMQConnector**: Framework's message queue abstraction
- **Trigger Configuration**: Declarative routing configuration
- **Error Handling**: Framework-level error management
- **Testing Support**: Framework testing utilities

## Development

### Adding New Operations

1. Add operation handling in `MessageProcessorLogic.handle()`
2. Create corresponding REST endpoint in `ProcessingController`
3. Add unit tests for the new functionality
4. Update integration tests if needed

### Extending the Pipeline

1. Add new @DL components for additional processing stages
2. Configure additional RabbitMQ exchanges/queues
3. Update routing configuration in `application.yml`
4. Implement appropriate error handling

## License

This example is part of the microservice framework project and follows the same license terms.
