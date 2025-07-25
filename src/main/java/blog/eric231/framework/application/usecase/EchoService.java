package blog.eric231.framework.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component("echo-service")
public class EchoService implements BusinessProcess {
    @Override
    public JsonNode handle(JsonNode request) {
        return request;
    }
}
