package blog.eric231.examples.mtlsauthrest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Service
public class MtlsProviderService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mtls.provider.base-url}")
    private String mtlsProviderBaseUrl;

    @Value("${mtls.provider.certificate-endpoint}")
    private String certificateEndpoint;

    @Value("${mtls.provider.auth-endpoint}")
    private String authEndpoint;

    public MtlsProviderService() {
        this.restTemplate = createMtlsRestTemplate();
    }

    public JsonNode validateWithProvider() {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            String authUrl = mtlsProviderBaseUrl + authEndpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> providerResponse = restTemplate.exchange(
                authUrl, HttpMethod.GET, entity, String.class);
            
            JsonNode providerResult = objectMapper.readTree(providerResponse.getBody());
            
            response.put("success", true);
            response.put("message", "Successfully validated with mTLS Provider");
            response.set("providerResponse", providerResult);
            response.put("providerUrl", authUrl);
            response.put("httpStatus", providerResponse.getStatusCode().value());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to validate with mTLS Provider: " + e.getMessage());
            response.put("providerUrl", mtlsProviderBaseUrl + authEndpoint);
        }
        
        return response;
    }

    public JsonNode registerCertificate(String certificateData) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            String registerUrl = mtlsProviderBaseUrl + certificateEndpoint;
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("certificate", certificateData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> providerResponse = restTemplate.exchange(
                registerUrl, HttpMethod.POST, entity, String.class);
            
            JsonNode providerResult = objectMapper.readTree(providerResponse.getBody());
            
            response.put("success", true);
            response.put("message", "Certificate registration request sent to mTLS Provider");
            response.set("providerResponse", providerResult);
            response.put("providerUrl", registerUrl);
            response.put("httpStatus", providerResponse.getStatusCode().value());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to register certificate with mTLS Provider: " + e.getMessage());
            response.put("providerUrl", mtlsProviderBaseUrl + certificateEndpoint);
        }
        
        return response;
    }

    public JsonNode getCertificates(String action) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            String url = mtlsProviderBaseUrl + certificateEndpoint;
            if (action != null && !action.isEmpty()) {
                url += "?action=" + action;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> providerResponse = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            JsonNode providerResult = objectMapper.readTree(providerResponse.getBody());
            
            response.put("success", true);
            response.put("message", "Successfully retrieved certificates from mTLS Provider");
            response.set("providerResponse", providerResult);
            response.put("providerUrl", url);
            response.put("httpStatus", providerResponse.getStatusCode().value());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve certificates from mTLS Provider: " + e.getMessage());
            response.put("providerUrl", mtlsProviderBaseUrl + certificateEndpoint);
        }
        
        return response;
    }

    public boolean isProviderAvailable() {
        try {
            String healthUrl = mtlsProviderBaseUrl + authEndpoint;
            ResponseEntity<String> response = restTemplate.exchange(
                healthUrl, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private RestTemplate createMtlsRestTemplate() {
        try {
            // Load client keystore
            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            try (InputStream keystoreStream = getClass().getClassLoader().getResourceAsStream("mtls-client-keystore.p12")) {
                if (keystoreStream != null) {
                    clientKeyStore.load(keystoreStream, "clientpass".toCharArray());
                } else {
                    // Create empty keystore if file not found
                    clientKeyStore.load(null, null);
                }
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientKeyStore, "clientpass".toCharArray());

            // Load truststore
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream truststoreStream = getClass().getClassLoader().getResourceAsStream("mtls-truststore.p12")) {
                if (truststoreStream != null) {
                    trustStore.load(truststoreStream, "mtlspass".toCharArray());
                } else {
                    // Create empty truststore if file not found
                    trustStore.load(null, null);
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