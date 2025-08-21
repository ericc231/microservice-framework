package blog.eric231.examples.oidcauthrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"blog.eric231.examples.oidcauthrest", "blog.eric231.framework"})
public class OidcAuthRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(OidcAuthRestApplication.class, args);
    }
}