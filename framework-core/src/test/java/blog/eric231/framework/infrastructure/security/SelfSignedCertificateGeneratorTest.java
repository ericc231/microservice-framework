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
        File keystoreFile = tempDir.resolve("keystore.p12").toFile();
        SelfSignedCertificateGenerator.generate(keystoreFile.getAbsolutePath());

        assertTrue(keystoreFile.exists());
        assertTrue(keystoreFile.length() > 0);
    }
}
