package blog.eric231.examples.basicauthrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "blog.eric231.framework",
    "blog.eric231.examples.basicauthrest"
})
public class BasicAuthRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicAuthRestApplication.class, args);
    }
}