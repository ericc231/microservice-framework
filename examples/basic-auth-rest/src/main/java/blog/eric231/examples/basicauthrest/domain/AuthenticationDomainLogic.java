package blog.eric231.examples.basicauthrest.domain;

import blog.eric231.framework.application.usecase.DL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain logic for Basic Authentication operations
 * Handles user authentication information and session management
 */
@DL("basic-auth")
@Component
public class AuthenticationDomainLogic {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationDomainLogic.class);
    
    /**
     * Get current authenticated user information
     */
    public Map<String, Object> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return createGuestUserInfo();
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authenticated", true);
        userInfo.put("authType", "Basic");
        userInfo.put("loginTime", LocalDateTime.now());
        
        // Get authorities/roles
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toSet());
        userInfo.put("authorities", authorities);
        
        // Check if user has admin role
        boolean isAdmin = authorities.stream()
            .anyMatch(auth -> auth.contains("ADMIN"));
        userInfo.put("isAdmin", isAdmin);
        
        logger.debug("Retrieved user info for: {}", authentication.getName());
        return userInfo;
    }
    
    /**
     * Check if current user has specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().contains(role.toUpperCase()));
    }
    
    /**
     * Get authentication status
     */
    public Map<String, Object> getAuthenticationStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", authentication != null && authentication.isAuthenticated());
        status.put("authType", "Basic Authentication");
        status.put("timestamp", LocalDateTime.now());
        
        if (authentication != null && authentication.isAuthenticated()) {
            status.put("principal", authentication.getName());
            status.put("authorities", authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList()));
        }
        
        return status;
    }
    
    /**
     * Create guest user information
     */
    private Map<String, Object> createGuestUserInfo() {
        Map<String, Object> guestInfo = new HashMap<>();
        guestInfo.put("username", "guest");
        guestInfo.put("authenticated", false);
        guestInfo.put("authType", "None");
        guestInfo.put("authorities", Set.of());
        guestInfo.put("isAdmin", false);
        guestInfo.put("message", "Please login to access user information");
        
        return guestInfo;
    }
    
    /**
     * Process authentication request
     */
    public Map<String, Object> processAuthenticationRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> userInfo = getCurrentUser();
        
        response.put("message", message != null ? message : "Authentication processed");
        response.put("user", userInfo);
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "basic-auth-rest");
        
        logger.info("Processed authentication request for user: {}", 
            userInfo.get("username"));
        
        return response;
    }
}