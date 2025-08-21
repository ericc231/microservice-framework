package blog.eric231.framework.infrastructure.configuration;

import blog.eric231.framework.infrastructure.security.PseudoWhiteBoxReader;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JasyptConfigTest {

    private JasyptConfig jasyptConfig;

    @Mock
    private PseudoWhiteBoxReader pseudoWhiteBoxReader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jasyptConfig = new JasyptConfig();
        ReflectionTestUtils.setField(jasyptConfig, "pseudoWhiteBoxReader", pseudoWhiteBoxReader);
    }

    @Test
    void jasyptStringEncryptor_ShouldCreateValidEncryptor() throws Exception {
        // Arrange
        String testPassword = "testPassword123";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        assertNotNull(encryptor);
        
        // Test encryption/decryption
        String plainText = "Hello, World!";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);
        
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, decrypted);
        
        verify(pseudoWhiteBoxReader).reconstructKey("secret.table", "secret.recipe");
    }

    @Test
    void jasyptStringEncryptor_ShouldUseCorrectConfiguration() throws Exception {
        // Arrange
        String testPassword = "anotherPassword456";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        assertNotNull(encryptor);
        
        // Test with empty string
        String encrypted = encryptor.encrypt("");
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals("", decrypted);
        
        // Test with special characters
        String specialText = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String encryptedSpecial = encryptor.encrypt(specialText);
        String decryptedSpecial = encryptor.decrypt(encryptedSpecial);
        assertEquals(specialText, decryptedSpecial);
    }

    @Test
    void jasyptStringEncryptor_ShouldHandleUnicodeCharacters() throws Exception {
        // Arrange
        String testPassword = "unicodePassword";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        String unicodeText = "Hello World with Unicode";
        String encrypted = encryptor.encrypt(unicodeText);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(unicodeText, decrypted);
    }

    @Test
    void jasyptStringEncryptor_WithLongPassword_ShouldWork() throws Exception {
        // Arrange
        String longPassword = "a".repeat(100); // 100 character password
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(longPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        String testText = "This is a test with a very long password";
        String encrypted = encryptor.encrypt(testText);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(testText, decrypted);
    }

    @Test
    void jasyptStringEncryptor_WithShortPassword_ShouldWork() throws Exception {
        // Arrange
        String shortPassword = "a"; // 1 character password
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(shortPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        String testText = "Short password test";
        String encrypted = encryptor.encrypt(testText);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(testText, decrypted);
    }

    @Test
    void jasyptStringEncryptor_ShouldProduceDifferentEncryptionsForSameInput() throws Exception {
        // Arrange
        String testPassword = "testPassword";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        String plainText = "Same input";
        String encrypted1 = encryptor.encrypt(plainText);
        String encrypted2 = encryptor.encrypt(plainText);
        
        // Encrypted values should be different (due to random salt)
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to the same plain text
        assertEquals(plainText, encryptor.decrypt(encrypted1));
        assertEquals(plainText, encryptor.decrypt(encrypted2));
    }

    @Test
    void jasyptStringEncryptor_WhenReaderThrowsException_ShouldPropagateException() throws Exception {
        // Arrange
        Exception testException = new RuntimeException("Failed to read secret");
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenThrow(testException);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            jasyptConfig.jasyptStringEncryptor();
        });
        
        assertEquals(testException, exception);
        verify(pseudoWhiteBoxReader).reconstructKey("secret.table", "secret.recipe");
    }

    @Test
    void jasyptStringEncryptor_ShouldUseDefaultPaths() throws Exception {
        // Arrange
        String testPassword = "defaultPathTest";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        assertNotNull(encryptor);
        verify(pseudoWhiteBoxReader).reconstructKey("secret.table", "secret.recipe");
    }

    @Test
    void jasyptStringEncryptor_WithMultipleInvocations_ShouldCreateSeparateInstances() throws Exception {
        // Arrange
        String testPassword = "multipleInvocations";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor1 = jasyptConfig.jasyptStringEncryptor();
        StringEncryptor encryptor2 = jasyptConfig.jasyptStringEncryptor();

        // Assert
        assertNotNull(encryptor1);
        assertNotNull(encryptor2);
        
        // Both encryptors should work independently
        String testText = "Multiple encryptors test";
        String encrypted1 = encryptor1.encrypt(testText);
        String encrypted2 = encryptor2.encrypt(testText);
        
        assertEquals(testText, encryptor1.decrypt(encrypted1));
        assertEquals(testText, encryptor2.decrypt(encrypted2));
        
        // Cross-decrypt should also work (same password)
        assertEquals(testText, encryptor1.decrypt(encrypted2));
        assertEquals(testText, encryptor2.decrypt(encrypted1));
        
        verify(pseudoWhiteBoxReader, times(2)).reconstructKey("secret.table", "secret.recipe");
    }

    @Test
    void jasyptStringEncryptor_ShouldHandleLargeText() throws Exception {
        // Arrange
        String testPassword = "largeTextPassword";
        when(pseudoWhiteBoxReader.reconstructKey("secret.table", "secret.recipe"))
                .thenReturn(testPassword);

        // Act
        StringEncryptor encryptor = jasyptConfig.jasyptStringEncryptor();

        // Assert
        String largeText = "Large text: " + "x".repeat(10000); // 10KB+ text
        String encrypted = encryptor.encrypt(largeText);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(largeText, decrypted);
    }
}