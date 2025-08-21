package blog.eric231.examples.oidcauthrest.logic;

import blog.eric231.examples.oidcauthrest.domain.User;
import blog.eric231.examples.oidcauthrest.domain.UserRepository;
import blog.eric231.framework.application.usecase.DL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@DL("create-user-process")
@Component
public class CreateUserLogic {
    
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public CreateUserLogic(UserRepository userRepository) {
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
            
            if (!request.has("username") || request.get("username").asText().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return response;
            }
            
            String username = request.get("username").asText().trim();
            String email = request.has("email") ? request.get("email").asText().trim() : username + "@springframework.org";
            String fullName = request.has("fullName") ? request.get("fullName").asText().trim() : username;
            
            if (userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return response;
            }
            
            if (userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return response;
            }
            
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setActive(true);
            
            User savedUser = userRepository.save(newUser);
            
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("id", savedUser.getId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("fullName", savedUser.getFullName());
            response.put("createdAt", savedUser.getCreatedAt().toString());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating user: " + e.getMessage());
        }
        
        return response;
    }
}