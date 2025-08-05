# Microservice Framework Core

This is the core library of a Spring Boot microservice framework designed with Clean Architecture principles. It provides a robust and extensible foundation for building microservices with configurable connectors and domain logic patterns.

## Features

*   **Clean Architecture:** Structured for clear separation of concerns (Domain, Application, Infrastructure).
*   **@DL Domain Logic Pattern:** Automatic discovery and registration of domain logic components using @DL annotation.
*   **@BP Business Process Pattern:** Support for business process identification and routing using @BP annotation.
*   **Configurable Connectors:** Supports dynamic routing of requests to business processes via configuration.
*   **Extension Mechanism:** Supports loading custom business logic from external JAR files.
*   **Pseudo-White-Box Secret Management:** Securely manages sensitive information using an obfuscated secret reconstruction mechanism.
*   **JPA and Database Support:** Built-in support for JPA with HikariCP connection pooling.
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

## Security Features

### Pseudo-White-Box Secret Management

The framework includes a secure secret management system using obfuscated reconstruction:

- **PseudoWhiteBoxGenerator**: Generates secret files
- **PseudoWhiteBoxSecretResolver**: Reconstructs secrets at runtime
- **Jasypt Integration**: Encrypts sensitive configuration values

## Contributing

Feel free to contribute to this project by submitting issues or pull requests.
