package blog.eric231.examples.oidcauthrest.logic;

import blog.eric231.examples.oidcauthrest.domain.User;
import blog.eric231.examples.oidcauthrest.domain.UserRepository;
import blog.eric231.framework.application.usecase.DL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@DL("user-profile-process")
@Component
public class UserProfileLogic {
    
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public UserProfileLogic(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public JsonNode execute(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return response;
            }
            
            String username = extractUsername(authentication);
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            User user;
            if (userOpt.isPresent()) {
                user = userOpt.get();
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            } else {
                user = createUserFromAuthentication(authentication, username);
            }
            
            response.put("success", true);
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("active", user.getActive());
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving user profile: " + e.getMessage());
        }
        
        return response;
    }
    
    private String extractUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("preferred_username");
        }
        return authentication.getName();
    }
    
    private User createUserFromAuthentication(Authentication authentication, String username) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(username + "@springframework.org");
        newUser.setFullName(username);
        newUser.setActive(true);
        newUser.setLastLogin(LocalDateTime.now());
        
        return userRepository.save(newUser);
    }
}