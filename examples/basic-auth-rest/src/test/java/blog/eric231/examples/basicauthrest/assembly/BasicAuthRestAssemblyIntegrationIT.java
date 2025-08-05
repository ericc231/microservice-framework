package blog.eric231.examples.basicauthrest.assembly;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assembly Integration Tests for Basic Auth REST Service
 * Tests the complete deployment package including assembly extraction and service startup
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BasicAuthRestAssemblyIntegrationIT {
    
    private static final String SERVICE_NAME = "basic-auth-rest";
    private static final int SERVICE_PORT = 8081;
    private static final String TARGET_DIR = "target";
    private static final String ASSEMBLY_DIR = TARGET_DIR + "/assembly-test";
    
    private static Process serviceProcess;
    private static Path assemblyPath;
    
    @BeforeAll
    static void setupAssemblyTest() throws Exception {
        System.out.println("=== Basic Auth REST Assembly Integration Test ===");
        
        // Find the assembly ZIP file
        Path targetPath = Paths.get(TARGET_DIR);
        assertTrue(Files.exists(targetPath), "Target directory should exist");
        
        Path zipFile = Files.list(targetPath)
            .filter(path -> path.toString().endsWith(".zip"))
            .filter(path -> path.toString().contains(SERVICE_NAME))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Assembly ZIP file not found. Run 'mvn package' first."));
        
        System.out.println("Found assembly: " + zipFile);
        
        // Create assembly test directory
        Path assemblyTestDir = Paths.get(ASSEMBLY_DIR);
        if (Files.exists(assemblyTestDir)) {
            deleteDirectory(assemblyTestDir.toFile());
        }
        Files.createDirectories(assemblyTestDir);
        
        // Extract ZIP file
        extractZip(zipFile.toString(), ASSEMBLY_DIR);
        
        // Find extracted directory
        assemblyPath = Files.list(assemblyTestDir)
            .filter(Files::isDirectory)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Extracted assembly directory not found"));
        
        System.out.println("Extracted to: " + assemblyPath);
        
        // Verify assembly structure
        verifyAssemblyStructure();
    }
    
    @AfterAll
    static void cleanupAssemblyTest() throws Exception {
        // Stop service if running
        if (serviceProcess != null && serviceProcess.isAlive()) {
            stopService();
        }
        
        // Clean up assembly directory
        Path assemblyTestDir = Paths.get(ASSEMBLY_DIR);
        if (Files.exists(assemblyTestDir)) {
            deleteDirectory(assemblyTestDir.toFile());
        }
        
        System.out.println("Assembly test cleanup completed");
    }
    
    @Test
    @Order(1)
    void verifyAssemblyContents() throws Exception {
        System.out.println("=== Verifying Assembly Contents ===");
        
        // Check main JAR
        Path mainJar = assemblyPath.resolve(SERVICE_NAME + ".jar");
        assertTrue(Files.exists(mainJar), "Main JAR should exist: " + mainJar);
        assertTrue(Files.size(mainJar) > 1024, "Main JAR should not be empty");
        
        // Check lib directory
        Path libDir = assemblyPath.resolve("lib");
        assertTrue(Files.exists(libDir), "Lib directory should exist");
        assertTrue(Files.isDirectory(libDir), "Lib should be a directory");
        
        long libCount = Files.list(libDir).count();
        assertTrue(libCount > 10, "Should have multiple dependency JARs, found: " + libCount);
        
        // Check bin directory
        Path binDir = assemblyPath.resolve("bin");
        assertTrue(Files.exists(binDir), "Bin directory should exist");
        
        Path startScript = binDir.resolve("start.sh");
        Path stopScript = binDir.resolve("stop.sh");
        assertTrue(Files.exists(startScript), "Start script should exist");
        assertTrue(Files.exists(stopScript), "Stop script should exist");
        
        // Check config directory
        Path configDir = assemblyPath.resolve("config");
        assertTrue(Files.exists(configDir), "Config directory should exist");
        
        Path applicationYml = configDir.resolve("application.yml");
        assertTrue(Files.exists(applicationYml), "Application configuration should exist");
        
        System.out.println("✓ Assembly contents verified successfully");
    }
    
    @Test
    @Order(2)
    void startServiceFromAssembly() throws Exception {
        System.out.println("=== Starting Service from Assembly ===");
        
        Path startScript = assemblyPath.resolve("bin/start.sh");
        
        // Make script executable (Linux/Mac)
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            ProcessBuilder chmodBuilder = new ProcessBuilder("chmod", "+x", startScript.toString());
            Process chmodProcess = chmodBuilder.start();
            chmodProcess.waitFor();
        }
        
        // Start the service
        ProcessBuilder serviceBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            serviceBuilder.command("cmd", "/c", "bash", startScript.toString());
        } else {
            serviceBuilder.command("bash", startScript.toString());
        }
        
        serviceBuilder.directory(assemblyPath.toFile());
        serviceBuilder.redirectErrorStream(true);
        
        System.out.println("Starting service with command: " + serviceBuilder.command());
        serviceProcess = serviceBuilder.start();
        
        // Wait for service to start
        System.out.println("Waiting for service to start...");
        boolean started = waitForServiceStart(60); // Wait up to 60 seconds
        
        assertTrue(started, "Service should start within 60 seconds");
        assertTrue(serviceProcess.isAlive(), "Service process should be running");
        
        System.out.println("✓ Service started successfully");
    }
    
    @Test
    @Order(3)
    void testServiceEndpoints() throws Exception {
        System.out.println("=== Testing Service Endpoints ===");
        
        TestRestTemplate restTemplate = new TestRestTemplate();
        String baseUrl = "http://localhost:" + SERVICE_PORT;
        
        // Test health endpoint
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", String.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
        System.out.println("✓ Health endpoint working");
        
        // Test unauthenticated API access
        ResponseEntity<String> unauthResponse = restTemplate.getForEntity(
            baseUrl + "/api/user/me", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, unauthResponse.getStatusCode());
        System.out.println("✓ Authentication required for protected endpoints");
        
        // Test authenticated API access
        ResponseEntity<String> authResponse = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(baseUrl + "/api/user/me", String.class);
        assertEquals(HttpStatus.OK, authResponse.getStatusCode());
        assertTrue(authResponse.getBody().contains("admin"));
        System.out.println("✓ Basic authentication working");
        
        // Test admin endpoint
        ResponseEntity<String> adminResponse = restTemplate
            .withBasicAuth("admin", "admin")
            .getForEntity(baseUrl + "/api/user/admin", String.class);
        assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        assertTrue(adminResponse.getBody().contains("Admin access granted"));
        System.out.println("✓ Admin endpoint working");
        
        // Test role-based access control
        ResponseEntity<String> userAdminResponse = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity(baseUrl + "/api/user/admin", String.class);
        assertEquals(HttpStatus.FORBIDDEN, userAdminResponse.getStatusCode());
        System.out.println("✓ Role-based access control working");
        
        System.out.println("✓ All service endpoints tested successfully");
    }
    
    @Test
    @Order(4)
    void stopServiceFromAssembly() throws Exception {
        System.out.println("=== Stopping Service from Assembly ===");
        
        Path stopScript = assemblyPath.resolve("bin/stop.sh");
        
        // Make script executable (Linux/Mac)
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            ProcessBuilder chmodBuilder = new ProcessBuilder("chmod", "+x", stopScript.toString());
            Process chmodProcess = chmodBuilder.start();
            chmodProcess.waitFor();
        }
        
        // Stop the service
        ProcessBuilder stopBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            stopBuilder.command("cmd", "/c", "bash", stopScript.toString());
        } else {
            stopBuilder.command("bash", stopScript.toString());
        }
        
        stopBuilder.directory(assemblyPath.toFile());
        Process stopProcess = stopBuilder.start();
        
        boolean stopped = stopProcess.waitFor(30, TimeUnit.SECONDS);
        assertTrue(stopped, "Stop script should complete within 30 seconds");
        
        // Wait for service to actually stop
        Thread.sleep(5000);
        
        // Verify service is stopped
        assertFalse(isServiceRunning(), "Service should be stopped");
        
        System.out.println("✓ Service stopped successfully");
    }
    
    // Helper methods
    
    private static void verifyAssemblyStructure() throws Exception {
        assertTrue(Files.exists(assemblyPath), "Assembly directory should exist");
        assertTrue(Files.isDirectory(assemblyPath), "Assembly path should be a directory");
        
        // Check for required directories
        String[] requiredDirs = {"bin", "lib", "config"};
        for (String dir : requiredDirs) {
            Path dirPath = assemblyPath.resolve(dir);
            assertTrue(Files.exists(dirPath), "Required directory should exist: " + dir);
        }
        
        // Check for main JAR
        Path mainJar = assemblyPath.resolve(SERVICE_NAME + ".jar");
        assertTrue(Files.exists(mainJar), "Main JAR should exist");
    }
    
    private static void extractZip(String zipFilePath, String destDir) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());
                
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Create parent directories
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    
                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
    
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    private static boolean waitForServiceStart(int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds; i++) {
            try {
                if (isServiceRunning()) {
                    return true;
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                // Continue waiting
            }
        }
        return false;
    }
    
    private static boolean isServiceRunning() {
        try {
            URL url = new URL("http://localhost:" + SERVICE_PORT + "/actuator/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void stopService() {
        if (serviceProcess != null) {
            serviceProcess.destroy();
            try {
                serviceProcess.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                serviceProcess.destroyForcibly();
            }
        }
    }
}