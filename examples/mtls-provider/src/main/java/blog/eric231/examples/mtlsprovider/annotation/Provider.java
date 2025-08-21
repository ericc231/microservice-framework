package blog.eric231.examples.mtlsprovider.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Annotation to mark classes as authentication providers.
 * Similar to @DL for domain logic and @BP for business processes,
 * @Provider identifies authentication provider components.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Provider {
    /**
     * The name/type of the provider
     * @return the provider name
     */
    String value();
    
    /**
     * Optional description of the provider
     * @return the provider description
     */
    String description() default "";
    
    /**
     * Authentication type supported by this provider
     * @return the authentication type
     */
    String authType() default "mtls";
}