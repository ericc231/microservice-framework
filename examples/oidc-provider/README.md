# OIDC Provider Example

This example demonstrates how to configure the framework to act as an OIDC (OpenID Connect) authentication server integrated with LDAP authentication. It uses Spring Authorization Server with LDAP backend for user authentication.

## Features

- **Spring Authorization Server**: Full OIDC/OAuth2 authorization server
- **LDAP Integration**: User authentication via LDAP backend  
- **JWT Token Support**: RSA-signed JWT access tokens and ID tokens
- **OIDC Endpoints**: Complete OIDC discovery and user info endpoints
- **Custom Login Page**: Thymeleaf-based login interface
- **@BP Business Process**: Framework integration with `@BP("oidc-auth-process")`

## OIDC Endpoints

The OIDC Provider exposes the following standard endpoints:

- **Authorization Endpoint**: `http://localhost:8083/oauth2/authorize`
- **Token Endpoint**: `http://localhost:8083/oauth2/token`
- **User Info Endpoint**: `http://localhost:8083/userinfo`
- **JWKS Endpoint**: `http://localhost:8083/oauth2/jwks`
- **OIDC Discovery**: `http://localhost:8083/.well-known/openid_configuration`
- **Login Page**: `http://localhost:8083/login`

## Configuration

**LDAP Authentication:**
- **LDAP URL**: `ldap://localhost:389`
- **Base DN**: `dc=springframework,dc=org`
- **User Pattern**: `uid={0},ou=people`
- **Admin User**: `uid=admin,ou=people,dc=springframework,dc=org`

**OIDC Client Configuration:**
- **Client ID**: `oidc-client`
- **Client Secret**: `secret`
- **Supported Scopes**: `openid`, `profile`, `email`, `read`, `write`
- **Grant Types**: `authorization_code`, `refresh_token`
- **Redirect URI**: `http://localhost:8080/login/oauth2/code/oidc-client`

## Running the OIDC Provider

1. **Ensure LDAP Provider is running:**
   ```bash
   cd examples/ldap-provider/bin
   ./start.sh
   ```

2. **Build the entire project:**
   ```bash
   cd microservice-parent
   mvn clean install
   ```

3. **Copy the OIDC Provider JAR to the extensions directory:**
   ```bash
   cp examples/oidc-provider/target/oidc-provider-0.0.1-SNAPSHOT.jar microservice-app/extensions/
   ```

4. **Start the OIDC Provider using its dedicated script:**
   ```bash
   cd examples/oidc-provider/bin
   ./start.sh
   ```

5. **Test the OIDC Provider:**
   
   **Option 1: Direct endpoint test**
   ```bash
   curl http://localhost:8083/auth/oidc
   ```
   
   **Option 2: OAuth2 Authorization Flow**
   ```bash
   # Step 1: Open authorization URL in browser
   http://localhost:8083/oauth2/authorize?response_type=code&client_id=oidc-client&redirect_uri=http://localhost:8080/login/oauth2/code/oidc-client&scope=openid%20profile%20email
   
   # Step 2: Login with LDAP credentials (admin/admin or user/password)
   # Step 3: Exchange authorization code for tokens at token endpoint
   ```

6. **Stop the OIDC Provider:**
   ```bash
   cd examples/oidc-provider/bin
   ./stop.sh
   ```

## Business Process Integration

The OIDC Provider includes a `@BP("oidc-auth-process")` business process that provides authentication status and OIDC configuration information:

```java
@BP("oidc-auth-process")
public class OidcAuthProcess implements BusinessProcess {
    @Override
    public JsonNode handle(JsonNode request) {
        // Returns authentication status and OIDC endpoints
    }
}
```

**Response Format:**
```json
{
  "authenticated": true,
  "username": "admin", 
  "issuer": "http://localhost:8083",
  "authorization_endpoint": "http://localhost:8083/oauth2/authorize",
  "token_endpoint": "http://localhost:8083/oauth2/token",
  "userinfo_endpoint": "http://localhost:8083/userinfo",
  "jwks_uri": "http://localhost:8083/oauth2/jwks"
}
```

## Testing with OIDC Client

For a complete test, use the `oidc-auth-rest` example which acts as an OIDC client:

1. Start LDAP Provider (port 389)
2. Start OIDC Provider (port 8083) 
3. Start OIDC Auth REST Service (port 8084)
4. Navigate to `http://localhost:8084/api/user/profile` - you'll be redirected through the OIDC flow

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   OIDC Client   │◄──►│  OIDC Provider   │◄──►│  LDAP Server    │
│  (port 8084)    │    │   (port 8083)    │    │  (port 389)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │ Framework Core   │
                       │ @BP Integration  │
                       └──────────────────┘
```

## Security Features

- **JWT RS256 Signing**: RSA key pair for token signing
- **PKCE Support**: Proof Key for Code Exchange (future enhancement)
- **Refresh Token Rotation**: Automatic refresh token rotation
- **Scope-based Authorization**: Fine-grained permission control
- **LDAP Authentication**: Backend user validation
- **CSRF Protection**: Cross-site request forgery protection