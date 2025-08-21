# OIDC Auth REST Service Example

This example demonstrates a comprehensive REST API service using OIDC authentication with the @DL (Domain Logic) pattern. It acts as an OIDC client that authenticates users via the OIDC Provider and provides user management functionality.

## Features

- **@DL Domain Logic Pattern**: Uses `@DL` annotations for clean domain separation
- **OIDC Client Integration**: OAuth2/OIDC client configuration with JWT token validation
- **User Management API**: RESTful endpoints for user profile and management
- **JWT Resource Server**: Validates JWT tokens from OIDC Provider
- **H2 Database Integration**: In-memory database for user data persistence
- **Automatic User Creation**: Creates users on first login from OIDC claims

## API Endpoints

### User Profile
- **GET** `/api/user/profile` - Get current user's profile information
- **Response**: User profile with OIDC claims and database information

### User Management  
- **GET** `/api/users` - List all active users
- **POST** `/api/users` - Create a new user
- **Response**: User list or creation confirmation

### Authentication Flow
- **GET** `/login/oauth2/code/oidc-client` - OAuth2 callback endpoint
- **GET** `/oauth2/authorization/oidc-client` - Initiate OAuth2 flow

## @DL Domain Logic Classes

### UserProfileLogic
```java
@DL("user-profile-process")
@Component
public class UserProfileLogic {
    public JsonNode execute(JsonNode request) {
        // Retrieves user profile from JWT claims and database
        // Creates user if first login
        // Updates last login timestamp
    }
}
```

### UserListLogic  
```java
@DL("user-list-process")
@Component
public class UserListLogic {
    public JsonNode execute(JsonNode request) {
        // Returns list of all active users
        // Requires valid JWT authentication
    }
}
```

### CreateUserLogic
```java
@DL("create-user-process") 
@Component
public class CreateUserLogic {
    public JsonNode execute(JsonNode request) {
        // Creates new user from request data
        // Validates uniqueness of username/email
    }
}
```

## Configuration

**OIDC Client Settings:**
```yaml 
spring:
  security:
    oauth2:
      client:
        registration:
          oidc-client:
            client-id: oidc-client
            client-secret: secret
            scope: openid,profile,email,read,write
            authorization-grant-type: authorization_code
            redirect-uri: http://localhost:8084/login/oauth2/code/oidc-client
        provider:
          oidc-client:
            issuer-uri: http://localhost:8083
```

**Framework Routing Configuration:**
```yaml
framework:
  connectors:
    rest:
      enabled: true
      authMode: oidc
  routing:
    - processName: "user-profile-process"
      triggers:
        - type: "rest"
          path: "/api/user/profile"
          method: "GET"
```

## Running the OIDC Auth REST Service

### Prerequisites
1. **LDAP Provider must be running on port 389**
2. **OIDC Provider must be running on port 8083**

### Setup Steps

1. **Start the LDAP Provider:**
   ```bash
   cd examples/ldap-provider/bin
   ./start.sh
   ```

2. **Start the OIDC Provider:**
   ```bash
   cd examples/oidc-provider/bin  
   ./start.sh
   ```

3. **Build the entire project:**
   ```bash
   cd microservice-parent
   mvn clean install
   ```

4. **Copy the OIDC Auth REST JAR to extensions:**
   ```bash
   cp examples/oidc-auth-rest/target/oidc-auth-rest-0.0.1-SNAPSHOT.jar microservice-app/extensions/
   ```

5. **Start the OIDC Auth REST Service:**
   ```bash
   cd examples/oidc-auth-rest/bin
   ./start.sh
   ```

### Testing the Service

1. **Test User Profile (triggers OIDC flow):**
   ```bash
   curl http://localhost:8084/api/user/profile
   ```
   This will redirect to OIDC Provider for authentication.

2. **Manual OAuth2 Flow:**
   - Navigate to: `http://localhost:8084/oauth2/authorization/oidc-client`
   - Login with LDAP credentials: `admin/admin` or `user/password`
   - You'll be redirected back with authentication

3. **Test with Bearer Token:**
   ```bash
   # First get a token from OIDC Provider
   TOKEN=$(curl -X POST http://localhost:8083/oauth2/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=authorization_code&code=YOUR_CODE&client_id=oidc-client&client_secret=secret")
   
   # Use token to access API
   curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8084/api/user/profile
   ```

4. **Create User via API:**
   ```bash
   curl -X POST http://localhost:8084/api/users \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","email":"test@example.com","fullName":"Test User"}'
   ```

5. **List All Users:**
   ```bash
   curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8084/api/users
   ```

### Stop the Service
```bash
cd examples/oidc-auth-rest/bin
./stop.sh
```

## Database Schema

**User Entity:**
```java
@Entity
@Table(name = "users")
public class User {
    private Long id;
    private String username;      // From OIDC 'preferred_username' claim
    private String email;         // From OIDC 'email' claim  
    private String fullName;      // From OIDC 'name' claim
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Boolean active;
}
```

**H2 Console Access:**
- URL: `http://localhost:8084/h2-console`
- JDBC URL: `jdbc:h2:mem:oidcdb`
- Username: `sa`
- Password: (empty)

## Authentication Flow

```
1. Client → GET /api/user/profile
2. Server → Redirect to OIDC Provider authorization
3. User → Login with LDAP credentials  
4. OIDC Provider → Redirect back with authorization code
5. Server → Exchange code for access token + ID token
6. Server → Validate JWT token
7. Server → Create/update user from JWT claims
8. Server → Return user profile data
```

## Security Features

- **JWT Token Validation**: Validates signatures against OIDC Provider JWKS
- **Automatic User Provisioning**: Creates users from OIDC claims on first login
- **Scope-based Authorization**: Validates required scopes in JWT tokens
- **CSRF Protection**: Enabled for state-changing operations
- **H2 Console Protection**: Database console access in development only

## Integration with Framework

This example demonstrates the framework's @DL pattern integration with OIDC:

1. **Framework Security**: Handles OIDC authentication configuration
2. **@DL Processing**: Domain logic executes with authenticated context
3. **Auto Routing**: Framework routes REST requests to @DL classes
4. **Security Context**: JWT claims available in domain logic execution