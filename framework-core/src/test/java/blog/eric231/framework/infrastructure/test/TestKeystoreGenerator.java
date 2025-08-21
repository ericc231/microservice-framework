package blog.eric231.framework.infrastructure.test;

import blog.eric231.framework.infrastructure.security.SelfSignedCertificateGenerator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class to generate test keystores for HTTPS testing.
 * This creates the test-keystore.p12 file needed by BaseHttpsTest.
 */
public class TestKeystoreGenerator {

    @Test
    void generateTestKeystore() throws Exception {
        // Generate test keystore in test resources
        String resourcePath = "src/test/resources/test-keystore.p12";
        Path keystorePath = Paths.get(resourcePath);
        
        // Create directories if they don't exist
        Files.createDirectories(keystorePath.getParent());
        
        // Generate the keystore
        SelfSignedCertificateGenerator.generate(resourcePath);
        
        System.out.println("Test keystore generated at: " + keystorePath.toAbsolutePath());
        
        // Also copy to other test resource directories
        copyToTestResources("examples/helloworld-service/src/test/resources/");
        copyToTestResources("examples/basic-rest-redis/src/test/resources/");
        copyToTestResources("examples/ldap-provider/src/test/resources/");
        copyToTestResources("examples/basic-auth-rest/src/test/resources/");
        copyToTestResources("examples/ldap-auth-rest/src/test/resources/");
        copyToTestResources("examples/oidc-auth-rest/src/test/resources/");
    }

    private void copyToTestResources(String targetDir) {
        try {
            Path targetPath = Paths.get(targetDir);
            if (Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
                Files.copy(Paths.get("src/test/resources/test-keystore.p12"), 
                          targetPath.resolve("test-keystore.p12"), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied test keystore to: " + targetPath);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not copy test keystore to " + targetDir + ": " + e.getMessage());
        }
    }

    /**
     * Standalone method to generate keystores
     */
    public static void main(String[] args) throws Exception {
        TestKeystoreGenerator generator = new TestKeystoreGenerator();
        generator.generateTestKeystore();
    }
}