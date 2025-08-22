package blog.eric231.examples.basicrestmqredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Basic REST-MQ-Redis example.
 * 
 * This example demonstrates a complex request-response pipeline:
 * 1. REST API with Basic Authentication receives requests
 * 2. First @DL processes request and sends message to RabbitMQ
 * 3. Second @DL consumes RabbitMQ message, stores data in Redis
 * 4. Second @DL sends reply back through RabbitMQ
 * 5. First @DL receives reply and returns response to REST client
 * 
 * Message flow: REST (Basic Auth) -> @DL1 -> MQ -> @DL2 -> Redis -> MQ Reply -> @DL1 -> Response
 */
@SpringBootApplication(scanBasePackages = {
    "blog.eric231.framework",
    "blog.eric231.examples.basicrestmqredis"
})
public class BasicRestMqRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicRestMqRedisApplication.class, args);
    }
}
