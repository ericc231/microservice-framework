package blog.eric231.examples.oidcprovider.process;

import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.BP;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@BP("oidc-auth-process")
@Component
public class OidcAuthProcess implements BusinessProcess {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode handle(JsonNode request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        ObjectNode response = objectMapper.createObjectNode();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            response.put("authenticated", true);
            response.put("username", authentication.getName());
            response.put("issuer", "http://localhost:8083");
            response.put("authorization_endpoint", "http://localhost:8083/oauth2/authorize");
            response.put("token_endpoint", "http://localhost:8083/oauth2/token");
            response.put("userinfo_endpoint", "http://localhost:8083/userinfo");
            response.put("jwks_uri", "http://localhost:8083/oauth2/jwks");
            response.put("message", "OIDC Provider is ready for authentication");
            
        } else {
            response.put("authenticated", false);
            response.put("message", "User not authenticated. Please login first.");
            response.put("login_url", "http://localhost:8083/login");
        }
        
        return response;
    }
}