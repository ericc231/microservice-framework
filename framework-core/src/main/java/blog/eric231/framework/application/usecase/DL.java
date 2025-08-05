package blog.eric231.framework.application.usecase;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Domain Logic annotation for identifying domain service components
 * that can be dynamically invoked by the framework's REST adapter.
 * 
 * This annotation replaces the older @BP (Business Process) annotation
 * and follows Clean Architecture principles where domain logic is
 * independent of infrastructure concerns.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface DL {
    
    /**
     * The unique identifier for this domain logic component.
     * This value is used for routing REST requests to the appropriate
     * domain logic handler.
     * 
     * @return the domain logic identifier
     */
    String value();
    
    /**
     * Optional description of what this domain logic component does.
     * 
     * @return description of the domain logic
     */
    String description() default "";
    
    /**
     * Version of this domain logic component.
     * Useful for API versioning and compatibility.
     * 
     * @return version string
     */
    String version() default "1.0";
}