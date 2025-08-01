package blog.eric231.examples.basicauthprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"blog.eric231.examples.basicauthprovider", "blog.eric231.framework"})
public class BasicAuthProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicAuthProviderApplication.class, args);
    }
}
