package blog.eric231.framework.infrastructure.configuration;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpsConfig configuration class
 */
class HttpsConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(HttpsConfig.class);

    @Test
    @DisplayName("Should not load HttpsConfig when SSL is disabled")
    void testHttpsConfigNotLoadedWhenSslDisabled() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HttpsConfig.class);
                    assertThat(context).doesNotHaveBean("servletContainer");
                });
    }

    @Test
    @DisplayName("Should load HttpsConfig when SSL is enabled")
    void testHttpsConfigLoadedWhenSslEnabled() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                });
    }

    @Test
    @DisplayName("Should create ServletWebServerFactory with HTTPS configuration")
    void testServletContainerCreation() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("servletContainer");
                    
                    ServletWebServerFactory factory = context.getBean("servletContainer", ServletWebServerFactory.class);
                    assertNotNull(factory);
                    assertThat(factory).isInstanceOf(TomcatServletWebServerFactory.class);
                    
                    TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
                    
                    // Verify additional connectors are configured
                    assertThat(tomcatFactory.getAdditionalTomcatConnectors()).isNotEmpty();
                    assertEquals(1, tomcatFactory.getAdditionalTomcatConnectors().size());
                });
    }

    @Test
    @DisplayName("Should configure HTTP to HTTPS redirect connector properly")
    void testHttpRedirectConnectorConfiguration() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=true")
                .run(context -> {
                    ServletWebServerFactory factory = context.getBean("servletContainer", ServletWebServerFactory.class);
                    TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
                    
                    // Get the redirect connector
                    Connector[] connectors = tomcatFactory.getAdditionalTomcatConnectors().toArray(new Connector[0]);
                    assertEquals(1, connectors.length);
                    
                    Connector redirectConnector = connectors[0];
                    
                    // Verify connector configuration
                    assertEquals("http", redirectConnector.getScheme());
                    assertEquals(8080, redirectConnector.getPort());
                    assertEquals(8443, redirectConnector.getRedirectPort());
                    assertFalse(redirectConnector.getSecure());
                    assertEquals("org.apache.coyote.http11.Http11NioProtocol", redirectConnector.getProtocolHandler().getClass().getName());
                });
    }

    @Test
    @DisplayName("Should create SecurityFilterChain when auth mode is bypass")
    void testSecurityFilterChainCreation() {
        contextRunner
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    assertThat(context).hasBean("httpsSecurityFilterChain");
                    
                    SecurityFilterChain filterChain = context.getBean("httpsSecurityFilterChain", SecurityFilterChain.class);
                    assertNotNull(filterChain);
                });
    }

    @Test
    @DisplayName("Should not create SecurityFilterChain when auth mode is not bypass")
    void testSecurityFilterChainNotCreatedWhenAuthModeNotBypass() {
        contextRunner
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=ldap"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle missing auth mode property gracefully")
    void testMissingAuthModeProperty() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=true")
                .run(context -> {
                    // Should still load HttpsConfig but not the SecurityFilterChain
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should create HttpsConfig instance directly")
    void testHttpsConfigDirectInstantiation() {
        // Given & When
        HttpsConfig httpsConfig = new HttpsConfig();
        
        // Then
        assertNotNull(httpsConfig);
        
        // Test servlet container creation
        ServletWebServerFactory factory = httpsConfig.servletContainer();
        assertNotNull(factory);
        assertThat(factory).isInstanceOf(TomcatServletWebServerFactory.class);
        
        TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
        assertThat(tomcatFactory.getAdditionalTomcatConnectors()).hasSize(1);
    }

    @Test
    @DisplayName("Should configure security headers correctly")
    void testSecurityHeadersConfiguration() throws Exception {
        contextRunner
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    HttpsConfig httpsConfig = context.getBean(HttpsConfig.class);
                    assertNotNull(httpsConfig);
                    
                    // Test that security filter chain can be created
                    // Note: Full testing of security configuration would require MockMvc
                    assertDoesNotThrow(() -> {
                        HttpSecurity httpSecurity = context.getBean(HttpSecurity.class);
                        SecurityFilterChain chain = httpsConfig.httpsSecurityFilterChain(httpSecurity);
                        assertNotNull(chain);
                    });
                });
    }

    @Test
    @DisplayName("Should handle connector creation without exceptions")
    void testConnectorCreationStability() {
        // Given
        HttpsConfig httpsConfig = new HttpsConfig();

        // When & Then
        assertDoesNotThrow(() -> {
            ServletWebServerFactory factory = httpsConfig.servletContainer();
            TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
            
            // Verify that connectors can be retrieved without issues
            Collection<Connector> connectors = tomcatFactory.getAdditionalTomcatConnectors();
            assertNotNull(connectors);
            assertFalse(connectors.isEmpty());
        });
    }

    @Test
    @DisplayName("Should configure Tomcat context properly")
    void testTomcatContextConfiguration() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=true")
                .run(context -> {
                    ServletWebServerFactory factory = context.getBean("servletContainer", ServletWebServerFactory.class);
                    TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
                    
                    // The context post-processing is tested indirectly through factory creation
                    assertNotNull(tomcatFactory);
                    
                    // Verify the factory is configured properly for HTTPS
                    assertThat(tomcatFactory.getAdditionalTomcatConnectors()).isNotEmpty();
                });
    }

    @Test
    @DisplayName("Should maintain consistent port configuration")
    void testPortConsistency() {
        contextRunner
                .withPropertyValues("server.ssl.enabled=true")
                .run(context -> {
                    ServletWebServerFactory factory = context.getBean("servletContainer", ServletWebServerFactory.class);
                    TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
                    
                    Connector[] connectors = tomcatFactory.getAdditionalTomcatConnectors().toArray(new Connector[0]);
                    Connector redirectConnector = connectors[0];
                    
                    // Verify ports are consistent with expectations
                    assertEquals(8080, redirectConnector.getPort()); // HTTP port
                    assertEquals(8443, redirectConnector.getRedirectPort()); // HTTPS port
                });
    }

    @Test
    @DisplayName("Should handle conditional properties correctly")
    void testConditionalProperties() {
        // Test with SSL enabled but different auth modes
        contextRunner
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=oauth2"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });

        // Test with SSL enabled and bypass auth mode
        contextRunner
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    assertThat(context).hasBean("httpsSecurityFilterChain");
                });
    }
}