package blog.eric231.framework.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base interface for Domain Logic components in the framework.
 * 
 * Domain Logic represents the core business rules and operations
 * that are independent of infrastructure concerns. This interface
 * provides a standard way for the framework to invoke domain operations
 * through the REST adapter.
 * 
 * Classes implementing this interface should be annotated with @DL
 * to be automatically discovered and registered by the framework.
 */
public interface DomainLogic {
    
    /**
     * Handle a domain operation with the provided input data.
     * 
     * This method is called by the framework's REST adapter when
     * a matching route is found. The implementation should contain
     * the core business logic for the operation.
     * 
     * @param input the input data for the operation (can be null)
     * @return the result of the domain operation
     */
    JsonNode handle(JsonNode input);
    
    /**
     * Get the operation name/type that this domain logic handles.
     * This is used for additional routing and logging purposes.
     * 
     * @return the operation name
     */
    default String getOperationName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Validate the input data before processing.
     * Override this method to provide custom input validation.
     * 
     * @param input the input data to validate
     * @return true if input is valid, false otherwise
     */
    default boolean validateInput(JsonNode input) {
        return true;
    }
    
    /**
     * Get metadata about this domain logic component.
     * This can include version information, capabilities, etc.
     * 
     * @return metadata as JsonNode
     */
    default JsonNode getMetadata() {
        return null;
    }
}