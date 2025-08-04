package blog.eric231.examples.basicauthrest.api;

import blog.eric231.examples.basicauthrest.domain.AuthenticationDomainLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for User operations
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private AuthenticationDomainLogic authDomainLogic;
    
    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> userInfo = authDomainLogic.getCurrentUser();
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * Admin only endpoint
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminInfo() {
        Map<String, Object> response = Map.of(
            "message", "Admin access granted",
            "user", authDomainLogic.getCurrentUser(),
            "adminFeatures", Map.of(
                "userManagement", true,
                "systemConfig", true,
                "auditLogs", true
            )
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Echo service with authentication
     */
    @GetMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestParam(defaultValue = "Hello from Basic Auth REST!") String message) {
        Map<String, Object> response = authDomainLogic.processAuthenticationRequest(message);
        return ResponseEntity.ok(response);
    }
}