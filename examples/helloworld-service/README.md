# Helloworld Service Example

This is a simple REST service example demonstrating the @BP (Business Process) pattern in the microservice framework. It returns "Hello from Business Process!" response.

## Features

- **@BP Business Process Pattern**: Uses `@BP("helloworld-process")` annotation
- **Simple REST Endpoint**: GET `/hello` endpoint
- **Framework Integration**: Demonstrates extension loading mechanism
- **Comprehensive Testing**: Unit tests with 13 test cases covering all scenarios

## Architecture

- **Business Process**: `HelloWorldBusinessProcess` with @BP annotation
- **Clean Output**: Returns consistent "Hello from Business Process!" message
- **Framework Extension**: Can be loaded as extension JAR

**Running the Helloworld Service:**

1.  **Ensure the entire project is built:**
    ```bash
    cd microservice-parent
    mvn clean install
    ```

2.  **Copy the Helloworld Service JAR to the `microservice-app` extensions directory:**
    ```bash
    cp examples/helloworld-service/target/helloworld-service-0.0.1-SNAPSHOT.jar microservice-app/extensions/
    ```

3.  **Start the Helloworld Service using its dedicated script:**
    ```bash
    cd examples/helloworld-service/bin
    ./start.sh
    ```
    This script will set the `MICROSERVICE_APP_EXTRA_CLASSPATH` environment variable and then navigate to `microservice-app/bin` to start the main application, which will then load the `helloworld-service.jar` from the specified classpath.

4.  **Test the endpoint:**
    Open your browser or use `curl` to access the service (default port is 8081):
    ```
    http://localhost:8081/hello
    ```
    You should receive "Hello, World!" as the response.

5.  **Stop the Helloworld Service:**
    ```bash
    cd examples/helloworld-service/bin
    ./stop.sh
    ```
    This will stop the main `microservice-app` instance.

## Testing

Run the comprehensive test suite:

```bash
# Build and test
mvn clean test

# Run specific test class
mvn test -Dtest=HelloWorldBusinessProcessTest
```

**Test Coverage:**
- **HelloWorldBusinessProcessTest** - 13 comprehensive tests including:
  - ✅ Null input handling
  - ✅ Various input types (objects, arrays, strings, numbers, booleans)
  - ✅ Consistent output verification
  - ✅ Multiple invocation testing
  - ✅ @BP annotation validation
  - ✅ BusinessProcess interface compliance
  - ✅ TextNode type validation

## Business Process Pattern

The service demonstrates the framework's @BP pattern:

```java
@BP("helloworld-process")
public class HelloWorldBusinessProcess implements BusinessProcess {
    @Override
    public JsonNode handle(JsonNode request) {
        return new TextNode("Hello from Business Process!");
    }
}
```

**Key Features:**
- Returns consistent message regardless of input
- Implements BusinessProcess interface
- Uses @BP annotation for framework discovery
- Handles all input types gracefully