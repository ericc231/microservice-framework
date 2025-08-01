package blog.eric231.examples.oidcprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"blog.eric231.examples.oidcprovider", "blog.eric231.framework"})
public class OidcProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OidcProviderApplication.class, args);
    }
}
