# Microservice Framework

This is a Spring Boot microservice framework designed with Clean Architecture principles. It aims to provide a robust and extensible foundation for building microservices, abstracting away communication protocols into configurable connectors.

## Features

*   **Clean Architecture:** Structured for clear separation of concerns (Domain, Application, Infrastructure).
*   **Configurable Connectors:** Supports dynamic routing of requests to business processes via configuration.
*   **Pseudo-White-Box Secret Management:** Securely manages sensitive information using an obfuscated secret reconstruction mechanism.
*   **Self-Signed SSL Certificate:** Automatic generation of self-signed certificates for local development.
*   **Operational Scripts:** Includes `start.sh` and `stop.sh` scripts for easy service management, supporting normal, debug, and OpenTelemetry agent modes.
*   **Extension Mechanism:** Supports loading custom business logic or components from external JAR files placed in the `extensions` directory.

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

### Stopping the Application

```bash
cd microservice-parent/microservice-app/bin
./stop.sh
```

## Extension Mechanism

To extend the application with custom business logic or components, simply place your compiled JAR files into the `microservice-parent/microservice-app/extensions` directory. These JARs will be automatically added to the application's classpath at startup.

## Configuration

Refer to `microservice-app/src/main/resources/application.yml` for framework configuration, including connector enablement and routing rules.

## Contributing

Feel free to contribute to this project by submitting issues or pull requests.