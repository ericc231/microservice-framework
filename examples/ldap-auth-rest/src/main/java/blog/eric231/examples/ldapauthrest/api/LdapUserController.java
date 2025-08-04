package blog.eric231.examples.ldapauthrest.api;

import blog.eric231.examples.ldapauthrest.domain.LdapAuthenticationDomainLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for LDAP User operations
 */
@RestController
@RequestMapping("/api/user")
public class LdapUserController {
    
    @Autowired
    private LdapAuthenticationDomainLogic ldapDomainLogic;
    
    /**
     * Get current LDAP user information
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> userInfo = ldapDomainLogic.getCurrentUser();
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * Admin only endpoint for LDAP users
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ADMINS')")
    public ResponseEntity<Map<String, Object>> getAdminInfo() {
        Map<String, Object> response = Map.of(
            "message", "LDAP Admin access granted",
            "user", ldapDomainLogic.getCurrentUser(),
            "ldapServer", ldapDomainLogic.getLdapServerStatus(),
            "adminFeatures", Map.of(
                "userManagement", "Manage LDAP users",
                "groupManagement", "Manage LDAP groups",
                "serverConfig", "Configure LDAP server settings",
                "auditLogs", "View LDAP audit logs"
            )
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Echo service with LDAP authentication
     */
    @GetMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestParam(defaultValue = "Hello from LDAP Auth REST!") String message) {
        Map<String, Object> response = ldapDomainLogic.processLdapAuthentication(message);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user's DN and LDAP attributes
     */
    @GetMapping("/attributes")
    public ResponseEntity<Map<String, Object>> getUserAttributes() {
        Map<String, Object> userInfo = ldapDomainLogic.getCurrentUser();
        Map<String, Object> groups = ldapDomainLogic.getUserGroups();
        
        Map<String, Object> response = Map.of(
            "userAttributes", userInfo,
            "groupMembership", groups,
            "ldapServer", ldapDomainLogic.getLdapServerStatus()
        );
        
        return ResponseEntity.ok(response);
    }
}