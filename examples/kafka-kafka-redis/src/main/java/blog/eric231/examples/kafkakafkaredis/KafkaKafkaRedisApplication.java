package blog.eric231.examples.kafkakafkaredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Kafka-Kafka-Redis example.
 * 
 * This example demonstrates a message pipeline where:
 * 1. Original Kafka messages are received and processed by first @DL component
 * 2. Processed messages are sent to another Kafka topic
 * 3. The intermediate Kafka messages are consumed by second @DL component  
 * 4. Final processed messages are stored in Redis
 * 
 * Message flow: Kafka Topic A -> @DL -> Kafka Topic B -> @DL -> Redis
 */
@SpringBootApplication(scanBasePackages = {
    "blog.eric231.framework",
    "blog.eric231.examples.kafkakafkaredis"
})
public class KafkaKafkaRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaKafkaRedisApplication.class, args);
    }
}
