# mTLS Auth REST Service Example

This example demonstrates an enterprise-grade REST API service using mutual TLS (mTLS) authentication with the @DL (Domain Logic) pattern. It provides secure client authentication using X.509 certificates, session management, and integration with the mTLS Provider for certificate validation and management.

## Features

- **@DL Domain Logic Pattern**: Uses `@DL` annotations for clean domain separation and business logic organization
- **Mutual TLS Authentication**: Secure client-server authentication using X.509 certificates
- **Certificate Validation**: Real-time certificate validation against client certificates
- **Session Management**: Comprehensive client session tracking with access statistics
- **Secure Data Access**: Protected endpoints providing encryption keys, tokens, and secure data
- **mTLS Provider Integration**: Seamless integration with mTLS Provider for certificate management
- **Client Activity Tracking**: Detailed logging and monitoring of client certificate usage
- **H2 Database Integration**: In-memory database for session and activity data persistence

## API Endpoints

### Certificate Management
- **GET** `/api/certificate/profile` - Get client certificate profile and session information
- **POST** `/api/certificate/validate` - Validate certificate with mTLS Provider
- **Response**: Certificate details, validation status, and session statistics

### Secure Data Access
- **GET** `/api/secure/data` - Access secure data with various data types
- **Parameters**: `dataType` (summary, encryption-key, session-stats, secure-token, client-activity)
- **Response**: Secure data based on authentication and requested type

### Client Information
- **GET** `/api/client/info` - Comprehensive client information including certificate details
- **Response**: Client certificate info, session history, and system statistics

## @DL Domain Logic Classes

### CertificateProfileLogic
```java
@DL("certificate-profile-process")
@Component
public class CertificateProfileLogic {
    public JsonNode execute(JsonNode request) {
        // Extracts client certificate from mTLS context
        // Creates/updates client session
        // Returns certificate profile and session info
        // Calculates certificate validity and expiration
    }
}
```

### CertificateValidationLogic
```java
@DL("certificate-validation-process")
@Component
public class CertificateValidationLogic {
    public JsonNode execute(JsonNode request) {
        // Validates certificate format
        // Communicates with mTLS Provider for registration/validation
        // Returns validation results from provider
    }
}
```

### SecureDataLogic
```java
@DL("secure-data-process")
@Component
public class SecureDataLogic {
    public JsonNode execute(JsonNode request) {
        // Provides secure data based on authentication
        // Supports multiple data types:
        // - encryption-key: AES-256 encryption keys
        // - session-stats: Session statistics and analytics
        // - secure-token: Temporary API access tokens
        // - client-activity: Client activity history
    }
}
```

### ClientInfoLogic
```java
@DL("client-info-process")
@Component
public class ClientInfoLogic {
    public JsonNode execute(JsonNode request) {
        // Comprehensive client information retrieval
        // Certificate details and validation status
        // Session history and activity tracking
        // System statistics and capabilities
    }
}
```

## Client Session Management

**ClientSession Entity:**
```java
@Entity
@Table(name = "client_sessions")
public class ClientSession {
    private Long id;
    private String clientDN;              // Certificate subject DN
    private String certificateFingerprint; // SHA-256 certificate fingerprint
    private String sessionId;             // Unique session identifier
    private LocalDateTime createdAt;       // Session creation time
    private LocalDateTime lastAccessed;   // Last access timestamp
    private Boolean isActive;             // Session active status
    private String clientIp;              // Client IP address
    private String userAgent;             // Client user agent
    private Long accessCount;             // Number of API accesses
}
```

## SSL/TLS Configuration

**Client SSL Configuration (application.yml):**
```yaml
server:
  port: 8086
  ssl:
    enabled: true
    key-store: classpath:mtls-client-keystore.p12
    key-store-password: clientpass
    key-store-type: PKCS12
    key-alias: client
    trust-store: classpath:mtls-truststore.p12
    trust-store-password: mtlspass
    trust-store-type: PKCS12
```

**Framework Configuration:**
```yaml
framework:
  connectors:
    rest:
      enabled: true
      authMode: mtls
  routing:
    - processName: "certificate-profile-process"
      triggers:
        - type: "rest"
          path: "/api/certificate/profile"
          method: "GET"
```

## mTLS Provider Integration

**Provider Service Configuration:**
```yaml
mtls:
  provider:
    base-url: https://localhost:8085
    certificate-endpoint: /api/certificates
    auth-endpoint: /auth/mtls
```

**MtlsProviderService Features:**
- Certificate registration with provider
- Real-time certificate validation
- Provider health monitoring
- SSL/TLS client configuration for provider communication

## Running the mTLS Auth REST Service

### Prerequisites
1. **mTLS Provider must be running on port 8085**
2. **SSL certificates must be generated and configured**

### Setup Steps

1. **Generate SSL certificates (if not done already):**
   ```bash
   # In mtls-provider directory
   cd examples/mtls-provider
   java -cp target/classes:target/lib/* blog.eric231.examples.mtlsprovider.util.CertificateGenerator
   ```

2. **Start the mTLS Provider:**
   ```bash
   cd examples/mtls-provider/bin
   ./start.sh
   ```

3. **Copy SSL certificates to mtls-auth-rest resources:**
   ```bash
   cp examples/mtls-provider/client-keystore.p12 examples/mtls-auth-rest/src/main/resources/mtls-client-keystore.p12
   cp examples/mtls-provider/mtls-truststore.p12 examples/mtls-auth-rest/src/main/resources/
   ```

4. **Build the entire project:**
   ```bash
   cd microservice-parent
   mvn clean install
   ```

5. **Copy the mTLS Auth REST JAR to extensions:**
   ```bash
   cp examples/mtls-auth-rest/target/mtls-auth-rest-0.0.1-SNAPSHOT.jar microservice-app/extensions/
   ```

6. **Start the mTLS Auth REST Service:**
   ```bash
   cd examples/mtls-auth-rest/bin
   ./start.sh
   ```

### Testing the Service

1. **Test Certificate Profile (requires client certificate):**
   ```bash
   curl -X GET https://localhost:8086/api/certificate/profile \
     --cert client.pem --key client-key.pem \
     -k
   ```

2. **Test Certificate Validation:**
   ```bash
   curl -X POST https://localhost:8086/api/certificate/validate \
     --cert client.pem --key client-key.pem \
     -H "Content-Type: application/json" \
     -d '{"certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"}' \
     -k
   ```

3. **Test Secure Data Access:**
   ```bash
   # Get encryption key
   curl -X GET "https://localhost:8086/api/secure/data?dataType=encryption-key" \
     --cert client.pem --key client-key.pem -k
   
   # Get session statistics
   curl -X GET "https://localhost:8086/api/secure/data?dataType=session-stats" \
     --cert client.pem --key client-key.pem -k
   
   # Get secure token
   curl -X GET "https://localhost:8086/api/secure/data?dataType=secure-token" \
     --cert client.pem --key client-key.pem -k
   ```

4. **Test Client Information:**
   ```bash
   curl -X GET https://localhost:8086/api/client/info \
     --cert client.pem --key client-key.pem \
     -k
   ```

### Stop the Services
```bash
# Stop mTLS Auth REST Service
cd examples/mtls-auth-rest/bin
./stop.sh

# Stop mTLS Provider
cd examples/mtls-provider/bin
./stop.sh
```

## API Response Examples

### Certificate Profile Response
```json
{
  "success": true,
  "authenticated": true,
  "clientDN": "CN=Test Client, OU=mTLS Client, O=Microservice Framework, C=US",
  "issuerDN": "CN=mTLS CA, OU=mTLS Provider, O=Microservice Framework, C=US",
  "serialNumber": "123456789",
  "validFrom": "2023-01-01T00:00:00Z",
  "validTo": "2024-01-01T00:00:00Z",
  "fingerprint": "SHA256:abcd1234...",
  "sessionId": "uuid-session-id",
  "accessCount": 5,
  "certificateValid": true,
  "certificateExpired": false,
  "daysUntilExpiry": 30
}
```

### Secure Data Response (Encryption Key)
```json
{
  "success": true,
  "dataType": "encryption-key",
  "encryptionKey": "base64-encoded-256-bit-key",
  "keyLength": 256,
  "algorithm": "AES-256",
  "generatedFor": "CN=Test Client",
  "generatedAt": "2023-12-01T10:30:00Z",
  "usage": "Client-specific encryption operations"
}
```

### Client Information Response
```json
{
  "success": true,
  "client": {
    "distinguishedName": "CN=Test Client",
    "issuerDN": "CN=mTLS CA",
    "serialNumber": "123456789",
    "isValid": true,
    "fingerprint": "SHA256:abcd1234...",
    "keyAlgorithm": "RSA",
    "keyUsage": ["digitalSignature", "keyEncipherment"]
  },
  "sessions": {
    "current": {
      "sessionId": "uuid-session-id",
      "accessCount": 10,
      "createdAt": "2023-12-01T09:00:00Z"
    },
    "totalSessions": 3,
    "activeSessions": 1
  },
  "system": {
    "totalActiveSessions": 5,
    "authenticationMethod": "mTLS (Mutual TLS)",
    "securityLevel": "HIGH"
  }
}
```

## Security Features

- **Mutual Authentication**: Both client and server authenticate each other using X.509 certificates
- **Certificate Validation**: Real-time validation against certificate database via mTLS Provider
- **Session Security**: Secure session management with fingerprint-based tracking
- **Access Control**: Role-based access control with ROLE_MTLS_CLIENT authority
- **Secure Data Generation**: Cryptographically secure key and token generation
- **Activity Auditing**: Comprehensive logging of client certificate usage and API access
- **SSL/TLS Encryption**: All communication encrypted using TLS with client certificate requirements

## Database Console Access

**H2 Console** (for development/testing only):
- URL: `https://localhost:8086/h2-console`
- JDBC URL: `jdbc:h2:mem:mtlsrestdb`
- Username: `sa`
- Password: (empty)

**Note**: Access requires valid client certificate.

## Integration Testing

The service includes comprehensive integration tests:

### Standard Tests (No Provider Required)
```bash
mvn test -Dtest=MtlsAuthRestIntegrationTest
```

### Provider Integration Tests (Requires Running Provider)
```bash
# Start mTLS Provider first
cd examples/mtls-provider/bin && ./start.sh

# Remove @Disabled annotation from MtlsProviderIntegrationTest
# Then run:
mvn test -Dtest=MtlsProviderIntegrationTest
```

## Architecture Integration

```
Client Cert → mTLS Auth REST (8086) ←→ mTLS Provider (8085)
     ↑              ↑                         ↑
X.509 Auth    @DL Domain Logic         Certificate Database
             Session Management         Certificate Validation
```

## Framework Integration

This example demonstrates advanced framework integration:

1. **Security Configuration**: Framework automatically configures mTLS when `authMode: mtls`
2. **Domain Logic Routing**: REST requests automatically routed to @DL annotated classes
3. **Certificate Context**: Framework provides X.509 certificate context to domain logic
4. **SSL Integration**: Automatic SSL/TLS configuration with client certificate requirements
5. **Provider Communication**: Secure communication with mTLS Provider using mutual TLS

## Troubleshooting

**Common Issues:**

1. **Certificate validation failures**: Ensure certificates are properly registered in mTLS Provider
2. **SSL handshake errors**: Verify client certificate and truststore configuration
3. **Provider communication failures**: Check mTLS Provider availability and SSL configuration
4. **Session management issues**: Verify H2 database configuration and JPA settings

**Debug Logging:**
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    blog.eric231: DEBUG
    javax.net.ssl: DEBUG
```

The mTLS Auth REST service provides enterprise-grade certificate-based authentication with comprehensive session management and secure data access capabilities, fully integrated with the microservice framework's security and routing infrastructure.