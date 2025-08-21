package blog.eric231.examples.mtlsauthrest.logic;

import blog.eric231.framework.application.usecase.DL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@DL("certificate-validation-process")
@Component
public class CertificateValidationLogic {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    
    @Value("${mtls.provider.base-url}")
    private String mtlsProviderBaseUrl;
    
    @Value("${mtls.provider.certificate-endpoint}")
    private String certificateEndpoint;
    
    public CertificateValidationLogic() {
        this.restTemplate = createMtlsRestTemplate();
    }
    
    public JsonNode execute(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Authentication required");
                return response;
            }
            
            if (!request.has("certificate") || request.get("certificate").asText().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Certificate data is required for validation");
                return response;
            }
            
            String certificateData = request.get("certificate").asText().trim();
            
            // Validate certificate format
            if (!certificateData.contains("-----BEGIN CERTIFICATE-----") || 
                !certificateData.contains("-----END CERTIFICATE-----")) {
                response.put("success", false);
                response.put("message", "Invalid certificate format. PEM format required.");
                return response;
            }
            
            // Call mTLS Provider to validate/register certificate
            String providerUrl = mtlsProviderBaseUrl + certificateEndpoint;
            
            ObjectNode providerRequest = objectMapper.createObjectNode();
            providerRequest.put("certificate", certificateData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(providerRequest.toString(), headers);
            
            try {
                ResponseEntity<String> providerResponse = restTemplate.exchange(
                    providerUrl, HttpMethod.POST, entity, String.class);
                
                JsonNode providerResult = objectMapper.readTree(providerResponse.getBody());
                
                response.put("success", true);
                response.put("message", "Certificate validation completed");
                response.put("certificateRegistered", providerResult.get("success").asBoolean());
                response.put("providerResponse", providerResult);
                response.put("validatedBy", "mTLS Provider");
                response.put("providerUrl", providerUrl);
                
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Failed to validate certificate with mTLS Provider: " + e.getMessage());
                response.put("providerError", true);
                response.put("providerUrl", providerUrl);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error during certificate validation: " + e.getMessage());
        }
        
        return response;
    }
    
    private RestTemplate createMtlsRestTemplate() {
        try {
            // Load client keystore
            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            try (InputStream keystoreStream = getClass().getClassLoader().getResourceAsStream("mtls-client-keystore.p12")) {
                if (keystoreStream != null) {
                    clientKeyStore.load(keystoreStream, "clientpass".toCharArray());
                }
            }
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientKeyStore, "clientpass".toCharArray());
            
            // Load truststore
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream truststoreStream = getClass().getClassLoader().getResourceAsStream("mtls-truststore.p12")) {
                if (truststoreStream != null) {
                    trustStore.load(truststoreStream, "mtlspass".toCharArray());
                }
            }
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            
            // Create SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            
            // For now, return basic RestTemplate - could be enhanced with SSL context later
            return new RestTemplate();
            
        } catch (Exception e) {
            // Return basic RestTemplate if SSL setup fails
            return new RestTemplate();
        }
    }
}