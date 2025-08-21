package blog.eric231.framework.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PseudoWhiteBoxGeneratorTest {

    @TempDir
    Path tempDir;

    private String tablePath;
    private String recipePath;

    @BeforeEach
    void setUp() {
        tablePath = tempDir.resolve("secret.table").toString();
        recipePath = tempDir.resolve("secret.recipe").toString();
    }

    @Test
    void testGenerateFiles() throws Exception {
        String secret = "testSecret";
        PseudoWhiteBoxGenerator.generate(secret, tablePath, recipePath);

        assertTrue(Files.exists(Path.of(tablePath)));
        assertTrue(Files.exists(Path.of(recipePath)));

        String tableContent = Files.readString(Path.of(tablePath));
        assertFalse(tableContent.isEmpty());

        Properties recipe = new Properties();
        recipe.load(Files.newInputStream(Path.of(recipePath)));

        assertNotNull(recipe.getProperty("salt"));
        assertNotNull(recipe.getProperty("iterations"));
        assertNotNull(recipe.getProperty("password"));
    }

    @Test
    void testGeneratedFilesCanBeReconstructed() throws Exception {
        String originalSecret = "MySuperSecretPassword123!";
        PseudoWhiteBoxGenerator.generate(originalSecret, tablePath, recipePath);

        PseudoWhiteBoxReader reader = new PseudoWhiteBoxReader();
        String reconstructedSecret = reader.reconstructKey(tablePath, recipePath);

        assertEquals(originalSecret, reconstructedSecret);
    }

    @Test
    void testGenerateWithEmptySecret() throws Exception {
        String secret = "";
        PseudoWhiteBoxGenerator.generate(secret, tablePath, recipePath);

        assertTrue(Files.exists(Path.of(tablePath)));
        assertTrue(Files.exists(Path.of(recipePath)));

        String tableContent = Files.readString(Path.of(tablePath));
        // Table content should still have dummy chars even for empty secret
        assertFalse(tableContent.isEmpty());

        PseudoWhiteBoxReader reader = new PseudoWhiteBoxReader();
        String reconstructedSecret = reader.reconstructKey(tablePath, recipePath);
        assertEquals(secret, reconstructedSecret);
    }

    @Test
    void testGenerateWithSpecialCharacters() throws Exception {
        String secret = "!@#$%^&*()_+{}[]|\\;:'\",.<>/?`~ ";
        PseudoWhiteBoxGenerator.generate(secret, tablePath, recipePath);

        PseudoWhiteBoxReader reader = new PseudoWhiteBoxReader();
        String reconstructedSecret = reader.reconstructKey(tablePath, recipePath);
        assertEquals(secret, reconstructedSecret);
    }
}
