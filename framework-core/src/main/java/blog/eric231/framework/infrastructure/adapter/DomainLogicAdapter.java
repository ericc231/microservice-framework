package blog.eric231.framework.infrastructure.adapter;

import blog.eric231.framework.application.usecase.DL;
import blog.eric231.framework.application.usecase.DomainLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Adapter class that wraps beans annotated with @DL into DomainLogic interface.
 * This allows the framework to treat any @DL annotated bean as a DomainLogic component
 * even if it doesn't explicitly implement the DomainLogic interface.
 */
public class DomainLogicAdapter implements DomainLogic {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainLogicAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final Object targetBean;
    private final DL dlAnnotation;
    private final String operationName;
    
    public DomainLogicAdapter(Object targetBean, DL dlAnnotation) {
        this.targetBean = targetBean;
        this.dlAnnotation = dlAnnotation;
        this.operationName = dlAnnotation.value();
    }
    
    @Override
    public JsonNode handle(JsonNode input) {
        try {
            // Try to find a suitable method to invoke
            Method handleMethod = findHandleMethod();
            
            if (handleMethod != null) {
                Object result = handleMethod.invoke(targetBean, input);
                return convertToJsonNode(result);
            } else {
                // Fallback: try to invoke a default method or return metadata
                logger.warn("No suitable handle method found for {}, returning metadata", operationName);
                return getMetadata();
            }
            
        } catch (Exception e) {
            logger.error("Error handling domain logic operation for {}: {}", operationName, e.getMessage(), e);
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", "Failed to execute domain logic");
            errorNode.put("message", e.getMessage());
            errorNode.put("operation", operationName);
            return errorNode;
        }
    }
    
    @Override
    public String getOperationName() {
        return operationName;
    }
    
    @Override
    public boolean validateInput(JsonNode input) {
        // Try to find a validation method in the target bean
        try {
            Method validateMethod = targetBean.getClass().getMethod("validateInput", JsonNode.class);
            Object result = validateMethod.invoke(targetBean, input);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (Exception e) {
            // Validation method not found or failed, use default validation
            logger.debug("No custom validation method found for {}, using default", operationName);
        }
        
        return DomainLogic.super.validateInput(input);
    }
    
    @Override
    public JsonNode getMetadata() {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("domainLogic", dlAnnotation.value());
        metadata.put("version", dlAnnotation.version());
        metadata.put("description", dlAnnotation.description());
        metadata.put("targetClass", targetBean.getClass().getSimpleName());
        metadata.put("adapter", true);
        
        // Try to get additional metadata from the target bean
        try {
            Method getMetadataMethod = targetBean.getClass().getMethod("getMetadata");
            Object result = getMetadataMethod.invoke(targetBean);
            if (result instanceof JsonNode) {
                JsonNode targetMetadata = (JsonNode) result;
                if (targetMetadata != null && targetMetadata.isObject()) {
                    ((ObjectNode) metadata).setAll((ObjectNode) targetMetadata);
                }
            }
        } catch (Exception e) {
            // Metadata method not found, use default
            logger.debug("No custom metadata method found for {}", operationName);
        }
        
        return metadata;
    }
    
    /**
     * Find a suitable method to handle the domain logic operation
     */
    private Method findHandleMethod() {
        Class<?> targetClass = targetBean.getClass();
        
        // Try to find method with JsonNode parameter
        try {
            return targetClass.getMethod("handle", JsonNode.class);
        } catch (NoSuchMethodException e) {
            // Method not found, try other variations
        }
        
        // Try to find method named after the operation
        try {
            return targetClass.getMethod(operationName, JsonNode.class);
        } catch (NoSuchMethodException e) {
            // Method not found
        }
        
        // Try to find any public method that accepts JsonNode
        for (Method method : targetClass.getMethods()) {
            if (method.getParameterCount() == 1 && 
                method.getParameterTypes()[0] == JsonNode.class &&
                !method.getName().equals("validateInput") &&
                !method.getName().equals("getMetadata")) {
                return method;
            }
        }
        
        return null;
    }
    
    /**
     * Convert method result to JsonNode
     */
    private JsonNode convertToJsonNode(Object result) {
        if (result == null) {
            return objectMapper.nullNode();
        }
        
        if (result instanceof JsonNode) {
            return (JsonNode) result;
        }
        
        try {
            return objectMapper.valueToTree(result);
        } catch (Exception e) {
            logger.warn("Failed to convert result to JsonNode for {}: {}", operationName, e.getMessage());
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("result", result.toString());
            errorNode.put("type", result.getClass().getSimpleName());
            return errorNode;
        }
    }
}