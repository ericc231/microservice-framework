package blog.eric231.examples.oidcprovider.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserInfoController {

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", authentication.getName());
        userInfo.put("preferred_username", authentication.getName());
        userInfo.put("name", authentication.getName());
        userInfo.put("given_name", authentication.getName());
        userInfo.put("family_name", "User");
        userInfo.put("email", authentication.getName() + "@springframework.org");
        userInfo.put("email_verified", true);
        
        return userInfo;
    }
}