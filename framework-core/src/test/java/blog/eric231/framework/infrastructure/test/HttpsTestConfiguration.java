package blog.eric231.framework.infrastructure.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Test configuration for HTTPS-enabled tests.
 * Provides SSL context configuration for testing HTTPS endpoints.
 */
@TestConfiguration
public class HttpsTestConfiguration {

    /**
     * Create a trust-all SSL context for testing HTTPS endpoints with self-signed certificates.
     * WARNING: This should only be used in test environments!
     */
    @Bean
    @Primary
    public SSLContext testSSLContext() throws Exception {
        // Create a trust manager that accepts all certificates (for testing only)
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust all client certificates for testing
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust all server certificates for testing
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        // Set as default SSL context for the JVM (test environment only)
        SSLContext.setDefault(sslContext);
        
        return sslContext;
    }

}