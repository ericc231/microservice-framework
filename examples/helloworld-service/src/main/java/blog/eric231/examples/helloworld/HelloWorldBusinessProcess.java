package blog.eric231.examples.helloworld;

import blog.eric231.framework.application.usecase.BP;
import blog.eric231.framework.application.usecase.BusinessProcess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

@BP("helloworld-process")
public class HelloWorldBusinessProcess implements BusinessProcess {

    @Override
    public JsonNode handle(JsonNode request) {
        return new TextNode("Hello from Business Process!");
    }
}
