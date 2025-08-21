package blog.eric231.examples.mtlsauthrest.logic;

import blog.eric231.examples.mtlsauthrest.domain.ClientSession;
import blog.eric231.examples.mtlsauthrest.domain.ClientSessionRepository;
import blog.eric231.framework.application.usecase.DL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.UUID;

@DL("certificate-profile-process")
@Component
public class CertificateProfileLogic {
    
    private final ClientSessionRepository clientSessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public CertificateProfileLogic(ClientSessionRepository clientSessionRepository) {
        this.clientSessionRepository = clientSessionRepository;
    }
    
    public JsonNode execute(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Client certificate authentication required");
                return response;
            }
            
            X509Certificate[] certificates = extractClientCertificates(httpRequest);
            if (certificates == null || certificates.length == 0) {
                response.put("success", false);
                response.put("message", "No client certificate found");
                return response;
            }
            
            X509Certificate clientCert = certificates[0];
            String clientDN = clientCert.getSubjectX500Principal().getName();
            String fingerprint = calculateFingerprint(clientCert);
            
            // Create or update client session
            ClientSession session = getOrCreateClientSession(clientDN, fingerprint, httpRequest);
            session.incrementAccessCount();
            clientSessionRepository.save(session);
            
            // Build response with certificate profile information
            response.put("success", true);
            response.put("authenticated", true);
            response.put("clientDN", clientDN);
            response.put("issuerDN", clientCert.getIssuerX500Principal().getName());
            response.put("serialNumber", clientCert.getSerialNumber().toString());
            response.put("validFrom", clientCert.getNotBefore().toString());
            response.put("validTo", clientCert.getNotAfter().toString());
            response.put("fingerprint", fingerprint);
            response.put("sessionId", session.getSessionId());
            response.put("accessCount", session.getAccessCount());
            response.put("firstAccess", session.getCreatedAt().toString());
            response.put("lastAccess", session.getLastAccessed().toString());
            
            // Certificate validation status
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validFrom = clientCert.getNotBefore().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime validTo = clientCert.getNotAfter().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            
            boolean isValid = now.isAfter(validFrom) && now.isBefore(validTo);
            boolean isExpired = now.isAfter(validTo);
            
            response.put("certificateValid", isValid);
            response.put("certificateExpired", isExpired);
            response.put("daysUntilExpiry", java.time.temporal.ChronoUnit.DAYS.between(now, validTo));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving certificate profile: " + e.getMessage());
        }
        
        return response;
    }
    
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    private X509Certificate[] extractClientCertificates(HttpServletRequest request) {
        if (request == null) return null;
        return (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    }
    
    private String calculateFingerprint(X509Certificate certificate) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(certificate.getEncoded());
        return java.util.Base64.getEncoder().encodeToString(digest);
    }
    
    private ClientSession getOrCreateClientSession(String clientDN, String fingerprint, HttpServletRequest request) {
        return clientSessionRepository.findByClientDNAndIsActiveTrue(clientDN)
            .orElseGet(() -> {
                ClientSession newSession = new ClientSession();
                newSession.setClientDN(clientDN);
                newSession.setCertificateFingerprint(fingerprint);
                newSession.setSessionId(UUID.randomUUID().toString());
                newSession.setClientIp(getClientIpAddress(request));
                newSession.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
                newSession.setIsActive(true);
                return newSession;
            });
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}