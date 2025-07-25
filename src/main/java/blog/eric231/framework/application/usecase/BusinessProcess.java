package blog.eric231.framework.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;

public interface BusinessProcess {
    JsonNode handle(JsonNode request);
}
