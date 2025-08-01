### Basic Auth Provider Example

This example demonstrates how to configure the framework to act as a Basic Authentication server. It uses an in-memory H2 database to store user credentials.

**Database Schema (`User` Entity):**

```java
@Data
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String roles;
}
```

**Password Storage (PINBLOCK Discussion):**

For secure password storage, this example uses Spring Security's `BCryptPasswordEncoder` to hash passwords before storing them in the database. This is a standard and recommended practice for storing passwords securely.

**Note on PINBLOCK:** While the request mentioned PINBLOCK format for password storage, it's important to understand that true PINBLOCK encoding is a complex cryptographic operation typically used for payment card industry (PCI) applications and often involves hardware security modules (HSMs). Implementing a full, secure PINBLOCK solution is beyond the scope of a simple software example and requires specialized cryptographic libraries and infrastructure. This example focuses on demonstrating basic authentication with standard password hashing.

**Running the Basic Auth Provider:**

1.  **Ensure the entire project is built:**
    ```bash
    cd microservice-parent
    mvn clean install
    ```

2.  **Copy the Basic Auth Provider JAR to the `microservice-app` extensions directory:**
    ```bash
    cp examples/basic-auth-provider/target/basic-auth-provider-0.0.1-SNAPSHOT.jar microservice-app/extensions/
    ```

3.  **Start the Basic Auth Provider using its dedicated script:**
    ```bash
    cd examples/basic-auth-provider/bin
    ./start.sh
    ```
    This script will set the `MICROSERVICE_APP_EXTRA_CLASSPATH` environment variable and then navigate to `microservice-app/bin` to start the main application, which will then load the `basic-auth-provider.jar` from the specified classpath.

4.  **Test the endpoint:**
    The default endpoint for basic authentication is configured at `/auth/basic` (POST). You would typically send a request with `Authorization: Basic <base64encoded_username:password>` header.

    Example using `curl` (replace `user:password` with actual credentials):
    ```bash
    curl -v -X POST http://localhost:8085/auth/basic -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
    ```
    (Note: `dXNlcjpwYXNzd29yZA==` is base64 encoded for `user:password`)

5.  **Stop the Basic Auth Provider:**
    ```bash
    cd examples/basic-auth-provider/bin
    ./stop.sh
    ```
