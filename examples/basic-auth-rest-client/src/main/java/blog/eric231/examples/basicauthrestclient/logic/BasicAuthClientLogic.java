package blog.eric231.examples.basicauthrestclient.logic;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import blog.eric231.examples.basicauthrestclient.config.ClientProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@DL("basic-auth-client.call")
@Component
public class BasicAuthClientLogic implements DomainLogic {

    private final ClientProperties properties;
    private final RestTemplate restTemplate;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public BasicAuthClientLogic(ClientProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public JsonNode handle(JsonNode input) {
        try {
            // Prepare URL
            String url = properties.getBaseUrl() + properties.getEndpointPath();

            // Prepare headers with Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String credentials = properties.getUsername() + ":" + properties.getPassword();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);

            // Forward the incoming payload (can be null)
            HttpEntity<JsonNode> request = new HttpEntity<>(input, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

            // Wrap response
            ObjectNode result = mapper.createObjectNode();
            result.put("status", response.getStatusCodeValue());
            result.set("upstream", response.getBody());
            result.put("target", url);
            return result;
        } catch (Exception e) {
            ObjectNode error = mapper.createObjectNode();
            error.put("error", "Failed to call basic-auth-rest");
            error.put("message", e.getMessage());
            return error;
        }
    }
}
