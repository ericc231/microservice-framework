### Helloworld Service Example

This is a simple REST service that returns "Hello, World!".

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
