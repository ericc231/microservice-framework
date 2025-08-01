# Microservice Framework

This is a Spring Boot microservice framework designed with Clean Architecture principles. It aims to provide a robust and extensible foundation for building microservices, abstracting away communication protocols into configurable connectors.

## Features

*   **Clean Architecture:** Structured for clear separation of concerns (Domain, Application, Infrastructure).
*   **Configurable Connectors & Dynamic Routing:** Supports dynamic routing of REST requests to business processes via configuration, leveraging the `@BP` annotation for process identification.
*   **Pseudo-White-Box Secret Management:** Securely manages sensitive information using an obfuscated secret reconstruction mechanism.
*   **Self-Signed SSL Certificate:** Automatic generation of self-signed certificates for local development.
*   **Operational Scripts:** Includes `start.sh` and `stop.sh` scripts for easy service management, supporting normal, debug, and OpenTelemetry agent modes.
*   **Extension Mechanism:** Supports loading custom business logic or components from external JAR files placed in the `extensions` directory. Business processes defined in extensions can be dynamically routed.

## Getting Started

### Prerequisites

*   Java 17 or higher
*   Maven

### Build the Project

Navigate to the `microservice-parent` directory and build the project:

```bash
cd microservice-parent
mvn clean install
```

### Generate Pseudo-White-Box Secret Files

Before running the application, you need to generate the `secret.table` and `secret.recipe` files. These files contain the obfuscated master password for Jasypt.

1.  **Run the generator tool:**
    ```bash
    # From the microservice-parent directory
    java -cp microservice-app/target/microservice-app-0.0.1-SNAPSHOT.jar blog.eric231.framework.infrastructure.security.PseudoWhiteBoxGenerator
    ```
    This will create `secret.table` and `secret.recipe` in your current directory.

2.  **Move the generated files:** Ensure `secret.table` and `secret.recipe` are in a location accessible by the application at runtime (e.g., in the same directory as the JAR, or specify their paths in `application.yml`).

### Running the Application

Navigate to the `microservice-parent/microservice-app/bin` directory and use the provided scripts:

```bash
cd microservice-parent/microservice-app/bin

# Normal mode
./start.sh

# Debug mode (listens on port 5005)
./start.sh debug

# OpenTelemetry Agent mode (requires opentelemetry-javaagent.jar in the bin directory)
./start.sh otel
```

**Configuration Files:**

Application configuration files (e.g., `application.yml`) can be placed in the `microservice-app/config` directory. This directory is automatically added to the classpath at startup, allowing for externalized configuration.

### Stopping the Application

```bash
cd microservice-parent/microservice-app/bin
./stop.sh
```

## Extension Mechanism

To extend the application with custom business logic or components, simply place your compiled JAR files into the `microservice-parent/microservice-app/extensions` directory. These JARs will be automatically added to the application's classpath at startup.

## Examples

This framework includes example microservices to demonstrate its capabilities. Each example is a separate Maven module located in the `examples/` directory. For detailed instructions on each example, please refer to their respective `README.md` files.

*   **Helloworld Service Example:** A simple REST service returning "Hello, World!". ([`examples/helloworld-service/README.md`](examples/helloworld-service/README.md))
*   **Basic Auth Provider Example:** Demonstrates basic authentication using an in-memory H2 database. ([`examples/basic-auth-provider/README.md`](examples/basic-auth-provider/README.md))
*   **LDAP Provider Example:** Placeholder for LDAP authentication server. ([`examples/ldap-provider/README.md`](examples/ldap-provider/README.md))
*   **OIDC Provider Example:** Placeholder for OIDC authentication server. ([`examples/oidc-provider/README.md`](examples/oidc-provider/README.md))
*   **mTLS Provider Example:** Placeholder for mTLS authentication server. ([`examples/mtls-provider/README.md`](examples/mtls-provider/README.md))


## Configuration

Refer to `microservice-app/src/main/resources/application.yml` for framework configuration, including connector enablement and routing rules.

### Database Configuration

This framework uses Spring Data JPA with HikariCP for database connection pooling. By default, it's configured to use an in-memory H2 database for development and testing purposes.

**Default H2 Configuration (`microservice-app/src/main/resources/application.yml`):**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
    username: sa
    password: 
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
      path: /h2-console
```

**Switching to Other Databases (e.g., PostgreSQL, MySQL):**

To use a different database, you need to:

1.  **Add the appropriate JDBC driver dependency** to `microservice-parent/pom.xml` (or `framework-core/pom.xml` if only that module needs it). For example, for PostgreSQL:

    ```xml
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    ```

2.  **Update `microservice-app/src/main/resources/application.yml`** with your database's connection details and driver class. For example, for PostgreSQL:

    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/your-database
        username: your-username
        password: your-password
        driver-class-name: org.postgresql.Driver
      jpa:
        database-platform: org.hibernate.dialect.PostgreSQLDialect # Or appropriate dialect for your DB
        hibernate:
          ddl-auto: update # Consider 'none' or 'validate' for production
      # HikariCP specific properties (optional, but recommended for fine-tuning)
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
    ```

## Contributing

Feel free to contribute to this project by submitting issues or pull requests.