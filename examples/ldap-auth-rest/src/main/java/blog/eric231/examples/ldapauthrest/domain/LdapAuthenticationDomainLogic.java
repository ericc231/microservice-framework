package blog.eric231.examples.ldapauthrest.domain;

import blog.eric231.framework.application.usecase.DL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain logic for LDAP Authentication operations
 * Handles LDAP user information, DN extraction, and group membership
 */
@DL("ldap-auth")
@Component
public class LdapAuthenticationDomainLogic {
    
    private static final Logger logger = LoggerFactory.getLogger(LdapAuthenticationDomainLogic.class);
    
    /**
     * Get current authenticated LDAP user information
     */
    public Map<String, Object> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return createGuestUserInfo();
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authenticated", true);
        userInfo.put("authType", "LDAP");
        userInfo.put("loginTime", LocalDateTime.now());
        
        // Extract LDAP-specific information
        if (authentication.getPrincipal() instanceof LdapUserDetails) {
            LdapUserDetails ldapUser = (LdapUserDetails) authentication.getPrincipal();
            userInfo.put("dn", ldapUser.getDn());
            userInfo.put("ldapServer", extractLdapServer(ldapUser.getDn()));
            
            // Extract additional LDAP attributes if available
            Map<String, Object> ldapAttributes = extractLdapAttributes(ldapUser);
            userInfo.putAll(ldapAttributes);
        }
        
        // Get authorities/roles
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toSet());
        userInfo.put("authorities", authorities);
        userInfo.put("groups", extractGroups(authorities));
        
        // Check admin privileges
        boolean isAdmin = hasAdminRole(authorities);
        userInfo.put("isAdmin", isAdmin);
        
        logger.debug("Retrieved LDAP user info for: {}", authentication.getName());
        return userInfo;
    }
    
    /**
     * Check if current user has specific role or group membership
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String roleToCheck = role.toUpperCase();
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> {
                String authority = auth.getAuthority().toUpperCase();
                return authority.contains(roleToCheck) || 
                       authority.contains("ROLE_" + roleToCheck) ||
                       authority.contains("GROUP_" + roleToCheck);
            });
    }
    
    /**
     * Get LDAP server connection status
     */
    public Map<String, Object> getLdapServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("serverUrl", "ldap://localhost:8389");
        status.put("baseDn", "dc=springframework,dc=org");
        status.put("userDnPattern", "uid={0},ou=people,dc=springframework,dc=org");
        status.put("groupSearchBase", "ou=groups,dc=springframework,dc=org");
        status.put("timestamp", LocalDateTime.now());
        
        // Check if LDAP server is accessible (simplified check)
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean connected = auth != null && auth.isAuthenticated();
            status.put("connected", connected);
            status.put("status", connected ? "Available" : "Disconnected");
        } catch (Exception e) {
            status.put("connected", false);
            status.put("status", "Error: " + e.getMessage());
        }
        
        return status;
    }
    
    /**
     * Process LDAP authentication request
     */
    public Map<String, Object> processLdapAuthentication(String message) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> userInfo = getCurrentUser();
        
        response.put("message", message != null ? message : "LDAP authentication processed");
        response.put("user", userInfo);
        response.put("ldapServer", getLdapServerStatus());
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "ldap-auth-rest");
        
        logger.info("Processed LDAP authentication request for user: {}", 
            userInfo.get("username"));
        
        return response;
    }
    
    /**
     * Get user's LDAP groups and roles
     */
    public Map<String, Object> getUserGroups() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Map.of("groups", Collections.emptyList(), "roles", Collections.emptyList());
        }
        
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toSet());
        
        List<String> groups = extractGroups(authorities);
        List<String> roles = authorities.stream()
            .filter(auth -> auth.startsWith("ROLE_"))
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groups);
        result.put("roles", roles);
        result.put("allAuthorities", authorities);
        result.put("username", authentication.getName());
        
        return result;
    }
    
    // Helper methods
    
    private Map<String, Object> createGuestUserInfo() {
        Map<String, Object> guestInfo = new HashMap<>();
        guestInfo.put("username", "guest");
        guestInfo.put("authenticated", false);
        guestInfo.put("authType", "None");
        guestInfo.put("authorities", Set.of());
        guestInfo.put("groups", List.of());
        guestInfo.put("isAdmin", false);
        guestInfo.put("message", "Please login with LDAP credentials");
        
        return guestInfo;
    }
    
    private Map<String, Object> extractLdapAttributes(LdapUserDetails ldapUser) {
        Map<String, Object> attributes = new HashMap<>();
        
        // Extract common LDAP attributes (simplified)
        String dn = ldapUser.getDn();
        if (dn != null) {
            attributes.put("organizationalUnit", extractOUFromDn(dn));
            attributes.put("domain", extractDomainFromDn(dn));
        }
        
        return attributes;
    }
    
    private String extractLdapServer(String dn) {
        if (dn == null) return "Unknown";
        
        // Extract domain components to determine LDAP server
        if (dn.contains("dc=springframework,dc=org")) {
            return "springframework.org";
        }
        
        return "localhost:8389";
    }
    
    private String extractOUFromDn(String dn) {
        if (dn == null) return "Unknown";
        
        if (dn.contains("ou=people")) return "people";
        if (dn.contains("ou=groups")) return "groups";
        
        return "Unknown";
    }
    
    private String extractDomainFromDn(String dn) {
        if (dn == null) return "Unknown";
        
        if (dn.contains("dc=springframework,dc=org")) {
            return "springframework.org";
        }
        
        return "localhost";
    }
    
    private List<String> extractGroups(Set<String> authorities) {
        return authorities.stream()
            .filter(auth -> auth.startsWith("ROLE_") && !auth.equals("ROLE_USER"))
            .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
            .collect(Collectors.toList());
    }
    
    private boolean hasAdminRole(Set<String> authorities) {
        return authorities.stream()
            .anyMatch(auth -> auth.toUpperCase().contains("ADMIN"));
    }
}