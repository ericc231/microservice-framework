package blog.eric231.examples.basicauthrest.web;

import blog.eric231.examples.basicauthrest.domain.AuthenticationDomainLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Web Controller for Basic Authentication UI
 */
@Controller
public class LoginController {
    
    @Autowired
    private AuthenticationDomainLogic authDomainLogic;
    
    /**
     * Login page
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    /**
     * Main dashboard after login
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> userInfo = authDomainLogic.getCurrentUser();
        model.addAttribute("user", userInfo);
        model.addAttribute("service", "Basic Auth REST Service");
        return "dashboard";
    }
    
    /**
     * User profile page
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        Map<String, Object> userInfo = authDomainLogic.getCurrentUser();
        model.addAttribute("user", userInfo);
        return "profile";
    }
    
    /**
     * Admin page
     */
    @GetMapping("/admin")
    public String admin(Model model) {
        Map<String, Object> userInfo = authDomainLogic.getCurrentUser();
        model.addAttribute("user", userInfo);
        
        if (authDomainLogic.hasRole("ADMIN")) {
            model.addAttribute("adminFeatures", Map.of(
                "userManagement", "Manage system users",
                "systemConfig", "Configure system settings",
                "auditLogs", "View system audit logs"
            ));
            return "admin";
        } else {
            model.addAttribute("error", "Access denied. Admin role required.");
            return "dashboard";
        }
    }
}