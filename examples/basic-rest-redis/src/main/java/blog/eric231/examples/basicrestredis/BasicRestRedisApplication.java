package blog.eric231.examples.basicrestredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Basic REST Redis Application
 * 
 * This Spring Boot application demonstrates Redis CRUD operations using the @DL (Domain Logic) pattern
 * from the microservice framework. It provides REST endpoints that interact with Redis through
 * domain logic components annotated with @DL.
 * 
 * Features:
 * - Redis CRUD operations with @DL pattern
 * - Automatic JSON serialization/deserialization
 * - Category-based data organization
 * - TTL (Time-To-Live) support
 * - Version control for optimistic locking
 * - Soft delete (archive) functionality
 * - Index cleanup for data consistency
 * 
 * Usage:
 * The application exposes REST endpoints that route to @DL annotated domain logic classes:
 * - redis-create: Create new data in Redis
 * - redis-read: Read data from Redis (single, category, all)
 * - redis-update: Update existing data in Redis
 * - redis-delete: Delete data from Redis (single, category, archive)
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "blog.eric231.examples.basicrestredis",  // Application components
    "blog.eric231.framework"                 // Framework components
})
public class BasicRestRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicRestRedisApplication.class, args);
    }
}