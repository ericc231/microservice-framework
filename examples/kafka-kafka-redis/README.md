# Kafka-Kafka-Redis Example

This example demonstrates a sophisticated message processing pipeline using the microservice framework's @DL (Domain Logic) pattern to create a multi-stage Kafka processing workflow that ultimately stores enriched data in Redis.

## Overview

The application implements a two-stage message processing pipeline:

1. **Stage 1 (Processing)**: Raw messages from input Kafka topic are processed and enriched by the first @DL component
2. **Stage 2 (Storage)**: Enriched messages from intermediate Kafka topic are stored in Redis with multiple indexing strategies by the second @DL component

This showcases how to build complex message pipelines using the framework's routing capabilities and demonstrates real-world patterns for message enrichment, transformation, and storage.

## Architecture

```
Input Topic (input-messages)
    ↓
@DL MessageProcessorLogic (kafka-message-processor)
    ↓ (enriches & categorizes)
Intermediate Topic (intermediate-messages)
    ↓
@DL MessageStorageLogic (kafka-message-storage)
    ↓ (indexes & stores)
Redis (multiple indexing strategies)
```

### Message Flow Details

#### Stage 1: Message Processing
- **Input**: Raw JSON messages from `input-messages` topic
- **Processing**: 
  - Generate unique processing ID and timestamp
  - Categorize users (admin, vip, regular, guest)
  - Calculate priority based on event type
  - Enrich with metadata and metrics
  - Preserve original message for audit
- **Output**: Enriched messages sent to `intermediate-messages` topic

#### Stage 2: Message Storage
- **Input**: Processed messages from `intermediate-messages` topic
- **Processing**:
  - Generate storage ID and final timestamp
  - Calculate total processing time
  - Store complete processing history
- **Output**: Multi-indexed storage in Redis with confirmation response

## Features

### Message Processing Features
- **User Categorization**: Automatic user classification based on ID patterns
- **Priority Calculation**: Event-type based priority assignment (urgent, critical, high, medium, low)
- **Data Enrichment**: Size calculation, field counting, processing metrics
- **Audit Trail**: Complete preservation of original messages and processing history

### Storage Features
- **Multiple Indexing**: 7 different Redis indexing strategies for flexible data access
- **TTL Management**: Different expiration times based on data importance
- **Processing Traceability**: Full audit trail from input to storage
- **Error Handling**: Graceful error handling with structured error responses

### Redis Indexing Strategies

| Index Type | Key Pattern | Max Items | TTL | Use Case |
|------------|-------------|-----------|-----|----------|
| Primary Storage | `message:{storageId}` | 1 | 24h | Direct message lookup |
| Processing Trace | `processing:{processingId}` | 1 | 24h | Tracing and debugging |
| User Category | `category:{userCategory}` | 100 | 24h | Query by user type |
| Priority Level | `priority:{priority}` | 50 | 12h | Priority-based retrieval |
| Event Type | `event:{eventType}` | 30 | 6h | Event analytics |
| User Messages | `user:{userId}` | 20 | 7d | User-specific history |
| Recent Messages | `recent-messages` | 200 | 48h | General activity feed |

## Prerequisites

- Java 17+
- Maven 3.6+
- Kafka (for production use)
- Redis (for production use)
- Docker (for integration testing with Testcontainers)

## Configuration

### Application Configuration (`application.yml`)

```yaml
spring:
  application:
    name: kafka-kafka-redis-example
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: kafka-kafka-redis-group
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

redis:
  host: localhost
  port: 6379
  database: 0

framework:
  connectors:
    kafka:
      enabled: true
  routing:
    # Stage 1: Process input and forward to intermediate topic
    - process-name: kafka-message-processor
      triggers:
        - type: kafka
          listen-topic: input-messages
          response-topic: intermediate-messages
    
    # Stage 2: Store processed messages in Redis
    - process-name: kafka-message-storage
      triggers:
        - type: kafka
          listen-topic: intermediate-messages
```

### User Categories

The system automatically categorizes users based on their user ID:

- **admin**: IDs starting with `admin_`
- **vip**: IDs starting with `vip_`
- **regular**: Numeric IDs (digits only)
- **guest**: All other patterns

### Priority Levels

Events are prioritized based on their type:

- **urgent**: error, security
- **critical**: purchase, payment
- **high**: login, logout
- **low**: view, click
- **medium**: all other events

## Running the Application

### 1. Start Required Services

Start Kafka and Zookeeper:
```bash
# Using Docker Compose
docker-compose up -d kafka zookeeper

# Or manually with Kafka installation
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
```

Create required topics:
```bash
kafka-topics.sh --create --topic input-messages --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic intermediate-messages --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

Start Redis:
```bash
# Using Docker
docker run -d -p 6379:6379 redis:6.2-alpine

# Or manually
redis-server
```

### 2. Build and Run the Application

```bash
# Build the entire project
mvn clean install

# Run the Kafka-Kafka-Redis example
mvn spring-boot:run -pl examples/kafka-kafka-redis
```

### 3. Send Test Messages

Send various message types to test the pipeline:

#### Admin User Login
```bash
kafka-console-producer.sh --topic input-messages --bootstrap-server localhost:9092
{"userId": "admin_001", "eventType": "login", "data": "Admin user login from dashboard"}
```

#### VIP User Purchase
```bash
kafka-console-producer.sh --topic input-messages --bootstrap-server localhost:9092
{"userId": "vip_456", "eventType": "purchase", "data": {"amount": 250.00, "product": "premium_service"}}
```

#### Regular User Activity
```bash
kafka-console-producer.sh --topic input-messages --bootstrap-server localhost:9092
{"userId": "12345", "eventType": "view", "data": "Product page view"}
```

#### Error Event
```bash
kafka-console-producer.sh --topic input-messages --bootstrap-server localhost:9092
{"userId": "guest_789", "eventType": "error", "data": "System timeout error"}
```

### 4. Verify Processing Results

Check Redis for stored and categorized messages:

```bash
# Connect to Redis CLI
redis-cli

# Check recent messages (latest 200)
LRANGE recent-messages 0 -1

# Check admin category messages
LRANGE category:admin 0 -1

# Check critical priority messages
LRANGE priority:critical 0 -1

# Check specific user messages
LRANGE user:vip_456 0 -1

# Get specific message by storage ID
GET message:{storageId}

# Get message by processing ID for tracing
GET processing:{processingId}

# Check TTL for various keys
TTL recent-messages
TTL category:admin
TTL user:vip_456
```

## Testing

### Unit Tests

Run unit tests for individual components:

```bash
# Test message processor logic
mvn test -pl examples/kafka-kafka-redis -Dtest=MessageProcessorLogicTest

# Test message storage logic
mvn test -pl examples/kafka-kafka-redis -Dtest=MessageStorageLogicTest
```

### Integration Tests

Run complete pipeline integration tests (requires Docker):

```bash
# Run all integration tests
mvn verify -pl examples/kafka-kafka-redis

# Run specific integration test
mvn test -pl examples/kafka-kafka-redis -Dtest=KafkaKafkaRedisIntegrationTest
```

The integration tests use Testcontainers to start real Kafka and Redis instances, ensuring end-to-end functionality testing.

## Monitoring and Observability

### Application Metrics

The application includes Spring Boot Actuator endpoints:

- Health check: `GET /actuator/health`
- Application info: `GET /actuator/info`
- Kafka health: `GET /actuator/health/kafka`
- Redis health: `GET /actuator/health/redis`

### Message Tracing

Each message can be traced through the entire pipeline using:

1. **Processing ID**: Generated in stage 1, carried through to stage 2
2. **Storage ID**: Generated in stage 2 for final storage reference
3. **Timestamps**: Processing and storage timestamps for performance analysis

### Performance Monitoring

Key performance indicators:

- **Processing Duration**: Time spent in stage 1 processing
- **Total Processing Time**: End-to-end time from stage 1 to stage 2
- **Message Throughput**: Messages processed per second
- **Redis Storage Efficiency**: Storage operations per message

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker addresses |
| `spring.kafka.consumer.group-id` | `kafka-kafka-redis-group` | Consumer group ID |
| `redis.host` | `localhost` | Redis server host |
| `redis.port` | `6379` | Redis server port |
| `redis.database` | `0` | Redis database number |
| `framework.connectors.kafka.enabled` | `true` | Enable Kafka connector |

## Error Handling and Recovery

### Processing Errors

- **Stage 1 errors**: Return structured error responses with original message preserved
- **Stage 2 errors**: Log errors and return error status with input message for debugging
- **Kafka errors**: Automatic retry with exponential backoff
- **Redis errors**: Graceful degradation with error logging

### Dead Letter Queue (Future Enhancement)

For production use, consider implementing:
- Dead letter topic for failed messages
- Retry policies with maximum attempt limits
- Error classification and routing

## Performance Optimization

### Recommended Settings

For high-throughput scenarios:

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1024
    producer:
      batch-size: 16384
      linger-ms: 5

redis:
  # Connection pooling configuration
  jedis:
    pool:
      max-active: 20
      max-idle: 10
```

### Scaling Considerations

- **Kafka Partitions**: Increase partitions for both topics to enable parallel processing
- **Consumer Instances**: Run multiple application instances for horizontal scaling
- **Redis Clustering**: Use Redis Cluster for high availability and performance
- **Monitoring**: Implement comprehensive monitoring for bottleneck identification

## Real-World Use Cases

This pattern is suitable for:

1. **Event Processing Pipelines**: Multi-stage event enrichment and storage
2. **Data Lake Ingestion**: ETL processes with intermediate transformation
3. **Real-time Analytics**: Stream processing with categorized storage
4. **Audit and Compliance**: Full message traceability with timed retention
5. **User Activity Tracking**: Categorized user behavior analysis
6. **System Monitoring**: Error categorization and priority-based alerting

## Extending the Example

### Adding New Processing Stages

To add a third processing stage:

1. Create new @DL component
2. Add routing configuration for new intermediate topic
3. Update tests and documentation

### Custom Categorization Logic

Modify the categorization methods in `MessageProcessorLogic`:
- `categorizeUser()`: Custom user classification
- `calculatePriority()`: Custom priority algorithms

### Additional Redis Indexing

Add new indexing strategies in `MessageStorageLogic`:
- Geographic indexing
- Time-based partitioning
- Content-based categorization

## Contributing

When extending this example:

1. Maintain the two-stage processing pattern
2. Add comprehensive unit and integration tests
3. Update configuration documentation
4. Preserve message traceability features
5. Follow the established error handling patterns

For more information about the microservice framework, see the main project documentation.
