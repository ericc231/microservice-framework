package blog.eric231.examples.oidcauthrest.logic;

import blog.eric231.examples.oidcauthrest.domain.User;
import blog.eric231.examples.oidcauthrest.domain.UserRepository;
import blog.eric231.framework.application.usecase.DL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@DL("user-list-process")
@Component
public class UserListLogic {
    
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public UserListLogic(UserRepository userRepository) {
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
            
            List<User> users = userRepository.findByActiveTrue();
            ArrayNode userArray = objectMapper.createArrayNode();
            
            for (User user : users) {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("id", user.getId());
                userNode.put("username", user.getUsername());
                userNode.put("email", user.getEmail());
                userNode.put("fullName", user.getFullName());
                userNode.put("active", user.getActive());
                userNode.put("createdAt", user.getCreatedAt().toString());
                userNode.put("lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null);
                userArray.add(userNode);
            }
            
            response.put("success", true);
            response.put("count", users.size());
            response.set("users", userArray);
            response.put("message", "Successfully retrieved " + users.size() + " users");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving users: " + e.getMessage());
        }
        
        return response;
    }
}