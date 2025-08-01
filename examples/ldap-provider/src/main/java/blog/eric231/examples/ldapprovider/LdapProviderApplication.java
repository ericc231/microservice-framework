package blog.eric231.examples.ldapprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapAutoConfiguration.class})
@ComponentScan(basePackages = {"blog.eric231.examples.ldapprovider", "blog.eric231.framework"})
public class LdapProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(LdapProviderApplication.class, args);
    }
}
