package blog.eric231.framework.infrastructure.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContextException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for security configuration error scenarios and edge cases
 */
@DisplayName("Security Configuration Error Tests")
class SecurityConfigurationErrorTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();

    @Test
    @DisplayName("Should handle missing SSL configuration gracefully")
    void testMissingSslConfiguration() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        // Missing required SSL properties
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    // Should still load without SSL keystore (Spring Boot handles this)
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                });
    }

    @Test
    @DisplayName("Should handle invalid SSL configuration")
    void testInvalidSslConfiguration() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "server.ssl.key-store=classpath:nonexistent.p12",
                        "server.ssl.key-store-password=wrongpassword",
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    // Context should still load, SSL validation happens at runtime
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                });
    }

    @Test
    @DisplayName("Should handle conflicting port configurations")
    void testConflictingPortConfiguration() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "server.port=8080", // Same as HTTP redirect port
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    // Should load but might cause port conflicts at runtime
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                });
    }

    @Test
    @DisplayName("Should handle invalid auth mode configurations")
    void testInvalidAuthModeConfiguration() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=invalid-mode"
                )
                .run(context -> {
                    // Should load but SecurityFilterChain won't be created
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle missing framework properties")
    void testMissingFrameworkProperties() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues("server.ssl.enabled=true")
                // No framework.connectors.rest.authMode property
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle SSL disabled with HTTPS config")
    void testSslDisabledWithHttpsConfig() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=false",
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    // HttpsConfig shouldn't load when SSL is disabled
                    assertThat(context).doesNotHaveBean(HttpsConfig.class);
                    assertThat(context).doesNotHaveBean("servletContainer");
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle empty or null property values")
    void testEmptyPropertyValues() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode="  // Empty value
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    // Empty authMode should not match "bypass"
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle case sensitivity in property values")
    void testCaseSensitiveProperties() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=TRUE", // Different case
                        "framework.connectors.rest.authMode=BYPASS" // Different case
                )
                .run(context -> {
                    // TRUE should be accepted as true
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    // BYPASS should not match "bypass" (case sensitive)
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle multiple auth mode values")
    void testMultipleAuthModeValues() {
        // Test various auth modes
        String[] authModes = {"bypass", "basic", "ldap", "oauth2", "mtls"};
        
        for (String authMode : authModes) {
            contextRunner
                    .withUserConfiguration(HttpsConfig.class)
                    .withPropertyValues(
                            "server.ssl.enabled=true",
                            "framework.connectors.rest.authMode=" + authMode
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(HttpsConfig.class);
                        assertThat(context).hasBean("servletContainer");
                        
                        // Only "bypass" should create SecurityFilterChain
                        if ("bypass".equals(authMode)) {
                            assertThat(context).hasBean("httpsSecurityFilterChain");
                        } else {
                            assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                        }
                    });
        }
    }

    @Test
    @DisplayName("Should handle malformed boolean values")
    void testMalformedBooleanValues() {
        String[] malformedValues = {"yes", "1", "on", "enabled", "TRUE", "False"};
        
        for (String value : malformedValues) {
            contextRunner
                    .withUserConfiguration(HttpsConfig.class)
                    .withPropertyValues("server.ssl.enabled=" + value)
                    .run(context -> {
                        // Spring Boot's property binding should handle most of these
                        // Only "true" (case insensitive) should enable SSL
                        if ("TRUE".equalsIgnoreCase(value)) {
                            assertThat(context).hasSingleBean(HttpsConfig.class);
                        } else {
                            // Most malformed values should result in false
                            assertThat(context).doesNotHaveBean(HttpsConfig.class);
                        }
                    });
        }
    }

    @Test
    @DisplayName("Should handle configuration with system properties")
    void testSystemPropertyOverrides() {
        // Set system property
        System.setProperty("server.ssl.enabled", "true");
        System.setProperty("framework.connectors.rest.authMode", "bypass");
        
        try {
            contextRunner
                    .withUserConfiguration(HttpsConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(HttpsConfig.class);
                        assertThat(context).hasBean("servletContainer");
                        assertThat(context).hasBean("httpsSecurityFilterChain");
                    });
        } finally {
            // Clean up system properties
            System.clearProperty("server.ssl.enabled");
            System.clearProperty("framework.connectors.rest.authMode");
        }
    }

    @Test
    @DisplayName("Should handle configuration precedence")
    void testConfigurationPrecedence() {
        // Application properties should override defaults
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=true",
                        "framework.connectors.rest.authMode=bypass"
                )
                .withSystemProperties(
                        "framework.connectors.rest.authMode=ldap" // System prop should override
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    // System property should take precedence, so no SecurityFilterChain
                    assertThat(context).doesNotHaveBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should handle profile-specific configurations")
    void testProfileSpecificConfiguration() {
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues(
                        "server.ssl.enabled=false", // Default profile
                        "spring.profiles.active=production"
                )
                .withPropertyValues(
                        "spring.config.activate.on-profile=production",
                        "server.ssl.enabled=true", // Production profile
                        "framework.connectors.rest.authMode=bypass"
                )
                .run(context -> {
                    // Production profile should enable SSL
                    assertThat(context).hasSingleBean(HttpsConfig.class);
                    assertThat(context).hasBean("servletContainer");
                    assertThat(context).hasBean("httpsSecurityFilterChain");
                });
    }

    @Test
    @DisplayName("Should maintain configuration isolation between tests")
    void testConfigurationIsolation() {
        // First context with SSL enabled
        contextRunner
                .withUserConfiguration(HttpsConfig.class)
                .withPropertyValues("server.ssl.enabled=true")
                .run(context1 -> {
                    assertThat(context1).hasSingleBean(HttpsConfig.class);
                    
                    // Second context with SSL disabled should be independent
                    contextRunner
                            .withUserConfiguration(HttpsConfig.class)
                            .withPropertyValues("server.ssl.enabled=false")
                            .run(context2 -> {
                                assertThat(context2).doesNotHaveBean(HttpsConfig.class);
                            });
                });
    }
}