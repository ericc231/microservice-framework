package blog.eric231.examples.basicauthrestclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "client.target")
public class ClientProperties {
    private String baseUrl = "http://localhost:8081"; // basic-auth-rest default port
    private String endpointPath = "/api/basic-auth";  // endpoint to call (configurable)
    private String username = "user";                  // basic auth username
    private String password = "password";              // basic auth password

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
