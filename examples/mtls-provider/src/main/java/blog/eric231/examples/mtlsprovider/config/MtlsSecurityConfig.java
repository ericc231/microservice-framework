package blog.eric231.examples.mtlsprovider.config;

import blog.eric231.examples.mtlsprovider.service.CertificateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;

import java.security.cert.X509Certificate;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class MtlsSecurityConfig {

    private final CertificateService certificateService;

    public MtlsSecurityConfig(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/auth/mtls").permitAll()
                .requestMatchers("/api/certificates").hasRole("CLIENT")
                .anyRequest().authenticated()
            )
            .x509(x509 -> x509
                .userDetailsService(username -> {
                    // Create a user details object for the authenticated certificate
                    return new User(username, "", 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")));
                })
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**")
            )
            .headers(headers -> headers
                .frameOptions().disable()
            );

        return http.build();
    }
}