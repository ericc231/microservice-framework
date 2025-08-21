package blog.eric231.framework.infrastructure.security;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PseudoWhiteBoxGenerator {

    private static final int ITERATION_BASE = 1000;
    private static final int SALT_LENGTH = 16;

    /**
     * Generates the secret.table and secret.recipe files.
     *
     * @param secret The secret string to be protected.
     * @param tablePath The path to save the secret.table file.
     * @param recipePath The path to save the secret.recipe file.
     * @throws IOException If an I/O error occurs.
     */
    public static void generate(String secret, String tablePath, String recipePath) throws Exception {
        char[] secretChars = secret.toCharArray();
        int secretLength = secretChars.length;

        // 1. Create a comprehensive character table (all printable ASCII characters)
        StringBuilder tableContent = new StringBuilder();
        // Add printable ASCII characters (32-126)
        for (int i = 32; i <= 126; i++) {
            tableContent.append((char) i);
        }
        // Add some additional dummy characters to obfuscate
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 32; i++) {
            tableContent.append((char) (random.nextInt(26) + 'a'));
        }
        
        // Shuffle the character table
        List<Character> charList = new ArrayList<>();
        for (char c : tableContent.toString().toCharArray()) {
            charList.add(c);
        }
        Collections.shuffle(charList, random);
        
        // Rebuild table content from shuffled list
        tableContent = new StringBuilder();
        for (Character c : charList) {
            tableContent.append(c);
        }

        // 2. Generate the recipe (secret.recipe)
        Properties recipe = new Properties();
        int[] indices = new int[secretLength];
        String tableStr = tableContent.toString();
        for (int i = 0; i < secretLength; i++) {
            indices[i] = tableStr.indexOf(secretChars[i]);
            if (indices[i] == -1) {
                throw new IllegalArgumentException("Character '" + secretChars[i] + "' not found in character table");
            }
        }

        // 3. Generate salt and iterations for obfuscation
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        int iterations = ITERATION_BASE + random.nextInt(1000);

        // 4. Obfuscate indices
        int[] obfuscatedIndices = new int[secretLength];
        for (int i = 0; i < secretLength; i++) {
            obfuscatedIndices[i] = indices[i] ^ (iterations + i) ^ (salt[i % SALT_LENGTH] & 0xFF);
        }

        // 5. Encrypt the indices with AES
        String indicesHex = intArrayToString(obfuscatedIndices);
        byte[] tableBytes = tableContent.toString().getBytes();
        
        // AES Key is SHA-256 of the table
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] aesKeyBytes = sha256.digest(tableBytes);
        SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");
        
        // IV is MD5 of the salt
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] ivBytes = md5.digest(bytesToHex(salt).getBytes(StandardCharsets.UTF_8));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
        
        // Encrypt the indices
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedIndices = cipher.doFinal(indicesHex.getBytes(StandardCharsets.UTF_8));
        String encryptedIndicesHex = bytesToHex(encryptedIndices);

        // 6. Save the recipe
        recipe.setProperty("salt", bytesToHex(salt));
        recipe.setProperty("iterations", String.valueOf(iterations));
        recipe.setProperty("password", encryptedIndicesHex);

        // Write files
        try (FileOutputStream tableOut = new FileOutputStream(tablePath)) {
            tableOut.write(tableContent.toString().getBytes());
        }

        try (FileOutputStream recipeOut = new FileOutputStream(recipePath)) {
            recipe.store(recipeOut, "Secret Recipe");
        }
    }

    private static String intArrayToString(int[] array) {
        StringBuilder sb = new StringBuilder();
        for (int value : array) {
            sb.append(String.format("%04x", value));
        }
        return sb.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        // --- Usage Example ---
        // Replace with your actual secret password
        String mySecretPassword = "ThisIsAStrongPasswordForJasypt!";

        // Define where to save the generated files
        String tableFilePath = "secret.table";
        String recipeFilePath = "secret.recipe";

        generate(mySecretPassword, tableFilePath, recipeFilePath);

        System.out.println("Successfully generated '" + tableFilePath + "' and '" + recipeFilePath + "'.");
        System.out.println("Please move these files to a secure location accessible by your application.");
    }
}
