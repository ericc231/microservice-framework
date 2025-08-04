package blog.eric231.examples.ldapauthrest.api;

import blog.eric231.examples.ldapauthrest.domain.LdapAuthenticationDomainLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for LDAP Authentication operations
 */
@RestController
@RequestMapping("/api/ldap")
public class LdapAuthController {
    
    @Autowired
    private LdapAuthenticationDomainLogic ldapDomainLogic;
    
    /**
     * Get LDAP server status
     */
    @GetMapping("/server/status")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        Map<String, Object> status = ldapDomainLogic.getLdapServerStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * Process LDAP authentication with message
     */
    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestParam(required = false) String message) {
        Map<String, Object> result = ldapDomainLogic.processLdapAuthentication(message);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get user's LDAP groups and roles
     */
    @GetMapping("/user/groups")
    public ResponseEntity<Map<String, Object>> getUserGroups() {
        Map<String, Object> groups = ldapDomainLogic.getUserGroups();
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Check if user has specific role or group
     */
    @GetMapping("/user/role/{role}")
    public ResponseEntity<Map<String, Object>> checkRole(@PathVariable String role) {
        boolean hasRole = ldapDomainLogic.hasRole(role);
        
        Map<String, Object> response = Map.of(
            "role", role,
            "hasRole", hasRole,
            "user", ldapDomainLogic.getCurrentUser(),
            "ldapServer", ldapDomainLogic.getLdapServerStatus()
        );
        
        return ResponseEntity.ok(response);
    }
}