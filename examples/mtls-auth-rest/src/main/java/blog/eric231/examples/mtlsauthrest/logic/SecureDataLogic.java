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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@DL("secure-data-process")
@Component
public class SecureDataLogic {
    
    private final ClientSessionRepository clientSessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();
    
    public SecureDataLogic(ClientSessionRepository clientSessionRepository) {
        this.clientSessionRepository = clientSessionRepository;
    }
    
    public JsonNode execute(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "mTLS authentication required for secure data access");
                return response;
            }
            
            String clientDN = authentication.getName();
            
            // Find client session
            ClientSession session = clientSessionRepository.findByClientDNAndIsActiveTrue(clientDN)
                .orElse(null);
            
            if (session == null) {
                response.put("success", false);
                response.put("message", "No active session found for client");
                return response;
            }
            
            // Update session access
            session.incrementAccessCount();
            clientSessionRepository.save(session);
            
            // Generate secure data based on request parameters
            String dataType = request.has("dataType") ? request.get("dataType").asText() : "summary";
            
            switch (dataType.toLowerCase()) {
                case "encryption-key":
                    return generateEncryptionKey(response, session);
                case "session-stats":
                    return getSessionStatistics(response, session);
                case "secure-token":
                    return generateSecureToken(response, session);
                case "client-activity":
                    return getClientActivity(response, clientDN);
                default:
                    return getSecureDataSummary(response, session);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error accessing secure data: " + e.getMessage());
        }
        
        return response;
    }
    
    private JsonNode generateEncryptionKey(ObjectNode response, ClientSession session) {
        // Generate a secure encryption key for the client
        byte[] keyBytes = new byte[32]; // 256-bit key
        secureRandom.nextBytes(keyBytes);
        String encryptionKey = Base64.getEncoder().encodeToString(keyBytes);
        
        response.put("success", true);
        response.put("dataType", "encryption-key");
        response.put("encryptionKey", encryptionKey);
        response.put("keyLength", 256);
        response.put("algorithm", "AES-256");
        response.put("generatedFor", session.getClientDN());
        response.put("generatedAt", LocalDateTime.now().toString());
        response.put("usage", "Client-specific encryption operations");
        
        return response;
    }
    
    private JsonNode getSessionStatistics(ObjectNode response, ClientSession session) {
        long totalActiveSessions = clientSessionRepository.countActiveSessions();
        List<ClientSession> recentSessions = clientSessionRepository.findRecentActiveSessions(
            LocalDateTime.now().minusHours(24));
        
        response.put("success", true);
        response.put("dataType", "session-stats");
        response.put("currentSession", createSessionInfo(session));
        response.put("totalActiveSessions", totalActiveSessions);
        response.put("recentSessions24h", recentSessions.size());
        response.put("sessionDuration", calculateSessionDuration(session));
        response.put("averageAccessInterval", calculateAverageAccessInterval(session));
        
        return response;
    }
    
    private JsonNode generateSecureToken(ObjectNode response, ClientSession session) {
        // Generate a secure token for API access
        byte[] tokenBytes = new byte[24];
        secureRandom.nextBytes(tokenBytes);
        String secureToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        response.put("success", true);
        response.put("dataType", "secure-token");
        response.put("token", secureToken);
        response.put("tokenType", "Bearer");
        response.put("issuedTo", session.getClientDN());
        response.put("issuedAt", LocalDateTime.now().toString());
        response.put("expiresAt", LocalDateTime.now().plusHours(1).toString());
        response.put("usage", "Temporary API access token");
        
        return response;
    }
    
    private JsonNode getClientActivity(ObjectNode response, String clientDN) {
        List<ClientSession> clientSessions = clientSessionRepository.findByClientDNOrderByCreatedAtDesc(clientDN);
        
        ArrayNode activityArray = objectMapper.createArrayNode();
        for (ClientSession session : clientSessions) {
            ObjectNode activityNode = createSessionInfo(session);
            activityArray.add(activityNode);
        }
        
        response.put("success", true);
        response.put("dataType", "client-activity");
        response.put("clientDN", clientDN);
        response.put("totalSessions", clientSessions.size());
        response.set("sessionHistory", activityArray);
        
        return response;
    }
    
    private JsonNode getSecureDataSummary(ObjectNode response, ClientSession session) {
        response.put("success", true);
        response.put("dataType", "summary");
        response.put("message", "Secure data access granted via mTLS authentication");
        response.put("accessLevel", "HIGH_SECURITY");
        response.put("authenticatedClient", session.getClientDN());
        response.put("sessionInfo", createSessionInfo(session));
        response.put("serverTime", LocalDateTime.now().toString());
        response.put("securityContext", "mTLS Certificate-based Authentication");
        
        // Add available data types
        ArrayNode availableTypes = objectMapper.createArrayNode();
        availableTypes.add("encryption-key");
        availableTypes.add("session-stats");
        availableTypes.add("secure-token");
        availableTypes.add("client-activity");
        response.set("availableDataTypes", availableTypes);
        
        return response;
    }
    
    private ObjectNode createSessionInfo(ClientSession session) {
        ObjectNode sessionInfo = objectMapper.createObjectNode();
        sessionInfo.put("sessionId", session.getSessionId());
        sessionInfo.put("clientDN", session.getClientDN());
        sessionInfo.put("accessCount", session.getAccessCount());
        sessionInfo.put("createdAt", session.getCreatedAt().toString());
        sessionInfo.put("lastAccessed", session.getLastAccessed().toString());
        sessionInfo.put("isActive", session.getIsActive());
        sessionInfo.put("clientIp", session.getClientIp());
        return sessionInfo;
    }
    
    private String calculateSessionDuration(ClientSession session) {
        LocalDateTime start = session.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.temporal.ChronoUnit.HOURS.between(start, now);
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(start, now) % 60;
        return String.format("%d hours, %d minutes", hours, minutes);
    }
    
    private String calculateAverageAccessInterval(ClientSession session) {
        if (session.getAccessCount() <= 1) {
            return "N/A";
        }
        
        LocalDateTime start = session.getCreatedAt();
        LocalDateTime last = session.getLastAccessed();
        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(start, last);
        long avgMinutes = totalMinutes / (session.getAccessCount() - 1);
        
        return String.format("%d minutes", avgMinutes);
    }
}