package blog.eric231.examples.mtlsauthrest.logic;

import blog.eric231.examples.mtlsauthrest.domain.ClientSession;
import blog.eric231.examples.mtlsauthrest.domain.ClientSessionRepository;
import blog.eric231.framework.application.usecase.DL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;

@DL("client-info-process")
@Component
public class ClientInfoLogic {
    
    private final ClientSessionRepository clientSessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ClientInfoLogic(ClientSessionRepository clientSessionRepository) {
        this.clientSessionRepository = clientSessionRepository;
    }
    
    public JsonNode execute(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "mTLS authentication required");
                return response;
            }
            
            String clientDN = authentication.getName();
            X509Certificate[] certificates = extractClientCertificates(httpRequest);
            
            if (certificates == null || certificates.length == 0) {
                response.put("success", false);
                response.put("message", "No client certificate found");
                return response;
            }
            
            X509Certificate clientCert = certificates[0];
            
            // Get all client information
            ObjectNode clientInfo = buildClientInfo(clientDN, clientCert, httpRequest);
            ObjectNode sessionInfo = buildSessionInfo(clientDN);
            ObjectNode systemInfo = buildSystemInfo();
            
            response.put("success", true);
            response.put("message", "Client information retrieved successfully");
            response.set("client", clientInfo);
            response.set("sessions", sessionInfo);
            response.set("system", systemInfo);
            response.put("retrievedAt", LocalDateTime.now().toString());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving client information: " + e.getMessage());
        }
        
        return response;
    }
    
    private ObjectNode buildClientInfo(String clientDN, X509Certificate certificate, HttpServletRequest request) {
        ObjectNode clientInfo = objectMapper.createObjectNode();
        
        try {
            // Certificate information
            clientInfo.put("distinguishedName", clientDN);
            clientInfo.put("issuerDN", certificate.getIssuerX500Principal().getName());
            clientInfo.put("serialNumber", certificate.getSerialNumber().toString());
            clientInfo.put("validFrom", certificate.getNotBefore().toString());
            clientInfo.put("validTo", certificate.getNotAfter().toString());
            
            // Certificate validation status
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime validFrom = certificate.getNotBefore().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime validTo = certificate.getNotAfter().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            
            clientInfo.put("isValid", now.isAfter(validFrom) && now.isBefore(validTo));
            clientInfo.put("isExpired", now.isAfter(validTo));
            clientInfo.put("daysUntilExpiry", java.time.temporal.ChronoUnit.DAYS.between(now, validTo));
            
            // Certificate fingerprint
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(certificate.getEncoded());
            String fingerprint = java.util.Base64.getEncoder().encodeToString(digest);
            clientInfo.put("fingerprint", fingerprint);
            
            // Key information
            clientInfo.put("keyAlgorithm", certificate.getPublicKey().getAlgorithm());
            clientInfo.put("signatureAlgorithm", certificate.getSigAlgName());
            
            // Key usage information
            boolean[] keyUsage = certificate.getKeyUsage();
            if (keyUsage != null) {
                ArrayNode keyUsageArray = objectMapper.createArrayNode();
                String[] usageNames = {"digitalSignature", "nonRepudiation", "keyEncipherment", 
                                     "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign"};
                for (int i = 0; i < Math.min(keyUsage.length, usageNames.length); i++) {
                    if (keyUsage[i]) {
                        keyUsageArray.add(usageNames[i]);
                    }
                }
                clientInfo.set("keyUsage", keyUsageArray);
            }
            
            // Request information
            if (request != null) {
                ObjectNode requestInfo = objectMapper.createObjectNode();
                requestInfo.put("remoteAddr", request.getRemoteAddr());
                requestInfo.put("remoteHost", request.getRemoteHost());
                requestInfo.put("remotePort", request.getRemotePort());
                requestInfo.put("userAgent", request.getHeader("User-Agent"));
                requestInfo.put("acceptLanguage", request.getHeader("Accept-Language"));
                requestInfo.put("acceptEncoding", request.getHeader("Accept-Encoding"));
                clientInfo.set("request", requestInfo);
            }
            
        } catch (Exception e) {
            clientInfo.put("error", "Error building client info: " + e.getMessage());
        }
        
        return clientInfo;
    }
    
    private ObjectNode buildSessionInfo(String clientDN) {
        ObjectNode sessionInfo = objectMapper.createObjectNode();
        
        try {
            // Current active session
            ClientSession currentSession = clientSessionRepository.findByClientDNAndIsActiveTrue(clientDN)
                .orElse(null);
            
            if (currentSession != null) {
                ObjectNode currentSessionInfo = objectMapper.createObjectNode();
                currentSessionInfo.put("sessionId", currentSession.getSessionId());
                currentSessionInfo.put("accessCount", currentSession.getAccessCount());
                currentSessionInfo.put("createdAt", currentSession.getCreatedAt().toString());
                currentSessionInfo.put("lastAccessed", currentSession.getLastAccessed().toString());
                currentSessionInfo.put("clientIp", currentSession.getClientIp());
                sessionInfo.set("current", currentSessionInfo);
            }
            
            // Session history
            List<ClientSession> allSessions = clientSessionRepository.findByClientDNOrderByCreatedAtDesc(clientDN);
            ArrayNode historyArray = objectMapper.createArrayNode();
            
            for (ClientSession session : allSessions) {
                ObjectNode sessionNode = objectMapper.createObjectNode();
                sessionNode.put("sessionId", session.getSessionId());
                sessionNode.put("accessCount", session.getAccessCount());
                sessionNode.put("createdAt", session.getCreatedAt().toString());
                sessionNode.put("lastAccessed", session.getLastAccessed().toString());
                sessionNode.put("isActive", session.getIsActive());
                sessionNode.put("clientIp", session.getClientIp());
                historyArray.add(sessionNode);
            }
            
            sessionInfo.set("history", historyArray);
            sessionInfo.put("totalSessions", allSessions.size());
            sessionInfo.put("activeSessions", allSessions.stream().mapToInt(s -> s.getIsActive() ? 1 : 0).sum());
            
        } catch (Exception e) {
            sessionInfo.put("error", "Error building session info: " + e.getMessage());
        }
        
        return sessionInfo;
    }
    
    private ObjectNode buildSystemInfo() {
        ObjectNode systemInfo = objectMapper.createObjectNode();
        
        try {
            // System statistics
            long totalActiveSessions = clientSessionRepository.countActiveSessions();
            List<ClientSession> recentSessions = clientSessionRepository.findRecentActiveSessions(
                LocalDateTime.now().minusHours(24));
            
            systemInfo.put("totalActiveSessions", totalActiveSessions);
            systemInfo.put("recentSessions24h", recentSessions.size());
            systemInfo.put("authenticationMethod", "mTLS (Mutual TLS)");
            systemInfo.put("securityLevel", "HIGH");
            systemInfo.put("serverTime", LocalDateTime.now().toString());
            
            // Server capabilities
            ArrayNode capabilities = objectMapper.createArrayNode();
            capabilities.add("X.509 Certificate Authentication");
            capabilities.add("Session Management");
            capabilities.add("Certificate Validation");
            capabilities.add("Secure Data Access");
            capabilities.add("Client Activity Tracking");
            systemInfo.set("capabilities", capabilities);
            
        } catch (Exception e) {
            systemInfo.put("error", "Error building system info: " + e.getMessage());
        }
        
        return systemInfo;
    }
    
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    private X509Certificate[] extractClientCertificates(HttpServletRequest request) {
        if (request == null) return null;
        return (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    }
}