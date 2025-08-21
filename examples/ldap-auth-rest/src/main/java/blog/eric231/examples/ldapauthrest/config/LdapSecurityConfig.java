package blog.eric231.examples.ldapauthrest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for LDAP Authentication REST Service
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class LdapSecurityConfig {

    /**
     * LDAP Context Source Configuration
     */
    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource("ldap://localhost:8389/dc=springframework,dc=org");
    }

    /**
     * Security configuration for API endpoints with LDAP authentication
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/ldap/server/status").permitAll()
                .requestMatchers("/api/user/admin").hasAnyRole("ADMIN", "ADMINS")
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(ldapAuthenticationProvider());

        return http.build();
    }

    /**
     * Security configuration for web interface with LDAP form login
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/admin").hasAnyRole("ADMIN", "ADMINS")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .httpBasic(withDefaults())
            .authenticationProvider(ldapAuthenticationProvider());

        return http.build();
    }

    /**
     * LDAP Authentication Provider
     */
    @Bean
    public org.springframework.security.ldap.authentication.LdapAuthenticationProvider ldapAuthenticationProvider() {
        // Create LDAP authentication provider
        org.springframework.security.ldap.authentication.BindAuthenticator authenticator = 
            new org.springframework.security.ldap.authentication.BindAuthenticator(contextSource());
        
        // Set user DN patterns
        authenticator.setUserDnPatterns(new String[]{"uid={0},ou=people"});
        
        // Set group search configuration
        org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator authoritiesPopulator = 
            new org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator(contextSource(), "ou=groups");
        
        authoritiesPopulator.setGroupRoleAttribute("cn");
        authoritiesPopulator.setGroupSearchFilter("(member={0})");
        authoritiesPopulator.setRolePrefix("ROLE_");
        authoritiesPopulator.setConvertToUpperCase(true);
        
        // Create authentication provider with both authenticator and authorities populator
        org.springframework.security.ldap.authentication.LdapAuthenticationProvider provider = 
            new org.springframework.security.ldap.authentication.LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        
        return provider;
    }
}