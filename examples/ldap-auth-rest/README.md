# LDAP Authentication REST Service

A full-featured REST API service demonstrating LDAP authentication with group-based authorization using the @DL (Domain Logic) pattern.

## Features

- **@DL Domain Logic Pattern**: Uses `@DL("ldap-auth")` annotation for domain logic identification
- **LDAP Authentication**: Connects to LDAP server for user authentication
- **Group-based Authorization**: LDAP group membership for role-based access control
- **REST API Endpoints**: Comprehensive REST API for LDAP operations
- **Web Interface**: Form-based login with HTML interface
- **LDAP User Details**: Extracts DN, organizational units, and group memberships
- **Server Status Monitoring**: LDAP server connection status endpoints

## Architecture

This service follows Clean Architecture principles:

- **Domain Layer**: `LdapAuthenticationDomainLogic` with @DL annotation
- **API Layer**: REST controllers (`LdapAuthController`, `LdapUserController`)
- **Web Layer**: Form-based controllers (`LdapLoginController`)
- **Infrastructure**: Spring Security LDAP configuration

## LDAP Configuration

Default LDAP server configuration:

- **Server URL**: `ldap://localhost:8389`
- **Base DN**: `dc=springframework,dc=org`
- **User DN Pattern**: `uid={0},ou=people,dc=springframework,dc=org`
- **Group Search Base**: `ou=groups,dc=springframework,dc=org`
- **Group Search Filter**: `(member={0})`

## API Endpoints

### LDAP Authentication API (`/api/ldap/`)

- `GET /api/ldap/server/status` - Get LDAP server status (public)
- `POST /api/ldap/auth?message=<msg>` - Process LDAP authentication request
- `GET /api/ldap/user/groups` - Get current user's LDAP groups and roles
- `GET /api/ldap/user/role/{role}` - Check if user has specific role or group membership

### User API (`/api/user/`)

- `GET /api/user/info` - Get current LDAP user information
- `GET /api/user/admin` - Admin-only endpoint (requires ADMIN or ADMINS role)

## Web Interface

- `/` - Home page (authenticated users only)
- `/login` - LDAP login form
- `/admin` - Admin page (requires ADMIN or ADMINS role)

## LDAP User Information

The service extracts comprehensive LDAP user information:

```json
{
  "username": "user1",
  "authenticated": true,
  "authType": "LDAP",
  "dn": "uid=user1,ou=people,dc=springframework,dc=org",
  "ldapServer": "springframework.org",
  "organizationalUnit": "people",
  "domain": "springframework.org",
  "authorities": ["ROLE_USER", "ROLE_ADMIN"],
  "groups": ["USER", "ADMIN"],
  "isAdmin": true
}
```

## Building and Running

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- LDAP server (or use the ldap-provider example)

### Build

```bash
mvn clean package
```

### Setup LDAP Server

You can use the included `ldap-provider` example:

1. Start the LDAP provider:
   ```bash
   cd ../ldap-provider
   mvn spring-boot:run
   ```

2. The LDAP server will start on port 8389 with test users

### Run Standalone

```bash
# Run the service
java -jar target/ldap-auth-rest-0.0.1-SNAPSHOT.jar

# Or using Maven
mvn spring-boot:run
```

The service will start on port 8080.

### Run as Framework Extension

1. Copy the JAR to the framework's extensions directory:
   ```bash
   cp target/ldap-auth-rest-0.0.1-SNAPSHOT.jar ../../microservice-parent/microservice-app/extensions/
   ```

2. Start the main framework application - the service will be automatically loaded.

## Testing

### API Testing with curl

```bash
# Check LDAP server status (public endpoint)
curl -X GET http://localhost:8080/api/ldap/server/status

# Login with LDAP user and get info
curl -u ben:benspassword -X GET http://localhost:8080/api/user/info

# Get user's LDAP groups
curl -u ben:benspassword -X GET http://localhost:8080/api/ldap/user/groups

# Check role membership
curl -u ben:benspassword -X GET http://localhost:8080/api/ldap/user/role/ADMIN

# Process LDAP authentication request
curl -u ben:benspassword -X POST "http://localhost:8080/api/ldap/auth?message=LDAP Auth Test"
```

### Web Interface Testing

1. Ensure LDAP server is running (ldap-provider example)
2. Open http://localhost:8080 in browser
3. Login with LDAP credentials (e.g., ben/benspassword)
4. Navigate to different pages to test group-based access

## Domain Logic (@DL) Pattern

The service demonstrates the framework's @DL pattern:

```java
@DL("ldap-auth")
@Component
public class LdapAuthenticationDomainLogic {
    // LDAP-specific domain logic implementation
}
```

This allows the framework to:
- Automatically discover and register LDAP domain logic
- Enable dynamic routing for LDAP operations
- Provide consistent architecture across authentication services

## LDAP Security Configuration

The service uses Spring Security LDAP with:

- **LDAP Authentication Provider**: Custom LDAP authentication configuration
- **Bind Authenticator**: User DN pattern-based authentication
- **Authority Populator**: Group membership to role mapping
- **API Security**: HTTP Basic with LDAP authentication for `/api/**`
- **Web Security**: Form-based LDAP authentication for web interface
- **Group-based Authorization**: LDAP group membership for access control

## Role and Group Mapping

- LDAP groups are mapped to Spring Security roles with `ROLE_` prefix
- Group names are converted to uppercase
- Admin privileges are detected from group membership containing "ADMIN"
- Multiple group membership is supported

## Testing

Run the test suite:

```bash
mvn test
```

Includes:
- Unit tests for LDAP controllers
- Integration tests for LDAP authentication flows
- LDAP connection and group membership tests

## LDAP Test Users

When using the ldap-provider example, test users include:

- **ben/benspassword** - Member of admin and developer groups
- **bob/bobspassword** - Member of developer group
- **joe/joespassword** - Member of user group

See `ldap-provider/src/main/resources/users.ldif` for complete user list.