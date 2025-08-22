package blog.eric231.examples.basicrestmqredis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the basic-rest-mq-redis example.
 * 
 * This configuration sets up Basic Authentication for REST endpoints,
 * allowing only authenticated users to access the processing endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Configure HTTP security with Basic Authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring HTTP Security with Basic Authentication");
        
        http
            // Disable CSRF for REST APIs
            .csrf(csrf -> csrf.disable())
            
            // Configure session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Allow health check endpoints without authentication
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // Allow access to API documentation (if enabled)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Require authentication for all API endpoints
                .requestMatchers("/api/**").authenticated()
                
                // Require authentication for processing endpoints
                .requestMatchers("/process/**").authenticated()
                
                // Allow all other requests (for development)
                .anyRequest().permitAll()
            )
            
            // Configure Basic Authentication
            .httpBasic(basic -> {
                basic.realmName("Basic Rest MQ Redis API");
            })
            
            // Configure error handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.warn("Authentication failed for request: {} - {}", 
                               request.getRequestURI(), authException.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Authentication required\",\"message\":\"" + 
                        authException.getMessage() + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    logger.warn("Access denied for request: {} - {}", 
                               request.getRequestURI(), accessDeniedException.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Access denied\",\"message\":\"" + 
                        accessDeniedException.getMessage() + "\"}"
                    );
                })
            );

        logger.info("HTTP Security configured successfully");
        return http.build();
    }

    /**
     * Configure password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure in-memory user details service with test users
     */
    @Bean
    public UserDetailsService userDetailsService() {
        logger.info("Configuring in-memory user details service");
        
        // Create test users with different roles
        UserDetails adminUser = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN", "USER")
            .build();

        UserDetails regularUser = User.builder()
            .username("user")
            .password(passwordEncoder().encode("user123"))
            .roles("USER")
            .build();

        UserDetails serviceUser = User.builder()
            .username("service")
            .password(passwordEncoder().encode("service123"))
            .roles("SERVICE")
            .build();

        logger.info("Configured users: admin (ADMIN,USER), user (USER), service (SERVICE)");
        
        return new InMemoryUserDetailsManager(adminUser, regularUser, serviceUser);
    }
}
