package blog.eric231.examples.basicauthrest.api;

import blog.eric231.examples.basicauthrest.domain.AuthenticationDomainLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for Basic Authentication operations
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationDomainLogic authDomainLogic;
    
    /**
     * Get authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> status = authDomainLogic.getAuthenticationStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * Process authentication with message
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processAuth(@RequestParam(required = false) String message) {
        Map<String, Object> result = authDomainLogic.processAuthenticationRequest(message);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Check if user has specific role
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<Map<String, Object>> checkRole(@PathVariable String role) {
        boolean hasRole = authDomainLogic.hasRole(role);
        
        Map<String, Object> response = Map.of(
            "role", role,
            "hasRole", hasRole,
            "user", authDomainLogic.getCurrentUser()
        );
        
        return ResponseEntity.ok(response);
    }
}