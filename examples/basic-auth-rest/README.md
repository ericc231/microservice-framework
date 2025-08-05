# Basic Authentication REST Service

A comprehensive REST API service demonstrating basic authentication with role-based access control using the @DL (Domain Logic) pattern.

## Features

- **@DL Domain Logic Pattern**: Uses `@DL("basic-auth")` annotation for domain logic identification
- **REST API Endpoints**: Comprehensive REST API for authentication operations
- **Web Interface**: Form-based login with HTML interface
- **Role-Based Access Control**: Admin, User, and Guest roles
- **In-Memory User Store**: Pre-configured users with different roles
- **Security Configuration**: Separate security configurations for API and web endpoints

## Architecture

This service follows Clean Architecture principles:

- **Domain Layer**: `AuthenticationDomainLogic` with @DL annotation
- **API Layer**: REST controllers (`AuthController`, `UserController`)
- **Web Layer**: Form-based controllers (`LoginController`)
- **Infrastructure**: Spring Security configuration

## Pre-configured Users

- **admin/admin** - ADMIN, USER roles
- **user/password** - USER role
- **guest/guest** - GUEST role

## API Endpoints

### Authentication API (`/api/auth/`)

- `GET /api/auth/status` - Get authentication status (public)
- `POST /api/auth/process?message=<msg>` - Process authentication request
- `GET /api/auth/role/{role}` - Check if user has specific role

### User API (`/api/user/`)

- `GET /api/user/info` - Get current user information
- `GET /api/user/admin` - Admin-only endpoint (requires ADMIN role)

## Web Interface

- `/` - Home page (authenticated users only)
- `/login` - Login form
- `/admin` - Admin page (requires ADMIN role)

## Building and Running

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build

```bash
mvn clean package
```

### Run Standalone

```bash
# Run the service
java -jar target/basic-auth-rest-0.0.1-SNAPSHOT.jar

# Or using Maven
mvn spring-boot:run
```

The service will start on port 8080.

### Run as Framework Extension

1. Copy the JAR to the framework's extensions directory:
   ```bash
   cp target/basic-auth-rest-0.0.1-SNAPSHOT.jar ../../microservice-parent/microservice-app/extensions/
   ```

2. Start the main framework application - the service will be automatically loaded.

## Testing

### API Testing with curl

```bash
# Check authentication status (public endpoint)
curl -X GET http://localhost:8080/api/auth/status

# Login and get user info (requires authentication)
curl -u admin:admin -X GET http://localhost:8080/api/user/info

# Check admin role
curl -u admin:admin -X GET http://localhost:8080/api/auth/role/ADMIN

# Try admin endpoint
curl -u admin:admin -X GET http://localhost:8080/api/user/admin

# Process authentication request
curl -u user:password -X POST "http://localhost:8080/api/auth/process?message=Hello"
```

### Web Interface Testing

1. Open http://localhost:8080 in browser
2. Login with any of the pre-configured users
3. Navigate to different pages to test role-based access

## Domain Logic (@DL) Pattern

The service demonstrates the framework's @DL pattern:

```java
@DL("basic-auth")
@Component
public class AuthenticationDomainLogic {
    // Domain logic implementation
}
```

This allows the framework to:
- Automatically discover and register domain logic
- Enable dynamic routing and service discovery
- Provide consistent architecture across services

## Security Configuration

The service uses Spring Security with:

- **API Security**: HTTP Basic authentication for `/api/**` endpoints
- **Web Security**: Form-based authentication for web interface
- **Role-based Authorization**: Different access levels for different endpoints
- **CSRF Protection**: Disabled for API endpoints, enabled for web interface

## Testing

Run the test suite:

```bash
mvn test
```

Includes:
- Unit tests for controllers
- Integration tests for authentication flows
- Assembly integration tests