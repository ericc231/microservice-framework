package blog.eric231.examples.mtlsprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"blog.eric231.examples.mtlsprovider", "blog.eric231.framework"})
public class MtlsProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtlsProviderApplication.class, args);
    }
}
