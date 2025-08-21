package blog.eric231.examples.mtlsauthrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"blog.eric231.examples.mtlsauthrest", "blog.eric231.framework"})
public class MtlsAuthRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtlsAuthRestApplication.class, args);
    }
}