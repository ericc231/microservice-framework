# Kafka-Redis Example

This example demonstrates how to use the microservice framework's Kafka connector with @DL (Domain Logic) pattern to consume Kafka messages and store processed data in Redis.

## Overview

The application listens to Kafka topics, processes incoming messages using domain logic annotated with `@DL`, and stores the processed data in Redis with automatic expiration. It showcases:

- **Kafka message consumption**: Using the framework's Kafka connector to listen on multiple topics
- **Domain Logic pattern**: Processing messages with `@DL` annotated components
- **Redis storage**: Storing processed messages with TTL and maintaining recent message lists
- **Flexible key extraction**: Smart extraction of message keys from different JSON structures
- **Error handling**: Graceful handling of processing errors with structured responses

## Architecture

```
Kafka Topics (test-messages, user-events)
    ↓
Framework Kafka Connector
    ↓
@DL MessageProcessingLogic
    ↓
Redis Storage (individual keys + recent messages list)
```

## Features

- **Multi-topic support**: Configured to listen on `test-messages` and `user-events` topics
- **Smart key extraction**: Automatically extracts keys from message fields like `id`, `key`, `userId`, or `message`
- **Dual Redis storage**: Stores each message individually and maintains a list of recent messages
- **TTL management**: Automatic expiration after 24 hours for both individual keys and lists
- **Comprehensive testing**: Unit tests with mocks and integration tests with Testcontainers

## Prerequisites

- Java 17+
- Maven 3.6+
- Kafka (for integration testing, Testcontainers will handle this)
- Redis (for integration testing, Testcontainers will handle this)

## Configuration

### Application Configuration (`application.yml`)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: kafka-redis-example-group
      auto-offset-reset: earliest

redis:
  host: localhost
  port: 6379
  database: 0

framework:
  kafka:
    enabled: true
    topics:
      - name: test-messages
        domain-logic: kafka-redis.message-processing
      - name: user-events
        domain-logic: kafka-redis.message-processing
```

### Redis Configuration

The application is configured to connect to Redis with the following default settings:
- Host: `localhost`
- Port: `6379`
- Database: `0`
- Password: (empty)

All settings can be overridden via environment variables or application properties.

## Running the Application

### 1. Start Required Services

Start Kafka:
```bash
# Using Docker Compose (create a docker-compose.yml with Kafka and Zookeeper)
docker-compose up -d kafka

# Or start manually with Kafka installation
bin/kafka-server-start.sh config/server.properties
```

Start Redis:
```bash
# Using Docker
docker run -d -p 6379:6379 redis:6.2-alpine

# Or start manually with Redis installation
redis-server
```

### 2. Build and Run the Application

```bash
# Build the entire project
mvn clean install

# Run the Kafka-Redis example
mvn spring-boot:run -pl examples/kafka-redis
```

### 3. Send Test Messages

Send messages to Kafka topics:

```bash
# Send to test-messages topic
kafka-console-producer.sh --topic test-messages --bootstrap-server localhost:9092
{"id": "test-123", "message": "Hello World", "timestamp": "2023-01-01T10:00:00"}

# Send to user-events topic
kafka-console-producer.sh --topic user-events --bootstrap-server localhost:9092
{"userId": "user-456", "action": "login", "ip": "192.168.1.1"}
```

### 4. Verify Redis Storage

Check stored messages in Redis:

```bash
# Connect to Redis CLI
redis-cli

# List all keys
KEYS kafka-message:*

# Get specific message
GET kafka-message:test-123

# Check recent messages list
LRANGE recent-messages 0 -1

# Check TTL
TTL kafka-message:test-123
```

## Message Processing Logic

### Key Extraction Strategy

The `MessageProcessingLogic` extracts Redis keys from messages using this priority order:

1. **id** field → `kafka-message:{id}`
2. **key** field → `kafka-message:{key}`
3. **userId** field → `kafka-message:user-{userId}`
4. **message** field → `kafka-message:{hash of message content}`
5. **fallback** → `kafka-message:{hash of entire message}`

### Storage Structure

Each processed message is stored in Redis with:

```json
{
  "id": "unique-uuid",
  "timestamp": "2023-01-01T10:00:00",
  "processed": true,
  "originalMessage": { /* original Kafka message */ }
}
```

Additionally, messages are added to a `recent-messages` list (max 100 items) for easy retrieval.

## Testing

### Unit Tests

Run unit tests for domain logic:

```bash
mvn test -pl examples/kafka-redis
```

### Integration Tests

Run integration tests with Testcontainers (requires Docker):

```bash
mvn verify -pl examples/kafka-redis
```

The integration tests use Testcontainers to start real Kafka and Redis instances, ensuring end-to-end functionality.

## Monitoring and Health Checks

The application includes Spring Boot Actuator endpoints:

- Health check: `GET /actuator/health`
- Application info: `GET /actuator/info`
- Kafka health: `GET /actuator/health/kafka`
- Redis health: `GET /actuator/health/redis`

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker addresses |
| `spring.kafka.consumer.group-id` | `kafka-redis-example-group` | Consumer group ID |
| `redis.host` | `localhost` | Redis server host |
| `redis.port` | `6379` | Redis server port |
| `redis.database` | `0` | Redis database number |
| `framework.kafka.enabled` | `true` | Enable Kafka connector |

## Error Handling

The application handles various error scenarios:

- **Kafka connectivity issues**: Automatic retry with backoff
- **Redis connectivity issues**: Graceful degradation with error logging
- **Message parsing errors**: Invalid JSON messages are logged but don't stop processing
- **Processing exceptions**: Domain logic errors are caught and returned as error responses

## Performance Considerations

- **Batch processing**: Kafka consumer processes up to 500 messages per poll
- **Redis connection pooling**: Uses Jedis connection pool for efficient Redis operations
- **TTL management**: Automatic cleanup prevents Redis memory buildup
- **Async processing**: Non-blocking message processing with proper acknowledgment

## Example Use Cases

This example can be adapted for various real-world scenarios:

1. **Event Sourcing**: Store user events from Kafka into Redis for fast retrieval
2. **Caching Layer**: Cache frequently accessed data from event streams
3. **Session Management**: Store user session data with automatic expiration
4. **Real-time Analytics**: Maintain recent activity counters and metrics
5. **Notification System**: Queue and track notification delivery status

## Troubleshooting

### Common Issues

1. **Kafka connection refused**
   - Ensure Kafka is running on `localhost:9092`
   - Check network connectivity and firewall settings

2. **Redis connection refused**
   - Ensure Redis is running on `localhost:6379`
   - Verify Redis authentication if password is set

3. **Messages not being processed**
   - Check Kafka topic exists and has messages
   - Verify consumer group configuration
   - Check application logs for domain logic errors

4. **Tests fail with Docker issues**
   - Ensure Docker is running for Testcontainers
   - Check Docker daemon accessibility
   - Verify sufficient system resources

### Logs

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    blog.eric231.framework: DEBUG
    blog.eric231.examples.kafkaredis: DEBUG
    org.springframework.kafka: DEBUG
    org.springframework.data.redis: DEBUG
```

## Contributing

When extending this example:

1. Add new domain logic by creating classes with `@DL` annotation
2. Configure topic mappings in `application.yml`
3. Update Redis storage patterns as needed
4. Add comprehensive tests for new functionality
5. Update this README with new features

For more information about the microservice framework, see the main project documentation.
