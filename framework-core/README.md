# Microservice Framework Core

This is the core library of a Spring Boot microservice framework designed with Clean Architecture principles. It provides a robust and extensible foundation for building microservices with configurable connectors and domain logic patterns.

## 🏆 Test Coverage & Quality

*   **✅ Test Coverage: 54%** with comprehensive test suite
*   **🔥 Component Coverage:**
    - **Infrastructure Connector: 99%** - DynamicRestController, RestConnector
    - **Infrastructure Adapter: 88%** - DomainLogicAdapter with full method discovery
    - **Domain Layer: 87%** - TestEntity with Lombok integration
    - **Security Layer: 86%** - PseudoWhiteBoxGenerator, Reader, Certificate generation
    - **Configuration Layer: 23%** - ProcessRegistry, FrameworkProperties, JasyptConfig
*   **📊 Test Statistics:**
    - **119 Total Tests** across 11 test classes
    - **All Tests Passing** (119/119) ✅
    - **Comprehensive Edge Cases** including null safety, error handling, and boundary conditions

## Features

*   **Clean Architecture:** Structured for clear separation of concerns (Domain, Application, Infrastructure).
*   **@DL Domain Logic Pattern:** Automatic discovery and registration of domain logic components using @DL annotation with adapter pattern.
*   **@BP Business Process Pattern:** Legacy support for business process identification and routing using @BP annotation.
*   **Dynamic REST Routing:** Supports dynamic routing of requests to domain logic via configuration with pattern matching.
*   **Configurable Connectors:** Multi-protocol support (REST, Kafka) with conditional enablement.
*   **Extension Mechanism:** Supports loading custom business logic from external JAR files.
*   **Pseudo-White-Box Secret Management:** Securely manages sensitive information using AES encryption with XOR obfuscation.
*   **JPA and Database Support:** Built-in support for JPA with HikariCP connection pooling.
*   **Redis Integration:** Comprehensive Redis support with both standalone and cluster adapters for caching and data storage.
*   **Process Registry:** Automatic registration and discovery of business processes and domain logic components.

## Architecture

The framework core provides the following key components:

### Domain Logic (@DL) Pattern

Domain logic components are identified using the `@DL` annotation:

```java
@DL("domain-identifier")
@Component
public class MyDomainLogic {
    // Domain logic implementation
}
```

### Business Process (@BP) Pattern

Business processes are identified using the `@BP` annotation:

```java
@BP("process-identifier")
@Component
public class MyBusinessProcess {
    // Business process implementation
}
```

### Process Registry

The `ProcessRegistry` automatically discovers and registers all components with @DL and @BP annotations, enabling:

- Dynamic service discovery
- Automatic routing configuration
- Extension loading support

## Usage

### As a Dependency

Add the framework core as a dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>blog.eric231</groupId>
    <artifactId>framework-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Building the Core

Navigate to the root directory and build:

```bash
mvn clean install
```

### Running Tests

Execute the comprehensive test suite:

```bash
# Run all tests
mvn test

# Run tests with detailed coverage report
mvn clean test jacoco:report

# Run specific test classes
mvn test -Dtest=DomainLogicAdapterTest

# View coverage report
open target/site/jacoco/index.html
```

### Coverage Details

The test suite includes:

1. **EchoServiceTest** - 14 tests for BusinessProcess implementation
2. **HelloWorldBusinessProcessTest** - 13 tests for example implementation  
3. **DomainLogicAdapterTest** - 12 tests for adapter pattern with method discovery
4. **ProcessRegistryTest** - 13 tests including edge cases and boundary conditions
5. **FrameworkPropertiesTest** - 12 tests for configuration properties
6. **RestConnectorTest** - 11 tests for REST endpoint handling
7. **TestEntityTest** - 16 tests for domain entity with Lombok
8. **DynamicRestControllerTest** - 9 tests for dynamic request routing
9. **JasyptConfigTest** - 10 tests for encryption configuration
10. **PseudoWhiteBoxGeneratorTest** - 4 tests for key generation
11. **PseudoWhiteBoxReaderTest** - 6 tests for key reconstruction

## Database Configuration

The framework includes JPA support with HikariCP connection pooling. Default configuration uses H2 in-memory database:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
```

## Redis Configuration

The framework provides comprehensive Redis support with both standalone and cluster modes:

### Standalone Redis Configuration

```yaml
framework:
  redis:
    mode: standalone
    standalone:
      host: localhost
      port: 6379
    database: 0
    password: mypassword  # optional
    timeout: 2000ms
    pool:
      max-total: 8
      max-idle: 8
      min-idle: 0
      max-wait: -1ms
```

### Redis Cluster Configuration

```yaml
framework:
  redis:
    mode: cluster
    cluster:
      nodes:
        - redis-node1:7001
        - redis-node2:7002
        - redis-node3:7003
      max-redirects: 3
    password: clusterpassword  # optional
    timeout: 2000ms
    pool:
      max-total: 20
      max-idle: 15
      min-idle: 5
      max-wait: 3000ms
```

### Using Redis Adapters

The framework automatically configures Redis adapters based on the mode setting:

```java
// Standalone Redis Adapter
@Autowired
private RedisAdapter redisAdapter;

// String operations
redisAdapter.set("key", "value");
redisAdapter.set("key", "value", Duration.ofMinutes(5)); // with expiration
String value = redisAdapter.get("key", String.class);
boolean exists = redisAdapter.exists("key");
redisAdapter.delete("key");

// Hash operations
redisAdapter.hset("hash", "field", "value");
Object hashValue = redisAdapter.hget("hash", "field");
Map<Object, Object> allFields = redisAdapter.hgetAll("hash");

// Set operations
redisAdapter.sadd("set", "member1", "member2");
Set<Object> members = redisAdapter.smembers("set");
boolean isMember = redisAdapter.sismember("set", "member1");

// Complex objects (automatic JSON serialization)
MyObject obj = new MyObject("test");
redisAdapter.set("object:key", obj);
MyObject retrieved = redisAdapter.get("object:key", MyObject.class);
```

```java
// Redis Cluster Adapter (same API with cluster-specific features)
@Autowired
private RedisClusterAdapter redisClusterAdapter;

// All standard operations work the same way
redisClusterAdapter.set("key", "value");

// Cluster-specific operations
String clusterInfo = redisClusterAdapter.getClusterInfo();
Set<String> clusterNodes = redisClusterAdapter.getClusterNodes();
int slot = redisClusterAdapter.getSlotForKey("mykey");
Map<String, Object> stats = redisClusterAdapter.getClusterStats();
```

### Redis Features

- **🔧 Automatic Configuration**: Based on `framework.redis.mode` setting
- **🚀 Connection Pooling**: Configurable Jedis connection pool with health checks  
- **📦 JSON Serialization**: Automatic object serialization/deserialization with Jackson
- **⚡ Performance Optimized**: Efficient connection management and error handling
- **🔍 Monitoring**: Built-in connection testing and Redis info retrieval
- **🎯 Type Safety**: Generic type support for seamless object conversion
- **🛡️ Error Handling**: Comprehensive exception handling with custom exceptions
- **📊 Cluster Support**: Full Redis Cluster topology management and slot-based operations

## Security Features

### Pseudo-White-Box Secret Management

The framework includes a secure secret management system using obfuscated reconstruction:

- **PseudoWhiteBoxGenerator**: Generates secret files
- **PseudoWhiteBoxSecretResolver**: Reconstructs secrets at runtime
- **Jasypt Integration**: Encrypts sensitive configuration values

## Contributing

Feel free to contribute to this project by submitting issues or pull requests.
