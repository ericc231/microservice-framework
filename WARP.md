# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a Spring Boot microservice framework designed with Clean Architecture principles. It provides a robust and extensible foundation for building microservices, abstracting communication protocols into configurable connectors.

## Common Development Commands

### Build Commands
```bash
# Build entire project from root
mvn clean install

# Build specific module 
mvn clean install -pl framework-core
mvn clean install -pl microservice-app
mvn clean install -pl examples/helloworld-service

# Skip tests during build
mvn clean install -DskipTests

# Build from specific module (resume from)
mvn clean install -rf :mtls-provider
```

### Test Commands
```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# Run specific module tests
mvn test -pl framework-core
mvn test -pl examples/helloworld-service

# Run specific test class
mvn test -Dtest=HelloWorldBusinessProcessTest
mvn test -Dtest=PseudoWhiteBoxGeneratorTest -pl framework-core
mvn test -Dtest=PseudoWhiteBox*Test -pl framework-core

# View coverage reports
# Reports available at: framework-core/target/site/jacoco/index.html
```

### Running Applications
```bash
# Generate secret files first (required)
java -cp microservice-app/target/microservice-app-0.0.1-SNAPSHOT.jar blog.eric231.framework.infrastructure.security.PseudoWhiteBoxGenerator

# Start main application
cd microservice-app/bin
./start.sh           # Normal mode
./start.sh debug     # Debug mode (port 5005)
./start.sh otel      # OpenTelemetry mode

# Stop application
cd microservice-app/bin
./stop.sh

# Run example services (after building)
cd examples/helloworld-service/bin
./start.sh
```

## Architecture Overview

### Framework Structure
```
microservice-framework/
├── framework-core/          # Core framework with Clean Architecture
├── microservice-app/        # Main application runner
└── examples/               # Example microservices demonstrating patterns
```

### Domain Logic Patterns

The framework supports two annotation patterns:

#### Modern @DL (Domain Logic) Pattern
```java
@DL("operation-name")
@Component
public class MyDomainLogic {
    public Map<String, Object> handle(JsonNode input) {
        // Domain logic implementation
    }
}
```

#### Legacy @BP (Business Process) Pattern
```java
@BP("process-name")
public class MyBusinessProcess implements BusinessProcess {
    public JsonNode handle(JsonNode request) {
        // Business process implementation
    }
}
```

### Key Components

1. **DomainLogicAdapter**: Automatically wraps @DL annotated beans into DomainLogic interface
2. **Dynamic REST Routing**: Routes REST requests to domain logic via configuration
3. **Connector System**: Abstracts communication protocols (REST, Kafka)
4. **Security Layer**: Supports Basic Auth, LDAP, OIDC/OAuth2, mTLS
5. **Extension Mechanism**: Loads custom JARs from `extensions/` directory

### Configuration Architecture

**Framework Configuration** (`microservice-app/src/main/resources/application.yml`):
```yaml
framework:
  connectors:
    rest:
      enabled: true
      authMode: bypass
    kafka:
      enabled: false
  routing:
    - processName: "echo-service"
      triggers:
        - type: "rest"
          path: "/api/echo"
          method: "POST"
```

**Security Features**:
- Self-signed SSL certificate generation
- Pseudo-White-Box secret management (requires `secret.table` and `secret.recipe` files)
- Multiple authentication providers with role-based access control

### Example Services Architecture

Each example demonstrates different aspects:
- **helloworld-service**: Basic @BP pattern
- **basic-auth-rest**: @DL pattern with Basic Authentication
- **ldap-auth-rest**: @DL pattern with LDAP integration
- **oidc-auth-rest**: @DL pattern with OIDC/OAuth2
- **mtls-auth-rest**: @DL pattern with mutual TLS
- **basic-rest-redis**: Redis integration patterns
- ***-provider** examples: Authentication provider implementations

### Test Coverage Strategy

The framework maintains 54% overall coverage with:
- Infrastructure Connector: 99%
- Infrastructure Adapter: 88% 
- Domain Layer: 87%
- Security Layer: 86%

200+ tests across 30 test classes covering unit tests, integration tests, edge cases, and provider integration tests.

### Module Dependencies

```
microservice-app (main executable)
└── framework-core (core framework)
    └── Spring Boot 3.2.2 + Spring Security + JPA
    └── Support for LDAP, OAuth2, mTLS, Redis, Kafka
```

**Key Libraries**:
- Spring Boot 3.2.2 with Java 17
- Jackson for JSON processing
- Jasypt for encryption
- BouncyCastle for cryptography
- H2 database (default, configurable to PostgreSQL/MySQL)
- JaCoCo for test coverage

### Development Workflow

1. **Initial Setup**: Generate secret files before first run
2. **Development**: Use @DL or @BP patterns for business logic
3. **Testing**: Comprehensive test suite with coverage reports
4. **Extension**: Place custom JARs in `extensions/` directory
5. **Configuration**: Externalize config in `microservice-app/config/` directory

### Important Notes

- **HTTPS Only**: Server runs on port 8443 (HTTPS), management on 8444
- **Extension Loading**: JARs in `extensions/` directory are automatically loaded
- **Secret Management**: Requires `secret.table` and `secret.recipe` files
- **Database**: Default H2 in-memory, configurable for production databases
- **Authentication**: Multiple modes supported through configuration
