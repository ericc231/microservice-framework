package blog.eric231.framework.infrastructure.test;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Base test class for HTTPS-enabled integration tests.
 * Provides common setup for testing with SSL/TLS enabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // SSL Configuration for tests
    "server.ssl.enabled=true",
    "server.ssl.key-store=classpath:test-keystore.p12",
    "server.ssl.key-store-password=testpass",
    "server.ssl.key-store-type=PKCS12",
    "server.ssl.key-alias=test",
    
    // Disable SSL hostname verification for tests
    "spring.webservices.wsdl.ssl.trust-all=true",
    
    // Framework configuration for testing
    "framework.security.self-signed-cert.enabled=true",
    "framework.connectors.rest.enabled=true",
    "framework.connectors.rest.authMode=bypass",
    
    // Logging configuration
    "logging.level.org.springframework.web=INFO",
    "logging.level.org.apache.http=INFO"
})
public abstract class BaseHttpsTest {

    @BeforeAll
    static void setupHttpsEnvironment() {
        // Disable SSL hostname verification for testing
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        
        // Trust all SSL certificates for testing (not for production!)
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
        
        try {
            // Create trust-all SSL context
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            }, new java.security.SecureRandom());
            
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup SSL context for testing", e);
        }
    }
}