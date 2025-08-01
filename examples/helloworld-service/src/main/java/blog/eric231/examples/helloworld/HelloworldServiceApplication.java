package blog.eric231.examples.helloworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"blog.eric231.examples.helloworld", "blog.eric231.framework"})
public class HelloworldServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloworldServiceApplication.class, args);
    }
}
