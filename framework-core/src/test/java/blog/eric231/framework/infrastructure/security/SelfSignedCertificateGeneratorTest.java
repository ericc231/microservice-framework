package blog.eric231.framework.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfSignedCertificateGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void testGenerateCertificate() throws Exception {
        // Set system property to ensure keystore is created in tempDir
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        SelfSignedCertificateGenerator.generate();

        File keystoreFile = tempDir.resolve("keystore.p12").toFile();
        assertTrue(keystoreFile.exists());
        assertTrue(keystoreFile.length() > 0);

        // Clean up system property
        System.clearProperty("user.dir");
    }
}
