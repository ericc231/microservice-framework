# mTLS Provider Example

This example demonstrates how to configure the framework to act as a mutual TLS (mTLS) authentication server with certificate management and database storage. It provides secure client authentication using X.509 certificates stored and validated against a database.

## Features

- **@Provider Annotation**: Uses `@Provider("mtls-certificate-provider")` for provider identification
- **Certificate Database Storage**: X.509 certificates stored in H2 database with metadata
- **Certificate Lifecycle Management**: Creation, validation, expiration tracking, and revocation
- **mTLS Authentication**: Mutual TLS with client certificate validation
- **Certificate Validation**: Real-time certificate validation against database records
- **@BP Business Process Integration**: Framework integration using business process pattern
- **Comprehensive Certificate Management**: Full CRUD operations for certificates

## Certificate Database Schema

**Certificate Entity:**
```java
@Entity
@Table(name = "certificates")
public class Certificate {
    private Long id;                    // Primary key
    private String subjectDN;           // Certificate subject distinguished name
    private String issuerDN;            // Certificate issuer distinguished name  
    private String serialNumber;        // Certificate serial number
    private String certificateData;     // Full PEM certificate data
    private LocalDateTime validFrom;    // Certificate validity start date
    private LocalDateTime validTo;      // Certificate validity end date
    private CertificateStatus status;   // ACTIVE, REVOKED, EXPIRED, SUSPENDED
    private String fingerprintSha256;   // SHA-256 fingerprint for uniqueness
    private String keyUsage;            // Certificate key usage extensions
    private String revocationReason;    // Reason for revocation (if applicable)
    private LocalDateTime createdAt;    // Database record creation time
    private LocalDateTime updatedAt;    // Last update time
    private LocalDateTime revokedAt;    // Revocation timestamp
}
```

## @Provider Architecture

The mTLS Provider demonstrates the new `@Provider` annotation pattern:

```java
@Provider(value = "mtls-certificate-provider", 
          description = "mTLS Certificate Management Provider",
          authType = "mtls")
@Service
public class CertificateService {
    // Certificate management logic
}
```

**Provider Annotation Features:**
- **Component Discovery**: Automatic Spring component scanning
- **Provider Identification**: Unique naming for authentication providers
- **Metadata Support**: Description and authentication type information
- **Framework Integration**: Seamless integration with security configuration

## API Endpoints

### mTLS Authentication
- **GET** `/auth/mtls` - Validate client certificate and return authentication status
- **Response**: Authentication result with certificate details

### Certificate Management
- **GET** `/api/certificates` - List certificates with filtering options
- **POST** `/api/certificates` - Register new certificate in database
- **Parameters**: `action` (list, active, expired, revoke)

## Business Process Integration

### MtlsAuthProcess
```java
@BP("mtls-auth-process")
@Component
public class MtlsAuthProcess implements BusinessProcess {
    @Override
    public JsonNode handle(JsonNode request) {
        // Extracts client certificate from request
        // Validates against database
        // Returns authentication status
    }
}
```

### CertificateManagementProcess
```java
@BP("certificate-management-process")
@Component  
public class CertificateManagementProcess implements BusinessProcess {
    // Handles certificate listing, filtering, and management operations
}
```

### CertificateRegisterProcess
```java
@BP("certificate-register-process")
@Component
public class CertificateRegisterProcess implements BusinessProcess {
    // Handles new certificate registration with validation
}
```

## SSL/TLS Configuration

**Server SSL Configuration (application.yml):**
```yaml
server:
  port: 8085
  ssl:
    enabled: true
    key-store: classpath:mtls-keystore.p12
    key-store-password: mtlspass
    key-store-type: PKCS12
    key-alias: mtls-server
    client-auth: need                    # Require client certificates
    trust-store: classpath:mtls-truststore.p12
    trust-store-password: mtlspass
    trust-store-type: PKCS12
```

## Certificate Generation

The provider includes a `CertificateGenerator` utility for creating test certificates:

```java
@Component
public class CertificateGenerator {
    public void generateServerKeyStoreAndTrustStore() throws Exception {
        // Generates server keystore (mtls-keystore.p12)
        // Generates trust store (mtls-truststore.p12) 
        // Generates sample client certificate (client-keystore.p12)
    }
}
```

**Generated Files:**
- `mtls-keystore.p12` - Server certificate and private key (password: mtlspass)
- `mtls-truststore.p12` - CA certificates for client validation (password: mtlspass)
- `client-keystore.p12` - Sample client certificate for testing (password: clientpass)

## Running the mTLS Provider

### Prerequisites
Generate SSL certificates first:

```java
// Run certificate generator utility
CertificateGenerator generator = new CertificateGenerator();
generator.generateServerKeyStoreAndTrustStore();
```

### Setup Steps

1. **Build the entire project:**
   ```bash
   cd microservice-parent
   mvn clean install
   ```

2. **Generate SSL certificates:**
   ```bash
   # Run the certificate generator utility
   cd examples/mtls-provider
   java -cp target/classes:target/lib/* blog.eric231.examples.mtlsprovider.util.CertificateGenerator
   ```

3. **Copy generated certificates to resources:**
   ```bash
   cp mtls-keystore.p12 src/main/resources/
   cp mtls-truststore.p12 src/main/resources/
   ```

4. **Copy the mTLS Provider JAR to extensions:**
   ```bash
   cp examples/mtls-provider/target/mtls-provider-0.0.1-SNAPSHOT.jar microservice-app/extensions/
   ```

5. **Start the mTLS Provider:**
   ```bash
   cd examples/mtls-provider/bin
   ./start.sh
   ```

6. **Register a client certificate:**
   ```bash
   # First extract the client certificate from keystore
   keytool -export -alias client -file client.crt -keystore client-keystore.p12 -storetype PKCS12 -storepass clientpass
   
   # Convert to PEM format
   openssl x509 -inform DER -in client.crt -out client.pem
   
   # Register in database via API (requires valid client cert)
   curl -X POST https://localhost:8085/api/certificates \
     --cert client.pem --key client-key.pem \
     -H "Content-Type: application/json" \
     -d '{"certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"}' \
     -k
   ```

### Testing mTLS Authentication

1. **Test with valid client certificate:**
   ```bash
   curl -X GET https://localhost:8085/auth/mtls \
     --cert client.pem --key client-key.pem \
     -k
   ```

2. **Test without client certificate (should fail):**
   ```bash
   curl -X GET https://localhost:8085/auth/mtls -k
   ```

3. **List all certificates:**
   ```bash
   curl -X GET https://localhost:8085/api/certificates \
     --cert client.pem --key client-key.pem \
     -k
   ```

4. **List only active certificates:**
   ```bash
   curl -X GET "https://localhost:8085/api/certificates?action=active" \
     --cert client.pem --key client-key.pem \
     -k
   ```

### Stop the Service
```bash
cd examples/mtls-provider/bin
./stop.sh
```

## Certificate Management Operations

### Register Certificate
```json
POST /api/certificates
{
  "certificate": "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----"
}
```

### List Certificates
```json
GET /api/certificates
{
  "success": true,
  "count": 3,
  "certificates": [
    {
      "id": 1,
      "subjectDN": "CN=Test Client",
      "issuerDN": "CN=mTLS CA", 
      "serialNumber": "123456789",
      "status": "ACTIVE",
      "validFrom": "2023-01-01T00:00:00",
      "validTo": "2024-01-01T00:00:00",
      "isValid": true,
      "fingerprint": "SHA256:abcd1234..."
    }
  ]
}
```

### Revoke Certificate
```json
GET /api/certificates
{
  "action": "revoke",
  "certificateId": 1,
  "reason": "Compromised private key"
}
```

## Security Features

- **Mutual Authentication**: Both client and server authenticate each other
- **Certificate Validation**: Real-time validation against database records
- **Certificate Lifecycle**: Automatic expiration detection and status management
- **Revocation Support**: Manual certificate revocation with audit trail
- **Fingerprint Verification**: SHA-256 fingerprint for certificate uniqueness
- **Key Usage Validation**: Certificate key usage extension validation
- **Audit Trail**: Complete audit trail of certificate operations

## Database Console Access

**H2 Console** (for development/testing only):
- URL: `https://localhost:8085/h2-console`  
- JDBC URL: `jdbc:h2:mem:mtlsdb`
- Username: `sa`
- Password: (empty)

**Note**: Access requires valid client certificate.

## Integration with Framework

The mTLS Provider demonstrates advanced framework integration:

1. **Security Configuration**: Framework automatically configures mTLS when `authMode: mtls`
2. **Business Process Routing**: REST requests automatically routed to @BP annotated classes  
3. **Provider Discovery**: @Provider annotation enables automatic component discovery
4. **Certificate Validation**: Framework integrates with certificate database for validation
5. **SSL Configuration**: Automatic SSL/TLS configuration with client certificate requirements

## Troubleshooting

**Common Issues:**

1. **Certificate validation failures**: Ensure certificates are properly registered in database
2. **SSL handshake errors**: Verify keystore/truststore configuration and passwords
3. **Client certificate required**: Configure client with valid certificate from truststore
4. **Database connection issues**: Check H2 database configuration and initialization

**Debug Logging:**
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    blog.eric231: DEBUG
```