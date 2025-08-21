package blog.eric231.examples.basicrestredis.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Redis data model for demonstration purposes.
 * Represents a generic data object that can be stored in Redis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisData {
    
    /**
     * Unique identifier for the data
     */
    private String id;
    
    /**
     * Name or title of the data
     */
    private String name;
    
    /**
     * Description of the data
     */
    private String description;
    
    /**
     * Value associated with the data
     */
    private String value;
    
    /**
     * Category or type of data
     */
    private String category = "default";
    
    /**
     * Additional metadata as key-value pairs
     */
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Creation timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Last modification timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * Status of the data (active, inactive, archived)
     */
    private String status = "active";
    
    /**
     * Version number for optimistic locking
     */
    private Long version = 1L;
    
    /**
     * Constructor with basic fields
     */
    public RedisData(String id, String name, String description, String value) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.value = value;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * Update the modification timestamp
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }
    
    /**
     * Check if the data is active
     */
    public boolean isActive() {
        return "active".equalsIgnoreCase(this.status);
    }
    
    /**
     * Mark as archived
     */
    public void archive() {
        this.status = "archived";
        touch();
    }
}