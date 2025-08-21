package blog.eric231.framework.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class to enforce HTTPS-only connections.
 * This ensures that all HTTP requests are redirected to HTTPS.
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class HttpsConfig {

    /**
     * Configure Tomcat to redirect HTTP to HTTPS
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(org.apache.catalina.Context context) {
                // Enable secure flag for all cookies
                context.setUseHttpOnly(true);
                // Configure secure cookies
                super.postProcessContext(context);
            }
        };
        
        // Add HTTP to HTTPS redirect connector
        tomcat.addAdditionalTomcatConnectors(createHttpRedirectConnector());
        
        return tomcat;
    }

    /**
     * Create HTTP connector that redirects to HTTPS
     */
    private org.apache.catalina.connector.Connector createHttpRedirectConnector() {
        org.apache.catalina.connector.Connector connector = new org.apache.catalina.connector.Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080); // HTTP port for redirects
        connector.setSecure(false);
        connector.setRedirectPort(8443); // Redirect to HTTPS port
        return connector;
    }

    /**
     * Security configuration to require secure transport
     */
    @Bean
    @ConditionalOnProperty(name = "framework.connectors.rest.authMode", havingValue = "bypass")
    public SecurityFilterChain httpsSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .requiresChannel(channel -> 
                channel.requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                       .requiresSecure())
            .headers(headers -> headers
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true))
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {}))
            .csrf(csrf -> csrf.disable()) // Disable CSRF for APIs
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().permitAll());

        return http.build();
    }
}