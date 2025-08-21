import blog.eric231.framework.infrastructure.configuration.FrameworkProperties;
import blog.eric231.framework.infrastructure.security.SelfSignedCertificateGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "framework.security.self-signed-cert.enabled", havingValue = "true")
public class SecurityConfig {

    private final FrameworkProperties frameworkProperties;
    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    @Autowired
    public SecurityConfig(FrameworkProperties frameworkProperties, LdapAuthenticationProvider ldapAuthenticationProvider) {
        this.frameworkProperties = frameworkProperties;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
    }

    @PostConstruct
    public void generateCertificate() throws Exception {
        SelfSignedCertificateGenerator.generate("keystore.p12");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String authMode = frameworkProperties.getConnectors().getRest().getAuthMode();

        http.csrf(csrf -> csrf.disable());

        switch (authMode.toLowerCase()) {
            case "bypass":
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
                break;
            case "basic":
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
                break;
            case "ldap":
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .authenticationProvider(ldapAuthenticationProvider)
                    .httpBasic(Customizer.withDefaults());
                break;
            case "oidc":
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwkSetUri("http://localhost:8083/oauth2/jwks"))
                    )
                    .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                    );
                break;
            case "mtls":
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .x509(Customizer.withDefaults());
                break;
            default:
                throw new IllegalArgumentException("Unknown authentication mode: " + authMode);
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "framework.connectors.rest.authMode", havingValue = "basic")
    public UserDetailsService inMemoryUserDetailsService(PasswordEncoder passwordEncoder) {
        // For basic authentication example
        UserDetails user = User.withUsername("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    @ConditionalOnProperty(name = "framework.connectors.rest.authMode", havingValue = "basic")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(name = "framework.connectors.rest.authMode", havingValue = "ldap")
    public LdapContextSource contextSource() {
        LdapContextSource ldapContextSource = new LdapContextSource();
        ldapContextSource.setUrl("ldap://localhost:8389"); // Embedded LDAP server
        ldapContextSource.setBase("dc=springframework,dc=org");
        ldapContextSource.setUserDn("uid=admin,ou=people,dc=springframework,dc=org");
        ldapContextSource.setPassword("admin");
        return ldapContextSource;
    }

    @Bean
    @ConditionalOnProperty(name = "framework.connectors.rest.authMode", havingValue = "ldap")
    public LdapAuthenticationProvider ldapAuthenticationProvider(LdapContextSource contextSource) {
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource);
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);
        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator);
        provider.setUserDetailsContextMapper(new LdapUserDetailsMapper());
        return provider;
    }
}