package blog.eric231.examples.ldapauthrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "blog.eric231.framework",
    "blog.eric231.examples.ldapauthrest"
})
public class LdapAuthRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LdapAuthRestApplication.class, args);
    }
}