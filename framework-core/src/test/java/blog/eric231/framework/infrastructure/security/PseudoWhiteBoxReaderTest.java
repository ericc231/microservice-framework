package blog.eric231.framework.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PseudoWhiteBoxReaderTest {

    @TempDir
    Path tempDir;

    private String tablePath;
    private String recipePath;
    private PseudoWhiteBoxReader reader;

    @BeforeEach
    void setUp() {
        tablePath = tempDir.resolve("secret.table").toString();
        recipePath = tempDir.resolve("secret.recipe").toString();
        reader = new PseudoWhiteBoxReader();
    }

    @Test
    void testReconstructKey() throws IOException {
        String originalSecret = "MyTestSecret123";
        PseudoWhiteBoxGenerator.generate(originalSecret, tablePath, recipePath);

        String reconstructedSecret = reader.reconstructKey(tablePath, recipePath);
        assertEquals(originalSecret, reconstructedSecret);
    }

    @Test
    void testReconstructKeyWithDifferentSecrets() throws IOException {
        String secret1 = "SecretA";
        PseudoWhiteBoxGenerator.generate(secret1, tablePath, recipePath);
        assertEquals(secret1, reader.reconstructKey(tablePath, recipePath));

        String secret2 = "AnotherSecretB";
        PseudoWhiteBoxGenerator.generate(secret2, tablePath, recipePath);
        assertEquals(secret2, reader.reconstructKey(tablePath, recipePath));
    }

    @Test
    void testReconstructKeyWithEmptySecret() throws IOException {
        String emptySecret = "";
        PseudoWhiteBoxGenerator.generate(emptySecret, tablePath, recipePath);
        assertEquals(emptySecret, reader.reconstructKey(tablePath, recipePath));
    }

    @Test
    void testReconstructKeyWithSpecialCharacters() throws IOException {
        String specialCharSecret = "!@#$%^&*()_+";
        PseudoWhiteBoxGenerator.generate(specialCharSecret, tablePath, recipePath);
        assertEquals(specialCharSecret, reader.reconstructKey(tablePath, recipePath));
    }

    @Test
    void testReconstructKeyThrowsIOExceptionIfFilesMissing() {
        assertThrows(IOException.class, () -> reader.reconstructKey("nonexistent.table", "nonexistent.recipe"));
    }

    @Test
    void testReconstructKeyThrowsNumberFormatExceptionIfRecipeCorrupted() throws IOException {
        String originalSecret = "test";
        PseudoWhiteBoxGenerator.generate(originalSecret, tablePath, recipePath);

        // Corrupt the recipe file
        String corruptedRecipeContent = "salt=abc\niterations=xyz\nindices=1,2,3";
        Files.writeString(Path.of(recipePath), corruptedRecipeContent);

        assertThrows(NumberFormatException.class, () -> reader.reconstructKey(tablePath, recipePath));
    }
}
