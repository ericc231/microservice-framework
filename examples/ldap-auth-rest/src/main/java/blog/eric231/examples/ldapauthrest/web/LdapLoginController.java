package blog.eric231.examples.ldapauthrest.web;

import blog.eric231.examples.ldapauthrest.domain.LdapAuthenticationDomainLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Web Controller for LDAP Authentication UI
 */
@Controller
public class LdapLoginController {
    
    @Autowired
    private LdapAuthenticationDomainLogic ldapDomainLogic;
    
    /**
     * LDAP Login page
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("ldapServer", ldapDomainLogic.getLdapServerStatus());
        return "ldap-login";
    }
    
    /**
     * Main dashboard after LDAP login
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> userInfo = ldapDomainLogic.getCurrentUser();
        Map<String, Object> serverStatus = ldapDomainLogic.getLdapServerStatus();
        
        model.addAttribute("user", userInfo);
        model.addAttribute("ldapServer", serverStatus);
        model.addAttribute("service", "LDAP Auth REST Service");
        
        return "ldap-dashboard";
    }
    
    /**
     * LDAP User profile page
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        Map<String, Object> userInfo = ldapDomainLogic.getCurrentUser();
        Map<String, Object> groups = ldapDomainLogic.getUserGroups();
        
        model.addAttribute("user", userInfo);
        model.addAttribute("groups", groups);
        model.addAttribute("ldapServer", ldapDomainLogic.getLdapServerStatus());
        
        return "ldap-profile";
    }
    
    /**
     * LDAP Admin page
     */
    @GetMapping("/admin")
    public String admin(Model model) {
        Map<String, Object> userInfo = ldapDomainLogic.getCurrentUser();
        
        if (ldapDomainLogic.hasRole("ADMIN") || ldapDomainLogic.hasRole("ADMINS")) {
            model.addAttribute("user", userInfo);
            model.addAttribute("ldapServer", ldapDomainLogic.getLdapServerStatus());
            model.addAttribute("adminFeatures", Map.of(
                "userManagement", "Manage LDAP users and groups",
                "serverConfig", "Configure LDAP server settings",
                "auditLogs", "View LDAP authentication logs",
                "groupSync", "Synchronize LDAP groups"
            ));
            return "ldap-admin";
        } else {
            model.addAttribute("user", userInfo);
            model.addAttribute("error", "Access denied. LDAP Admin role required.");
            return "ldap-dashboard";
        }
    }
}