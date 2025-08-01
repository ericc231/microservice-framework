package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.infrastructure.security.SelfSignedCertificateGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "framework.security.self-signed-cert.enabled", havingValue = "true")
public class SecurityConfig {

    @PostConstruct
    public void generateCertificate() throws Exception {
        SelfSignedCertificateGenerator.generate("keystore.p12");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            );
        return http.build();
    }
}